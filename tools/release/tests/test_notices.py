import importlib.util
import io
from pathlib import Path
import tempfile
import unittest
import zipfile

spec = importlib.util.spec_from_file_location("notices", Path(__file__).parents[1] / "notices.py")
notices = importlib.util.module_from_spec(spec)
spec.loader.exec_module(notices)

LICENSE = """<licenses><license><name>Apache-2.0</name><url>https://www.apache.org/licenses/LICENSE-2.0</url></license></licenses>"""


class NoticesTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.cache = self.root / "cache"
        self.artifact = self.cache / "example" / "library" / "1.2.3" / "artifact" / "library.aar"
        self.artifact.parent.mkdir(parents=True)
        nested = io.BytesIO()
        with zipfile.ZipFile(nested, "w") as jar:
            jar.writestr("META-INF/LICENSE", "Nested copyright")
        with zipfile.ZipFile(self.artifact, "w") as aar:
            aar.writestr("META-INF/NOTICE", "Publisher notice")
            aar.writestr("classes.jar", nested.getvalue())
        self.pom = self.artifact.parent.parent / "pom" / "library.pom"
        self.write_pom(self.pom, LICENSE)
        inventory = self.root / "app/build/reports/release-dependencies.tsv"
        inventory.parent.mkdir(parents=True)
        inventory.write_text(f"example:library:1.2.3\t{self.artifact}\n", encoding="utf-8")
        (self.root / "docs").mkdir()

    def write_pom(self, path, body):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(f'<project xmlns="http://maven.apache.org/POM/4.0.0">{body}</project>', encoding="utf-8")

    def test_reads_exact_coordinate_and_nested_declarations(self):
        text, count = notices.generate(self.root)
        self.assertIn("`example:library:1.2.3`", text)
        self.assertIn("Publisher notice", text)
        self.assertIn("Nested copyright", text)
        self.assertEqual(2, count)
        self.assertEqual((text, count), notices.generate(self.root))

    def test_inherits_license_from_cached_parent(self):
        self.write_pom(self.pom, "<parent><groupId>example</groupId><artifactId>parent</artifactId><version>4</version></parent>")
        self.write_pom(self.cache / "example/parent/4/pom/parent.pom", LICENSE)
        text, _ = notices.generate(self.root)
        self.assertIn("Apache-2.0", text)

    def test_missing_publisher_pom_fails(self):
        self.pom.unlink()
        with self.assertRaisesRegex(SystemExit, "Missing publisher POM"):
            notices.generate(self.root)

    def test_missing_license_metadata_fails(self):
        self.write_pom(self.pom, "")
        with self.assertRaisesRegex(SystemExit, "Missing license metadata"):
            notices.generate(self.root)

    def test_check_rejects_drift_without_overwriting(self):
        destination = self.root / "docs/THIRD_PARTY_NOTICES.md"
        destination.write_text("previous inventory\n", encoding="utf-8")
        with self.assertRaisesRegex(SystemExit, "stale"):
            notices.update_notices(self.root, check=True)
        self.assertEqual("previous inventory\n", destination.read_text(encoding="utf-8"))
        notices.update_notices(self.root)
        notices.update_notices(self.root, check=True)


if __name__ == "__main__":
    unittest.main()
