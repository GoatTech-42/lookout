#!/usr/bin/env python3
"""Dependency consistency: what the mod jar needs vs declares vs bundles.

Rules (26.1.2 binding check, finding H; QA AMEND 2026-11-20):
1. every third-party library the sources import is declared in
   fabric.mod.json depends (mod-id via the known import map)
2. every declared non-platform dependency is bundled jar-in-jar IN THE
   EXACT TARGET ARTIFACT, and the bundle must hold at four layers: an
   include/jarJar rule in the ACTIVE build route file; a valid nested
   archive under META-INF/jars in that one jar whose own fabric.mod.json
   registers the same mod id at the included version; a jars[].file entry
   in the OUTER artifact's fabric.mod.json naming that exact nested path;
   and the outer artifact's shipped depends declaring the mod id with a
   constraint the nested version satisfies. prose docs never satisfy this
   (QA counterexample 2026-11-19: "mixinextras is bundled" prose passed
   the gate with the include removed). jars are never unioned across
   build/libs: a stale jar from another route or run must not satisfy, or
   mask, this jar's check.
3. platform keys (fabricloader, minecraft, java, fabric-api) are exempt

Run from the repo root:
  python3 tests/check_deps.py [--jar PATH] [--build-file PATH]
--jar: the exact target artifact. Default: the single non-sources jar
under build/libs; zero or several is a loud FAIL, never a union.
--build-file: the build route file whose include rules are checked
(default build.gradle; pass build-remap.gradle for the remap route)."""
import argparse, glob, io, json, os, re, sys, zipfile

fails = []
def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ((" " + detail) if (not ok and detail) else ""))
    if not ok: fails.append(name)

IMPORT_TO_MODID = {"com.llamalad7.mixinextras": "mixinextras"}
PLATFORM = {"fabricloader", "minecraft", "java", "fabric-api"}

ap = argparse.ArgumentParser()
ap.add_argument("--jar", help="exact target artifact to inspect")
ap.add_argument("--build-file", default="build.gradle", help="active build route file")
args = ap.parse_args()

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

# include rules come from the ACTIVE route file, comment-stripped: a prose
# mention is not a bundle rule, and another route's rule says nothing here.
try:
    gradle = "\n".join(l.split("//")[0] for l in open(args.build_file))
except OSError:
    gradle = ""
    check("build-file-readable", False, f"cannot read {args.build_file}")

# include coordinate versions per mod id: include "group:artifact:version"
include_versions = {}
for m in re.finditer(r'^\s*(?:include|jarJar)\b\s+["\']([\w.\-]+):([\w.\-]+):([\w.\-+]+)["\']', gradle, re.M):
    artifact, version = m.group(2), m.group(3)
    for modid in declared | src_imports:
        if modid in artifact.lower():
            include_versions[modid] = version

def version_satisfies(have, constraint):
    if not isinstance(have, str) or not isinstance(constraint, str):
        return False
    if constraint.strip() == "*":
        return True
    have_t = tuple(int(x) for x in re.findall(r"\d+", have))
    m = re.fullmatch(r">=\s*([\d.]+)", constraint.strip())
    if m:
        want_t = tuple(int(x) for x in m.group(1).split("."))
        return have_t >= want_t
    return have == constraint.strip()

if args.jar:
    jars = [args.jar]
    check("jar-exists", os.path.isfile(args.jar), args.jar)
else:
    jars = [j for j in glob.glob("build/libs/*.jar") if not j.endswith("-sources.jar")]
    check("jar-unambiguous", len(jars) == 1,
          f"found {len(jars)} non-sources jar(s) under build/libs; pass --jar to bind the exact artifact")

# inspect the exact artifact only; never union across the build dir
nested_entries = {}
outer = None
outer_error = None
if jars and os.path.isfile(jars[0]):
    try:
        with zipfile.ZipFile(jars[0]) as z:
            for n in z.namelist():
                if n.startswith("META-INF/jars/") and n.endswith(".jar"):
                    nested_entries[n] = z.read(n)
            outer = json.loads(z.read("fabric.mod.json"))
            if not isinstance(outer, dict):
                raise ValueError("fabric.mod.json is not a JSON object")
    except Exception as e:
        outer = None
        outer_error = f"{type(e).__name__}: {e}"
check("outer-metadata-readable", outer is not None,
      f"outer artifact unreadable or fabric.mod.json missing/invalid ({outer_error})" if outer_error
      else "outer artifact fabric.mod.json missing or invalid")

shipped = set()
outer_jar_entries = set()
if outer is not None:
    dep_field = outer.get("depends", {})
    check("shipped-depends-typed", isinstance(dep_field, dict),
          f"outer depends must be an object, got {type(dep_field).__name__}")
    if isinstance(dep_field, dict):
        shipped = set(dep_field) - PLATFORM
    jars_field = outer.get("jars", [])
    jars_typed = (isinstance(jars_field, list)
                  and all(isinstance(j, dict) and isinstance(j.get("file"), str) for j in jars_field))
    check("shipped-jars-typed", jars_typed,
          "outer jars must be a list of objects with string file values")
    if jars_typed:
        outer_jar_entries = {j.get("file") for j in jars_field}
    # '<version>.x' is accepted as platform metadata syntax (the
    # minecraft key ships e.g. '26.1.x'); platform constraints are
    # syntax-checked here and NEVER resolved. The regex permits '.x' on
    # ANY depends key - it is not key-restricted - but a bundled dep
    # shipping '.x' still fails closed, because version_satisfies below
    # (bundled deps only, e.g. mixinextras) resolves '>=', an exact pin,
    # or '*' and treats '.x' as a literal equality miss. 'Platform-only'
    # describes the supported semantics, not the parsing.
    constraint_reasons = []
    if isinstance(dep_field, dict):
        for dep_name, constraint in dep_field.items():
            if not isinstance(constraint, str):
                constraint_reasons.append(f"{dep_name}: constraint is {type(constraint).__name__}, not a string")
            elif not re.fullmatch(r"(>=\d+(\.\d+)*|\d+(\.\d+)*|\d+(\.\d+)*\.x|\*)", constraint.strip()):
                constraint_reasons.append(
                    f"{dep_name}: constraint {constraint!r} is none of '>=<version>', an exact pinned version, a '<version>.x' range, or '*'")
    check("shipped-constraints-typed", not constraint_reasons,
          "; ".join(constraint_reasons))
    shipped_missing = src_imports - shipped
    check("shipped-covers-imports", not shipped_missing,
          f"missing from shipped depends: {sorted(shipped_missing)}" if shipped_missing
          else f"shipped={sorted(shipped)}")

for dep in sorted(declared):
    in_gradle = dep in include_versions
    matching = {n: b for n, b in nested_entries.items() if dep in n.lower()}
    ok_nested = registered = shipped_ok = False
    reasons = []
    for n, b in matching.items():
        try:
            with zipfile.ZipFile(io.BytesIO(b)) as nz:
                nfmj = json.loads(nz.read("fabric.mod.json"))
            if not isinstance(nfmj, dict):
                raise ValueError("nested fabric.mod.json is not a JSON object")
        except Exception as e:
            reasons.append(f"{n}: invalid nested archive ({type(e).__name__}: {e})")
            continue
        if nfmj.get("id") != dep:
            reasons.append(f"{n}: nested fabric.mod.json id={nfmj.get('id')!r} != {dep!r}")
            continue
        want = include_versions.get(dep)
        if want and nfmj.get("version") != want:
            reasons.append(f"{n}: nested version {nfmj.get('version')!r} != included {want!r}")
            continue
        ok_nested = True
        if n not in outer_jar_entries:
            reasons.append(f"{n}: not registered in outer fabric.mod.json jars[]")
            continue
        registered = True
        if dep not in shipped:
            reasons.append(f"{dep} not in shipped depends")
            continue
        if not version_satisfies(nfmj.get("version", ""), outer["depends"][dep]):
            reasons.append(f"{n}: nested version {nfmj.get('version')!r} does not satisfy shipped {outer['depends'][dep]!r}")
            continue
        shipped_ok = True
    if not matching:
        reasons.append("no nested archive under META-INF/jars")
    detail = (f"include={in_gradle} nested={ok_nested} registered={registered} shipped={shipped_ok}"
              + ("" if (ok_nested and registered and shipped_ok) else " (" + "; ".join(reasons) + ")"))
    check(f"resolved-{dep}", in_gradle and ok_nested and registered and shipped_ok,
          detail + " (route file, exact artifact, valid nested registration, outer jars[] registration and shipped depends all required)")

check("fabric.mod.json-valid", bool(fmj.get("depends")) and bool(fmj.get("id")))
print("SUITE:", "all pass" if not fails else f"failures {fails}")
sys.exit(0 if not fails else 1)
