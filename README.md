# TasteIndia

TasteIndia is a native Android recipe browser for the Android Intern Assignment. It loads the authoritative India collection from TheMealDB, enriches it with complete recipe details, filters by stable IDs, and keeps favourite snapshots available offline.

| Recipes | Complete details |
| --- | --- |
| ![TasteIndia recipe list](docs/screenshots/recipes.png) | ![TasteIndia recipe details](docs/screenshots/details.png) |

## Run it

Requirements: Android Studio Narwhal 3 Feature Drop (2025.2.2) or newer, JDK 17, and Android SDK 35. No API key or account is required; TheMealDB permits test key `1` for educational development.

1. Open this directory in Android Studio and let Gradle sync.
2. Run the `app` configuration on an API 26+ emulator/device.
3. Or use `./gradlew test assembleDebug` (`gradlew.bat test assembleDebug` on Windows).

On Windows, `python tools/bootstrap.py` can install the pinned Gradle 8.9 and Android SDK 35 packages under this repository on `D:` and create `local.properties`. The optional `python tools/bootstrap_emulator.py` installs a headless Android 15 review emulator under the same `.tools` directory.

The wrappers default `GRADLE_USER_HOME` to `.gradle-user-home` inside this project. When the repository is on `D:`, dependencies and Gradle distributions stay on `D:` too. Set `GRADLE_USER_HOME` yourself if you prefer another disk. The local bootstrap also installs its Android SDK under `.tools`; both directories are ignored by Git.

For Android Studio on a space-limited system drive, set **Settings → Build Tools → Gradle → Gradle user home** to `D:\Zoomcars\.gradle-user-home`. Keep the SDK path from `local.properties` (`D:\Zoomcars\.tools\android-sdk`).

The first recipe load performs a real V1 GET. If the provider is down, the app explains the fallback and loads checked-in public response fixtures. Tap **Retry** to attempt the live API again.

Toolchain: Kotlin 2.0.21, AGP 8.7.3, Gradle 8.9, Compose BOM 2024.12.01, Java 17, minSdk 26, target/compile SDK 35.

## Architecture and state

`data` contains Retrofit DTOs, URL validation, response mapping, repository orchestration, and an atomic JSON file store. `domain` owns immutable recipe/filter models and the pure intersection function. `ui` has separate list and detail ViewModels exposing immutable `StateFlow`, plus Compose screens.

Routes are `recipes`, `favourites`, and `details/{mealId}`. Only the stable meal ID crosses the navigation boundary. Each list destination owns its `SavedStateHandle`; Navigation Compose saves its LazyColumn state, so query, filters, sort, and scroll return exactly after details. Recipes and Favourites open the same detail route.

Dependencies are provided through the small `AppContainer`. Both `MealsRepository` and `LocalStore` are interfaces so tests can replace network and persistence without Android globals.

## API and filtering

Endpoints:

- `filter.php?a=Indian`, with a documented compatibility retry using `a=India` because the October 2026 V1 area catalogue and records currently use `India`
- `lookup.php?i={mealId}` for complete details

The larger valid area response establishes the authoritative base ID set while the provider migrates its area label. The two area-label checks run concurrently, and at most four detail lookups run at once; lookups are deduplicated and cached by ID. Category and normalized first ingredient choices come from those details. Search, category, ingredient, and favourites are intersected locally, then sorted. No result outside the base ID set can enter the list. Rapid input is debounced and transformed from the latest immutable filter state.

`strIngredient1…20` and `strMeasure1…20` become ordered `Ingredient` values; blank ingredients disappear. Known recipe fields get deliberate UI, valid HTTP(S) source/video links open through Android, and additional meaningful non-empty V1 fields appear in **More details**. IDs, blank fields, and raw JSON keys are intentionally omitted. Image URLs are data rather than links, so they appear only as images with failure placeholders.

## Cache, offline use, and reliability

Successful collections are written to `cache.json`. Favourite recipe snapshots live separately in `favourites.json`, so removing the cache cannot remove saved recipes. Writes use temporary and backup files before replacement. Network calls have explicit connect/read/call timeouts, HTTP errors become readable messages, details requests deduplicate, and cancelled coroutines do not mutate Compose state.

Checked-in fixtures and small fallback thumbnails are public TheMealDB V1 responses captured by `tools/fetch_fixtures.py`. They keep reviewer and unit-test flows deterministic. The first fallback is cached so later offline launches render immediately, while its review-fixture origin remains visibly labelled; it is never presented as a successful live refresh.

## Tests

`gradlew test` runs offline after dependencies are present. Focused tests protect:

- dynamic response serialization and the 20 ingredient/measure pairs;
- strict ID intersection within the India base set;
- latest-query behavior under rapid input;
- SavedStateHandle filter/sort restoration;
- sorting identity, URL validation, and blank-field omission.

## Design and accessibility

The interface uses an ivory/forest/saffron editorial palette with an adaptive dark theme. Controls keep Material touch targets, text scales, selection has labels/checkmarks beyond color, result count uses a polite live region, images avoid redundantly announcing their visible titles, and favourite/source/video controls have descriptive screen-reader labels. Loading, empty, error, retry, offline fixture, partial-field, image-failure, and long-instruction states have deliberate layouts.

## Attribution and responsible use

Recipe text, metadata, fixtures, remote images, and checked-in fallback thumbnails are from [TheMealDB](https://www.themealdb.com/api.php). The provider permits key `1` for development and educational use but requires a paid subscription for app-store publication; this assessment build is not intended for public release. Third-party libraries are AndroidX/Jetpack Compose (Apache 2.0), Retrofit/OkHttp (Apache 2.0), Kotlin Serialization/Coroutines (Apache 2.0), and Coil (Apache 2.0).

## Assumptions and tradeoffs

The current public service returns three India records and calls the area `India`; older assignment text says `Indian`. Both labels are accepted only at the boundary, while recipe details still must declare `India` or `Indian`. JSON files are proportional for this small exercise; Room would be the next step for large, queryable offline collections. The details cache has no time eviction because the provider dataset is small and an explicit retry refreshes it.

Time record: the initial Codex-assisted implementation was completed on 27 October 2026. Before submission, the candidate must add their actual review, testing, and modification time here. Checked-in fixtures reflect the 15 public India records available on that date and should be recaptured when the provider changes the collection.

