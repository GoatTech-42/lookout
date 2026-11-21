# server receipt parser: real-log corpus results

bound to the server-corpus slice (test_rig_proof.py srv-corpus-* and
mutant-srv-corpus-*). parse_server_log ran over 18 committed meshac
26.3 evidence server logs under docs/build-evidence/ plus the
committed server-good.log fixture.

## what the corpus shows

- receipt counts pinned per log: 15 evidence logs carry exactly 2
  receipts (one `logged in with entity id` + one `joined the game`
  line per observed join), transitions-server.log carries 4 (two
  joins), and 2 evidence logs carry 0 (failed-readiness and
  observer-boot-failure: boots that never reached a join). 15 + 1 +
  2 = 18 evidence logs; plus the fixture at 2 = 19 pinned files.
  on the zero-receipt logs g3 scores FAIL rather than UNVERIFIED -
  that verdict is source-derived policy from score.py's documented
  contract, observed here only as parser counts, not asserted by
  this slice.
- the pins hold COUNTS only: the real lines carry a player handle and
  stay out of the suite and this manifest (triage evidence is the
  sha256 per file below, not line content).
- bounded corpus: receipts are any-player join lines, never a
  per-target receipt (the g3 row's documented limit stands), and
  these are meshac evidence logs, so this qualifies parser behavior
  on real server-log formats, not lookout detection.
- mutation protection: mutant-srv-corpus-killed runs the 19 pinned
  counts against a never-matching receipt regex in a mutant tree and
  shows exactly the 17 receipt-bearing files mismatch, after the
  healthy copy shows 0 mismatches (mutant-srv-corpus-baseline-healthy,
  both asserted with subprocess returncode 0).

## corpus evidence manifest (18 evidence logs + fixture)

sha256 (first 16 hex) per file.

| file | sha256 (16) |
|---|---|
| docs/build-evidence/abilities26.3/abilities-constructed-first/abilities-constructed-server.log | 88e65e92d33f8da2 |
| docs/build-evidence/abilities26.3/abilities-constructed-server.log | 0ec3393037bcb6ea |
| docs/build-evidence/abilities26.3/abilities-failed-hotbar/abilities-server.log | 700c349a072b1765 |
| docs/build-evidence/abilities26.3/abilities-failed-input/abilities-server.log | ab131951090665dc |
| docs/build-evidence/abilities26.3/abilities-interrupted/abilities-server.log | 72bc6b0e388d1721 |
| docs/build-evidence/abilities26.3/failed-readiness/abilities-server.log | 948b324444834063 |
| docs/build-evidence/anchor26.3/anchor-server.log | a74cabfab0eef8dc |
| docs/build-evidence/anchor26.3/first-anchor-run/anchor-server.log | 9731716da47535cd |
| docs/build-evidence/anchor26.3/observer-boot-failure/anchor-server.log | cc9297fd92d6ee47 |
| docs/build-evidence/held26.3/first-run/held-server.log | a0e4975336383a6c |
| docs/build-evidence/held26.3/held-server.log | 3db473b4f9140b80 |
| docs/build-evidence/lifecycle26.3/lifecycle-server.log | 18152c9545216d19 |
| docs/build-evidence/lifecycle26.3/transitions-server.log | b0ed7ea82cf0256c |
| docs/build-evidence/move26.3/failure2/move-server.log | 052005d05da8acee |
| docs/build-evidence/move26.3/move-failure1-server.log | d9a546158bbdb2da |
| docs/build-evidence/move26.3/move-server.log | c3e7905489ffa002 |
| docs/build-evidence/outbound26.3/outbound-server.log | 9975bb86c9c7097e |
| docs/build-evidence/session-pong26.3/anchor-server.log | 95e1a046e544c7a8 |

fixture (19th file, synthetic id-only): rig/proof/fixtures/server-good.log,
sha256 (16) 0dd95610c2681165 (upgraded from the pre-hardening shorthand
"entity N joined the game" to the observed real forms; the count of 2
is one login line + one join line, not two distinct joins).

## echo hardening (server-echoes slice)

the old parser matched "joined the game" / "logged in with entity id"
anywhere on any line, so a player chatting "X joined the game" (or the
System chat form) produced a false receipt, and headerless
receipt-shaped lines counted. parse_server_log now requires a
recognized launcher/log4j header and matches only the two observed
receipt forms anchored at payload start: "System chat: <handle> joined
the game" and the verified login shape "<name>[/<address>] logged in
with entity id <n> at (" where the name is \w{3,16} directly followed
by the bracketed address. player chat logs as "<handle> ...": the join
form requires the payload to start with "System chat: ", and the login
form's name+bracketed-address shape cannot be satisfied by a
"<handle>" chat prefix - including the minimal bypass form
"<handle> logged in with entity id N at (...)", pinned as an isolated
negative (srv-echo-login-minimal-negative) with the genuine login form
pinned as a positive (srv-echo-login-genuine-positive). this is a
bounded claim over the committed corpus formats, not a universal
statement about server log shapes.

- the characterization change is explicit: the pre-hardening behavior
  (echoes + headerless lines counted) is captured as red evidence and
  replaced by the pins srv-echo-real-forms-counted (exact line
  content, synthetic fixture) and srv-echo-chat-and-headerless-
  filtered
- corpus regression: all 19 pinned counts unchanged
- mutation protection after a healthy baseline:
  mutant-srv-anchor-dropped-killed (unanchored join pattern + search
  re-counts the two chat join echoes),
  mutant-srv-login-shape-relaxed-killed (login shape relaxed to a bare
  token re-counts the minimal chat echo),
  mutant-srv-header-required-dropped-killed (headerless line scanned
  as its own payload counts), mutant-srv-corpus-killed (both receipt
  patterns disabled -> zero receipts -> exactly the 17 receipt-bearing
  pins break; the 2 zero-receipt logs still match)
- no ambiguity machinery on the server side: a receipt-shaped line
  outside the anchored forms is simply excluded, not surfaced
- g3's documented limit stands: any-player join lines, never a
  per-target receipt

fixture (synthetic id-only): rig/proof/fixtures/server-echoes.log,
sha256 (16) 9d7ea1f25459af55.
