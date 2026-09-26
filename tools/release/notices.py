"""Generate notices from the exact resolved runtime artifacts and their published POMs."""
from pathlib import Path
import io
import zipfile
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[2]
lines = ["# 第三方依赖声明", "", "本应用使用下列开源组件，各组件保留原有版权和许可证。", "",
         "此清单依据 Gradle releaseRuntimeClasspath 的实际解析结果及发布者 POM 生成。", "",
         "| 组件 | 许可证 |", "| --- | --- |"]
embedded = []
ns = {"m": "http://maven.apache.org/POM/4.0.0"}
for row in (root / "app/build/reports/release-dependencies.tsv").read_text().splitlines():
    coordinate, path = row.split("\t")
    artifact = Path(path)
    poms = sorted(artifact.parent.parent.glob("*/*.pom"))
    if not poms:
        raise SystemExit(f"Missing publisher POM for {coordinate}")
    pom = ET.parse(poms[0])
    licenses = pom.findall("./m:licenses/m:license", ns)
    for _ in range(8):
        if licenses:
            break
        parent = pom.find("./m:parent", ns)
        if parent is None:
            break
        parts = [parent.findtext("m:" + name, namespaces=ns) for name in ("groupId", "artifactId", "version")]
        parents = sorted(artifact.parents[4].joinpath(*parts).glob("*/*.pom"))
        if not parents:
            break
        pom = ET.parse(parents[0])
        licenses = pom.findall("./m:licenses/m:license", ns)
    if not licenses:
        raise SystemExit(f"Missing license metadata for {coordinate}")
    label = "; ".join(f"[{entry.findtext('m:name', namespaces=ns)}]({entry.findtext('m:url', namespaces=ns)})" for entry in licenses)
    lines.append(f"| `{coordinate}` | {label} |")
    def inspect(archive):
        for name in archive.namelist():
            if name.startswith("META-INF/") and any(word in name.upper() for word in ("LICENSE", "NOTICE", "COPYRIGHT")) and not name.endswith("/"):
                text = archive.read(name).decode("utf-8", errors="replace")
                embedded.append(f"## {coordinate} — {name}\n\n{text}\n")
    with zipfile.ZipFile(artifact) as archive:
        inspect(archive)
        if "classes.jar" in archive.namelist():
            with zipfile.ZipFile(io.BytesIO(archive.read("classes.jar"))) as classes:
                inspect(classes)
lines += ["", "## 原始声明", "", "以下内容原样摘自分发组件中的 LICENSE、NOTICE 与 COPYRIGHT 文件。", ""]
lines.extend(embedded)
(root / "docs/THIRD_PARTY_NOTICES.md").write_text("\n".join(lines).rstrip() + "\n", encoding="utf-8")
print(f"Generated notices for {len((root / 'app/build/reports/release-dependencies.tsv').read_text().splitlines())} runtime artifacts; {len(embedded)} embedded declarations.")
