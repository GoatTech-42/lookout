#!/usr/bin/env python3
"""Mutants for the choreo parser guards. Each mutant must make EXACTLY its own
guard's test(s) fail and nothing else. Run: python3 tests/mut_choreo.py"""
import os, subprocess, sys, tempfile

SRC = os.path.expanduser("~/work/lane-run/bot/choreo.py")
TEST = os.path.expanduser("~/work/lane-run/tests/test_choreo.py")

MUTANTS = {
    # G1: accept any entity's answer, not just the queried one
    "M-G1-ignore-entity-name": (
        "if m.group(1) == entity",
        "if True"),
    # G2: accept truncated payloads (no bracket / no trailing f required)
    "M-G2-loose-payload": (
        'POS_PAYLOAD = re.compile(r"^\\[(-?\\d+(?:\\.\\d+)?)d, (-?\\d+(?:\\.\\d+)?)d, (-?\\d+(?:\\.\\d+)?)d\\]$")\nNUM_PAYLOAD = re.compile(r"^(-?\\d+\\.\\d+)f$")',
        'POS_PAYLOAD = re.compile(r"^\\[(-?\\d+(?:\\.\\d+)?)d, (-?\\d+(?:\\.\\d+)?)d, (-?\\d+(?:\\.\\d+)?)d\\]?$")\nNUM_PAYLOAD = re.compile(r"^(-?\\d+\\.\\d*)f?$")'),
    # G3: a missing reply is silently tolerated instead of aborting
    "M-G3-timeout-fallback": (
        "        a = get_pos(serverdir, name, timeout)\n        time.sleep(gap)\n        b = get_pos(serverdir, name, timeout)",
        "        try:\n            a = get_pos(serverdir, name, timeout)\n        except TimeoutError:\n            a = (0.0, 0.0, 0.0)\n        time.sleep(gap)\n        try:\n            b = get_pos(serverdir, name, timeout)\n        except TimeoutError:\n            b = a"),
    # G4: oldest duplicate wins instead of freshest
    "M-G4-first-match-wins": (
        "for payload in reversed(answers(serverdir, mark, entity)):",
        "for payload in answers(serverdir, mark, entity):"),
    # G5: skip the expected-coordinates check
    "M-G5-no-coord-check": (
        "if expect is not None and math.dist(b, expect) > place_eps:",
        "if False:"),
    # G6: drift never aborts
    "M-G6-always-static": (
        "if drift > eps:",
        "if False:"),
}

# map: mutant -> test names expected to fail (exactly these)
EXPECT = {
    "M-G1-ignore-entity-name": {"G1-entity-name-matched"},
    "M-G2-loose-payload": {"G2-truncated-pos-missing-bracket", "G2-truncated-health-missing-f", "G2-truncated-health-truncated-no-f"},
    "M-G3-timeout-fallback": {"G3-missing-reply-aborts"},
    "M-G4-first-match-wins": {"G4-freshest-wins"},
    "M-G5-no-coord-check": {"G5-wrong-coords-aborts"},
    "M-G6-always-static": {"G6-drift-aborts"},
}

src = open(SRC).read()
overall = True
for name, (a, b) in MUTANTS.items():
    if a not in src:
        print(f"{name}: PATTERN-MISSING")
        overall = False
        continue
    d = tempfile.mkdtemp()
    p = os.path.join(d, "choreo.py")
    open(p, "w").write(src.replace(a, b, 1))
    r = subprocess.run([sys.executable, TEST], env={**os.environ, "CHOREO": p},
                       capture_output=True, text=True, timeout=120)
    failed = {l.split()[1] for l in r.stdout.splitlines() if l.startswith("FAIL")}
    want = EXPECT[name]
    if failed == want:
        print(f"{name}: KILLED exactly by {sorted(failed)}")
    elif failed & want == want:
        print(f"{name}: KILLED but also broke {sorted(failed - want)}")
    else:
        print(f"{name}: SURVIVED (failed={sorted(failed)})")
        overall = False
print("MUTANTS:", "all killed exactly" if overall else "PROBLEMS")
sys.exit(0 if overall else 1)
