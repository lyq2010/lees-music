"""Delete only older Lee's Music versions after the new mirror has been verified."""
import json
import re
import urllib.parse
import urllib.request

ASSET = re.compile(r"lees-music-(\d+)\.(\d+)\.(\d+)(?:\.apk|-source\.zip|-SHA256SUMS\.txt)")


def stale_keys(keys, version):
    current = tuple(map(int, version.split(".")))
    if len(current) != 3:
        raise ValueError("Invalid current version")
    return sorted({key for key in keys if (match := ASSET.fullmatch(key))
                   and tuple(map(int, match.groups())) < current})


def prune_cos(client, bucket, version):
    keys, marker, seen = [], "", set()
    while True:
        params = dict(Bucket=bucket, Prefix="lees-music-", MaxKeys=1000)
        if marker:
            params["Marker"] = marker
        page = client.list_objects(**params)
        keys.extend(item["Key"] for item in page.get("Contents", []) or [])
        if page.get("IsTruncated") not in (True, "true"):
            break
        marker = page.get("NextMarker")
        if not marker or marker in seen:
            raise ValueError("Invalid COS pagination; nothing deleted")
        seen.add(marker)
    for key in stale_keys(keys, version):
        client.delete_object(Bucket=bucket, Key=key)
        print(f"Deleted old Lee's Music mirror: {key}")


def cloudflare_request(token, method, path, params=None):
    url = "https://api.cloudflare.com/client/v4" + path
    if params:
        url += "?" + urllib.parse.urlencode(params)
    request = urllib.request.Request(url, method=method, headers={"Authorization": "Bearer " + token})
    with urllib.request.urlopen(request, timeout=30) as response:
        data = json.load(response)
    if not data.get("success"):
        raise RuntimeError("Cloudflare API request failed")
    return data


def prune_r2(request, bucket_path, version):
    keys, cursor, seen = [], "", set()
    while True:
        params = {"prefix": "lees-music-", "per_page": "1000"}
        if cursor:
            params["cursor"] = cursor
        page = request("GET", bucket_path, params)
        keys.extend(item["key"] for item in page.get("result", []) or [])
        info = page.get("result_info") or {}
        if not info.get("is_truncated"):
            break
        cursor = info.get("cursor")
        if not cursor or cursor in seen:
            raise ValueError("Invalid R2 pagination; nothing deleted")
        seen.add(cursor)
    for key in stale_keys(keys, version):
        request("DELETE", bucket_path + "/" + urllib.parse.quote(key, safe=""))
        print(f"Deleted old Lee's Music mirror: {key}")
