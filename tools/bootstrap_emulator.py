"""Optional D-drive Android 35 emulator for visual review (about 4 GB extracted)."""
from pathlib import Path
import concurrent.futures, hashlib, os, shutil, subprocess, tempfile, urllib.request, zipfile

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / '.tools'
SDK = TOOLS / 'android-sdk'
AVD = TOOLS / 'avd'

PACKAGES = [
    ('repository/emulator-windows_x64-16632760.zip', '5ecdf654031e9791e20c336c8d87d2238cb69f3a', SDK / 'emulator'),
    ('repository/sys-img/android/x86_64-35_r02.zip', '2d857d170c0d1b827149565da34b3383e5306f7f', SDK / 'system-images/android-35/default/x86_64'),
]

def install(item):
    relative, sha1, destination = item
    name = Path(relative).name
    if destination.exists(): return
    archive = TOOLS / name
    if not archive.exists():
        print('Downloading', name, flush=True)
        urllib.request.urlretrieve('https://dl.google.com/android/' + relative, archive)
    if hashlib.sha1(archive.read_bytes()).hexdigest() != sha1:
        raise RuntimeError('Checksum mismatch for ' + name)
    stage = Path(tempfile.mkdtemp(prefix='emulator-package-', dir=TOOLS))
    with zipfile.ZipFile(archive) as value: value.extractall(stage)
    roots = [p for p in stage.iterdir() if p.is_dir()]
    if len(roots) != 1: raise RuntimeError('Unexpected archive layout: ' + name)
    destination.parent.mkdir(parents=True, exist_ok=True)
    roots[0].rename(destination)
    stage.rmdir()

def main():
    with concurrent.futures.ThreadPoolExecutor(2) as pool: list(pool.map(install, PACKAGES))
    emulator_package = SDK / 'emulator/package.xml'
    emulator_package.write_text('''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ns2:repository xmlns:ns2="http://schemas.android.com/repository/android/common/02"
 xmlns:ns5="http://schemas.android.com/repository/android/generic/02"
 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
 <license id="android-sdk-preview-license" type="text"/>
 <localPackage path="emulator" obsolete="false">
  <type-details xsi:type="ns5:genericDetailsType"/>
  <revision><major>37</major><minor>3</minor><micro>5</micro></revision>
  <display-name>Android Emulator</display-name>
  <uses-license ref="android-sdk-preview-license"/>
 </localPackage>
</ns2:repository>''', encoding='utf-8')
    env = dict(os.environ, ANDROID_HOME=str(SDK), ANDROID_SDK_ROOT=str(SDK), ANDROID_AVD_HOME=str(AVD))
    if not (AVD / 'TasteIndia_API_35.avd').exists():
        AVD.mkdir(parents=True, exist_ok=True)
        manager = SDK / 'cmdline-tools/latest/bin/avdmanager.bat'
        subprocess.run([str(manager), 'create', 'avd', '--force', '--name', 'TasteIndia_API_35',
                        '--package', 'system-images;android-35;default;x86_64', '--device', 'pixel_6'],
                       input='no\n', text=True, env=env, check=True)
    print('Emulator ready:', AVD, flush=True)

if __name__ == '__main__': main()
