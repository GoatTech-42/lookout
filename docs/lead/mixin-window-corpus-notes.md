# window detector: real-log corpus results (r3)

bound to the window-corpus slice (test_rig_proof.py win-corpus-* and
mutant-corpus-*). the detector ran over 17 committed meshac 26.3
evidence client logs under docs/build-evidence/ plus the committed
client-good.log fixture (18 files total, NOT 18 independent boots: the
evidence logs come from a handful of runs and their retries). lookout
was not loaded in those runs (meshac evidence), so this qualifies
behavior on a bounded negative corpus, not detection.

## what the corpus shows

- zero automatic detector findings on this bounded negative corpus:
  no same-line matches and no window findings on any of the 18 files.
  per-log triage (grep `dev\.lookout`, manifest below): no dev.lookout
  line exists in any evidence log, so there was nothing to find.
- header grammar: the launcher form `[hh:mm:ss] [thread/LEVEL]:`
  covers all but one line per boot log; the log4j direct form covers
  that one (the early `Could not determine local host name` init
  error). no third variant appears in the corpus.
- 15 of the 17 evidence logs carry 31 ERROR/WARN entries whose stack
  traces exceed the 30-continuation cap (lifecycle-client.log and
  transitions-client.log have none), so they surface as TRUNCATED for
  human review:
  1. log4j init `Could not determine local host name`
     (UnknownHostException) at the top of the log
  2. `[Minecraft Services Discovery/WARN]: Failed to connect`
     (authlib) shortly after
  in the inspected prefix neither shows lookout content; per the
  truncation rule the omitted content past the cap is NOT QUALIFIED -
  the harness detail says exactly that, and these entries are never
  described as benign or cleared.

## tuning decision (QA 8.7, wording amended 8.9)

keep the 30-continuation cap and the UNVERIFIED verdict; wording only.
a truncated entry reports "entry truncated at 30 continuations;
omitted content not qualified" (unconditional; an in-prefix finding,
when one exists, is listed separately). the 15 truncation-bearing
evidence logs each need that review; any future boot needs review only
IF a truncation occurs - this corpus makes no prediction about how
often that will be. a review confirms or investigates each entry;
a repeated known stack is quick to confirm but is never pre-cleared.

## corpus evidence manifest (17 evidence logs)

sha256 (first 16 hex) per file, with the per-log dev.lookout triage
count. the suite pins the exact parser footprint per file
(win-corpus-*), and mutant-corpus-* shows a bad parser (cap removed)
breaks exactly the 15 pinned truncation footprints.

| file | sha256 (16) | dev.lookout lines |
|---|---|---|
| docs/build-evidence/abilities26.3/abilities-constructed-client.log | d18f903531bc3bd6 | 0 |
| docs/build-evidence/abilities26.3/abilities-constructed-first/abilities-constructed-client.log | 3b0c102c68ba3113 | 0 |
| docs/build-evidence/abilities26.3/abilities-failed-hotbar/abilities-client.log | 00754ebd93eda218 | 0 |
| docs/build-evidence/abilities26.3/abilities-failed-input/abilities-client.log | 4498f59d51065916 | 0 |
| docs/build-evidence/abilities26.3/abilities-interrupted/abilities-client.log | 44f9335ee18c6a44 | 0 |
| docs/build-evidence/abilities26.3/failed-readiness/abilities-client.log | 6f1dfff11912c6e2 | 0 |
| docs/build-evidence/anchor26.3/anchor-client.log | 1ed877a5b5c08c13 | 0 |
| docs/build-evidence/anchor26.3/first-anchor-run/anchor-client.log | 995af60c87471dc3 | 0 |
| docs/build-evidence/held26.3/first-run/held-client.log | b6f3a38cbd8de08d | 0 |
| docs/build-evidence/held26.3/held-client.log | ed75b3ad3398378f | 0 |
| docs/build-evidence/lifecycle26.3/lifecycle-client.log | 52b538fdaf95e103 | 0 |
| docs/build-evidence/lifecycle26.3/transitions-client.log | 8ffafb7047a0c542 | 0 |
| docs/build-evidence/move26.3/failure2/move-client.log | 4a7732bda52dc4d2 | 0 |
| docs/build-evidence/move26.3/move-client.log | f0e1db21b09d9366 | 0 |
| docs/build-evidence/move26.3/move-failure1-client.log | 384ef5ae3b804808 | 0 |
| docs/build-evidence/outbound26.3/outbound-client.log | a518e71f9f5dad6e | 0 |
| docs/build-evidence/session-pong26.3/anchor-client.log | 3834f4d55bf45aa5 | 0 |

fixture (18th file, synthetic): rig/proof/fixtures/client-good.log,
sha256 (16) 2ff3181e4d6d6843.
