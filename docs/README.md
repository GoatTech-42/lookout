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

- tests/check_deps.py binds the exact target artifact and the active build
  route: pass --jar PATH and --build-file (build-remap.gradle for the remap
  route). it never unions jars under build/libs; a bare run with zero or
  several non-sources jars fails loud. the nested archive is inspected (valid
  zip, its fabric.mod.json id and version), so matching filenames or stale
  neighbour jars never satisfy it. fixtures and mutants live in
  tests/test_check_deps.py and need no gradle run.
- tests/check_r4_loudness.py is a lexical gate, not a runtime one: it proves
  the warn exists in the handler before the fallback, not that it fires in a
  running client. runtime proof is the 26.1.2 first-capture smoke (stimulus 4).
