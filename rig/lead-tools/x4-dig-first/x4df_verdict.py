#!/usr/bin/env python3
"""Frozen X4 dig-first verdict function (r5, fail-closed). Pure; no host contact.
Input: JSON {"on":[rows],"off":[rows]}. Each leg needs exactly 6 rows = 2 ore cells x reps 1..3, unique, no unknown cells.
Common row fields (normalizer evidence): ts_unit=="epoch_ms", clock_id (non-empty string, identical across ALL rows, the server wall-clock epoch the anchors and bot receive times were normalized to), anchor_src (non-empty string naming the rig trace/line the epochs came from), cover_trace_id (non-empty string identifying the trace line of the cover-cell mutation), cover[x,y,z] must equal the expected cover of the ore cell.
ON row: cell[x,y,z], rep, initial, cover_break_ok, final_block, anchor_valid, head_epoch_ms, return_epoch_ms, recv_epoch_ms|null, send_epoch_ms|null, capture_end_epoch_ms.
OFF row: cell, rep, initial, cover_break_ok, off_mutation_return_epoch_ms (verified cover mutation), off_capture_end_epoch_ms, off_ore_updates (int count of ore block updates seen after the mutation). Frozen OFF observation window = 2500 ms.
Exit code: 0 PASS/PASS-conservative, 1 FAIL, 2 INCONCLUSIVE, 3 NOT_QUALIFIED or unreadable input. HEAD<=RETURN allowed (1 ms uncertainty, sub-ms mutation possible). Verdicts: PASS, PASS-conservative, FAIL, INCONCLUSIVE, NOT_QUALIFIED. Any malformed/inconsistent datum is NOT_QUALIFIED, never PASS."""
import json, math, sys
ORES = {(10, -20, 10), (10, -20, 26)}
COVER = {(10, -20, 10): (9, -20, 10), (10, -20, 26): (9, -20, 26)}
OFF_WINDOW_MS = 2500
def num(x): return isinstance(x, (int, float)) and not isinstance(x, bool) and math.isfinite(x) and x >= 0
def common_ok(r):
    if r.get("ts_unit") != "epoch_ms": return False
    for k in ("clock_id", "anchor_src", "cover_trace_id"):
        if not isinstance(r.get(k), str) or not r[k].strip(): return False
    try: return tuple(r["cover"]) == COVER[tuple(r["cell"])]
    except Exception: return False
def on_row(r):
    if not common_ok(r): return "NOT_QUALIFIED"
    if r.get("initial") != "deepslate": return "NOT_QUALIFIED"
    if r.get("cover_break_ok") is not True: return "NOT_QUALIFIED"
    ce, rc, s = r.get("capture_end_epoch_ms"), r.get("recv_epoch_ms"), r.get("send_epoch_ms")
    if not num(ce): return "NOT_QUALIFIED"
    if rc is not None and not num(rc): return "NOT_QUALIFIED"
    if rc is not None and rc > ce: return "NOT_QUALIFIED"
    if r.get("anchor_valid") is True:
        h, rt = r.get("head_epoch_ms"), r.get("return_epoch_ms")
        if not (num(h) and num(rt)) or not h <= rt or rt > ce: return "NOT_QUALIFIED"
        if s is not None and not num(s): return "NOT_QUALIFIED"
        if rc is None: return "FAIL" if ce - rt >= 5000 else "INCONCLUSIVE"   # FAIL only if the 5000 ms window was fully observed
        if rc < h: return "NOT_QUALIFIED"                                       # receive before the mutation: bad clock or order
        if s is not None and (not num(s) or s > rc): return "NOT_QUALIFIED"   # send optional with an anchor; if present it must be ordered
        if r.get("final_block") != "diamond_ore": return "INCONCLUSIVE"
        up, lo = rc - h + 1, rc - rt - 1
        return "PASS" if up <= 50 else "FAIL" if lo > 50 else "INCONCLUSIVE"
    if r.get("anchor_valid") is not False: return "NOT_QUALIFIED"
    if rc is None or not num(s): return "INCONCLUSIVE"
    if rc < s: return "NOT_QUALIFIED"
    if r.get("final_block") != "diamond_ore": return "INCONCLUSIVE"
    return "PASS-conservative" if rc - s <= 49 else "INCONCLUSIVE"
def off_row(r):
    if not common_ok(r) or r.get("initial") != "diamond_ore" or r.get("cover_break_ok") is not True: return "NOT_QUALIFIED"
    m, e, n = r.get("off_mutation_return_epoch_ms"), r.get("off_capture_end_epoch_ms"), r.get("off_ore_updates")
    if not (num(m) and num(e)) or e < m or isinstance(n, bool) or not isinstance(n, int) or n < 0: return "NOT_QUALIFIED"
    if n > 0: return "FAIL"
    return "OFF_OK" if e - m >= OFF_WINDOW_MS else "INCONCLUSIVE"   # bare "unchanged" never passes: the full window must be observed
def shape_ok(rows):
    if not isinstance(rows, list) or len(rows) != 6: return False
    seen = set()
    for r in rows:
        if not isinstance(r, dict): return False
        try:
            if not isinstance(r["cell"], list) or not all(isinstance(x, int) and not isinstance(x, bool) for x in r["cell"]): return False
            c = tuple(r["cell"]); k = (c, r["rep"])
        except Exception: return False
        if c not in ORES or r["rep"] not in (1, 2, 3) or isinstance(r["rep"], bool) or k in seen: return False
        seen.add(k)
    return len(seen) == 6
def leg(rows, fn, good):
    if not shape_ok(rows): return "NOT_QUALIFIED", []
    v = [fn(r) for r in rows]
    if "NOT_QUALIFIED" in v: return "NOT_QUALIFIED", v
    if "FAIL" in v: return "FAIL", v
    if all(x == good for x in v): return good, v
    if good == "PASS" and all(x in ("PASS", "PASS-conservative") for x in v): return "PASS-conservative", v
    return "INCONCLUSIVE", v
def verdict(d):
    if not isinstance(d, dict): return {"verdict": "NOT_QUALIFIED", "why": "top-level not an object"}
    try:
        rows = (d.get("on") or []) + (d.get("off") or [])
        clocks = {r.get("clock_id") for r in rows if isinstance(r, dict)}
        if len(clocks) != 1: return {"verdict": "NOT_QUALIFIED", "why": "clock_id not identical across all rows"}
        on, ov = leg(d.get("on"), on_row, "PASS"); off, fv = leg(d.get("off"), off_row, "OFF_OK")
    except Exception as ex:
        return {"verdict": "NOT_QUALIFIED", "why": "malformed input: " + type(ex).__name__}
    st = (on, off)   # global precedence across both legs: NOT_QUALIFIED, then FAIL, then INCONCLUSIVE, else the ON verdict
    if "NOT_QUALIFIED" in st: v = "NOT_QUALIFIED"
    elif "FAIL" in st: v = "FAIL"
    elif "INCONCLUSIVE" in st: v = "INCONCLUSIVE"
    else: v = on
    return {"verdict": v, "on": on, "off": off, "on_cells": ov, "off_cells": fv}
EXIT = {"PASS": 0, "PASS-conservative": 0, "FAIL": 1, "INCONCLUSIVE": 2, "NOT_QUALIFIED": 3}
C = dict(ts_unit="epoch_ms", clock_id="srv", anchor_src="rig:cover", cover_trace_id="t1", cover=[9, -20, 10])
def _on(**k):
    b = dict(C, cell=[10, -20, 10], rep=1, initial="deepslate", cover_break_ok=True, final_block="diamond_ore", anchor_valid=True,
             head_epoch_ms=1000, return_epoch_ms=1002, recv_epoch_ms=1030, send_epoch_ms=None, capture_end_epoch_ms=7000); b.update(k); return b
def _off(c, r, **k):
    b = dict(C, cell=list(c), cover=list(COVER[tuple(c)]), rep=r, initial="diamond_ore", cover_break_ok=True, off_mutation_return_epoch_ms=1000, off_capture_end_epoch_ms=3600, off_ore_updates=0); b.update(k); return b
def _mk(extra=None):
    on = [_on(cell=list(c), cover=list(COVER[c]), rep=r) for c in sorted(ORES) for r in (1, 2, 3)]
    off = [_off(c, r) for c in sorted(ORES) for r in (1, 2, 3)]
    return {"on": on, "off": off}
def selftest():
    A = on_row
    assert A(_on()) == "PASS"
    assert A(_on(recv_epoch_ms=1100)) == "FAIL"
    assert A(_on(return_epoch_ms=1040, recv_epoch_ms=1075)) == "INCONCLUSIVE"
    assert A(_on(recv_epoch_ms=900)) == "NOT_QUALIFIED"
    assert A(_on(head_epoch_ms=1100, return_epoch_ms=1002)) == "NOT_QUALIFIED"
    assert A(_on(head_epoch_ms=1000, return_epoch_ms=1000)) == "PASS"           # HEAD==RETURN allowed
    assert A(_on(anchor_valid=False, send_epoch_ms=1000, recv_epoch_ms=900)) == "NOT_QUALIFIED"
    assert A(_on(anchor_valid=False, send_epoch_ms=1000, recv_epoch_ms=1040)) == "PASS-conservative"
    assert A(_on(anchor_valid=False, send_epoch_ms=None)) == "INCONCLUSIVE"
    assert A(_on(send_epoch_ms=1010)) == "PASS"                                 # anchored, ordered send ok
    assert A(_on(send_epoch_ms=2000)) == "NOT_QUALIFIED"                        # send after recv
    assert A(_on(send_epoch_ms=float("nan"))) == "NOT_QUALIFIED"
    assert A(_on(send_epoch_ms="x")) == "NOT_QUALIFIED"
    assert A(_on(recv_epoch_ms=None)) == "FAIL"
    assert A(_on(recv_epoch_ms=None, capture_end_epoch_ms=3000)) == "INCONCLUSIVE"
    assert A(_on(initial="stone")) == "NOT_QUALIFIED"
    assert A(_on(cover_break_ok=False)) == "NOT_QUALIFIED"
    assert A(_on(final_block="deepslate")) == "INCONCLUSIVE"
    for bad in (float("nan"), float("inf"), -5, "1030", True):
        assert A(_on(recv_epoch_ms=bad)) == "NOT_QUALIFIED", bad
    assert A(_on(head_epoch_ms=None)) == "NOT_QUALIFIED"
    assert A(_on(capture_end_epoch_ms=None)) == "NOT_QUALIFIED"
    assert A(_on(recv_epoch_ms=8000)) == "NOT_QUALIFIED"
    assert A(_on(anchor_valid="yes")) == "NOT_QUALIFIED"
    for k, v in (("ts_unit", "s"), ("clock_id", ""), ("anchor_src", None), ("cover_trace_id", " "), ("cover", [9, -20, 11])):
        assert A(_on(**{k: v})) == "NOT_QUALIFIED", k
    O = off_row
    assert O(_off((10, -20, 10), 1)) == "OFF_OK"
    assert O(_off((10, -20, 10), 1, off_capture_end_epoch_ms=3400)) == "INCONCLUSIVE"   # 2400 ms < 2500 window
    assert O(_off((10, -20, 10), 1, off_ore_updates=1)) == "FAIL"
    assert O(_off((10, -20, 10), 1, off_ore_updates=None)) == "NOT_QUALIFIED"
    assert O(_off((10, -20, 10), 1, off_ore_updates=True)) == "NOT_QUALIFIED"
    assert O(_off((10, -20, 10), 1, off_capture_end_epoch_ms=500)) == "NOT_QUALIFIED"   # end before mutation
    assert O(_off((10, -20, 10), 1, off_mutation_return_epoch_ms=None)) == "NOT_QUALIFIED"
    assert O(_off((10, -20, 10), 1, off_unchanged=True, off_capture_end_epoch_ms=1001)) == "INCONCLUSIVE"  # bare boolean does not pass
    assert O(_off((10, -20, 10), 1, initial="deepslate")) == "NOT_QUALIFIED"
    assert verdict(_mk())["verdict"] == "PASS"
    d = _mk(); d["on"][1]["rep"] = 1; assert verdict(d)["verdict"] == "NOT_QUALIFIED"
    d = _mk(); d["on"] = d["on"][:5]; assert verdict(d)["verdict"] == "NOT_QUALIFIED"
    d = _mk(); d["on"][0]["cell"] = [11, -20, 10]; assert verdict(d)["verdict"] == "NOT_QUALIFIED"
    d = _mk(); del d["off"]; assert verdict(d)["verdict"] == "NOT_QUALIFIED"
    d = _mk(); d["off"][0]["off_ore_updates"] = 2; assert verdict(d)["verdict"] == "FAIL"
    d = _mk(); d["off"][0]["off_capture_end_epoch_ms"] = 1200; assert verdict(d)["verdict"] == "INCONCLUSIVE"
    d = _mk(); d["off"][0]["initial"] = "stone"; assert verdict(d)["verdict"] == "NOT_QUALIFIED"
    d = _mk(); d["on"][0]["recv_epoch_ms"] = 1100; d["on"][1]["recv_epoch_ms"] = 900; assert verdict(d)["verdict"] == "NOT_QUALIFIED"
    d = _mk(); d["on"][0]["recv_epoch_ms"] = 1100; assert verdict(d)["verdict"] == "FAIL"
    d = _mk(); d["on"][2]["clock_id"] = "other"; assert verdict(d)["verdict"] == "NOT_QUALIFIED"
    for junk in ([], 5, None, "x", {"on": 5, "off": 5}, {"on": [[]] * 6, "off": [[]] * 6}, {"on": [{"cell": [[1]], "rep": 1}] * 6, "off": []}, {"on": [{"cell": {"a": 1}, "rep": 1}] * 6, "off": []}):
        assert verdict(junk)["verdict"] == "NOT_QUALIFIED", junk
    # mixed-leg precedence (r5)
    d = _mk(); d["on"][0]["initial"] = "stone"; d["off"][0]["off_ore_updates"] = 1; assert verdict(d)["verdict"] == "NOT_QUALIFIED"   # ON NQ + OFF FAIL
    d = _mk(); d["on"][0]["cell"] = [10, -20, 10]; d["off"][0]["off_capture_end_epoch_ms"] = 1200; d["on"][0]["head_epoch_ms"] = 5; d["on"][0]["return_epoch_ms"] = 4; assert verdict(d)["verdict"] == "NOT_QUALIFIED"  # OFF short + ON invalid
    d = _mk(); d["on"][0]["recv_epoch_ms"] = 1100; d["off"][0]["off_capture_end_epoch_ms"] = 1200; assert verdict(d)["verdict"] == "FAIL"   # ON FAIL beats OFF INCONCLUSIVE
    d = _mk(); d["on"][0]["return_epoch_ms"] = 1040; d["on"][0]["recv_epoch_ms"] = 1075; d["off"][0]["off_ore_updates"] = 1; assert verdict(d)["verdict"] == "FAIL"  # ON INCONCLUSIVE + OFF FAIL
    d = _mk(); d["on"][0]["return_epoch_ms"] = 1040; d["on"][0]["recv_epoch_ms"] = 1075; assert verdict(d)["verdict"] == "INCONCLUSIVE"
    d = _mk(); d["off"][0]["initial"] = "stone"; d["on"][1]["recv_epoch_ms"] = 1100; assert verdict(d)["verdict"] == "NOT_QUALIFIED"   # OFF NQ + ON FAIL
    d = _mk()
    for r in d["on"]: r.update(anchor_valid=False, send_epoch_ms=1000, recv_epoch_ms=1040)
    assert verdict(d)["verdict"] == "PASS-conservative"
    assert EXIT["NOT_QUALIFIED"] == 3
    print("selftest OK: adversarial asserts for every r4 item, synthetic inputs only, no rig data")
if __name__ == "__main__":
    if sys.argv[1:] == ["--selftest"]: selftest()
    else:
        try: out = verdict(json.load(open(sys.argv[1])))
        except Exception as ex: out = {"verdict": "NOT_QUALIFIED", "why": "unreadable input: " + type(ex).__name__}
        print(json.dumps(out)); sys.exit(EXIT[out["verdict"]])
