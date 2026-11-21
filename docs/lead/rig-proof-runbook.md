# rig proof runbook (lookout runtime rows)

Scores the matrix's rig-gated rows from operator-supplied evidence files.
The harness (rig/proof/proof.py) never launches anything. RAM gate first,
in its OWN step: `free -m` available >= 1600 MB before any launch; launches
happen only in later steps. Teardown is TERM to exact owned recorded
PID/PGID only, never pattern kills. See RUNBOOK-combat.md for the combat
rig itself; this page covers only proof scoring.

## inputs (all operator-supplied paths, nothing guessed)

- `--client-log`: the watcher client's log from a real boot on the rig,
  with MESHAC_TRACE set (any nonempty value is on) so trace alert lines
  exist, and `teleportJumpStats: true` in the client config so
  `[lookout-stats]` lines exist (they are per-tick sampled stats gated
  on that flag and the configured movement floor, NOT on MESHAC_TRACE).
  Required for boot, target-delivery, chat-capture, null-gui.
- `--config`: the watcher client's config/lookout.json. Required for g2-optin.
- `--server-log`: the rig server's log. Required for g3-receipt.
- `--arm-counts`: text file of `arm=<name> sent=<n>` lines accumulated per
  arm (killaura, reach, multiaura, timer). Required for g4-coverage.
- null-gui windows: the rig wrapper injects
  `[lookout-proof] null-gui-window begin` and
  `[lookout-proof] null-gui-window end` lines (exact literals; the proof
  harness pins them) into the captured
  client log around the window where the chat GUI is unavailable (e.g.
  early boot or an instrumented null-GUI build). lookout itself logs
  nothing special there; the markers are the operator's window claim.
- `--row <name>`: score one row instead of all.

## rows, expected outputs, pass/fail

| row | PASS needs | FAIL | UNVERIFIED |
|---|---|---|---|
| boot (surface check) | lookout in the loader mod list, watching line present, no same-line mixin error matches, no in-entry window findings, no truncated error entries (severity + dev.lookout + mixin vocabulary, order-insensitive; window detector per docs/lead/mixin-window-detector-design.md) | any of those missing | no client log, or window candidates / truncated error entries awaiting human block review |
| g2-optin (G2) | master enabled true AND killaura, reach, timer, multiAura all true in lookout.json | any of those off/missing/wrong-typed | no/unreadable config |
| target-delivery | some entity has >=5 `[lookout-stats]` lines (narrow sampled-stats smoke heuristic: count-only, duplicated or concatenated lines can satisfy it, 5 is chosen) | none does (`teleportJumpStats` off in the config, per-tick deltas never over the floor, or no packet flow) | no client log |
| chat-capture | >=1 alert line (plain or trace) on the log surface | no alert lines | no client log |
| null-gui (R9) | >=1 alert line inside a marker window | quiet window | no markers in the log |
| g3-receipt | >=1 join/receipt line in the server log (any player; NOT a per-target receipt) | none | no server log |
| g4-coverage | every arm meets its minimum (killaura 6, reach 6, multiaura 3, timer 6) | any arm short | no counts file |

Expected output: one `<verdict> <row> (<label>): <detail>` line per row,
then `SUITE: all pass` or `SUITE: not proven [...]`. Exit 0 only when
every scored row is PASS; UNVERIFIED is nonzero, never pass-by-absence.
`--dry-run` scores the committed fixtures (rig/proof/fixtures/) and must
print all PASS.

## what a PASS does not prove

Log-level proof only. boot is a surface check: mod-list entry + watching
line + no detected mixin errors is NOT full G1 weave proof (it does not
show the mixin actually applied and fired). the window detector reduces
the multi-line blind spot but does not close it: INFO/DEBUG-severity
failures, package-less summaries and config-name-only errors stay
invisible to scoring, and window-only findings score UNVERIFIED until a
human reviews the block (see docs/lead/mixin-error-multiline-limits.md). null-gui relies on operator
window markers and does not prove the GUI was actually null. g3 counts
any-player join lines and is not a receipt for the intended target.
chat-capture scores the log surface, not visual chat rendering.
target-delivery proves only that `[lookout-stats]` lines reached the log
for some entity: not that it was the intended target, and not general
packet delivery. the lines are per-tick sampled stats, emitted while
`teleportJumpStats` is on and the per-tick horizontal delta exceeds the
configured floor (default 1.0 blocks); they can repeat within one
relocation, can include grace/exempt movement, and are not discrete
teleport events. the count is a line count (duplicated or concatenated
lines can satisfy it) and 5 is a chosen smoke heuristic. the direct
per-handler trace receipts (onTeleport/onVelocity/onSwing/onDamage/
onProjectileSpawn/onTotemPop/onTotemEquip under MESHAC_TRACE) are more
direct delivery evidence where available; the inspected sources have no
general per-target movement-delivery receipt. A green run here does not satisfy the combat arms'
watcher-note halves (RUNBOOK-combat.md verdict mapping), and no row
proves punish-safety or any other-version behavior.

## human review overrides the harness

block review (see docs/lead/mixin-error-multiline-limits.md) runs per
scored boot/log. a failure-shaped block triggers investigation and the
run scores UNVERIFIED until a human confirms or clears it; a confirmed
failure overrides any harness verdict for that run, even when the row
printed PASS. PASS is never the operational verdict against a confirmed
failure. real excerpts enter shared artifacts only after redaction to
IDs.

## abort conditions

Stop and mark the session UNVERIFIED, never PASS, when: RAM below the
gate before launch; any dev.lookout mixin error in the boot log; a
stimulus bot not verified protocol-matched to the target; any unowned or
unrecorded PID at teardown; the client log shows no watching line after a
successful boot claim.
