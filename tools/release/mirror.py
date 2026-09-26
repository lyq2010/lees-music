"""Mirror verified public release assets. Publish the feed only after the APK is available."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import urllib.request
import urllib.error

assets = Path("assets")
manifest = json.loads((assets / "latest-lees-music.json").read_text(encoding="utf-8"))
apk_name = manifest["apk"]
if Path(apk_name).name != apk_name or not apk_name.startswith("lees-music-") or not apk_name.endswith(".apk"):
    raise SystemExit("Invalid APK filename")
apk = assets / apk_name
if apk.stat().st_size != manifest["size"] or hashlib.sha256(apk.read_bytes()).hexdigest() != manifest["sha256"]:
    raise SystemExit("APK integrity check failed")
files = sorted(p for p in assets.iterdir() if p.is_file() and p.name != "latest-lees-music.json")
version = manifest["version"]
expected_names = {f"lees-music-{version}.apk", f"lees-music-{version}-source.zip", f"lees-music-{version}-SHA256SUMS.txt"}
if {p.name for p in files} != expected_names:
    raise SystemExit("Unexpected or incomplete release assets")
for line in (assets / f"lees-music-{version}-SHA256SUMS.txt").read_text().splitlines():
    expected_hash, name = line.split("  ", 1)
    if name not in expected_names or hashlib.sha256((assets / name).read_bytes()).hexdigest() != expected_hash:
        raise SystemExit("Release checksum mismatch")
if sys.argv[1] == "cos":
    from qcloud_cos import CosConfig, CosS3Client
    client = CosS3Client(CosConfig(Region=os.environ["TENCENT_COS_REGION"],
        SecretId=os.environ["TENCENT_COS_SECRET_ID"], SecretKey=os.environ["TENCENT_COS_SECRET_KEY"]))
    base = "https://releases.angelolee.cn"
    def upload(path):
        client.upload_file(Bucket=os.environ["TENCENT_COS_BUCKET"], LocalFilePath=str(path), Key=path.name,
            CacheControl="no-cache" if path.suffix == ".json" else "public, max-age=31536000, immutable")
elif sys.argv[1] == "r2":
    base = "https://plt-releases.leenbsl.workers.dev"
    if not os.environ.get("CLOUDFLARE_ACCOUNT_ID"):
        request = urllib.request.Request("https://api.cloudflare.com/client/v4/accounts",
            headers={"Authorization": "Bearer " + os.environ["CLOUDFLARE_API_TOKEN"]})
        with urllib.request.urlopen(request, timeout=30) as response:
            accounts = json.load(response)
        if not accounts.get("success") or len(accounts.get("result", [])) != 1:
            raise SystemExit("Set CLOUDFLARE_ACCOUNT_ID explicitly; account is ambiguous")
        os.environ["CLOUDFLARE_ACCOUNT_ID"] = accounts["result"][0]["id"]
    def upload(path):
        subprocess.run(["npx", "--yes", "wrangler@4", "r2", "object", "put",
            f"packing-list-releases/{path.name}", "--file", str(path), "--remote",
            "--content-type", "application/json" if path.suffix == ".json" else "application/octet-stream"], check=True)
else:
    raise SystemExit("Expected cos or r2")

def fetch(name):
    request = urllib.request.Request(f"{base}/{name}", headers={"User-Agent": "Mozilla/5.0 LeesMusicRelease", "Cache-Control": "no-cache"})
    return urllib.request.urlopen(request, timeout=60)

try:
    with fetch("latest-lees-music.json") as response:
        existing = json.load(response)
    if existing["versionCode"] > manifest["versionCode"] or (
            existing["versionCode"] == manifest["versionCode"] and existing != manifest):
        raise SystemExit("Refusing to roll back or replace an already published version")
except urllib.error.HTTPError as error:
    if error.code != 404:
        raise

for file in files:
    upload(file)
with fetch(apk_name) as response:
    digest = hashlib.sha256()
    size = 0
    while chunk := response.read(1024 * 1024):
        size += len(chunk)
        if size > manifest["size"]:
            raise SystemExit("Remote APK size mismatch")
        digest.update(chunk)
    if size != manifest["size"] or digest.hexdigest() != manifest["sha256"]:
        raise SystemExit("Remote APK verification failed; feed not published")
upload(assets / "latest-lees-music.json")
with fetch("latest-lees-music.json") as response:
    if json.load(response) != manifest:
        raise SystemExit("Published feed differs from release")
print(f"Verified {sys.argv[1]} mirror: {manifest['version']}")
