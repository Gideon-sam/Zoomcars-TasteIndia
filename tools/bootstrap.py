"""Install pinned Android/Gradle tools locally; never modifies global installations."""
from pathlib import Path
import os, urllib.request, zipfile, subprocess, concurrent.futures, tempfile, shutil, hashlib

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / '.tools'
TOOLS.mkdir(exist_ok=True)

def download(url, path):
    if not path.exists() or path.stat().st_size == 0:
        print('Downloading', url, flush=True)
        urllib.request.urlretrieve(url, path)

def gradle():
    archive = TOOLS / 'gradle-8.9-bin.zip'
    download('https://services.gradle.org/distributions/gradle-8.9-bin.zip', archive)
    if not (TOOLS / 'gradle-8.9').exists():
        with zipfile.ZipFile(archive) as z: z.extractall(TOOLS)

def android():
    sdk = TOOLS / 'android-sdk'
    manager = sdk / 'cmdline-tools/latest/bin/sdkmanager.bat'
    if not manager.exists():
        archive = TOOLS / 'commandlinetools.zip'
        download('https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip', archive)
        stage = TOOLS / 'sdk-stage'
        with zipfile.ZipFile(archive) as z: z.extractall(stage)
        manager.parent.parent.parent.mkdir(parents=True, exist_ok=True)
        (stage / 'cmdline-tools').rename(manager.parent.parent)
    # sdkmanager's Java HTTP client is blocked in some review environments. These
    # are the same official packages from Google's repository catalogue.
    packages = [
        ('platform-35_r02.zip', '0bb560a90a7a2cbd0dd8348224d518b638fe7949', sdk / 'platforms/android-35'),
        ('build-tools_r35_windows.zip', 'af059bb67cf7786f45ee0db85e2d24985df1b4b6', sdk / 'build-tools/35.0.0'),
        ('platform-tools_r37.0.1-win.zip', 'e03e78b1d80b396f1c3358e31251cb31740e1110', sdk / 'platform-tools'),
    ]
    def install(item):
        name, expected_sha1, destination = item
        if destination.exists(): return
        archive = TOOLS / name
        download('https://dl.google.com/android/repository/' + name, archive)
        if hashlib.sha1(archive.read_bytes()).hexdigest() != expected_sha1:
            raise RuntimeError('Checksum mismatch for ' + name)
        stage = Path(tempfile.mkdtemp(prefix='android-package-', dir=TOOLS))
        with zipfile.ZipFile(archive) as z: z.extractall(stage)
        roots = [p for p in stage.iterdir() if p.is_dir()]
        if len(roots) != 1: raise RuntimeError('Unexpected package layout for ' + name)
        destination.parent.mkdir(parents=True, exist_ok=True)
        roots[0].rename(destination)
        stage.rmdir()
    with concurrent.futures.ThreadPoolExecutor(3) as pool:
        list(pool.map(install, packages))
    licenses = sdk / 'licenses'
    licenses.mkdir(parents=True, exist_ok=True)
    (licenses / 'android-sdk-license').write_text('24333f8a63b6825ea9c5514f83c2829b004d1fee\n')
    (ROOT / 'local.properties').write_text('sdk.dir=' + sdk.as_posix() + '\n')

if __name__ == '__main__':
    with concurrent.futures.ThreadPoolExecutor(2) as pool:
        list(pool.map(lambda f: f(), [gradle, android]))
    print('Toolchain installed in .tools', flush=True)
