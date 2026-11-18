#!/usr/bin/env python3
"""X11v2 rig.jsonl contract validator + pair timeline. Pure parser, no server contact.
Usage: x11v2_parse.py FILE.jsonl   |   x11v2_parse.py --selftest
Contract (from the X11v2 patch): ev=visit {t,ep,n,viewers[],resumes[]}; ev=pair {t,ep,viewer,ent,d,vis,first,f,p,why};
ev=watch {t,ep,ns,pos[3],block}; ev=eval gains f,p (X7c stack: also c,us,ph).
first: -2 LOS not run (why 1/2), -1 ran and all blocked, 0-12 eye origin, 13-25 velocity-led origin. why: 0 none,1 near,2 exempt,3 combat.
pair.vis is RAW LOS (not combat-forced). Rig ON = instrumented timing, never cost-neutral."""
import json, sys
REQ = {"visit": {"t": int, "ep": int, "n": int, "viewers": list, "resumes": list},
       "pair": {"t": int, "ep": int, "viewer": str, "ent": str, "d": (int, float), "vis": bool, "first": int, "f": int, "p": int, "why": int},
       "watch": {"t": int, "ep": int, "ns": int, "pos": list, "block": str},
       "eval": {"t": int, "viewer": int, "ent": int, "vis": bool, "act": str, "f": int, "p": int}}
def check(o):
    errs = []; ev = o.get("ev")
    if o.get("v") != 1: errs.append("v!=1")
    for k, ty in REQ.get(ev, {}).items():
        if k not in o: errs.append(f"{ev}: missing {k}")
        elif not isinstance(o[k], ty) or (ty is int and isinstance(o[k], bool)): errs.append(f"{ev}: {k} type")
    if ev == "pair" and not errs:
        if o["why"] not in (0, 1, 2, 3): errs.append("pair: why range")
        if not -2 <= o["first"] <= 25: errs.append("pair: first range")
        if o["why"] in (1, 2) and o["first"] != -2: errs.append("pair: why1/2 needs first=-2")
        if o["why"] in (0, 3) and o["first"] == -2: errs.append("pair: LOS ran but first=-2")
        if o["vis"] and o["why"] in (0, 3) and o["first"] < 0: errs.append("pair: vis without clear sample")
        if not o["vis"] and o["why"] in (0, 3) and o["first"] >= 0: errs.append("pair: clear sample but vis false")
    if ev == "visit" and not errs:
        if o["n"] != len(o["viewers"]): errs.append("visit: n != len(viewers)")
        if any(not isinstance(x, int) or x < -1 for x in o["resumes"]): errs.append("visit: resumes")
    if ev == "watch" and not errs:
        if len(o["pos"]) != 3: errs.append("watch: pos")
        if not (o["block"] in ("UNLOADED", "OUT_OF_BOUNDS") or ":" in o["block"]): errs.append("watch: block id")
    return errs
def load(lines):
    recs, bad = [], []
    for i, ln in enumerate(lines, 1):
        ln = ln.strip()
        if not ln: continue
        try: o = json.loads(ln)
        except ValueError: bad.append((i, ["bad json"])); continue
        e = check(o)
        (bad.append((i, e)) if e else recs.append(o))
    return recs, bad
def timeline(recs, viewer, ent):
    """Per tracked pair: first tick vis, ticks of evaluation, gaps between evaluations, fails at each, HIDE/SHOW ticks if eval lines exist."""
    P = [r for r in recs if r["ev"] == "pair" and r["viewer"] == viewer and r["ent"] == ent]
    ts = [r["t"] for r in P]
    gaps = [b - a for a, b in zip(ts, ts[1:])]
    return {"evals": len(P), "gaps": gaps, "max_gap": max(gaps) if gaps else None,
            "first_clear_tick": next((r["t"] for r in P if r["vis"] and r["why"] in (0, 3)), None),
            "fails_series": [(r["t"], r["f"]) for r in P]}
def visit_coverage(recs, ids):
    V = [r for r in recs if r["ev"] == "visit"]
    return {i: sum(1 for r in V if i in r["viewers"]) for i in ids}, len(V)
def report(lines):
    recs, bad = load(lines)
    c = {}
    for r in recs: c[r["ev"]] = c.get(r["ev"], 0) + 1
    return {"records": len(recs), "by_ev": c, "bad": bad[:20], "bad_n": len(bad)}
FIX = [  # SYNTHETIC parser fixture, not data from any rig run
 '{"v":1,"ev":"visit","t":10,"ep":1000,"n":2,"viewers":[3,4],"resumes":[-1]}',
 '{"v":1,"ev":"pair","t":10,"ep":1000,"viewer":"A","ent":"B","d":20.0,"vis":false,"first":-1,"f":1,"p":0,"why":0}',
 '{"v":1,"ev":"pair","t":14,"ep":1200,"viewer":"A","ent":"B","d":19.0,"vis":true,"first":5,"f":0,"p":1,"why":0}',
 '{"v":1,"ev":"pair","t":15,"ep":1250,"viewer":"A","ent":"B","d":6.0,"vis":true,"first":-2,"f":0,"p":2,"why":1}',
 '{"v":1,"ev":"watch","t":10,"ep":1000,"ns":5,"pos":[1,2,3],"block":"minecraft:stone"}',
 '{"v":1,"ev":"watch","t":12,"ep":1100,"ns":9,"pos":[1,2,3],"block":"UNLOADED"}',
 '{"v":1,"ev":"eval","t":10,"viewer":3,"ent":4,"vis":false,"act":"NONE","f":1,"p":0}']
BAD = ['{"v":1,"ev":"pair","t":1,"ep":1,"viewer":"A","ent":"B","d":3,"vis":true,"first":4,"f":0,"p":0,"why":1}',
 '{"v":1,"ev":"visit","t":1,"ep":1,"n":3,"viewers":[1],"resumes":[-1]}',
 '{"v":1,"ev":"watch","t":1,"ep":1,"ns":1,"pos":[1,2],"block":"stone"}',
 '{"v":1,"ev":"pair","t":1,"ep":1,"viewer":"A","ent":"B","d":3,"vis":true,"first":-1,"f":0,"p":0,"why":0}', 'not json']
def selftest():
    r, b = load(FIX); assert not b and len(r) == 7, b
    r2, b2 = load(BAD); assert len(b2) == 5 and not r2, b2
    t = timeline(r, "A", "B"); assert t["evals"] == 3 and t["max_gap"] == 4 and t["first_clear_tick"] == 14, t
    cov, n = visit_coverage(r, [3, 4, 9]); assert cov == {3: 1, 4: 1, 9: 0} and n == 1
    print("selftest OK: fixture 7/7 valid, 5/5 bad rejected, timeline + coverage as expected (synthetic fixture, no rig data)")
if __name__ == "__main__":
    if sys.argv[1:] == ["--selftest"]: selftest()
    else: print(json.dumps(report(open(sys.argv[1])), indent=1))
