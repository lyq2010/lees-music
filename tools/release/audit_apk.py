"""Reject development payloads while preserving required runtime and legal assets."""
import json
from pathlib import Path
import sys
import zipfile

apk = Path(sys.argv[1])
allowed_assets = {
    "assets/LICENSE", "assets/THIRD_PARTY_NOTICES.md", "assets/PublicSuffixDatabase.list",
    "assets/dexopt/baseline.prof", "assets/dexopt/baseline.profm",
}
with zipfile.ZipFile(apk) as archive:
    entries = archive.infolist()
    for entry in entries:
        name = entry.filename
        if name.startswith("assets/") and not entry.is_dir() and name not in allowed_assets:
            raise SystemExit(f"Unexpected packaged asset: {name}")
        if name.startswith(("docs/", "tools/", ".github/")) or name.endswith((".p12", ".jks", ".keystore", ".java", ".kt", ".map")):
            raise SystemExit(f"Development/private file in APK: {name}")
    print(json.dumps({"apkBytes": apk.stat().st_size,
        "compressedDexBytes": sum(e.compress_size for e in entries if e.filename.endswith(".dex")),
        "assets": sorted(e.filename for e in entries if e.filename.startswith("assets/"))}, indent=2))
