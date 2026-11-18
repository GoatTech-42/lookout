# meshac v1 scope and daily plan (draft for Tue Nov 17 - Sun Nov 22)

Written Mon Nov 16 ~19:30 PST, updated 20:35 PST by the meshac continuity lead. Base: 2af4b70 (private repo). Nothing here is landed, released or run on a host unless stated.

## 1. Bar (from Luke)
- Versions 1.21.11, 26.1.2, 26.2, 26.3. Detect every detectable hack with no false positives.
- Benchmark clients: Wurst, LiquidBounce, Meteor, plus Meadow (private rig only).
- Veil: anti-ESP, xray, dynamic hiding, terrain masking. Anti-dupe water throttle (off by default). Bedrock basics.
- No extra checks beyond upstream ("vulcan has everything"). Credits to upstream.
- ZKM dropped for v1 (Luke, Nov 16 16:32). Publishing under his name is still his call.

## 2. Honest status
- Target Sunday Nov 22: **V1 RC**, not full v1, unless Box evidence changes it. Full bar is not reachable by Sunday on current evidence.
- Why: 179 of 180 checks are class-gated (ContextGate NEEDS_WORLD_CACHE / NEEDS_CLIENT_VERSION), the 4 targets are built on 26.3 only so far, and no detector has a qualified capture. One capture so far is NOT_QUALIFIED.
- New static finding (W1 table, 2af4b70): the 179 world-cache count is mostly transitive. Only two call sites seed it (CollisionState:1252, VelocityTracker:96, both sulfur proximity). 85 check classes inherit it through CollisionState. Checks read the live ServerLevel, not the tracker. Proposal only, gate unchanged: the first static table listed 22 of 85 as block-independent. QA read all 22: 16 confirmed none (AimD/F/H/N/O, BadPacketsA/Y, ElytraA, HitboxA/B, InvalidA/F, KillAuraC, NoSaddleA, ReachB, VelocityB), 5 direct wrong (NoSlowB, ElytraB, ElytraI, InvalidC, ReachA; 23% miss), 1 indirect (InvalidB, fed by ghost-block machinery, keep gated). Causes: is*-getter mapping and ticksSince* counters reset under block-derived conditions. The corrected rerun (rev2, extended taint rule) flags all 5 and keeps the 16 (ElytraA hand-adjudicated to none, matches QA). Method needs a third class beyond direct read and reset-condition taint: signal produced by block-dependent machinery. CollisionState consumers: 68 block-view dependent (upper bound), 17 by the script. All 16 still sit behind the version gate.
- Packet order (static, not runtime-verified): our outbound hook sees packets before encoding, inbound after decode and after Via inbound translation, so we see server-version objects both ways. A palette mirror decodes at server version. Velocity hook (ByteEncoderCompletionMixin) runs on server-version bytes before Via's encoder; its after-send task fires after the downstream chain. No Via rewrite/cancel is registered for SET_ENTITY_MOTION or EXPLODE across the 1.21.11 to 26.3 hops (public ViaBackwards master, static). Open: wire-layout diff, Luke's pinned Via versions, and the listener has no isSuccess() guard (candidate hardening pending).
- Real gap is semantic: checks see the server's block view, upstream sees the client-acknowledged view. A block-id palette mirror is the parity-faithful path. No cheap subset found.

Veil review (static, 12 porter VERIFY markers): 11 ok, 1 unknown (ClipContext VISUAL leaf behavior needs runtime test E5 per target), 0 wrong. Veil.java :143 `isInvisible() && false` is dead code (safe). Wiring exists for tick/grace/forgetPlayer/attacked. XrayCore/EspCore semantics not yet reviewed.

## 3. Findings and candidates since Nov 16 morning (all candidate-only)
| Item | State |
|---|---|
| Post-update fence (WorldBlockTracker, branch candidate/post-update-fence, r2) | Box run: A2 44 bound, 0 immediate; A3 58 probes 0 mismatches; A1 362 comparisons 0 differences. One capture, NOT_QUALIFIED. QA source PASS. Held, not pushed. |
| Confirmation ordering lock (ConfirmationTracker) | Real hazard in our port: reply on netty thread, 13 send sites on server tick thread. Upstream has the same code on one thread. QA model PASS. Compiles at 26.3. |
| trackedEntities ConcurrentHashMap | QA model PASS. Compiles at 26.3. |
| MotionState.pendingServerPositions snapshot | QA PASS (traced). Compiles at 26.3. |
| CollisionState:978 living flag (was canSerialize substitute) | Real false-negative defect: non-living entities set touchedEntity. QA mutation red. hasSupplier checked at 26.3 only. |
| handleEquipment ordering | Parked: dead code, no caller, no reader. |
| VelocityTracker maxima never reset | Faithful to upstream. No diff. |
| Explosion knockback mapping | OK on 26.1.2/26.2/26.3. |
| Bedrock blanket | Servers with Geyser/Floodgate classes resolve UNKNOWN for all players. No safe Java-positive signal exists. Known v1 limit, told to Luke as a limit not a fix. |
| Veil quit sweep A', thread confinement B, doc comments C, stale prune D (EspCore pairs leak and cross-thread HashMap) | Real: EspCore.forget had no callers; attacked() on netty vs tick on server thread. QA models executed, all PASS (C has a one-line comment nit). Order A', B, C, D is stable, reverse fails. |
Full stack (lock, map, MotionState, living, A', B, C, D, E, F; 9 files, +117/-32) applies in order on 2af4b70. Box compile matrix (Nov 16 evening, JDK 25.0.5, compile only): BUILD SUCCESSFUL on 26.3, 26.2, 26.1.2 and 1.21.11 (remap, release 21; resumed once after a TERM). Earlier failed logs were the build box toolchain (a JRE without a compiler; JDK 21 vs a Gradle dependency needing 25), not source; the unpatched 26.3 comparison failed for the same reason, so no clean-vs-stack compile on a good JDK yet. Not run, no host test. Landing needs an explicit GO. Also queued as v1 cleanup: Veil/Xray/Esp doc nits (600 ms not 200 ms, 10% not 5%, segmentHitsBox unused, nonexistent VeilAdapter name). XrayCore/EspCore: arch executed 38/38 standalone checks, 0 wrong.

Runtime experiments queued for Box (one laptop test at a time, only after Luke is off, scratch builds only): E5 (Veil ClipContext VISUAL leaf behavior per target), E-velocity (26.1.2 dev server with meshac + ViaFabric + ViaBackwards, one 1.21.11 client offline mode, Via packet debug on SET_ENTITY_MOTION/EXPLODE, one knockback and one TNT; pass = the after-send task fires once per packet after the Via log line and the client applies the motion).
E6 (Veil chunk masking, with E5): on each target confirm extractChunkData/calculateChunkSize loop getSections() in array order, one call per section, run on the server thread, and a mined ore is not resurrected after a chunk send. Arch's static review of ChunkDataMixin/ServerLevelVeilMixin: 0 wrong at 26.3 (server keeps real blocks, reveal only when exposed). Limits: Veil.CACHE has no eviction on chunk unload (diff F pending); block changes without UPDATE_CLIENTS skip invalidation and reveal.
**V1 blocker for enabling xray (found 19:43, source-confirmed):** Veil.CACHE was keyed by chunk x, z and section only (Veil.java:67-68 ck()), with no dimension. Two dimensions with a chunk at the same coordinates (overworld and nether 0,0) share an entry, so the second gets the first's cached mods and the restore writes the first dimension's real states into the second's live section. Server-side block corruption, latent while xrayOn is false (default). Diff F keys on (ServerLevel, chunk) and evicts on unload; diff E guards section order. E (section-order guard) and F (cache key with dimension + unload eviction): QA PASS at source and model level, mutations red, not a real-server run. Nits: evict E's mislogged set on the sweep, reword E's warn text, CK holds a strong ServerLevel ref.

## 3b. TOP PRIORITY (Luke, Mon Nov 16 8:42 PM): anti-ESP / anti-xray (Veil)
Luke: do everything possible, ESP is the biggest problem on servers today and movement hacks are easy to detect. Status at 2af4b70, source only:
- Built (never run on a real client): ore/chest/spawner masking in chunk packets with seeded fake ore (XrayCore, ChunkDataMixin), reveal on exposure (ServerLevelVeilMixin), entity line-of-sight culling with hysteresis, combat and vehicle exemptions (EspCore, TrackedEntityMixin), static review 0 wrong at 26.3, QA-passed fixes for the pairs leak, thread race, dimension cache key and section-order guard (stack A'-F).
- OFF by design: Config.applyVeil() hard-sets xrayOn, espOn and cullMobs to false ("qualification hold"). Turning it on is a separate decision from the detector gates; it needs the rig client tests (docs/veil/CLIENT-TEST-SPEC.md X1-X7, E1-E11) with Meteor and Wurst on the private rig first. Cull mobs stays opt-in.
- By Sunday Nov 22 (if Box rig time is available, one laptop test at a time): stack compiles on all four targets; rig runs of X1-X6 and E1-E10 on 26.1.2 (the rig version) with real Meteor/Wurst, recorded per scenario pass/fail; legit controls X4, X5, E2-E7, E10 with zero visible differences; perf X7/E11 numbers; on pass, ship with xray and esp ON by default only if the evidence is clean, otherwise ship off and say so.
- After Sunday: per-version rig runs on 26.2, 26.3, 1.21.11 (no mineflayer past 26.1, so these need real-client runs), sound/particle position leak closure (below), tile-entity data minimization beyond chests/spawners, tuning nearRadius and cull timing against ping.
- **Veil lag/starvation findings (amp model, source-confirmed at Veil.java:172-196, 2af4b70):** (a) `if (budget <= 0) return;` exits the whole tick, so viewers and pairs later in the loop are never evaluated that tick and a pair already hidden stays hidden with no fail-open: a player can be invisible to someone adjacent under load. (b) the 200 budget counts pairs, not clips; modeled worst case about 5,200 clips per tick. (c) the per-viewer scan walks all level entities (modeled 0.5 ms at 40 bots/500 entities, 5.9 ms at 60p/15k, 36 ms at 150p/40k before any ray). (d) cold xray chunk encode about 2.7 ms per chunk on the server thread, join burst of 20 chunks about 53 ms in one tick; reveal invalidation up to 33 ms at 5000 block changes. (e) pairs leak and CACHE growth (diffs A', D, F). **Gate: Veil does not ship ON until the starvation fix (a) with a fail-open and its test, a real clip budget, and a bounded cold-encode path are in, tested and QA-passed.** Candidate diffs in progress: A2 fair rotor, B2 clip/time caps (defaults 1200 clips, 1.5 ms are guesses), E1/E2 stale-hidden fail-open at 100 ticks (apply and compile on 26.3 on top of the stack plus G; QA pending); spread-over-ticks for the prune/cache sweeps queued. Arch diff G (sound-entity packets for hidden entities dropped) compiles on 26.3. Spec fixes from QA: the E1 250 ms bar is unreachable (3 checks x 4 ticks = up to 600 ms), E3 present-at-first-LOS-tick is unreachable at 4-tick cadence.
- Run sheet for 26.1.2 (QA, veil-run-sheet-26-1-2-4d243bbb.md): E1 bar 650 ms at the default preset (cullAfterChecks stays 3; false positives matter more than speed), E3 present within 4 ticks of the rig-computed first LOS, X7a steady-state keeps the 1 ms bar, X7b cold-join report only. E10 sub-cases (respawn, teleport, dimension change) fail until Veil.grace is wired beyond join (only caller is PlayerDataManager.java:27); a candidate diff is assigned to arch. E11a is expected red on the base and green with A2.
- Known leak classes to audit now (static, no run): sound and particle packets carry world coordinates for hidden players and entities (footsteps/sound radar), hidden-entity metadata/equipment/velocity packets, block-entity data of hidden blocks, light and heightmap data in chunk packets (cave air gaps are visible even when ore is masked), entity packets sent before the first cull check, spectator/teleport edges.
- What server-side can and cannot stop (be honest in release notes): CAN stop entities and ores behind solid cover from being sent at all, so ESP, tracers, chest finders and ore xray stop working on them; fake ore removes ore-position inference; freecam learns nothing new since LOS uses the real position. CANNOT stop: anything already sent (entities inside nearRadius 8, entities in true line of sight, the player's corner-peek window where the cull lags by up to about 600 ms), sound-based radar until the sound audit is closed, cave shapes from light data, seed-based ore prediction, purely client-side cosmetics (fullbright, outlines of already-sent things), and a cheater who turns it into a movement problem (this is where the detector checks matter). It is a tradeoff against lag and legit visibility, tuned by hysteresis; ESP cannot be detected passively, only reduced.
- Not in upstream: any detection of ESP behavior (pre-aim at hidden targets, hits through walls, honeypot entities) is new work beyond Vulcan parity; Luke's earlier rule said no extra checks, his 8:42 words say do everything possible, so I propose only the hits-through-walls/raycast reach path first and honeypots only if Main/Luke want them.

## 4. Gates and limits (unchanged)
Detector gates stay closed. No gate flip, no release, no spending, no public post as Luke, no deploys without review/GO. One laptop test at a time. Foreground commands only. Keys via vault only. No Meadow jar outside the private rig. No real accounts, no production join. A capture is NOT_QUALIFIED until a qualified run says otherwise. Upstream sources are read-only reference, nothing copied (PROVENANCE.txt in the recovered tar is an unverified external claim).

## 5. Day plan
- **Tue Nov 17**: W1 table rerun with extended taint rule (frontend), QA adjudication; Veil review (arch). Per-target API table (amp). Via packet order answer (frontend). Decide with Luke/Main: land the 4 candidate diffs (needs Box real-run first), fence call.
- **Wed Nov 18** (anti-ESP first in the Box queue after the compile matrix): Veil rig runs X1-X6/E1-E10 on 26.1.2, then Box real run of the stacked candidates on the single laptop rig (A1/A2/A3 plus legit-movement suite). W3: 1.21.11 and 26.1.2 / 26.2 builds (remap toolchain, Java 21 for 1.21.11).
- **Thu Nov 19**: W1 first increment: block-id mirror from chunk palettes at OutboundDispatcher :51/:59/:63, model-tested off-host; gate split proposal reviewed.
- **Fri Nov 20**: W4 qualification matrix: Wurst, LiquidBounce, Meteor, Meadow captures, each on the rig, Box executes, results labeled NOT_QUALIFIED unless a qualified run holds.
- **Sat Nov 21**: W5 veil/admin/punishment UX and docs freeze. Credits and PARITY.md updated. False-positive pass on legit suite.
- **Sun Nov 22** (Luke's target date): RC cut decision. Honest RC vs full report to Luke, open limits listed, report only what ran.

Adjust if Box capacity slips.

## 6. Owners
Lead review and source custody: continuity lead. Host runs: Box (via Main). Spare slices: frontend (version/world), amp (confirmation/tracker concurrency), arch (CollisionState/Veil), QA (verification, mutations). Each slice: base 2af4b70, failing-then-passing off-host test, diff checked by `git apply --check` and compile before it goes to review.

## 7. Open decisions
1. Land the four candidate diffs, and when (needs Box run, then Main GO).
2. Fence the other trackers? Unmeasured.
3. Gate split (proposal only).
4. RC vs full v1 cut for Sunday.
5. Public release remains Luke's call.

## 3c. Update, Mon Nov 16 9:22 PM (source/model/compile only, nothing run on a host)
- Veil candidate train: A', B, C, D, E, F, G2, A2, B2, E1, E2, C, D2, S1, S2, Q1, H, queue-on-success v2. QA PASS on source+model for all but queue v2 (re-review pending after the thread-confinement fix). Compiles on 26.3 (bundle v4, sha 86e1f55f). Not yet on 26.2/26.1.2/1.21.11.
- Held: K (grace) - QA found it would give flagged players 40 ticks of visibility via self-initiated setbacks; J2 (needs record accessors x()/y()/z() on 26.3, plus quit cleanup); I (needs re-diff on the full train). Grace gap stays open: Veil.grace is join-only.
- QA coverage gaps: reverting B2 is invisible to E11b's 5,200 bound (only ms/clips-per-tick shows it); reverting E1 invisible to E11a, scenario E12 added. Nit: staleHiddenTicks=100 may flicker above about 2,500 in-range pairs per viewer. Sliced sweeps give ~33/~50-tick prune/evict latency.
- Rig: rig-only switch (-Dmeshac.rig=1) for private runs, never landed. Meteor exists only for 26.2, mineflayer tops at 26.1.2, so Meteor scenarios need a 26.2 rig or the laptop; the run sheet (v3) is 26.1.2-only. Rendered-client items need the laptop or an unproven virtual display. Capped rig results are not unconstrained server perf.
- W1 block mirror inc.1 and HitWallA (off by default) compile on the train; QA review, lazy allocation and a change-feed design come before any enablement. Gates stay closed.

## 3d. Update, 9:28 PM
- Bundle v5 (sha aea5a94f): base train plus G2, G3-mirror, H, queue-on-success v3; options rig (with -Dmeshac.rig.cull=1 for E9), wall, held (I/J2/K, pending arch revisions). 26.3 compile PASS, no host runs.
- Queue-on-success: v3 QA PASS. Thread model per QA: velocity pipeline runs on the connection's netty loop (26.1.2 bytecode); P1 rig probe settles it. ConfirmationTracker lock diff premise may change; candidate only until P1.
- W1 mirror v2: QA PASS as consumer-less increment; onPlacement uses immediate handlePlacement (changelog item). Memory: enable only for players with a mirror-dependent check (shared sections out, Veil masks are per viewer). ~15/64/90 MB per player at view 10 (synthetic).
- HitWallA enablement gates (dead by design, off by default): (1) eye height 0.4 for swimming/crawling or exempt those poses; (2) bulk-read mirror API (worst case about 17k synchronized reads per attack eval on the event loop); plus mirror, change-feed, tests and lag budget.
- Rig: E9 cullMobs only with both -Dmeshac.rig=1 and -Dmeshac.rig.cull=1.

## 3e. Update, 9:46 PM (Box compile matrix, v7b)
- Base train: BUILD PASS on 26.3, 26.2, 26.1.2, 1.21.11 (Box). Held group (I2, J3, K2, M, L2) also PASS: default on 26.3, `legacy-particle` on 26.2, 26.1.2, 1.21.11. Only 26.3 has x()/y()/z() on the particle packet. Per-target rule is in APPLY-ORDER (v7c, comment only).
- Still compile-only. No host runs. Next: server-side offline rig per run sheet v5 (P1 thread probe, E-series, X-series), Meteor/rendered-client items on the laptop later when Luke is off. Detector gates, Veil flags stay closed; rig switch is private only.

## 3f. Update, Nov 17 8:10 AM (first real rig runs, 26.1.2 server-side offline, bot only)
- P1: onConfirmation and onMove run on Netty Epoll IO #1 (1 bot, 84 ping/pong, 71 moves). Settles those two callbacks only.
- X1 PASS, X2 original field PASS, X2 full-section PASS on the QA-verified corrected oracle (the original expanded oracle stays NOT_QUALIFIED, recorded), X3 PASS (identical hashes, capture + 5 relogs + restarts). No X6 chests in this arena, no client render claim, performance unqualified. Evidence: meshac-xstatic-evidence.tar-3e8eeae9.gz (sha d0b4ffd7...), results meshac-xstatic-results-df33560f.jsonl.
- Static findings since 9 PM: attack/command/swing/placement HEAD hooks double-fire (netty then server-thread replay); DD3 (FirstSight replay dedup) is the candidate, QA PASS; dd1/dd2 retracted (they would delay dispatch behind netty movement). Honeypot HP3/HP3c: design only, off by default, rig-only, QA PASS, nothing enabled. Bundle v11 (sha 9f5b5c3b) is current. Open: four-target compile of v11 and P1b counters (Box).

## 3g. Update 8:42 Nov 17 (v12 train)
- Current bundle: meshac-candidate-stack-v12-2af4b70.tgz (v11 + HP3d under honeypot option). Candidate only, nothing landed.
- Compile (Box matrix): all 4 targets compile on v11/v12. 1.21.11 uses the remap route: ./gradlew build -Premap=1 -Pminecraft_version=1.21.11 -Pfabric_api_version=0.141.6+1.21.11 -Pmc_range=1.21.11 -Pjava_min=21. HP3d fixed the 1.21.11 Entity.nextEntityId bridge (QA PASS on real 1.21.11 jar).
- Packet shapes (Box javap): Attack packet split on 26.1.2/26.2/26.3 only; 1.21.11 attacks ride Interact. Spectate packet on 26.1.2 only. handlePong empty and keepalive has no ensure on all 4.
- dd3 replay dedup: QA PASS (source/model).
- P1b (v12 full stack, 26.1.2, honeypot off): 0 injection errors across 7 patterns, meshac loaded, bot joined/left, clean exit. First Mixin application evidence. Build sha 9d35e622.
- P1c (thread/count probe, 26.1.2): control shows 2 runs per packet (Netty then Server thread) for interaction, entity action, digging, placement, swing; dd3 shows 1. Accepted callback precedes next move. 109 confirmations, 0 duplicates, all Netty. PASS_observed_single_packet_per_method. Limits: packet callbacks, one bot, no burst/overflow/stress.
- KNOWN QUALIFICATION GAP: Mineflayer 4.39.0 / minecraft-data 3.119.0 lack 26.2 and 26.3, so bot-join and dd3 counts on those two are NOT_RUN (bot-less boot only). Rig bot for "26.1.2" resolves to protocol 775 ("26.1"). 1.21.11 (774) supported.
- Still unqualified: runtime behavior of detectors (gates closed), E11 numbers, X8 chunk resend, X7 perf.

## 3h. Update 8:50 Nov 17 (v12 four-target boot)
- v12 boots on all 4 targets: 0 injection hits each (Mixin application observed on 26.1.2, 26.2, 26.3, 1.21.11). Bot joins done on 26.1.2 and 1.21.11; NOT_RUN on 26.2/26.3 (no protocol data).
- Honeypot counter runtime untested (option OFF in these boots).
- Server disk 73.0% (used/size) after cleanup pending, above the 70% soft target, below the 85% hard stop.
- Next: X4/X5/E1/E2/E4 (request file box-next-X4-X5-E1-E2-request-20261117.md).

## 3i. Update 12:45 Nov 17 (anti-ESP / xray runtime, 26.1.2 rig, jar 63cf63dd, seed 202611170853)
- E1 wall cull latency: PASS under the frozen oracle. Controller pre-wall-command anchor epoch 1794938158443, victim remove packet 1794938158846, delta 403 ms (conservative upper bound 404) vs 650 ms bar. The anchor is pre-mutation (conservative), recovered read-only from the retained capture and trace (recovered-E1-trigger-8bed7f17.json), not a rerun. Bot-side present at warmup_done.
- E2 open-victim control: consistent with PASS (victim present at end, 0 remove events, 6 s window). E4 near radius: consistent with PASS (same). Neither capture records that the wall is complete stone, so both stay "within the oracle's finite window; wall completeness from fixture setup only".
- X4 dig-past v3 (frozen oracle): PASS. All 8 path cells dug, 4 walks done, final z 12.53 (>=12.35), x within 0.2, ore preserved. ON leg ore single packet: server cover mutation head 1794938718771, return ...775, ore packet received ...776; conservative bracket 0..6 ms vs 50 ms bar. OFF control behaved as expected. Evidence archive sha dfb2064f.
- X4 dig-first: first oracle failed in setup (two legacy gamerule names rejected by the 26.1.2 parser); corrected post-hoc, no geometry change, first pass stays NOT_QUALIFIED_setup_failed.
- X5 legit control: Box reported qualified; I have not seen its receipt. Do not cite until verified.
- Limits: one bot pair, finite windows, no instrumentation of per-pair evaluation ticks; no client render claim.
- Known gap: Mineflayer 4.39.0 has no 26.2/26.3 data. Slice A (frontend) is trying a raw-protocol/newer-data bot; verdict due this evening, else option (b) boot-only with the gap stated.
- Slices out: A (26.2/26.3 bot, frontend), B (Modrinth listing draft, amp), C REV2 (rig instrumentation for E11a/E11b/X7 plus bounded cold-encode, arch). Admin tools: no spec from Luke, out of the plan.
- Box batch 2 (E12, E10+E10e, E3 control+ping) written, waiting on QA oracle review.

## 3j. Update 4:00 PM Nov 17 (M1, M2 r9, logging patches, licence, policy). Capped 26.1.2 rig; not unconstrained server performance.
- E11a eval gap: measured FAIL vs the frozen 24-tick bar (coverage 1560/1560 pairs; open p50 8, max 96; wall p50 52, max 360). M1 confirmed the 1.5 ms wall-clock cap (MAX_NANOS_PER_TICK) as the binding stop (open: 455 time / 216 pairs; wall: 761 time / 89 clips). The bar assumed 200 pairs/tick, a spec defect vs B2's caps. It stays a FAIL; not edited after the fact.
- M2 r9: 40/40 repeats, orchestration complete, NOT qualified until QA verifies. Hide wait about 6-31 s, show latency 0.6-5.7 s, ratio of medians about 3.7-3.8. Correlations weak. Suggestive only, computed from the repeat table, not raw eval logs. Consistent with cullAfterChecks 3 (three real checks) plus scheduling gaps; not separable from the table. Latency label: "tick-end-position-to-receipt latency, with sampling uncertainty"; never "conservative" or "bounded".
- Logging patches (rig-only, behind -Dmeshac.rig=1, no behavior change, results labelled INSTRUMENTED): X7b (stop reason), X7c (per-pair cost c/us/ph, ep/ns, pos line), X11v2 (visit line with per-level cursors, f/p on eval, tracked-pair line with first = -2/-1/0..25 and why code, non-loading WATCH). Order X7b, X7c, X11v2 on train rig held x7 x8 dd3. Compiled on 26.3 only; QA source review pending; no rig run. Parser/validator: x11v2_parse.py (synthetic selftest only).
- Policy candidates (not frozen, not in baseline): two-class scheduler (moved pairs ~70% oldest-first, aging sweep >= 30%); reused results are not real checks; X9 and X10 stay out of any claim until a live A/B. Bars to re-derive from data: M2a p99 <= 600 ms, M2b p99 <= 5000 ms, moved real-check age p99 <= 8 ticks, stationary <= 100.
- Licence: OpenVulcan has no licence; README says NOT OFFICIAL, educational/research only, DMCA warning. Luke's rights statement is unverified; no documented grant is on file. Publishing is held until a documented grant exists and Luke gives a final yes to the listing. AGPL cannot be applied to the derivative.
- Still open: hide-delay split (needs X11 logs), 26.2/26.3 bot-join (Slice A), dig-toward qualification, honeypot Zone B, Sunday RC vs full v1 (Main's call).
