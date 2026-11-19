# 26.1.2 shim-delivery capture checklist (rig 3, first real session)

companion to the run-sheet gate (lookout-26-1-2-first-capture-gate-20261119.md).
trace on (MESHAC_TRACE=1). each row: stimulus, the exact watcher.log line the
7a2918f instrumentation emits, and how to read it. a PRESENT line proves the
shim DELIVERED the event (arrival), nothing more - not exactly-once (except
R3's explicit count) and never a per-hit server receipt (G4 stays UNQUALIFIED).

| # | stimulus | required line shape | read |
|---|----------|--------------------|------|
| R1 | shoot one bow arrow + one crossbow spectral arrow | TWO `[lookout-trace] onProjectileSpawn speed=<nonzero> projectile=<id> owner=<id>` | zero lines = R1 live; speed 0.0 = starved payload |
| R2 | punch the target once (knockback) | `[lookout-trace] onVelocity id=<TARGET id>` within 2 ticks of the damage line | absent or wrong-id = R2 live (velocity grace mis-keyed) |
| R3 | `/tp` the target 50 blocks once | EXACTLY ONE `[lookout-trace] onTeleport id=<TARGET id>` | none = R3 live (routing split); two = double-inject, also a finding |
| R4 | one melee hit + one fall-damage event | TWO `[lookout-trace] onDamage ... typeKey=<k>` with DISTINCT non-empty k | any empty typeKey = R4 live; the mixin loud warn (`damage-type key unwrap empty`) must ALSO be absent on a healthy build |
| R5 | give the target a totem, kill them once | `[lookout-trace] onTotemPop id=<TARGET id>` at the death tick | none = R5 live (event id 35 moved) |
| R6 | target moves a totem into offhand | `[lookout-trace] onTotemEquip id=<TARGET id>` within ~1 s | none = R6 live (slot/accessor mismatch) |
| R7 | target swings once with empty hand | `[lookout-trace] onSwing id=<TARGET id>` | none = R7 live (swing stream absent) |

plus, from the first provoke:
- R8 boot greps: zero `ERROR` lines mentioning dev.lookout mixin in watcher.log,
  AND the shipped jar's lookout.mixins.json still says `"required": true`
  (a shim diff touching required is itself the finding, no capture needed).
- R10 boot grep: the mixin apply block's compatibilityLevel matches the
  launched java (26.1.2 template, not a hardcoded literal).
- R9 dual surface: one known provoke's note appears in BOTH watcher chat and
  watcher.log; chat-only or log-only = R9 live and every chat-scored row is
  invalid (7a2918f makes log-side unconditional; a missing log line on a fired
  alert then means the sink never ran).

any missing/aberrant line: that risk is live, the build is NOT reviewable,
stop and report - do not score rows on a starved check.
