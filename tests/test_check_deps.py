#!/usr/bin/env python3
"""Fixtures for tests/check_deps.py binding rules (QA AMEND 2026-11-20).

Builds synthetic mod jars in a temp dir and runs check_deps.py against
them from the real repo root (real fabric.mod.json declares mixinextras,
real src/main imports it). No gradle run and no clean-output precondition:
every mutant is a synthetic artifact or route file.

Expected outcomes:
  A valid bundle: include rule, valid nested archive, outer jars[]
    registration and shipped depends all correct        -> PASS (exit 0)
  B stale neighbour jar with a valid bundle, --jar
    points at an EMPTY jar                              -> FAIL (no union)
  C nested entry with matching name, garbage bytes      -> FAIL (invalid zip)
  D nested valid zip, wrong fabric.mod id / version     -> FAIL
  E empty jar, include rule present                     -> FAIL nested=False
  F valid jar, route file with only a prose mention     -> FAIL include=False
  G bare run, two non-sources jars in build/libs        -> FAIL ambiguous
  H nested archive present but outer jars[] omits it    -> FAIL registered=False
  I outer jars[] names a different path                 -> FAIL registered=False
  J outer shipped depends missing / unsatisfiable       -> FAIL shipped=False
  K malformed outer zips (empty, non-zip, truncated)    -> FAIL outer-metadata-readable, no traceback
Run from the repo root: python3 tests/test_check_deps.py"""
import json, os, subprocess, sys, tempfile, zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CHECK = os.path.join(ROOT, "tests", "check_deps.py")
DEP = "mixinextras"
VER = "0.5.5"
NESTED = f"META-INF/jars/{DEP}-fabric-{VER}.jar"
INCLUDE = f'include "io.github.llamalad7:mixinextras-fabric:{VER}"\n'

def nested_jar(modid=DEP, version=VER):
    import io
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w") as z:
        z.writestr("fabric.mod.json", json.dumps({"id": modid, "version": version}))
    return buf.getvalue()

def mod_jar(path, nested_name=None, nested_bytes=None, register=True, depends=None,
            register_path=None):
    reg = []
    if nested_name and register:
        reg = [{"file": register_path or NESTED}]
    outer = {"id": "meshac",
             "depends": depends if depends is not None else {DEP: f">={VER}"},
             "jars": reg}
    with zipfile.ZipFile(path, "w") as z:
        z.writestr("fabric.mod.json", json.dumps(outer))
        if nested_name:
            z.writestr(NESTED if nested_name is True else nested_name, nested_bytes)

def run(jar=None, build_file=None, cwd=ROOT):
    cmd = [sys.executable, CHECK]
    if jar: cmd += ["--jar", jar]
    if build_file: cmd += ["--build-file", build_file]
    return subprocess.run(cmd, cwd=cwd, capture_output=True, text=True)

fails = []
def expect(name, res, want_exit, must_contain=None):
    ok = res.returncode == want_exit and (must_contain is None or must_contain in res.stdout)
    print(("PASS " if ok else "FAIL ") + name + f" (exit={res.returncode}, want {want_exit})")
    if not ok:
        fails.append(name)
        print(res.stdout[-600:])

with tempfile.TemporaryDirectory() as td:
    bf = os.path.join(td, "route.gradle")
    open(bf, "w").write(INCLUDE)
    bf_prose = os.path.join(td, "route-prose.gradle")
    open(bf_prose, "w").write("// mixinextras is bundled, trust this comment\n")

    good = os.path.join(td, "good.jar"); mod_jar(good, True, nested_jar())
    empty = os.path.join(td, "empty.jar"); mod_jar(empty)
    stale = os.path.join(td, "stale.jar"); mod_jar(stale, True, nested_jar())
    garbage = os.path.join(td, "garbage.jar"); mod_jar(garbage, True, b"not a zip at all")
    wrongid = os.path.join(td, "wrongid.jar"); mod_jar(wrongid, True, nested_jar(modid="othermod"))
    wrongver = os.path.join(td, "wrongver.jar"); mod_jar(wrongver, True, nested_jar(version="0.4.9"))
    noreg = os.path.join(td, "noreg.jar"); mod_jar(noreg, True, nested_jar(), register=False)
    # registration names a path that is not the nested entry
    wrongreg = os.path.join(td, "wrongreg.jar")
    mod_jar(wrongreg, True, nested_jar(), register_path="META-INF/jars/other.jar")
    nodep = os.path.join(td, "nodep.jar"); mod_jar(nodep, True, nested_jar(), depends={})
    baddep = os.path.join(td, "baddep.jar"); mod_jar(baddep, True, nested_jar(), depends={DEP: ">=9.9.9"})

    expect("A-valid-bundle", run(good, bf), 0)
    expect("B-stale-neighbour-not-unioned", run(empty, bf), 1, "nested=False")
    expect("C-invalid-nested-bytes", run(garbage, bf), 1, "invalid nested archive")
    expect("D1-nested-wrong-id", run(wrongid, bf), 1, "!=")
    expect("D2-nested-wrong-version", run(wrongver, bf), 1, "version")
    expect("E-empty-jar", run(empty, bf), 1, "nested=False")
    expect("F-mutant-prose-only-route", run(good, bf_prose), 1, "include=False")
    expect("H-absent-registration", run(noreg, bf), 1, "registered=False")
    expect("I-wrong-registration-path", run(wrongreg, bf), 1, "registered=False")
    expect("J1-shipped-dep-missing", run(nodep, bf), 1, "shipped=False")
    expect("J2-shipped-constraint-unsatisfiable", run(baddep, bf), 1, "shipped=False")

    # G: bare run in a minimal tree with two non-sources jars must fail loud
    g = os.path.join(td, "gtree")
    os.makedirs(os.path.join(g, "build", "libs"))
    os.makedirs(os.path.join(g, "src", "main", "resources"))
    os.makedirs(os.path.join(g, "src", "main", "java"))
    json.dump({"id": "meshac", "depends": {DEP: f">={VER}"}},
              open(os.path.join(g, "src", "main", "resources", "fabric.mod.json"), "w"))
    mod_jar(os.path.join(g, "build", "libs", "a.jar"), True, nested_jar())
    mod_jar(os.path.join(g, "build", "libs", "b.jar"), True, nested_jar())
    open(os.path.join(g, "build.gradle"), "w").write(INCLUDE)
    expect("G-bare-run-ambiguous", run(cwd=g), 1, "pass --jar")

    # K: malformed outer zips must be structured FAILs, never a traceback
    for label, payload in [("empty", b""),
                           ("nonzip", b"this is plain text, no zip magic at all"),
                           ("truncated", b"PK\x03\x04 truncated garbage, not a real archive")]:
        bad = os.path.join(td, "bad-" + label + ".zip")
        open(bad, "wb").write(payload)
        r = run(bad, bf)
        expect("K-malformed-outer-zip-" + label, r, 1, "outer-metadata-readable")
        ok = "Traceback" not in r.stderr
        print(("PASS " if ok else "FAIL ") + "K-no-traceback-" + label)
        if not ok:
            fails.append("K-no-traceback-" + label)
            print(r.stderr[-400:])

print("SUITE:", "all pass" if not fails else f"failures {fails}")
sys.exit(0 if not fails else 1)
