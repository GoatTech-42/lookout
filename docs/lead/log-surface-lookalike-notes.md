# log-surface lookalike corpus notes (r2)

companion to the pins in tests/test_rig_proof.py (lookalike-* and
mutant-lookalike-*). fixtures (synthetic, ID-only):

- rig/proof/fixtures/client-lookalikes.log
  sha256 3bc5c8555b9311e3c3c431744f073b9a64b082c94f1b6e79c491f234e3120f42
- rig/proof/fixtures/client-lookalikes-watching-echo.log
  sha256 fc0c081af275fcb4d5c67c6f4a529bb47c6a678492033f6aa474f7095179410a

the pins are a FROZEN CHARACTERIZATION of current parser behavior. they
are not requirements to keep this parsing: a hardening diff is expected
to change them explicitly, and the mutant families below distinguish
spoof removal from over-strict parsing.

## the spoof surface, as measured on this bounded corpus

every lookout marker format is plain text on an unauthenticated log
surface. a chat participant (or any other log writer) can echo the
formats, and the parser counts them exactly like real emissions:

| line (fixture, 0-indexed) | echoed marker | parser result today |
|---|---|---|
| 1 | watching literal in [CHAT] | watching = True (indistinguishable from the real line 0) |
| echo fixture | watching literal in [CHAT], no genuine line | watching = True (lookalike-watching-echo-only) |
| 2 | plain alert | counted: entity=21 check=killaura_facing |
| 3 | trace alert | counted: entity=22 check=reach trace=True |
| 4 | stats line | counted: stats {21: 1} |
| 5 + 7 | null-gui begin/end pair | window recorded: (5, 7) |
| 6 | alert inside the echoed window | counted; satisfies the null-gui row (tested) |
| 8 | ERROR + dev.lookout + mixin vocab in chat text | counted as a same-line mixin error; FAILs boot (tested) |
| 9 + 10 | ERROR text in a chat header + dev.lookout continuation | NO window finding: the entry severity comes from the header LEVEL field (INFO), not from text content |

## scorer implications - tested through the actual scorer

- target-delivery: 5 echoed stats lines PASS the row (one echoed line
  does not; the threshold is 5). test: lookalike-scorer-delivery-pass.
- chat-capture: a single echoed alert PASSes the row.
  test: lookalike-scorer-chat-capture-pass.
- null-gui: an echoed begin/alert/end sequence PASSes the row.
  test: lookalike-scorer-null-gui-pass.
- boot: the combined fixture's echoed mixin error FAILs boot even with
  watching true; the same log without the echoed error PASSes
  (positive control). tests: lookalike-scorer-boot-fail,
  lookalike-scorer-boot-control-pass.

## mutation families, labeled honestly

- chat-filter mutants (alert, watching) demonstrate SPOOF REMOVAL:
  echoed markers stop counting while real-prefixed controls keep
  counting (mutant-lookalike-alert-chatfilter-killed,
  mutant-lookalike-watch-chatfilter-killed).
- anchored/exact mutants are BEHAVIOR-CHANGE CONTROLS: over-strict on
  purpose - they kill real prefixed lines too - proving only that the
  pins change when parsing changes
  (mutant-lookalike-alert-anchored-control,
  mutant-lookalike-watch-exact-control).

## what this is not

- no claim about other log writers, other marker orderings, or
  real-world frequency: the statements above cover these bounded
  fixtures only.
- no parser hardening is included, and nothing here authenticates
  markers: even a hardened [CHAT] filter would not authenticate
  arbitrary log writers.
