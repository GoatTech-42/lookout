#!/usr/bin/env python3
"""Offline tests for the combat choreography parsers and verification guards.

Every guard G1-G6 has a red-before test here and a dedicated mutant in
mut_choreo.py; the mutant must make exactly its own guard's test(s) fail.
Run: python3 tests/test_choreo.py  (green against the real choreo.py)
"""
import importlib.util, os, sys, tempfile, threading

CHOREO = os.environ.get("CHOREO", os.path.expanduser("~/work/lane-run/bot/choreo.py"))
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

print("SUITE:", "all pass" if not fails else f"failures {fails}")
sys.exit(0 if not fails else 1)
