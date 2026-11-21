# mixin error detection: limits and next steps

bound to main 6bdd2ab. the rig-proof boot row scores the watcher client
log with a single-line detector: severity (ERROR/WARN), the dev.lookout
package and mixin vocabulary (mixin/apply/inject/callback), each matched
independently anywhere on the same line.

## what the single-line detector misses

real mixin failures often span lines:

- a one-line summary ("ERROR Mixin apply failed") followed by the class
  on the NEXT line ("Caused by: ... dev.lookout.client.AttackHooks"), so
  no single line carries severity + package + vocabulary together
- stack traces where dev.lookout frames appear several lines below the
  ERROR header, without mixin vocabulary on the same line
- wrapped exceptions ("Caused by:" chains) where only the innermost line
  names the failing callback
- per-callback injection errors logged at DEBUG/INFO on some loaders,
  which the severity rule deliberately excludes; if a target ever logs
  failures at INFO, the detector will not see them

the detector CAN miss these forms, and QA replayed the first shape
against the healthy fixture: boot still scored PASS. so a boot PASS
means exactly "no same-line match", never "no mixin errors". treat any
PASS that was not backed by a block review (below) as unproven, not
green.

## how a rig run would capture them

1. boot the watcher client on the rig with the log captured to a file
   (RUNBOOK-combat.md's boot step, plus `tee` to a dated evidence file).
2. grep the captured log for `dev.lookout` with generous context
   (`grep -n -B5 -A30 'dev\.lookout'`) and human-review the blocks for
   EVERY scored boot/log, not once per target. this grep is triage, not
   a complete audit: errors that never name dev.lookout (package-less
   summaries, config-name-only failures) can still be missed. any
   failure-shaped block triggers investigation and the run scores
   UNVERIFIED until a human confirms or clears it; a confirmed failure
   overrides the row verdict (see the runbook).
3. before a real excerpt enters any shared artifact (fixtures, notes,
   reports), redact it to IDs: player names, session ids, paths and
   chat text out, entity ids in. synthetic fixtures stay id-only.
4. if a multi-line failure form appears, extend the detector with a
   sliding-window pass (severity line opens a window; package +
   vocabulary within N lines counts as one error) plus red fixtures
   built from the redacted captured block, before the window form is
   trusted for scoring.

status: the window detector designed in
mixin-window-detector-design.md has landed in the proof harness
(parsers/score + fixtures + demonstrated mutants, synthetic id-only
fixtures). window-only findings and truncated error entries now score
UNVERIFIED for human block review instead of passing silently; the
same-line FAIL scan is unchanged. this does NOT close the limitation:
the exclusions above (INFO/DEBUG severity, package-less summaries,
config-name-only errors) still stand, and the detector's fixtures are
synthetic until real rig captures feed them. the three exclusions are
frozen as explicit pins (win-f13-info-excluded,
win-f20-packageless-invisible, win-f21-configname-invisible, each with
its boot-PASS pin; the mutants demonstrate THESE relaxations -
severity-dropped widens the severity gate, package-dropped removes the
package requirement, package-widened relaxes the package pattern to
the bare name), so a future detection change must edit the pins
explicitly instead of silently widening coverage. DEBUG is not
separately pinned: it shares f13's INFO code path (one severity gate),
so the f13 pin covers it structurally but no DEBUG fixture exists.

next step when the rig is unheld: capture one real boot log per target,
run step 2, and record the outcome per boot. one clean boot closes only
that boot. real multi-line blocks are redacted to ids and become
detector fixtures before the window form is trusted for scoring; finite
clean observations do not remove a known parser blind spot.
