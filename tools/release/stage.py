"""Stage immutable APK, corresponding source, checksum and update manifest."""
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys

root = Path(__file__).resolve().parents[2]
metadata = json.loads((root / "app/build/outputs/apk/release/output-metadata.json").read_text())
element = metadata["elements"][0]
version = element["versionName"]
if sys.argv[1] != f"v{version}" or int(version.split(".")[-1]) >= 50:
    raise SystemExit("Tag/version mismatch or patch version >= 50")
assets = root / "artifacts/release"
assets.mkdir(parents=True, exist_ok=True)
apk = assets / f"lees-music-{version}.apk"
shutil.copyfile(root / "app/build/outputs/apk/release" / element["outputFile"], apk)
source = assets / f"lees-music-{version}-source.zip"
subprocess.run(["git", "archive", "--format=zip", f"--output={source}", "HEAD"], cwd=root, check=True)
digest = hashlib.sha256(apk.read_bytes()).hexdigest()
manifest = dict(version=version, versionCode=element["versionCode"], packageName=metadata["applicationId"],
                apk=apk.name, sha256=digest, size=apk.stat().st_size, minSdk=26,
                notes=(root / "docs/RELEASE_NOTES.md").read_text(encoding="utf-8").strip())
(assets / "latest-lees-music.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
(assets / f"lees-music-{version}-SHA256SUMS.txt").write_text("\n".join(
    f"{hashlib.sha256(p.read_bytes()).hexdigest()}  {p.name}" for p in (apk, source)) + "\n", encoding="utf-8")
