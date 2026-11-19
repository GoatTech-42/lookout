#!/usr/bin/env python3
"""Offline tests for the combat choreography parsers and verification guards.

Every guard G1-G6 has a red-before test here and a dedicated mutant in
mut_choreo.py; the mutant must make exactly its own guard's test(s) fail.
Run: python3 tests/test_choreo.py  (green against the real choreo.py)
"""
import importlib.util, os, sys, tempfile, threading

HERE = os.path.dirname(os.path.abspath(__file__))
CHOREO = os.environ.get("CHOREO", os.path.join(HERE, "..", "bot", "choreo.py"))
spec = importlib.util.spec_from_file_location("choreo", CHOREO)
choreo = importlib.util.module_from_spec(spec)
spec.loader.exec_module(choreo)

fails = []
def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + (" " + detail if detail else ""))
    if not ok:
        fails.append(name)


class FakeServer:
    """Fifo-backed fake console. One queued entry per console command; an entry
    may be one line or a list of lines (interleaved replies in one window).
    With repeat_last=False a command past the queue gets NO reply."""
    def __init__(self, entries, repeat_last=False):
        self.dir = tempfile.mkdtemp()
        self.replies = []
        for e in entries:
            e = [e] if isinstance(e, str) else e
            self.replies.append("".join(l + "\n" for l in e))
        self.repeat_last = repeat_last
        self.log = os.path.join(self.dir, "run9-lane.log")
        open(self.log, "w").write("")
        self.fifo = os.path.join(self.dir, "in.fifo")
        os.mkfifo(self.fifo)
        threading.Thread(target=self._srv, daemon=True).start()

    def _srv(self):
        i = 0
        while True:
            with open(self.fifo) as f:
                f.read()
            if i < len(self.replies):
                r = self.replies[i]
            elif self.repeat_last:
                r = self.replies[-1]
            else:
                i += 1
                continue
            with open(self.log, "a") as f:
                f.write(r)
            i += 1


def line(entity, payload):
    return f"[00:00:00] [Server thread/INFO]: System chat: {entity} has the following entity data: {payload}"


# --- base parser checks -------------------------------------------------
m = choreo.POS_PAYLOAD.match("[-47.5d, -60.0d, 8.5d]")
check("pos-parse", m and tuple(float(g) for g in m.groups()) == (-47.5, -60.0, 8.5))
m = choreo.NUM_PAYLOAD.match("19.8f")
check("health-parse-frac", m and abs(float(m.group(1)) - 19.8) < 1e-9)

# --- G1: answers are matched to the queried entity name -----------------
fs = FakeServer([[line("VictimA", "[-47.5d, -60.0d, 8.5d]"),
                  line("VictimB", "[-99.0d, -60.0d, -99.0d]")]])
# one reply window carries VictimB's answer first, then VictimA's; only
# VictimA's may be accepted for a VictimA query
try:
    pos = choreo.get_pos(fs.dir, "VictimA", timeout=3.0)
    check("G1-entity-name-matched", pos == (-47.5, -60.0, 8.5), str(pos))
except TimeoutError:
    check("G1-entity-name-matched", False, "timed out waiting for the right entity")

# --- G2: truncated replies are rejected ---------------------------------
for payload, tag in [("[-47.5d, -60.0d, 8.5d", "pos-missing-bracket"),
                     ("[-47.5d, -60.0d,", "pos-missing-coord"),
                     ("19.", "health-missing-f"),
                     ("19.8", "health-truncated-no-f")]:
    fs = FakeServer([line("VictimA", payload)])
    try:
        choreo.get_pos(fs.dir, "VictimA", timeout=1.2) if "pos" in tag else choreo.get_health(fs.dir, "VictimA", timeout=1.2)
        check("G2-truncated-" + tag, False, "accepted truncated payload")
    except TimeoutError:
        check("G2-truncated-" + tag, True)

# --- G3: a missing reply times out and aborts ---------------------------
fs = FakeServer([line("VictimA", "[-47.5d, -60.0d, 8.5d]")])
try:
    choreo.verify_static(fs.dir, "VictimA", gap=0.1, timeout=0.8, tries=1)
    # first sample consumed the only reply; second sample must time out
    check("G3-missing-reply-aborts", False, "proceeded without the second sample")
except TimeoutError:
    check("G3-missing-reply-aborts", True)
except SystemExit:
    check("G3-missing-reply-aborts", True)
except Exception as e:
    check("G3-missing-reply-aborts", False, "crashed instead of a clean abort: " + type(e).__name__)

# --- G4: freshest duplicate wins ----------------------------------------
fs = FakeServer([[line("VictimA", "[-61.0d, -60.0d, 5.0d]"),
                  line("VictimA", "[-47.5d, -60.0d, 8.5d]")]])
pos = choreo.get_pos(fs.dir, "VictimA", timeout=3.0)
check("G4-freshest-wins", pos == (-47.5, -60.0, 8.5), str(pos))

# --- G5: static but wrong coordinates aborts ----------------------------
fs = FakeServer([line("VictimA", "[-61.0d, -60.0d, 5.0d]"),
                 line("VictimA", "[-61.0d, -60.0d, 5.0d]")])
try:
    choreo.verify_static(fs.dir, "VictimA", gap=0.1, tries=1, expect=(-47.5, -60.0, 8.5))
    check("G5-wrong-coords-aborts", False, "static-at-wrong-place accepted")
except SystemExit:
    check("G5-wrong-coords-aborts", True)

# --- G6: non-static victim aborts (control from the live drift) ---------
fs = FakeServer([line("VictimA", "[-61.0d, -60.0d, 14.4d]"),
                 line("VictimA", "[-61.0d, -60.0d, 2.9d]")])
try:
    choreo.verify_static(fs.dir, "VictimA", gap=0.1, tries=1)
    check("G6-drift-aborts", False, "drifting victim accepted")
except SystemExit:
    check("G6-drift-aborts", True)

# --- green control: static at the right place is accepted ---------------
fs = FakeServer([line("VictimA", "[-47.5d, -60.0d, 8.5d]"),
                 line("VictimA", "[-47.5d, -60.0d, 8.5d]")])
pos = choreo.verify_static(fs.dir, "VictimA", gap=0.1, tries=1, expect=(-47.5, -60.0, 8.5))
check("green-static-at-place-accepted", pos == (-47.5, -60.0, 8.5), str(pos))

# --- G7: verify-arm compares attacker distance against the reach band -----
def arm_fs(atk_payload):
    return FakeServer([line("VictimA", "[-47.5d, -60.0d, 8.5d]"),
                       line("VictimA", "[-47.5d, -60.0d, 8.5d]"),
                       line("VictimB", "[-46.0d, -60.0d, 9.2d]"),
                       line("VictimB", "[-46.0d, -60.0d, 9.2d]"),
                       line("ProvokeBot", atk_payload)])

fs = arm_fs("[952.5d, -60.0d, 8.5d]")
try:
    choreo.verify_arm(fs.dir, "VictimA", "VictimB", "ProvokeBot", 3.0, gap=0.05)
    check("G7-far-attacker-aborts", False, "1000-block probe exited normally")
except SystemExit:
    check("G7-far-attacker-aborts", True)

fs = arm_fs("[-46.6d, -60.0d, 8.5d]")  # 0.9 from A: killaura band in the reach arm
try:
    choreo.verify_arm(fs.dir, "VictimA", "VictimB", "ProvokeBot", 6.7, min_reach=4.6, gap=0.05)
    check("G7b-close-attacker-aborts", False, "killaura-band geometry accepted for the reach arm")
except SystemExit:
    check("G7b-close-attacker-aborts", True)

fs = arm_fs("[-42.3d, -60.0d, 8.5d]")  # 5.2 from A: inside [4.6, 6.7]
da, db, pa, pb = choreo.verify_arm(fs.dir, "VictimA", "VictimB", "ProvokeBot", 6.7, min_reach=4.6, gap=0.05)
check("G7-green-within-band-accepted", abs(da - 5.2) < 0.05, f"da={da:.2f}")

# --- G8: decoy chat/mod lines containing the answer text are rejected -----
decoy = ("[00:00:00] [Server thread/INFO]: <EvilBot> System chat: "
         "VictimA has the following entity data: [-1.0d, -1.0d, -1.0d]")
fs = FakeServer([[line("VictimA", "[-47.5d, -60.0d, 8.5d]"), decoy]])
pos = choreo.get_pos(fs.dir, "VictimA", timeout=3.0)
check("G8-decoy-line-rejected", pos == (-47.5, -60.0, 8.5), str(pos))

# --- bot2 arm alignment + automatic guards (source gates) ------------------
BOT2 = os.environ.get("BOT2", os.path.join(HERE, "..", "bot", "bot2.py"))
b2 = open(BOT2).read()
check("T-bot2-ka-spec-dist",
      "KA_DIST = 2.5" in b2 and "near(eidA, 2.0)" not in b2 and "near(eidL, 2.0)" not in b2,
      "killaura placements must all use KA_DIST = 2.5 (spec P1)")
check("T-bot2-prearm-guard",
      b2.count("guard_begin(") >= 4 and "args = ['verify-arm', SERVERDIR" in b2,
      f"guard_begin occurrences: {b2.count('guard_begin(')}")
check("T-bot2-count-guard",
      b2.count("guard_end(") >= 4 and "ArmGuard(" in b2 and "guard.hit(victim)" in b2,
      f"guard_end occurrences: {b2.count('guard_end(')}; bot2 must use the shared ArmGuard counter")

# --- G9: post-arm server-side damage receipt ------------------------------
fs = FakeServer([line("VictimA", "14.0f")])
after = choreo.check_damage_receipt(fs.dir, "VictimA", 20.0, "killaura", timeout=3.0)
check("G9-damage-received-accepted", after == 14.0, str(after))
fs = FakeServer([line("VictimA", "20.0f")])
try:
    choreo.check_damage_receipt(fs.dir, "VictimA", 20.0, "killaura", timeout=3.0)
    check("G9-no-damage-aborts", False, "zero-delta provoke accepted")
except SystemExit:
    check("G9-no-damage-aborts", True)

# --- whole-arm wiring: guard_begin -> attacks -> guard_end, per arm --------
for arm in ("killaura", "reach", "multiaura"):
    i = b2.find(f"choreo.run_arm('{arm}'")
    seg = b2[i:i + 500] if i != -1 else ""
    st, gb, ge = seg.find("stage_to("), seg.find(f"guard_begin('{arm}'"), seg.find(f"guard_end('{arm}'")
    if gb == -1 or ge == -1:
        # presence is owned (and mutant-pinned) by the guard-count gates
        check(f"T-bot2-wiring-{arm}", True, "presence owned by the guard-count gates")
    else:
        check(f"T-bot2-wiring-{arm}", 0 <= st < gb < ge,
              "run_arm args must be stage_to -> guard_begin -> guard_end")
check("T-bot2-fail-closed",
      "ARM GUARD FAIL: NO VICTIMS SEEN" in b2 and "sys.exit(1)" in b2,
      "no victims or an unstaged arm must fail closed, never exit 0")
check("T-bot2-guard-end-receipts",
      "guard.end(arm, before, expect_sent, expect_damaged, receipt)" in b2
      and "for v in expect_damaged:" in open(CHOREO).read().split("def end")[1],
      "guard_end must route every attacked victim through the choreo receipt check")

# --- run_arm: stage -> verify -> attacks, positions evolving across arms --
class Rig:
    def __init__(self):
        self.d = None
        self.events = []

def mk_stage(rig, d, ok=True):
    def s():
        if not ok:
            return False
        rig.d = d
        rig.events.append(("stage", d))
        return True
    return s

def mk_verify(rig, lo, hi):
    def v():
        rig.events.append(("verify", rig.d))
        if not (lo <= rig.d <= hi):
            raise SystemExit(f"band {lo}..{hi}")
        return {"VictimA": 20.0}
    return v

def mk_attacks(rig, n):
    def a():
        rig.events.append(("attacks", n))
    return a

def mk_post(rig):
    def p(before):
        rig.events.append(("post", sorted(before)))
    return p

rig = Rig()
choreo.run_arm("killaura", mk_stage(rig, 2.5), mk_verify(rig, 0, 3.0), mk_attacks(rig, 6), mk_post(rig))
choreo.run_arm("reach", mk_stage(rig, 5.2), mk_verify(rig, 4.6, 6.7), mk_attacks(rig, 6), mk_post(rig))
choreo.run_arm("multiaura", mk_stage(rig, 2.5), mk_verify(rig, 0, 3.0), mk_attacks(rig, 8), mk_post(rig))
check("T-arm-sequence-evolving", rig.events == [
    ("stage", 2.5), ("verify", 2.5), ("attacks", 6), ("post", ["VictimA"]),
    ("stage", 5.2), ("verify", 5.2), ("attacks", 6), ("post", ["VictimA"]),
    ("stage", 2.5), ("verify", 2.5), ("attacks", 8), ("post", ["VictimA"])],
    str(rig.events))

rig = Rig()
try:
    choreo.run_arm("killaura", mk_stage(rig, None, ok=False), mk_verify(rig, 0, 3.0), mk_attacks(rig, 6), mk_post(rig))
    check("T-arm-stage-fail-closed", False, "unstaged arm proceeded")
except SystemExit:
    check("T-arm-stage-fail-closed", rig.events == [], str(rig.events))
except Exception as e:
    check("T-arm-stage-fail-closed", False, "wrong failure type: " + type(e).__name__)

# QA 8.0 defect repro: attacker still at the killaura spot (2.5) when the
# reach arm's band is verified -> abort, and no attack may have run
rig = Rig()
try:
    choreo.run_arm("reach", mk_stage(rig, 2.5), mk_verify(rig, 4.6, 6.7), mk_attacks(rig, 6), mk_post(rig))
    check("T-arm-band-abort-before-attacks", False, "attacked outside the band")
except SystemExit:
    check("T-arm-band-abort-before-attacks", rig.events == [("stage", 2.5), ("verify", 2.5)], str(rig.events))

# --- ArmGuard whole-sequence: per-arm DELTAS through the real plumbing ----
class FakeIO:
    def __init__(self):
        self.health = {'VictimA': 20.0, 'VictimB': 20.0}
    def receipt(self, arm, v, before_health):
        return self.health[v] < before_health

def drive_arm(g, io, arm, hits, expect):
    before = g.begin(arm, dict(io.health))
    for v, n in hits.items():
        for _ in range(n):
            g.hit(v)
            io.health[v] -= 1.0
    g.end(arm, before, expect, tuple(hits), io.receipt)

# QA 8.2 red-before: on a337f8a semantics (cumulative compare) this aborts
# at reach with sent=12 != 6. With delta snapshots the full sequence passes.
io = FakeIO()
g = choreo.ArmGuard()
try:
    drive_arm(g, io, 'killaura', {'VictimA': 6}, {'VictimA': 6})
    drive_arm(g, io, 'reach', {'VictimA': 6}, {'VictimA': 6})
    drive_arm(g, io, 'multiaura', {'VictimA': 4, 'VictimB': 4}, {'VictimA': 4, 'VictimB': 4})
    check("T-guard-sequence-deltas", g.sent == {'VictimA': 16, 'VictimB': 4}, str(g.sent))
except SystemExit:
    check("T-guard-sequence-deltas", False, "aborted mid-sequence (cumulative compare?)")

io = FakeIO()
g = choreo.ArmGuard()
try:
    drive_arm(g, io, 'killaura', {'VictimA': 5}, {'VictimA': 6})
    check("T-guard-short-count-aborts", False, "5 sent accepted as 6")
except SystemExit:
    check("T-guard-short-count-aborts", True)

io = FakeIO()
g = choreo.ArmGuard()
def flat_receipt(arm, v, before_health):
    return False  # server felt nothing
try:
    before = g.begin('killaura', dict(io.health))
    for _ in range(6):
        g.hit('VictimA')
    g.end('killaura', before, {'VictimA': 6}, ('VictimA',), flat_receipt)
    check("T-guard-no-receipt-aborts", False, "zero-delta arm accepted")
except SystemExit:
    check("T-guard-no-receipt-aborts", True)

print("SUITE:", "all pass" if not fails else f"failures {fails}")
sys.exit(0 if not fails else 1)
