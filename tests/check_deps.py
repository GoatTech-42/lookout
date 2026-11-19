#!/usr/bin/env python3
"""Dependency consistency: what the mod jar needs vs declares vs bundles.

Rules (26.1.2 binding check, finding H):
1. every third-party library the sources import is declared in
   fabric.mod.json depends (mod-id via the known import map)
2. every declared non-platform dependency is EITHER bundled jar-in-jar
   (META-INF/jars in the built jar, or an include/jarJar rule in
   build.gradle) OR named as a required install in docs/README.md
3. platform keys (fabricloader, minecraft, java, fabric-api) are exempt
Run from the repo root: python3 tests/check_deps.py"""
import glob, json, os, re, sys, zipfile

fails = []
def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + (" " + detail if detail else ""))
    if not ok: fails.append(name)

IMPORT_TO_MODID = {"com.llamalad7.mixinextras": "mixinextras"}
PLATFORM = {"fabricloader", "minecraft", "java", "fabric-api"}

fmj = json.load(open("src/main/resources/fabric.mod.json"))
declared = set(fmj.get("depends", {})) - PLATFORM

src_imports = set()
for path in glob.glob("src/main/java/**/*.java", recursive=True):
    for m in re.finditer(r"^import\s+(static\s+)?([\w.]+);", open(path).read(), re.M):
        pkg = m.group(2)
        for prefix, modid in IMPORT_TO_MODID.items():
            if pkg.startswith(prefix):
                src_imports.add(modid)

missing = src_imports - declared
check("declared-covers-imports", not missing, f"missing: {sorted(missing)}" if missing else f"declared={sorted(declared)}")

# comment-stripped: a prose mention is not a bundle rule
gradle = "\n".join(l.split("//")[0] for l in open("build.gradle"))
readme = open("docs/README.md").read() if os.path.exists("docs/README.md") else ""
jars = glob.glob("build/libs/*.jar")
bundled = set()
for jar in jars:
    with zipfile.ZipFile(jar) as z:
        bundled |= {n.split("/")[2].lower() for n in z.namelist()
                    if n.startswith("META-INF/jars/") and n.endswith(".jar")}

for dep in sorted(declared):
    in_gradle = bool(re.search(r"^\s*(include|jarJar)\b.*" + re.escape(dep), gradle, re.M))
    in_jar = any(dep in b for b in bundled)
    in_doc = dep in readme.lower()
    check(f"resolved-{dep}", in_gradle or in_jar or in_doc,
          f"gradle-include={in_gradle} bundled={in_jar} documented={in_doc}")

check("fabric.mod.json-valid", bool(fmj.get("depends")) and bool(fmj.get("id")))
print("SUITE:", "all pass" if not fails else f"failures {fails}")
sys.exit(0 if not fails else 1)
