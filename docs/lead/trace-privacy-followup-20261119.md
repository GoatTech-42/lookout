# trace/privacy follow-up to af9b9de

Scope: source checks and Java simulation only. No runtime/client validation,
new jar, or rig run. G4 remains UNQUALIFIED in bot-only mode.

## changes

- F-B: shared ChatAlerter keeps its pre-GUI evidence line. Both branches log
  entity ID and check ID only; trace mode adds a trace marker to the same line.
  Names and messages never reach the log: they stay in the local chat feed, and
  the suppression messages that carry name lists are excluded from the log
  surface. The initial packet (through d58a0d5) used presence as the switch, so
  even an empty value enabled capture. The empty-trace follow-up changes both
  consumers: unset or empty is off; any nonempty value, including "0", is on.
  Earlier revisions of this note described named trace logs; that behavior was
  removed by the log-privacy fix and no longer exists.
- F-A: at the originally reviewed tree there was exactly one ChatAlerter, in
  src/main, and no variant override. That changed with the accepted 26.1.2
  variant-seam slice: src/mc26.1.2/java carries a ChatAlerter override for the
  renamed chat route (Gui.getChat().addClientSystemMessage), and build.gradle
  excludes the src/main copy for variant 26.1.2 only; other variants keep the
  reference copy. The gate discovers src/*/java/dev/lookout/mc/ChatAlerter.java
  and checks every match, so both copies are gated. A synthetic silent second
  variant proves discovery is enforced. This does not establish that the
  complete 1.21.11 build compiles or runs.
- F-E: the gate tokenizes comments and quoted literals, checks the first real
  statement in each of the seven handlers, and checks the helper's trace guard.
  R9 now checks named arguments separately from default-log privacy and position.
  A comment containing return and a wrapped trace call are accepted.
- Run-plan nit: JAVA25 is a host-specific substitution, verified before launch.
- F-C/F-D are unchanged. No extra mixin-entry traces or inject source tags.

## replay

Run from the repository root:

```
python3 tests/check_trace_coverage.py
python3 tests/test_trace_gate.py
```

The gate is deliberately a narrow source-shape check, not a Java parser or a
control-flow verifier. Its strict branch shape must be reviewed if the logging
implementation changes. Green does not prove GUI-null behavior in a live client,
log persistence, shim delivery, or first-capture smoke success.

For red-before of the gate itself:

```
git show af9b9de:tests/check_trace_coverage.py > /tmp/trace-old-gate.py
python3 tests/test_trace_gate.py --gate /tmp/trace-old-gate.py
```

Expected: exit 1, 3/11 fixtures pass. M1/M2/M3 all survive the old gate;
second-variant/default-privacy regressions survive too; the comment and wrapped
call fixtures false-fail. Missing-handler and missing-field checks already work.
The alerter fixture input is the corrected current source for this gate replay.

Separately, the original ChatAlerter from af9b9de with the new gate fails exactly
R9-private-default-main. The current source passes it. The logging edit began
before its regression was written; this separate frozen-base replay is the
red-before evidence, not a claim of test-first implementation.

## observed results

- compileJava: success with a bounded 512 MiB Gradle heap; deprecation and
  unchecked warnings remain. Gradle test: NO-SOURCE, not a test pass.
- Source gate: 14/14 checks pass.
- Gate fixtures: 11/11 pass. Three original F-E mutants each fail exactly their
  own check. Silent second variant fails exactly its three variant checks.
- Two privacy mutants fail exactly R9-private-default-main.
- Dependency gate: 4/4; it inspects previously built jars, not a new jar.
- R4 loudness gate: pass.
- Choreography suite: 32/32. Its 20 mutants are killed with exactly their mapped
  failures, replayed in four five-mutant batches.
- LookoutSim: PASS. LookoutGuardSim: 28/28 red-controlled fixtures, PASS.

The first combined verification command hit a 120-second execution limit during
choreography mutation replay. No partial mutation result is credited. All 20
were then rerun in bounded batches. No host processes were killed by pattern.

No meshac parity source was changed. No thresholds, timeouts, skips, or accepted
outputs were relaxed. The existing source gate was tightened and fixture-tested;
independent QA still owns acceptance.
