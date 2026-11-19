#!/usr/bin/env python3
"""First-capture trace coverage gate (26.1.2 shim risk review R1-R9, source-only).

The 7-stimulus first-capture smoke scores shim risks by grepping watcher.log for
per-handler trace lines. A handler with NO trace line is already a silent-pass
hole: the capture cannot tell "shim starved" from "check abstained". This gate:
1. every probed engine handler emits a MESHAC_TRACE-gated trace line AT ENTRY
   (before the enabled check and every early return), carrying the probe's key
   fields (R4: typeKey; R1: speed).
2. ChatAlerter logs every alert BEFORE the gui route can swallow it (R9).
3. every shipped mixins json keeps "required": true (R8's loudness).
Run from the repo root: python3 tests/check_trace_coverage.py"""
import glob, json, re, sys

fails = []
def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + (" " + detail if detail else ""))
    if not ok: fails.append(name)

ENG = "src/main/java/dev/lookout/core/LookoutEngine.java"
src = open(ENG).read()

def body_of(signature):
    i = src.find(signature)
    if i == -1: return None
    j = src.find("{", i)
    depth, k = 1, j + 1
    while k < len(src) and depth:
        if src[k] == "{": depth += 1
        elif src[k] == "}": depth -= 1
        k += 1
    return src[j:k]

HANDLERS = {  # probe -> fields the trace line must carry
    "onVelocity(int entityId)": ["entityId"],
    "onTeleport(int entityId)": ["entityId"],
    "onSwing(int entityId)": ["entityId"],
    "onDamage(int victimId, int causeId, int directId, String typeKey)": ["typeKey"],
    "onProjectileSpawn(double x, double y, double z, double speed, int projectileId, int ownerId)": ["speed"],
    "onTotemPop(int entityId)": ["entityId"],
    "onTotemEquip(int entityId)": ["entityId"],
}
for sig, fields in HANDLERS.items():
    name = sig.split("(")[0]
    body = body_of("public void " + sig)
    if body is None:
        check(f"trace-{name}", False, "handler missing")
        continue
    tpos = body.find("trace(")
    rpos = body.find("return")
    ok = tpos != -1 and (rpos == -1 or tpos < rpos)
    missing = [f for f in fields if f not in (body[tpos:body.find("\n", tpos)] if tpos != -1 else "")]
    check(f"trace-{name}", ok and not missing,
          f"entry-trace={'yes' if ok else 'NO'} fields-missing={missing}")

check("trace-env-gated", 'System.getenv("MESHAC_TRACE")' in src,
      "trace lines must stay gated on MESHAC_TRACE")

al = open("src/main/java/dev/lookout/mc/ChatAlerter.java").read()
log_pos = al.find("LOGGER.info")
gui_pos = al.find("if (client.gui == null)")
check("R9-alert-logged-before-gui", log_pos != -1 and gui_pos != -1 and log_pos < gui_pos,
      "every alert must hit the log even when the chat route is unavailable")

for mj in sorted(glob.glob("src/main/resources/*mixins*.json")):
    data = json.load(open(mj))
    check(f"R8-required-{mj.split('/')[-1]}", data.get("required") is True)

print("SUITE:", "all pass" if not fails else f"failures {fails}")
sys.exit(0 if not fails else 1)
