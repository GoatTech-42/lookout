#!/usr/bin/env python3
"""Frozen-victim / flat-ground combat choreography for the lookout lane rig.

Design contract (main, 2026-11-18): every combat arm needs a console-verified
static victim Pos before a hit counts as exercised. Idle players drift
server-side at the y=-60 basin (~2.9 b/s, tp alone does not pin them), so
victims are frozen with slowness 255 + gravity 0 and staged on a filled
platform, then verified static (two Pos samples 3s apart, epsilon 0.05)
before the attacker starts. The orchestrator drives the server console via
the fifo and never trusts client-side tracking for geometry.

Parser guards (each pinned by a red-before test and a mutant in
tests/test_choreo.py + tests/mut_choreo.py):
  G1 answers are matched to the queried entity name - interleaved replies for
     other entities are never accepted
  G2 payload regexes are strict and line-anchored - a truncated reply
     (missing digits, bracket or the trailing f) is rejected, never parsed
  G3 a missing reply times out and aborts the choreography - never silently
     proceeds
  G4 when several replies for the same entity arrive in one window (stale
     duplicate + fresh), the freshest wins
  G5 a static victim at the WRONG coordinates aborts (expect check) - static
     is necessary, not sufficient
  G6 non-static victims abort (drift check)
  G7 verify-arm compares the attacker distance against the reach band and
     ABORTS outside it (a 1000-block probe at max_reach 3.0 never passes)
  G8 answer lines are anchored to the server-log line origin - a chat/mod
     decoy containing the answer text is never accepted
  G9 arm receipt: post-arm victim Health must have DROPPED server-side,
     compared by check_damage_receipt - a provoke the server never felt aborts
  arms run through run_arm: STAGE -> server-verify band -> attack, fail-closed
     (QA 8.0: verifying before staging aborted every combined sequence -
     after killaura at 2.5 the reach band 4.6+ can never verify, and vice
     versa; the intended position must be walked to FIRST)

known limits (documented, not guarded):
  - "freshest" (G4) is the freshest COMPLETE line in the buffer at read time;
    a reply landing mid-read is only seen on the next poll.
  - the log file is pinned once per query, so rotation between mark and reads
    cannot swap files; a NEW run*-lane.log created during a query is followed
    only from the next query on (rotation unqualified beyond that).

Usage:
  python3 choreo.py prepare SERVERDIR VICTIMA VICTIMB   # freeze, stage, verify static
  python3 choreo.py verify-arm SERVERDIR VICTIMA VICTIMB ATTACKER DIST  # Pos checks pre-arm
  python3 choreo.py health SERVERDIR VICTIM            # last Health value
Console answers are read from SERVERDIR/run*-lane.log (newest).
"""
import glob, math, os, re, sys, time

ANS_RE = re.compile(r"^\[[0-9:]{8}\] \[[^\]]+\]: System chat: (\w+) has the following entity data: (.*)$", re.M)
POS_PAYLOAD = re.compile(r"^\[(-?\d+(?:\.\d+)?)d, (-?\d+(?:\.\d+)?)d, (-?\d+(?:\.\d+)?)d\]$")
NUM_PAYLOAD = re.compile(r"^(-?\d+\.\d+)f$")


def latest_log(serverdir):
    logs = glob.glob(os.path.join(serverdir, "run*-lane.log"))
    if not logs:
        raise SystemExit("no run*-lane.log in " + serverdir)
    return max(logs, key=os.path.getmtime)


def console(serverdir, cmd):
    with open(os.path.join(serverdir, "in.fifo"), "w") as f:
        f.write(cmd + "\n")


def answers(logpath, mark, entity):
    """Complete-line entity-data answers for `entity` written after `mark` in
    the pinned log file, oldest first. A partial trailing line is dropped (it
    may be mid-write)."""
    with open(logpath) as f:
        f.seek(mark)
        buf = f.read()
    buf = buf[: buf.rfind("\n") + 1]
    return [m.group(2).strip() for m in ANS_RE.finditer(buf) if m.group(1) == entity]


def query(serverdir, cmd, entity, kind, timeout=5.0):
    """Send a console command and return the freshest well-formed answer for
    `entity`: POS_PAYLOAD for kind 'pos', NUM_PAYLOAD otherwise. Raises
    TimeoutError when no strict answer arrives in time - the caller aborts."""
    logpath = latest_log(serverdir)  # pinned: rotation between mark and reads
    mark = os.path.getsize(logpath)  #        cannot swap files mid-query
    console(serverdir, cmd)
    rx = POS_PAYLOAD if kind == "pos" else NUM_PAYLOAD
    t0 = time.time()
    while time.time() - t0 < timeout:
        for payload in reversed(answers(logpath, mark, entity)):
            m = rx.match(payload)
            if m:
                return m
        time.sleep(0.3)
    raise TimeoutError(f"no strict {kind} answer for {entity} to: {cmd}")


def get_pos(serverdir, name, timeout=5.0):
    m = query(serverdir, f"data get entity {name} Pos", name, "pos", timeout)
    return tuple(float(g) for g in m.groups())


def get_health(serverdir, name, timeout=5.0):
    m = query(serverdir, f"data get entity {name} Health", name, "num", timeout)
    return float(m.group(1))


def verify_static(serverdir, name, gap=3.0, eps=0.05, tries=3, expect=None,
                  place_eps=0.75, timeout=5.0):
    """Two Pos samples `gap` apart must agree within eps, and land within
    place_eps of `expect` when given. Raises SystemExit otherwise."""
    for attempt in range(tries):
        a = get_pos(serverdir, name, timeout)
        time.sleep(gap)
        b = get_pos(serverdir, name, timeout)
        drift = math.dist(a, b)
        if drift > eps:
            print(f"drift {drift:.3f} > {eps} for {name} (attempt {attempt+1})")
            continue
        if expect is not None and math.dist(b, expect) > place_eps:
            raise SystemExit(
                f"victim {name} static at {tuple(round(v,2) for v in b)}, "
                f"not at expected {expect} - abort before any arm")
        print(f"static ok: {name} at {tuple(round(v,2) for v in b)} (drift {drift:.3f})")
        return b
    raise SystemExit(f"victim {name} not static - abort before any arm")


def prepare(serverdir, va, vb, platform=(190, 60, 190)):
    x, y, z = platform
    # stage a solid platform so nothing depends on basin terrain or flow
    console(serverdir, f"fill {x-3} {y-1} {z-3} {x+3} {y-1} {z+3} minecraft:stone")
    time.sleep(1)
    for n in (va, vb):
        console(serverdir, f"effect give {n} minecraft:slowness infinite 255 true")
        console(serverdir, f"attribute {n} minecraft:gravity base set 0")
        console(serverdir, f"attribute {n} minecraft:knockback_resistance base set 1")
        time.sleep(0.5)
    console(serverdir, f"tp {va} {x} {y} {z}")
    console(serverdir, f"tp {vb} {x+1.5} {y} {z+0.7}")
    time.sleep(2)
    pa = verify_static(serverdir, va, expect=(x + 0.5, y, z + 0.5))
    pb = verify_static(serverdir, vb, expect=(x + 2.0, y, z + 1.2))
    sep = math.dist(pa, pb)
    print(f"victims staged {sep:.2f} apart; frozen + static. attacker may start.")


def verify_arm(serverdir, va, vb, attacker, max_reach, min_reach=0.0, gap=2.0):
    """Pre-arm: both victims static AND attacker inside the reach band
    [min_reach, max_reach] of victim A per SERVER-SIDE Pos (client tracking
    is not evidence). ABORTS outside the band: a probe 1000 blocks out, or a
    killaura-band attacker in the reach arm, never exits normally."""
    pa = verify_static(serverdir, va, gap=gap)
    pb = verify_static(serverdir, vb, gap=gap)
    atk = get_pos(serverdir, attacker)
    da = math.dist(atk, pa)
    db = math.dist(atk, pb)
    print(f"attacker {tuple(round(v,2) for v in atk)} -> A {da:.2f}, B {db:.2f} (reach band {min_reach}..{max_reach})")
    if da > max_reach:
        raise SystemExit(f"attacker {da:.2f} from {va} exceeds max_reach {max_reach} - abort before arm")
    if da < min_reach:
        raise SystemExit(f"attacker {da:.2f} from {va} inside min_reach {min_reach} - killaura-band geometry in the wrong arm, abort")
    return da, db, pa, pb


class ArmGuard:
    """Per-arm attack-count + damage-receipt guard, shared by bot2 and tests.

    The counters are cumulative across the whole run, so end() compares
    DELTAS from the pre-arm snapshot, never totals (QA 8.2: killaura passed
    at 6, then reach aborted on 12 != 6 and multiaura on 16 != 4)."""
    def __init__(self, log=print):
        self.sent = {'VictimA': 0, 'VictimB': 0}
        self.log = log

    def hit(self, victim):
        self.sent[victim] += 1

    def begin(self, arm, health):
        """Pre-arm snapshot: victim Health map plus the counter values."""
        return {'health': dict(health), 'sent': dict(self.sent)}

    def end(self, arm, before, expect_sent, expect_damaged, receipt):
        """Post-arm: exact per-arm DELTA sent counts, and receipt(arm, victim,
        pre-arm health) must be truthy per attacked victim. SystemExit on any
        failure - an arm that fell short never exits normally."""
        ok = True
        for v, n in expect_sent.items():
            got = self.sent[v] - before['sent'].get(v, 0)
            if got != n:
                self.log(f"ARM GUARD FAIL {arm} {v}: sent {got} attacks this arm, spec wants {n}")
                ok = False
        for v in expect_damaged:
            if not receipt(arm, v, before['health'][v]):
                self.log(f"ARM GUARD FAIL {arm} {v}: no server-side damage received")
                ok = False
        if not ok:
            raise SystemExit(1)
        self.log(f"arm guard ok: {arm} - G4 note: sent counts + health delta only; "
                 f"per-hit server receipt counts UNVERIFIED at 4-tick cadence")


def run_arm(name, stage, pre_arm, attacks, post_arm):
    """Fail-closed arm sequence: STAGE the intended geometry first,
    server-verify the band SECOND, then attack. stage() returns False when
    the position cannot be staged (no victims, placement impossible) and the
    run aborts - an unexercised arm never exits normally. pre_arm() returns
    the pre-arm state handed to post_arm()."""
    if not stage():
        raise SystemExit(
            f"ARM GUARD FAIL {name}: could not stage the intended position - "
            f"arm not exercised, refusing to continue")
    before = pre_arm()
    attacks()
    post_arm(before)


def bind_victims(spawned, required=("VictimA", "VictimB")):
    """Explicit victim identity binding: name -> eid, from spawn-time UUID
    identification, never arrival order. spawned maps live eid -> victim name.
    Fails closed (ValueError) when a required name has no live eid, or when two
    live eids carry the same name (a replaced id whose predecessor was never
    seen to leave is ambiguous - never guess which one is real)."""
    by_name = {}
    for eid, name in spawned.items():
        if name in by_name:
            raise ValueError(
                f"victim {name} has two live entity ids {by_name[name]} and {eid}; "
                f"ambiguous binding, aborting instead of guessing")
        by_name[name] = eid
    out = {}
    for name in required:
        eid = by_name.get(name)
        if eid is None:
            raise ValueError(f"victim identity missing: {name} not seen")
        out[name] = eid
    return out


def check_damage_receipt(serverdir, name, before, arm, timeout=5.0):
    """Post-arm server-side receipt: victim Health must be strictly below
    `before`. Raises SystemExit otherwise - zero-delta means the provoke
    never reached the server (G3 receipt, automatic)."""
    after = get_health(serverdir, name, timeout)
    print(f"guard {arm} {name} health {before} -> {after}")
    if not after < before:
        raise SystemExit(
            f"ARM GUARD FAIL {arm} {name}: no server-side damage received "
            f"({before} -> {after})")
    return after


if __name__ == "__main__":
    cmd = sys.argv[1]
    if cmd == "prepare":
        prepare(sys.argv[2], sys.argv[3], sys.argv[4])
    elif cmd == "verify-arm":
        verify_arm(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], float(sys.argv[6]),
                   float(sys.argv[7]) if len(sys.argv) > 7 else 0.0)
    elif cmd == "health":
        print(get_health(sys.argv[2], sys.argv[3]))
    elif cmd == "receipt":
        check_damage_receipt(sys.argv[2], sys.argv[3], float(sys.argv[4]), sys.argv[5])
    else:
        raise SystemExit(__doc__)
