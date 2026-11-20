#!/usr/bin/env python3
"""Fixtures for tests/check_mixins_config.py.

Synthetic jars in a temp dir; the gate is caller-bound (--jar,
--java-min). Every fixture asserts a line-prefix FAIL/PASS and never a
traceback. The mutant control strips the compatibility-level check and
must let the wrong-level fixture pass, after proving a healthy baseline
on the valid fixture. Run from the repo root:
python3 tests/test_check_mixins_config.py"""
import json, os, subprocess, sys, tempfile, zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GATE = os.path.join(ROOT, "tests", "check_mixins_config.py")

GOOD = {"required": True, "package": "dev.lookout.mixin",
        "compatibilityLevel": "JAVA_21", "client": ["ClientPacketListenerMixin"],
        "injectors": {"defaultRequire": 1}}

def make_jar(path, cfg="absent"):
    with zipfile.ZipFile(path, "w") as z:
        if cfg == "absent":
            z.writestr("dummy.txt", "x")
        elif isinstance(cfg, bytes):
            z.writestr("lookout.mixins.json", cfg)
        else:
            z.writestr("lookout.mixins.json", json.dumps(cfg))

def run(jar, java_min=21, script=GATE):
    return subprocess.run([sys.executable, script, "--jar", jar, "--java-min", str(java_min)],
                          cwd=ROOT, capture_output=True, text=True)

def has_prefix(res, prefix):
    return any(l.startswith(prefix) for l in res.stdout.splitlines())

fails = []
def expect_fail(name, res, want_exit, check_name):
    ok = res.returncode == want_exit and has_prefix(res, "FAIL " + check_name)
    print(("PASS " if ok else "FAIL ") + name
          + f" (exit={res.returncode}, want {want_exit}, FAIL {check_name})")
    if not ok:
        fails.append(name); print((res.stdout + res.stderr)[-400:])

def expect_pass(name, res):
    ok = res.returncode == 0 and has_prefix(res, "PASS mixins-config-present")
    print(("PASS " if ok else "FAIL ") + name + f" (exit={res.returncode}, want 0)")
    if not ok:
        fails.append(name); print((res.stdout + res.stderr)[-400:])

with tempfile.TemporaryDirectory() as td:
    def jar(name, cfg):
        p = os.path.join(td, name + ".jar"); make_jar(p, cfg); return p
    f1 = jar("f1", dict(GOOD))
    f2 = jar("f2", {**GOOD, "required": False})
    f3 = jar("f3", {**GOOD, "compatibilityLevel": "JAVA_17"})
    f4 = jar("f4", {**GOOD, "client": ["OtherMixin"]})
    f5 = jar("f5", "absent")
    f6 = jar("f6", b"{not json")
    f7 = jar("f7", [1, 2])
    f8 = jar("f8", b"null")

    expect_pass("F1-valid-config", run(f1))
    expect_fail("F2-required-false", run(f2), 1, "mixins-required")
    expect_fail("F3-wrong-compat-level", run(f3), 1, "mixins-compatibility-level")
    expect_fail("F4-mixin-missing", run(f4), 1, "mixins-client-array")
    expect_fail("F5-config-absent", run(f5), 1, "mixins-config-present")
    expect_fail("F6-malformed-json", run(f6), 1, "mixins-config-parseable")
    expect_fail("F7-config-non-object", run(f7), 1, "mixins-config-parseable")
    expect_fail("F8-config-null", run(f8), 1, "mixins-config-parseable")
    for name, p in [("F2", f2), ("F3", f3), ("F4", f4), ("F5", f5), ("F6", f6), ("F7", f7), ("F8", f8)]:
        r = run(p)
        ok = "Traceback" not in r.stderr
        print(("PASS " if ok else "FAIL ") + name + "-no-traceback")
        if not ok:
            fails.append(name + "-no-traceback"); print(r.stderr[-400:])

    # mutant: gate minus the compatibility-level check must let F3 pass,
    # after a healthy baseline on F1
    cur = open(GATE).read()
    block = '''        check("mixins-compatibility-level", cfg.get("compatibilityLevel") == want_level,
              f"compatibilityLevel={cfg.get('compatibilityLevel')!r} want {want_level}")
'''
    assert block in cur, "compatibility-level check missing from committed gate"
    mut = os.path.join(td, "check_mixins_config_nocompat.py")
    open(mut, "w").write(cur.replace(block, ""))
    b = run(f1, script=mut)
    ok = b.returncode == 0 and "Traceback" not in b.stderr
    print(("PASS " if ok else "FAIL ") + f"mutant-no-compatcheck-baseline-healthy (exit={b.returncode})")
    if not ok:
        fails.append("mutant-no-compatcheck-baseline-healthy")
    r = run(f3, script=mut)
    ok = (r.returncode == 0 and "Traceback" not in r.stderr
          and not has_prefix(r, "FAIL mixins-compatibility-level"))
    print(("PASS " if ok else "FAIL ") + "F3-mutant-no-compatcheck-killed"
          + " (want exit 0, no compat FAIL: wrong level slips through)")
    if not ok:
        fails.append("F3-mutant-no-compatcheck-killed")

    # mutant 2: reinstate the v1 None-sentinel bypass (parsed null
    # conflated with no parsed object); the null fixture must then exit 0
    sentinel = """    try:
        cfg = json.loads(raw)
    except (json.JSONDecodeError, UnicodeDecodeError):
        check(\"mixins-config-parseable\", False, \"lookout.mixins.json is not valid JSON\")
    if cfg is not None and not isinstance(cfg, dict):
        check(\"mixins-config-parseable\", False, \"lookout.mixins.json is not a JSON object\")
        cfg = None
    if cfg is not None:
"""
    fixed = """    parsed = False
    try:
        cfg = json.loads(raw)
        parsed = True
    except (json.JSONDecodeError, UnicodeDecodeError):
        check(\"mixins-config-parseable\", False, \"lookout.mixins.json is not valid JSON\")
    if parsed and not isinstance(cfg, dict):
        # JSON null/false/0 parse fine but are not a config; a None
        # sentinel here would conflate parsed-null with unparseable
        check(\"mixins-config-parseable\", False, \"lookout.mixins.json is not a JSON object\")
        parsed = False
    if parsed:
"""
    assert fixed in cur, "fixed parse block missing from committed gate"
    mut2 = os.path.join(td, "check_mixins_config_sentinel.py")
    open(mut2, "w").write(cur.replace(fixed, sentinel))
    b = run(f1, script=mut2)
    ok = b.returncode == 0 and "Traceback" not in b.stderr
    print(("PASS " if ok else "FAIL ") + f"mutant-sentinel-baseline-healthy (exit={b.returncode})")
    if not ok:
        fails.append("mutant-sentinel-baseline-healthy")
    r = run(f8, script=mut2)
    ok = (r.returncode == 0 and "Traceback" not in r.stderr
          and not has_prefix(r, "FAIL mixins-config-parseable"))
    print(("PASS " if ok else "FAIL ") + "F8-mutant-sentinel-bypass-killed"
          + " (want exit 0: null config slips through the sentinel)")
    if not ok:
        fails.append("F8-mutant-sentinel-bypass-killed")

print("SUITE:", "all pass" if not fails else f"failures {fails}")
sys.exit(0 if not fails else 1)
