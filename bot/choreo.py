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

Usage:
  python3 choreo.py prepare SERVERDIR VICTIMA VICTIMB   # freeze, stage, verify static
  python3 choreo.py verify-arm SERVERDIR VICTIMA VICTIMB ATTACKER DIST  # Pos checks pre-arm
  python3 choreo.py health SERVERDIR VICTIM            # last Health value
Console answers are read from SERVERDIR/run*-lane.log (newest).
"""
import glob, math, os, re, sys, time

ANS_RE = re.compile(r"System chat: (\w+) has the following entity data: (.*)$", re.M)
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


def answers(serverdir, mark, entity):
    """Complete-line entity-data answers for `entity` written after `mark`,
    oldest first. A partial trailing line is dropped (it may be mid-write)."""
    with open(latest_log(serverdir)) as f:
        f.seek(mark)
        buf = f.read()
    buf = buf[: buf.rfind("\n") + 1]
    return [m.group(2).strip() for m in ANS_RE.finditer(buf) if m.group(1) == entity]


def query(serverdir, cmd, entity, kind, timeout=5.0):
    """Send a console command and return the freshest well-formed answer for
    `entity`: POS_PAYLOAD for kind 'pos', NUM_PAYLOAD otherwise. Raises
    TimeoutError when no strict answer arrives in time - the caller aborts."""
    mark = os.path.getsize(latest_log(serverdir))
    console(serverdir, cmd)
    rx = POS_PAYLOAD if kind == "pos" else NUM_PAYLOAD
    t0 = time.time()
    while time.time() - t0 < timeout:
        for payload in reversed(answers(serverdir, mark, entity)):
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


def verify_arm(serverdir, va, vb, attacker, max_reach):
    """Pre-arm: both victims static AND attacker within max_reach of victim A
    per SERVER-SIDE Pos (client tracking is not evidence)."""
    pa = verify_static(serverdir, va, gap=2.0)
    pb = verify_static(serverdir, vb, gap=2.0)
    atk = get_pos(serverdir, attacker)
    da = math.dist(atk, pa)
    db = math.dist(atk, pb)
    print(f"attacker {tuple(round(v,2) for v in atk)} -> A {da:.2f}, B {db:.2f} (reach {max_reach})")
    return da, db, pa, pb


if __name__ == "__main__":
    cmd = sys.argv[1]
    if cmd == "prepare":
        prepare(sys.argv[2], sys.argv[3], sys.argv[4])
    elif cmd == "verify-arm":
        verify_arm(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], float(sys.argv[6]))
    elif cmd == "health":
        print(get_health(sys.argv[2], sys.argv[3]))
    else:
        raise SystemExit(__doc__)
