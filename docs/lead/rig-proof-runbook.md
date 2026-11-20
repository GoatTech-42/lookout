# rig proof runbook (lookout runtime rows)

Scores the matrix's rig-gated rows from operator-supplied evidence files.
The harness (rig/proof/proof.py) never launches anything. RAM gate first,
in its OWN step: `free -m` available >= 1600 MB before any launch; launches
happen only in later steps. Teardown is TERM to exact owned recorded
PID/PGID only, never pattern kills. See RUNBOOK-combat.md for the combat
rig itself; this page covers only proof scoring.

## inputs (all operator-supplied paths, nothing guessed)

- `--client-log`: the watcher client's log from a real boot on the rig,
  with MESHAC_TRACE set (any nonempty value is on) so `[lookout-stats]`
  and trace alert lines exist. Required for boot, target-delivery,
  chat-capture, null-gui.
- `--config`: the watcher client's config/lookout.json. Required for g2-optin.
- `--server-log`: the rig server's log. Required for g3-receipt.
- `--arm-counts`: text file of `arm=<name> sent=<n>` lines accumulated per
  arm (killaura, reach, multiaura, timer). Required for g4-coverage.
- null-gui windows: the rig wrapper injects
  `[lookout-proof] null-gui-window begin` / `end` lines into the captured
  client log around the window where the chat GUI is unavailable (e.g.
  early boot or an instrumented null-GUI build). lookout itself logs
  nothing special there; the markers are the operator's window claim.
- `--row <name>`: score one row instead of all.

## rows, expected outputs, pass/fail

| row | PASS needs | FAIL | UNVERIFIED |
|---|---|---|---|
| boot (surface check) | lookout in the loader mod list, watching line present, no detected mixin error lines (severity + dev.lookout + mixin vocabulary, order-insensitive) | any of those missing | no client log |
| g2-optin (G2) | master enabled true AND killaura, reach, timer, multiAura all true in lookout.json | any of those off/missing/wrong-typed | no/unreadable config |
| target-delivery | some entity reaches 5 `[lookout-stats]` samples | none does (trace off, no packets, wrong target) | no client log |
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
show the mixin actually applied and fired). null-gui relies on operator
window markers and does not prove the GUI was actually null. g3 counts
any-player join lines and is not a receipt for the intended target.
chat-capture scores the log surface, not visual chat rendering.
target-delivery proves packet flow for some entity, not that it was the
intended target. A green run here does not satisfy the combat arms'
watcher-note halves (RUNBOOK-combat.md verdict mapping), and no row
proves punish-safety or any other-version behavior.

## abort conditions

Stop and mark the session UNVERIFIED, never PASS, when: RAM below the
gate before launch; any dev.lookout mixin error in the boot log; a
stimulus bot not verified protocol-matched to the target; any unowned or
unrecorded PID at teardown; the client log shows no watching line after a
successful boot claim.
