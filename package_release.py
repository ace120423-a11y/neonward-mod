"""Refresh the existing distribution without changing third-party dependencies."""
from pathlib import Path
import hashlib
import json
import shutil
import zipfile

root = Path(__file__).resolve().parent
version = (root / "release/VERSION").read_text().strip()
jar = root / "build/neonward-0.1.0.jar"
pack = root / "distribution/NeonWard-Friends.mrpack"
data = jar.read_bytes()
with zipfile.ZipFile(pack) as source:
    assert source.testzip() is None
    index = json.loads(source.read("modrinth.index.json"))
    extra = {n: source.read(n) for n in source.namelist()
             if n not in ("modrinth.index.json", "overrides/mods/neonward-0.1.0.jar")}
index.update(versionId=version, name=f"Neon Ward v{version}",
             summary="NeonWard client pack: Frost Citadel, new enemies and multiplayer improvements. No world/player data.")
entries = [f for f in index["files"] if f["path"] == "mods/neonward-0.1.0.jar"]
assert len(entries) == 1
entries[0].update(hashes={alg: hashlib.new(alg, data).hexdigest() for alg in ("sha1", "sha512")},
                  fileSize=len(data), downloads=[f"https://github.com/ace120423-a11y/neonward-mod/releases/download/v{version}/neonward-0.1.0-v{version}.jar"])
backup = root / f"work/before-package-{version}.mrpack"
backup.parent.mkdir(exist_ok=True)
if not backup.exists():
    shutil.copy2(pack, backup)
temporary = pack.with_suffix(".tmp")
with zipfile.ZipFile(temporary, "w", zipfile.ZIP_DEFLATED) as target:
    target.writestr("modrinth.index.json", json.dumps(index, ensure_ascii=False, indent=2))
    for name, content in extra.items():
        target.writestr(name, content)
    target.writestr("overrides/mods/neonward-0.1.0.jar", data)
with zipfile.ZipFile(temporary) as check:
    assert check.testzip() is None
    assert len(check.namelist()) == len(set(check.namelist()))
    assert check.read("overrides/mods/neonward-0.1.0.jar") == data
    assert json.loads(check.read("modrinth.index.json"))["versionId"] == version
temporary.replace(pack)
shutil.copy2(jar, root / "distribution/neonward-0.1.0.jar")
print(f"Verified v{version}: {len(index['files'])} mods, matching embedded/download jar, ZIP integrity OK")
