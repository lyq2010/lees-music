import sys
import unittest
from pathlib import Path
from unittest.mock import Mock
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from prune import stale_keys, prune_cos, prune_r2


class CleanupTests(unittest.TestCase):
    def test_only_older_exact_music_assets(self):
        keys = ["lees-music-0.1.0.apk", "lees-music-0.1.0-source.zip", "lees-music-0.1.0-SHA256SUMS.txt",
                "lees-music-0.1.1.apk", "lees-music-0.1.2.apk", "latest-lees-music.json",
                "LeesMail_0.1.0_x64-setup.exe", "lees-music-backup.apk", "backup/lees-music-0.1.0.apk"]
        self.assertEqual(sorted(keys[:3]), stale_keys(keys, "0.1.1"))
        self.assertEqual([], stale_keys(["lees-music-0.1.10.apk"], "0.1.2"))

    def test_cos_collects_all_pages_then_deletes_only_old(self):
        client = Mock()
        client.list_objects.side_effect = [
            {"Contents": [{"Key": "lees-music-0.1.0.apk"}], "IsTruncated": "true", "NextMarker": "next"},
            {"Contents": [{"Key": "lees-music-0.1.1.apk"}]}]
        prune_cos(client, "bucket", "0.1.1")
        client.delete_object.assert_called_once_with(Bucket="bucket", Key="lees-music-0.1.0.apk")
        self.assertEqual("next", client.list_objects.call_args.kwargs["Marker"])

    def test_cos_incomplete_listing_deletes_nothing(self):
        client = Mock()
        client.list_objects.return_value = {"Contents": [{"Key": "lees-music-0.1.0.apk"}], "IsTruncated": True}
        with self.assertRaises(ValueError):
            prune_cos(client, "bucket", "0.1.1")
        client.delete_object.assert_not_called()

    def test_r2_pagination_and_delete_scope(self):
        request = Mock(side_effect=[
            {"result": [{"key": "lees-music-0.1.0.apk"}], "result_info": {"is_truncated": True, "cursor": "next"}},
            {"result": [{"key": "latest-lees-music.json"}, {"key": "lees-music-0.1.1.apk"}]}, {}])
        prune_r2(request, "/objects", "0.1.1")
        self.assertEqual(("DELETE", "/objects/lees-music-0.1.0.apk"), request.call_args.args)
        self.assertEqual(3, request.call_count)

    def test_r2_incomplete_listing_deletes_nothing(self):
        request = Mock(return_value={"result": [{"key": "lees-music-0.1.0.apk"}], "result_info": {"is_truncated": True}})
        with self.assertRaises(ValueError):
            prune_r2(request, "/objects", "0.1.1")
        self.assertEqual(1, request.call_count)
