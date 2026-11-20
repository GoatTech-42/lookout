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
  L wrong-typed metadata (outer non-object, depends as
    list, jars entries non-dict, nested non-object)     -> named FAILs, no traceback
  M constraint/jars boundary: non-string jars[].file,
    non-string or malformed depends constraints         -> named FAILs, no traceback
    all-valid incl. '*' platform dep and bundled '*'
    dependency                                          -> PASS (exit 0)
  Mutant controls: the pre-boundary checker at bc8b711
    must fail M1-M4 with its documented pre-boundary defect
    (M1 TypeError, M2 AttributeError, M3/M4 silent
    shipped=False), and the committed checker minus its
    wildcard short-circuit must fail M6 with FAIL
    resolved-mixinextras. every mutant first proves a
    healthy baseline (fixture A passes under the mutant).
    missing git history reports NOT_QUALIFIED and fails
    the suite. named assertions match line-prefix
    FAIL/PASS lines, never bare substrings
Run from the repo root: python3 tests/test_check_deps.py"""
import json, os, subprocess, sys, tempfile, zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CHECK = os.path.join(ROOT, "tests", "check_deps.py")
DEP = "mixinextras"
VER = "0.5.5"
NESTED = f"META-INF/jars/{DEP}-fabric-{VER}.jar"
INCLUDE = f'include "io.github.llamalad7:mixinextras-fabric:{VER}"\n'

def nested_jar(modid=DEP, version=VER, raw=None):
    import io
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w") as z:
        z.writestr("fabric.mod.json", raw if raw is not None else json.dumps({"id": modid, "version": version}))
    return buf.getvalue()

def mod_jar(path, nested_name=None, nested_bytes=None, register=True, depends=None,
            register_path=None, raw_outer=None):
    reg = []
    if nested_name and register:
        reg = [{"file": register_path or NESTED}]
    outer = raw_outer if raw_outer is not None else json.dumps({"id": "meshac",
             "depends": depends if depends is not None else {DEP: f">={VER}"},
             "jars": reg})
    with zipfile.ZipFile(path, "w") as z:
        z.writestr("fabric.mod.json", outer)
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

def has_prefix(res, prefix):
    return any(l.startswith(prefix) for l in res.stdout.splitlines())

def expect_fail(name, res, want_exit, check_name, marker=None):
    matched = [l for l in res.stdout.splitlines() if l.startswith("FAIL " + check_name)]
    ok = (res.returncode == want_exit
          and any(marker is None or marker in l for l in matched))
    print(("PASS " if ok else "FAIL ") + name
          + f" (exit={res.returncode}, want {want_exit}, FAIL {check_name}"
          + (f" line with {marker!r})" if marker else ")"))
    if not ok:
        fails.append(name)
        print(res.stdout[-600:])

def expect_line(name, res, want_exit, prefix):
    ok = res.returncode == want_exit and has_prefix(res, prefix)
    print(("PASS " if ok else "FAIL ") + name
          + f" (exit={res.returncode}, want {want_exit}, line {prefix!r})")
    if not ok:
        fails.append(name)
        print(res.stdout[-600:])

def mutant_run(script, jar, build_file):
    return subprocess.run([sys.executable, script, "--jar", jar, "--build-file", build_file],
                          cwd=ROOT, capture_output=True, text=True)

def mutant_baseline(name, script, good_jar, build_file):
    b = mutant_run(script, good_jar, build_file)
    ok = b.returncode == 0 and "Traceback" not in b.stderr
    print(("PASS " if ok else "FAIL ") + name
          + f"-baseline-healthy (exit={b.returncode})")
    if not ok:
        fails.append(name + "-baseline-healthy")
        print((b.stdout + b.stderr)[-400:])
    return ok

def mutant_kill(name, script, jar, build_file, evidence):
    r = mutant_run(script, jar, build_file)
    ok, why = evidence(r)
    print(("PASS " if ok else "FAIL ") + name + f" ({why})")
    if not ok:
        fails.append(name)
        print((r.stdout + r.stderr)[-500:])

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
    expect_fail("B-stale-neighbour-not-unioned", run(empty, bf), 1, "resolved-mixinextras", "nested=False")
    expect_fail("C-invalid-nested-bytes", run(garbage, bf), 1, "resolved-mixinextras", "invalid nested archive")
    expect_fail("D1-nested-wrong-id", run(wrongid, bf), 1, "resolved-mixinextras", "!=")
    expect_fail("D2-nested-wrong-version", run(wrongver, bf), 1, "resolved-mixinextras", "version")
    expect_fail("E-empty-jar", run(empty, bf), 1, "resolved-mixinextras", "nested=False")
    expect_fail("F-mutant-prose-only-route", run(good, bf_prose), 1, "resolved-mixinextras", "include=False")
    expect_fail("H-absent-registration", run(noreg, bf), 1, "resolved-mixinextras", "registered=False")
    expect_fail("I-wrong-registration-path", run(wrongreg, bf), 1, "resolved-mixinextras", "registered=False")
    expect_fail("J1-shipped-dep-missing", run(nodep, bf), 1, "resolved-mixinextras", "shipped=False")
    expect_fail("J2-shipped-constraint-unsatisfiable", run(baddep, bf), 1, "resolved-mixinextras", "shipped=False")

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
    expect_fail("G-bare-run-ambiguous", run(cwd=g), 1, "jar-unambiguous", "pass --jar")

    # K: malformed outer zips must be structured FAILs, never a traceback
    for label, payload in [("empty", b""),
                           ("nonzip", b"this is plain text, no zip magic at all"),
                           ("truncated", b"PK\x03\x04 truncated garbage, not a real archive")]:
        bad = os.path.join(td, "bad-" + label + ".zip")
        open(bad, "wb").write(payload)
        r = run(bad, bf)
        expect_fail("K-malformed-outer-zip-" + label, r, 1, "outer-metadata-readable")
        ok = "Traceback" not in r.stderr
        print(("PASS " if ok else "FAIL ") + "K-no-traceback-" + label)
        if not ok:
            fails.append("K-no-traceback-" + label)
            print(r.stderr[-400:])

    # L: wrong-typed metadata must be named FAILs, never tracebacks
    l1 = os.path.join(td, "l1.jar"); mod_jar(l1, True, nested_jar(), raw_outer="[]")
    r = run(l1, bf)
    expect_fail("L1-outer-non-object", r, 1, "outer-metadata-readable")
    l2 = os.path.join(td, "l2.jar")
    mod_jar(l2, True, nested_jar(),
            raw_outer=json.dumps({"id": "meshac", "depends": [DEP], "jars": [{"file": NESTED}]}))
    r = run(l2, bf)
    expect_fail("L2-depends-as-list", r, 1, "shipped-depends-typed")
    l3 = os.path.join(td, "l3.jar")
    mod_jar(l3, True, nested_jar(),
            raw_outer=json.dumps({"id": "meshac", "depends": {DEP: f">={VER}"}, "jars": [NESTED]}))
    r = run(l3, bf)
    expect_fail("L3-jars-entries-non-dict", r, 1, "shipped-jars-typed")
    l4 = os.path.join(td, "l4.jar"); mod_jar(l4, True, nested_jar(raw="[]"))
    r = run(l4, bf)
    expect_fail("L4-nested-non-object", r, 1, "resolved-mixinextras", "not a JSON object")
    for name, r in [("L1", run(l1, bf)), ("L2", run(l2, bf)), ("L3", run(l3, bf)), ("L4", run(l4, bf))]:
        ok = "Traceback" not in r.stderr
        print(("PASS " if ok else "FAIL ") + name + "-no-traceback")
        if not ok:
            fails.append(name + "-no-traceback")
            print(r.stderr[-400:])

    # M: constraint/jars boundary - named FAILs, never tracebacks
    m1 = os.path.join(td, "m1.jar")
    mod_jar(m1, True, nested_jar(),
            raw_outer=json.dumps({"id": "meshac", "depends": {DEP: f">={VER}"},
                                  "jars": [{"file": [NESTED]}]}))
    m2 = os.path.join(td, "m2.jar"); mod_jar(m2, True, nested_jar(), depends={DEP: 5})
    m3 = os.path.join(td, "m3.jar"); mod_jar(m3, True, nested_jar(), depends={DEP: ">=x.y"})
    m4 = os.path.join(td, "m4.jar"); mod_jar(m4, True, nested_jar(), depends={DEP: "[0.5,)"})
    m5 = os.path.join(td, "m5.jar")
    mod_jar(m5, True, nested_jar(), depends={DEP: f">={VER}", "fabric-api": "*"})
    m6 = os.path.join(td, "m6.jar"); mod_jar(m6, True, nested_jar(), depends={DEP: "*"})
    expect_line("M1-jars-file-non-string", run(m1, bf), 1, "FAIL shipped-jars-typed")
    expect_line("M2-constraint-non-string", run(m2, bf), 1, "FAIL shipped-constraints-typed")
    expect_line("M3-constraint-malformed", run(m3, bf), 1, "FAIL shipped-constraints-typed")
    expect_line("M4-constraint-maven-range", run(m4, bf), 1, "FAIL shipped-constraints-typed")
    expect_line("M5-valid-with-wildcard-platform", run(m5, bf), 0, "PASS shipped-constraints-typed")
    expect_line("M6-bundled-wildcard-resolves", run(m6, bf), 0, "PASS resolved-mixinextras")
    for name, jar in [("M1", m1), ("M2", m2), ("M3", m3), ("M4", m4)]:
        r = run(jar, bf)
        ok = "Traceback" not in r.stderr
        print(("PASS " if ok else "FAIL ") + name + "-no-traceback")
        if not ok:
            fails.append(name + "-no-traceback")
            print(r.stderr[-400:])

    # mutant 1: the pre-boundary checker at bc8b711 must fail M1-M4 with
    # its documented pre-boundary defect, after proving a healthy
    # baseline (fixture A). pinned by sha, not HEAD~1: after this commit
    # lands, HEAD~1 HAS the boundary checks. missing history means the
    # mutation coverage is not qualified and the suite must not pass.
    def pre_m1(r):
        ok = (r.returncode == 1 and "TypeError: unhashable type" in r.stderr
              and not has_prefix(r, "FAIL shipped-jars-typed"))
        return ok, "want exit 1 + TypeError (unhashable jars[].file), no typed check"
    def pre_m2(r):
        ok = (r.returncode == 1 and "AttributeError" in r.stderr
              and not has_prefix(r, "FAIL shipped-constraints-typed"))
        return ok, "want exit 1 + AttributeError (non-string constraint), no typed check"
    def pre_m34(r):
        resolved = [l for l in r.stdout.splitlines() if l.startswith("FAIL resolved-mixinextras")]
        ok = (r.returncode == 1 and "Traceback" not in r.stderr
              and any("shipped=False" in l for l in resolved)
              and not has_prefix(r, "FAIL shipped-constraints-typed"))
        return ok, "want exit 1, no traceback, silent shipped=False, no typed check"
    prev = subprocess.run(["git", "show",
                           "bc8b711d6fe8cbef30d546db837be0f8aa063c69:tests/check_deps.py"],
                          cwd=ROOT, capture_output=True, text=True)
    if prev.returncode != 0:
        print("NOT_QUALIFIED mutant-preboundary (pre-boundary commit not in history)")
        fails.append("mutant-preboundary-not-qualified")
    else:
        mut1 = os.path.join(td, "check_deps_pre.py")
        open(mut1, "w").write(prev.stdout)
        if mutant_baseline("mutant-preboundary", mut1, good, bf):
            mutant_kill("M1-mutant-preboundary-killed", mut1, m1, bf, pre_m1)
            mutant_kill("M2-mutant-preboundary-killed", mut1, m2, bf, pre_m2)
            mutant_kill("M3-mutant-preboundary-killed", mut1, m3, bf, pre_m34)
            mutant_kill("M4-mutant-preboundary-killed", mut1, m4, bf, pre_m34)

    # mutant 2: committed checker minus the wildcard short-circuit must
    # fail M6 with FAIL resolved-mixinextras, after a healthy baseline
    def lit_m6(r):
        ok = (r.returncode == 1 and "Traceback" not in r.stderr
              and has_prefix(r, "FAIL resolved-mixinextras"))
        return ok, "want exit 1, no traceback, FAIL resolved-mixinextras"
    cur = open(CHECK).read()
    snippet = "    if constraint.strip() == \"*\":\n        return True\n"
    assert snippet in cur, "wildcard short-circuit missing from committed checker"
    mut2 = os.path.join(td, "check_deps_literal.py")
    open(mut2, "w").write(cur.replace(snippet, ""))
    if mutant_baseline("mutant-literal-wildcard", mut2, good, bf):
        mutant_kill("M6-mutant-literal-wildcard-killed", mut2, m6, bf, lit_m6)

print("SUITE:", "all pass" if not fails else f"failures {fails}")
sys.exit(0 if not fails else 1)
