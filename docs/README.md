# Documentation map

Start with the current rebuild evidence, not historical detection claims.

- [PARITY.md](../PARITY.md): dated implementation and evidence ledger. Later corrections supersede earlier claims.
- [ELIGIBILITY.md](ELIGIBILITY.md): static class-dependency inventory, not a list of active detectors.
- [SAFETY-TESTS.md](SAFETY-TESTS.md): headless test coverage and what it cannot prove.
- [REPOSITORY.md](REPOSITORY.md): branch consolidation, recovery hashes and old-code retention.
- [CONTRIBUTING.md](../CONTRIBUTING.md): build routes, tests and private-use limits.
- `build-evidence/boot26.3/`: bare shipping-jar startup failures, repairs and successful boot.
- `build-evidence/lifecycle26.3/`: separate-observer local client lifecycle evidence.

Dependencies: mixinextras (>=0.5.5) is bundled inside the shipped jar
(META-INF/jars, MIT licence), so no separate install is needed; the 26.1.2
binding check found it declared but unbundled. tests/check_deps.py keeps the
declared set, the jar's needs and the bundle in step.

Other design and rig documents may describe the older meshac implementation or unrun plans. Their presence does not qualify this rebuild. Never turn a previous check result into a new-version claim without a matching artifact, configuration and run.

## test caveats (QA 9.0, 2026-11-19)

- tests/check_deps.py unions every jar under build/libs when checking bundled
  deps. run it on a CLEAN build and inspect the selected ship jar; a stale jar
  with a bundle can mask a dropped include (the gradle-rule half of the gate
  still catches it, but the jar half is only as honest as the build dir).
- tests/check_r4_loudness.py is a lexical gate, not a runtime one: it proves
  the warn exists in the handler before the fallback, not that it fires in a
  running client. runtime proof is the 26.1.2 first-capture smoke (stimulus 4).
