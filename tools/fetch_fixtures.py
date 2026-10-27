"""Capture public V1 responses only. Existing fixtures survive provider failures."""
from pathlib import Path
import urllib.request, json, concurrent.futures

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'app/src/main/assets/fixtures'
DEST.mkdir(parents=True, exist_ok=True)
BASE = 'https://www.themealdb.com/api/json/v1/1/'

def get(endpoint):
    with urllib.request.urlopen(BASE + endpoint, timeout=25) as r:
        return json.load(r)

def main():
    legacy = get('filter.php?a=Indian')
    current = get('filter.php?a=India')
    # During the provider's area-label migration both endpoints can respond with
    # different subsets. Prefer the larger authoritative area response.
    base = max((legacy, current), key=lambda value: len(value.get('meals') or []))
    (DEST / 'live-indian-response.json').write_text(json.dumps(base, indent=2))
    ids = [m['idMeal'] for m in base.get('meals') or []]
    # Known public Indian records, used only when the current collection is unavailable.
    ids = ids or ['52795', '52805', '52807', '52835', '52839', '52843', '52844', '52905', '52906', '52907', '52908', '52909']
    def fetch(mid):
        try:
            data = get('lookup.php?i=' + mid)
            meals = [m for m in data.get('meals') or [] if m.get('strArea') in ('Indian', 'India') or m.get('strCountry') == 'India']
            print(mid, [m['strMeal'] for m in meals], flush=True)
            return meals
        except Exception as e:
            print(mid, str(e), flush=True)
            return []
    with concurrent.futures.ThreadPoolExecutor(3) as pool:
        meals = [m for group in pool.map(fetch, ids) for m in group]
    if meals:
        (DEST / 'indian-meals.json').write_text(json.dumps({'meals': meals}, indent=2), encoding='utf-8')
        print('Captured', len(meals), 'public records')
    else:
        print('Provider returned no Indian lookup records. Existing fixtures preserved.')

if __name__ == '__main__': main()
