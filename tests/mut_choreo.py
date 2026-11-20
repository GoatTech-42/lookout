#!/usr/bin/env python3
"""Mutants for the choreo parser guards, arm guards, and bot2 arm wiring.
Each mutant must make EXACTLY its own test(s) fail and nothing else.
Run: python3 tests/mut_choreo.py"""
import os, subprocess, sys, tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
FILES = {
    "choreo": (os.path.join(HERE, "..", "bot", "choreo.py"), "CHOREO"),
    "bot2":   (os.path.join(HERE, "..", "bot", "bot2.py"), "BOT2"),
}
TEST = os.path.join(HERE, "test_choreo.py")

# (file, pattern, replacement)
MUTANTS = {
    # G1: accept any entity's answer, not just the queried one
    "M-G1-ignore-entity-name": ("choreo",
        "if m.group(1) == entity",
        "if True"),
    # G2: accept truncated payloads (no bracket / no trailing f required)
    "M-G2-loose-payload": ("choreo",
        'POS_PAYLOAD = re.compile(r"^\\[(-?\\d+(?:\\.\\d+)?)d, (-?\\d+(?:\\.\\d+)?)d, (-?\\d+(?:\\.\\d+)?)d\\]$")\nNUM_PAYLOAD = re.compile(r"^(-?\\d+\\.\\d+)f$")',
        'POS_PAYLOAD = re.compile(r"^\\[(-?\\d+(?:\\.\\d+)?)d, (-?\\d+(?:\\.\\d+)?)d, (-?\\d+(?:\\.\\d+)?)d\\]?$")\nNUM_PAYLOAD = re.compile(r"^(-?\\d+\\.\\d*)f?$")'),
    # G3: a missing reply is silently tolerated instead of aborting
    "M-G3-timeout-fallback": ("choreo",
        "        a = get_pos(serverdir, name, timeout)\n        time.sleep(gap)\n        b = get_pos(serverdir, name, timeout)",
        "        try:\n            a = get_pos(serverdir, name, timeout)\n        except TimeoutError:\n            a = (0.0, 0.0, 0.0)\n        time.sleep(gap)\n        try:\n            b = get_pos(serverdir, name, timeout)\n        except TimeoutError:\n            b = a"),
    # G4: oldest duplicate wins instead of freshest
    "M-G4-first-match-wins": ("choreo",
        "for payload in reversed(answers(logpath, mark, entity)):",
        "for payload in answers(logpath, mark, entity):"),
    # G5: skip the expected-coordinates check
    "M-G5-no-coord-check": ("choreo",
        "if expect is not None and math.dist(b, expect) > place_eps:",
        "if False:"),
    # G6: drift never aborts
    "M-G6-always-static": ("choreo",
        "if drift > eps:",
        "if False:"),
    # G7: attacker beyond max_reach never aborts
    "M-G7-no-max-abort": ("choreo",
        "    if da > max_reach:",
        "    if False:"),
    # G7b: attacker inside min_reach never aborts (reach arm gets killaura geometry)
    "M-G7b-no-min-abort": ("choreo",
        "    if da < min_reach:",
        "    if False:"),
    # G8: answer regex unanchored - a chat/mod decoy line is accepted
    "M-G8-unanchored-ans": ("choreo",
        'ANS_RE = re.compile(r"^\\[[0-9:]{8}\\] \\[[^\\]]+\\]: System chat: (\\w+) has the following entity data: (.*)$", re.M)',
        'ANS_RE = re.compile(r"System chat: (\\w+) has the following entity data: (.*)$", re.M)'),
    # G9: zero-delta receipt never aborts
    "M-G9-no-receipt-abort": ("choreo",
        "    if not after < before:",
        "    if False:"),
    # bot2: killaura loop drifts back to 2.0 off-spec
    "M-bot2-ka-dist-drift": ("bot2",
        "spot = near(eidA, KA_DIST)  # recompute from live ents: victims wander",
        "spot = near(eidA, 2.0)  # recompute from live ents: victims wander"),
    # bot2: killaura pre-arm guard removed
    "M-bot2-no-prearm-guard": ("bot2",
        "                   lambda: guard_begin('killaura', 3.0),\n",
        ""),
    # bot2: killaura count guard removed
    "M-bot2-no-count-guard": ("bot2",
        "lambda before: guard_end('killaura', before, {'VictimA': 6}, ('VictimA',)))",
        "None)"),
    # bot2: stage and guard args swapped (verify would run before staging)
    "M-bot2-wiring-killaura-late": ("bot2",
        "choreo.run_arm('killaura',\n                   lambda: stage_to(KA_DIST, 100.0),\n                   lambda: guard_begin('killaura', 3.0),",
        "choreo.run_arm('killaura',\n                   lambda: guard_begin('killaura', 3.0),\n                   lambda: stage_to(KA_DIST, 100.0),"),
    # bot2: no-victims path exits 0 again
    "M-bot2-open-exit": ("bot2",
        "    log('ARM GUARD FAIL: NO VICTIMS SEEN - combat arms not exercised, failing closed')\n    sys.exit(1)",
        "    log('NO VICTIMS SEEN - combat arms not exercised')"),
    # run_arm: staging failure no longer aborts
    "M-arm-stage-optional": ("choreo",
        "    if not stage():",
        "    if not stage() and False:"),
    # run_arm: attacks before the band verify
    "M-arm-verify-after-attacks": ("choreo",
        "    before = pre_arm()\n    attacks()",
        "    attacks()\n    before = pre_arm()"),
    # ArmGuard: receipt check skipped
    "M-arm-no-receipt-loop": ("choreo",
        "            if not receipt(arm, v, before['health'][v]):",
        "            if False:"),
    # ArmGuard: cumulative totals compared instead of per-arm deltas (QA 8.2)
    "M-arm-cumulative-counts": ("choreo",
        "            got = self.sent[v] - before['sent'].get(v, 0)",
        "            got = self.sent[v]"),
    # ArmGuard: short arms accepted
    "M-arm-shortcount-ok": ("choreo",
        "            if got != n:",
        "            if False:"),
    # bind_victims: arrival order decides the binding (the original bot2 defect)
    "M-bind-arrival-order": ("choreo",
        "    for name in required:\n        eid = by_name.get(name)",
        "    for name in required:\n        eid = list(spawned)[required.index(name)] if required.index(name) < len(spawned) else None"),
    # bind_victims: two live ids for one victim silently last-wins
    "M-bind-no-ambiguity-check": ("choreo",
        "        if name in by_name:\n            raise ValueError(\n                f\"victim {name} has two live entity ids {by_name[name]} and {eid}; \"\n                f\"ambiguous binding, aborting instead of guessing\")\n        by_name[name] = eid",
        "        by_name[name] = eid"),
    # bind_victims: a missing identity is accepted
    "M-bind-missing-ok": ("choreo",
        "        if eid is None:\n            raise ValueError(f\"victim identity missing: {name} not seen\")",
        "        if eid is None and False:\n            raise ValueError(f\"victim identity missing: {name} not seen\")"),
    # bot2: eidA/eidB assigned by entity arrival order again
    "M-bot2-arrival-order-binding": ("bot2",
        "        _bound = choreo.bind_victims(name_by_eid)",
        "        _bound = {'VictimA': list(ents)[0], 'VictimB': list(ents)[1] if len(ents) > 1 else None}"),
}

EXPECT = {
    "M-G1-ignore-entity-name": {"G1-entity-name-matched"},
    "M-G2-loose-payload": {"G2-truncated-pos-missing-bracket", "G2-truncated-health-missing-f", "G2-truncated-health-truncated-no-f"},
    "M-G3-timeout-fallback": {"G3-missing-reply-aborts"},
    "M-G4-first-match-wins": {"G4-freshest-wins"},
    "M-G5-no-coord-check": {"G5-wrong-coords-aborts"},
    "M-G6-always-static": {"G6-drift-aborts"},
    "M-G7-no-max-abort": {"G7-far-attacker-aborts"},
    "M-G7b-no-min-abort": {"G7b-close-attacker-aborts"},
    "M-G8-unanchored-ans": {"G8-decoy-line-rejected"},
    "M-G9-no-receipt-abort": {"G9-no-damage-aborts"},
    "M-bot2-ka-dist-drift": {"T-bot2-ka-spec-dist"},
    "M-bot2-no-prearm-guard": {"T-bot2-prearm-guard"},
    "M-bot2-no-count-guard": {"T-bot2-count-guard"},
    "M-bot2-wiring-killaura-late": {"T-bot2-wiring-killaura"},
    "M-arm-no-receipt-loop": {"T-guard-no-receipt-aborts"},
    "M-arm-cumulative-counts": {"T-guard-sequence-deltas"},
    "M-arm-shortcount-ok": {"T-guard-short-count-aborts"},
    "M-bot2-open-exit": {"T-bot2-fail-closed"},
    "M-bind-arrival-order": {"T-bind-order-ba"},
    "M-bind-no-ambiguity-check": {"T-bind-replaced-ambiguous"},
    "M-bind-missing-ok": {"T-bind-missing-a", "T-bind-missing-b"},
    "M-bot2-arrival-order-binding": {"T-bot2-identity-bound"},
    "M-arm-stage-optional": {"T-arm-stage-fail-closed"},
    "M-arm-verify-after-attacks": {"T-arm-sequence-evolving", "T-arm-band-abort-before-attacks"},
}

ONLY = set(filter(None, os.environ.get("MUT_ONLY", "").split(",")))
if ONLY:
    MUTANTS = {k: v for k, v in MUTANTS.items() if k in ONLY}
real = {k: open(p).read() for k, (p, _) in FILES.items()}
overall = True
for name, (fk, a, b) in MUTANTS.items():
    if a not in real[fk]:
        print(f"{name}: PATTERN-MISSING")
        overall = False
        continue
    d = tempfile.mkdtemp()
    env = dict(os.environ)
    for k, (p, envvar) in FILES.items():
        if k == fk:
            mp = os.path.join(d, os.path.basename(p))
            open(mp, "w").write(real[k].replace(a, b, 1))
            env[envvar] = mp
        else:
            env[envvar] = p
    r = subprocess.run([sys.executable, TEST], env=env,
                       capture_output=True, text=True, timeout=180)
    failed = {l.split()[1] for l in r.stdout.splitlines() if l.startswith("FAIL")}
    want = EXPECT[name]
    if failed == want:
        print(f"{name}: KILLED exactly by {sorted(failed)}")
    elif failed & want == want:
        print(f"{name}: KILLED but also broke {sorted(failed - want)}")
        overall = False
    else:
        print(f"{name}: SURVIVED (failed={sorted(failed)})")
        overall = False
print("MUTANTS:", "all killed exactly" if overall else "PROBLEMS")
sys.exit(0 if overall else 1)
