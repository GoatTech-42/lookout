# 26.1.2 run-sheet gate: one-pass first-capture smoke (from arch shim risk review e3d0a2e2)

Fold into QA's 26.1.2 run sheet (veil-run-sheet-26-1-2-4d243bbb.md) as a gate
BEFORE any row verdict. QA owns the run sheet; this is the lane's ready-to-insert
block, per main 2026-11-18 23:56.

## gate: 7-stimulus first-capture smoke (~10 min, trace on)

Run before scoring any R1-R22 row on 26.1.2. Require all 7 capture lines:

1. bow arrow            -> R1 first capture
2. punch for knockback  -> R2 first capture
3. /tp 50 blocks        -> R3 first capture
4. melee hit + fall dmg -> R4 first capture (damage-type unwrap path; empty unwrap
                           must ALSO print the new loud warn, see below)
5. totem pop            -> R5 first capture
6. totem to offhand     -> R6 first capture
7. empty-hand swing     -> R7 first capture

Plus, from the first provoke:
- boot grep: shim binding lines              -> R8
- boot grep: mixin apply block, compatibilityLevel matches launched java -> R10
- dual-surface alert check                   -> R9

Any missing line: that risk is live, the build is NOT reviewable. Stop and report.
Do not score rows on a starved check.

## R4 companion change (landed with this doc)

ClientPacketListenerMixin.onClientboundDamageEvent now warns loudly when
packet.sourceType().unwrapKey() is empty, before falling back to the empty-string
typeKey. Gate: tests/check_r4_loudness.py (mutant: warn stripped -> FAIL).
