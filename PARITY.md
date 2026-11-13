# Meshac port parity matrix (Vulcan 2.9.7.28 -> dev.meshac)

Legend: PORTED = behavior matches upstream; DIFF = deliberate platform adaptation, behavior-preserving; BLOCKER = unresolved parity gap, path must fail safe; UNSUPPORTED = upstream feature not ported.

## Blockers (must fail safe until implemented)
- client-version resolution: PlayerData.clientVersion is UNKNOWN. Vulcan used packetevents per-client protocol detection; a 26.3 Fabric server cannot assume client == server (Via/Bedrock translators). Historical correction below: UNKNOWN was implemented with a wrong highest ordinal. PacketEvents ClientVersion.UNKNOWN uses protocol -1; newer predicates are false and older predicates true. Resolution hook point: PlayerData#setClientVersion, to be fed by a ViaVersion/Geyser soft hook. (raised 09:12, confirmed by parent)
- client-acknowledged world cache: WorldBlockTracker returns air / no-tracked-blocks (Vulcan empty-cache semantics). Needs outbound chunk/block-change interception + palette decode to rebuild the acknowledged view. Affects hasTrackedBlockNearby (VelocityTracker unloaded-position tolerance) and any check calling WorldBlockTracker.getBlockState. (raised 09:12, confirmed by parent)

## Deliberate differences (behavior-preserving)
- Block classification: Bukkit Material.toString() matching -> registry-id uppercase matching (BlockUtils.nameOf). Modern ids cover the legacy names Vulcan matched; legacy-only names (e.g. GRASS_PATH) match via PATH substring on DIRT_PATH.
- MaterialSets.blockHardness keyed by Block; hardness via Block#defaultDestroyTime (VERIFY at compile).
- Kicks: player.kickPlayer -> connection.disconnect(Component); messages from MeshSettings unchanged.
- Confirmation packets: pre-1.17 WINDOW_CONFIRMATION branch dropped; only PING (server is 26.3).
- Potion/attribute wrappers: packetevents PotionType/Attribute -> Mojmap MobEffects/Attributes holders; amplifier math verbatim.
- GameMode -> GameType; DamageCause -> local enum with same constant names, fed by server damage hooks.
- Client-brand blacklist/whitelist gate: UNSUPPORTED (non-detection feature); brand field kept for alerts.
- Discord webhook alerts/punishments: UNSUPPORTED (non-detection feature).
- PlaceholderAPI: UNSUPPORTED.

## Counts are not proof
File/field/line counts record coverage of the port, not behavioral equality. Parity evidence comes
from the check-by-check comparison table (to be built per check family) and runtime tests.

## Gating rule (parent directives 09:12, 09:13)
UNKNOWN client version and empty world cache are NOT treated as safe defaults. Any check whose
transitive dependencies read the client version or the client-acknowledged world cache is
suppressed from evaluation while that context is unavailable - no handle(), no VL, no heat, no
alerts, no enforcement. Suppression is visible: startup log line per gated check plus parity
status output.

Dependency classification is BUILD-TIME static metadata: scripts/analyze_deps.py scans the ported
tree, builds the dev.meshac class reference graph, seeds direct uses (getClientVersion / version
helper predicates / getWorldBlockTracker), propagates transitively, and generates
check/CheckDependencies.java. Packaged jars carry no sources, so there is no runtime scanning.
FAIL-CLOSED: a check with no metadata entry (new check, stale generation) is gated as if it needed
every unavailable context. Upstream semantics are restored only when the real context lands.

QUALIFICATION (not yet done - required before calling the gate qualified):
- packaged/no-source condition: build the jar, confirm the gate behaves identically without any
  source tree present.
- indirect dependency: a check that only calls a helper which reads WorldBlockTracker must be
  gated; test one such chain explicitly.
- stale-metadata: delete a check's metadata entry and confirm it fails closed.

## 2026-11-13 09:15 update
- ContextGate v2 (ack of parent 09:13:21 directive): build-time static metadata via scripts/analyze_deps.py -> check/CheckDependencies.java. Fail-closed on unknown/missing metadata. No runtime scanning. QUALIFICATION TESTS NOT YET RUN: packaged/no-source condition, indirect/transitive suppression. Not claiming implemented/qualified until those pass.
- alert/AlertManager: ported gate order (checkEnabled, punishmentDelay, offline, TPS, ping, bypass perm, geyser/floodgate/vivecraft), VL increment, category counters, alert interval/min-vl gating. DROPPED (rebrand/scope): bungee, discord, API events; bypass node vulcan.bypass.* -> meshac.bypass.*. VERIFY: exact punish trigger condition at Vulcan AlertManager.java:394 - my port uses violations > maxViolations with VL reset; must diff against upstream line before calling verbatim.
- punish/PunishmentHandler: gate order preserved (punishable, cooldown, bypass). Routes to punish/HeatLadder adapter: currently LOG-ONLY, strictly side-effect-free, until the ladder is ported from the meshac repo (heat ladder v2).
- check/manager/CheckManager: 179 check classes listed one-for-one from upstream checkTypes with package mapping from the impl tree (not 200 - actual count is 179). Reflection construction per player.
- MeshServices: static hub replacing PluginServices; TPS via MinecraftServer#getAverageTickTime; alerts broadcast to op-level-2 players with meshac.alerts.
- Perms: op-level based (level 2), no permissions plugin on Fabric. SEMANTIC NOTE: upstream used a permissions plugin; op-level-2 is coarser. Flagged, not resolved.
- world/EntityTypeMatcher: ported (registry-id name match).
- CollisionState: still mechanical transform, NOT hand-verified, pending compile.

## Punishment trigger - verified against upstream (09:15)
Exact comparison done (AlertManager.java:386-395 + punishment/PunishmentHandler.java):
- Trigger condition: violations >= maxViolations && player online. My first draft used > and an isPunishable check - corrected.
- Gate order upstream: checkPunishable -> !experimental -> JudgementDay (DROPPED, feature not ported) -> trigger.
- VL reset: upstream resets ALL player checks' VL inside PunishmentHandler after punishment (resetCheckViolations forEach), NOT in AlertManager. Port now matches; AlertManager-side reset removed.
- Punishment body upstream = configured commands + optional broadcast + bungee forward. Port routes to meshac heat ladder instead (Luke's design, log-only until ladder ported); gate order preserved.

## 2026-11-13 09:20 - bulk check transform + event path
- scripts/port_checks.py: mechanical transform of all 179 Vulcan check impls into dev.meshac.check.impl. NOT compile-verified. TimerA hand-ported verbatim (never clobbered by script).
- Event path added: dev.meshac.event.{MeshEvent, MeshMoveEvent, MeshBlockPlaceEvent, MeshBlockBreakEvent, MeshInteractEvent, MeshProjectileLaunchEvent, MeshBlock} replacing the Bukkit event types 26 checks consume via handleEvent(Event) instanceof chains. AbstractCheck.handleEvent(MeshEvent) hook added.
- MoveEventDispatcher: faithful port of BlockInteractionListener PlayerMoveEvent feed (authoritative ServerLevel reads: surrounding/deep-floor/ground materials, waterlogged, nearby entities, frozen-setback redirect). Fired at TAIL of handleMovePlayer (main thread, post-vanilla-move). Upstream fired mid-processing at MONITOR priority - timing difference noted, both read post-update authoritative state.
- util/Vector (Bukkit-compatible subset, written fresh), Location.toVector, geometry/ray/RaySegment ported.
- MeshServices.tick: TickProcessor core (confirmation sends, lag spikes >90ms or <10ms after 500 ticks). Dropped: punishment stats broadcast, JudgementDay, discord cooldown.
- hookedBoatTicks: map present in MeshServices, POPULATION GAP (PlayerFishEvent capture not ported; fishing hook mixin needed). BoatFly hooked exemption inert until then.
- MANUAL (6 files, excluded from "ported" claims): FastBreakA (needs BukkitRegistry break-time port incl. drops/enchant/haste/fatigue), BadPacketsA (PlayerAbilities wrapper), BadPacketsF (SteerVehicle/PlayerInput wrapper), BadPacketsT (KeepAlive wrapper details), BadPackets5 (Vector3f), ScaffoldN done except verify RaySegment usage.
- Transform VERIFY list for compile: BlockState::canOcclude for Material::isOccluding; entity package shuffles (camel/horse/strider) in 26.3; MobEffects holders vs hasEffect; Enchantments ResourceKey via EnchantUtil.level; getEntityId()->getId() global rename; Direction for BlockFace.

## Correction (parent 09:49): ServerLevel feed is NON-PARITY, not faithful
MoveEventDispatcher and the CollisionState authoritative ServerLevel feeds were labeled
"faithful" - retracted. The authoritative ServerLevel is a different input domain than the
client-acknowledged world cache; whether this changes any check outcome is an OPEN parity
question to be resolved per check family before any faithful claim. Relabeled in code.

## 2026-11-13 09:53 - manual checks done, outbound + veil ported
- 5 manual checks ported: BadPacketsA (abilities; pre-1.9 flightAllowed branch dropped - 26.3 packet carries only isFlying), BadPacketsT (keepalive id via raw packet), BadPackets5 (cursor via Placement record), FastBreakA (with world/BreakTimeUtil port of BukkitRegistry: hardness*drops/1.5or5/tool level/efficiency n^2+1/haste/fatigue formula preserved; tier levels kept from upstream switch), BadPacketsF INERT (26.3 replaced SteerVehicle analog floats with boolean PlayerInput - no analog signal to threshold; fail-safe, never flags; equivalent signal TBD).
- OutboundDispatcher + ServerGamePacketListenerSendMixin: full EntityMetadataTracker routing ported (login, ping confirmations, explosion/velocity knockback, attributes, mob effects, server position -> MotionState+RelativeMoveTracker+checks, metadata, abilities, game event, entity moves/rot/teleport/spawn/destroy, passengers). VERIFY at compile: 26.3 accessor names flagged in class javadoc. Dropped: legacy window-confirmation branch, WorldBlockTracker.dispatchServerPacket (client-cache stub).
- Event dispatch wired: abilities inbound hook, LEFT_CLICK_BLOCK interact event from digging, block break (ServerPlayerGameMode.destroyBlock RETURN on success) and block place (useItemOn RETURN, BlockItem only, placed-pos approximation flagged).
- chunkveil + anti-ESP ported WHOLESALE from Luke's original meshac (veil/ package + 4 mixins), wired: Veil.tick in server tick, grace on join, forgetPlayer on quit, attacked on ATTACK interact. Config wiring to MeshSettings TODO (currently Veil defaults: xray on, esp on, cullMobs on, fakeRatio 0.02).
- ServerLevel authoritative feed = NON-PARITY (open question, per parent 09:49). Event cancellation: no protection-plugin equivalent, isCancelled always false.
- STILL MISSING for runnable slice: compile route (awaiting Luke's repo decision), CollisionState hand-verify, ContextGate qualification tests, tests themselves.

## 2026-11-13 10:21 - first Java 25 build passes

Evidence: `docs/build-evidence/2026-11-13-{compile,build}.log`. `compileJava`
and full `build` passed. No remapping task exists in this unobfuscated 26.3
Loom build, so this is type-check/package evidence, not a claimed remap test.
Gradle reports `test NO-SOURCE`. No runtime qualification, no deployment.

Port differences requiring behavioral qualification:
- 26.3 attack is a separate ServerboundAttackPacket. Both attack and interaction
  route through the existing INTERACT_ENTITY canonical event. Audit helper
  `isAttackPacket`: some check consumers may assume it means attack only.
- Sneak is absent from ServerboundPlayerCommandPacket. Input-packet sneak edge
  feeding is still missing; dependent checks must be gated until implemented.
- New dig actions CHANGE_DESTROY_DIRECTION and STAB retain distinct wrapper
  values. They are not silently mapped onto existing dig/use actions.
- Server relative entity motion now uses VecDelta decoded with a tracked base.
  This needs comparison with the vanilla client's codec lifecycle, rotation-only
  behavior and relative teleport flags. Runtime parity NOT established.
- EntityType constants moved to EntityTypes; boat registry-name matching must
  also cover rafts/chest variants. Current suffix match needs qualification.
- MobEffects renamed SPEED/SLOWNESS/JUMP_BOOST; permission level 2 is now the
  COMMANDS_GAMEMASTER native permission. Coarse permissions remain NON-PARITY.
- Bukkit's configurable maximumNoDamageTicks has no verified native equivalent.
  KillAuraL is suppressed by ContextGate and locally returns on absent data.
  COMBO_MODE treats unknown damage-window data as exempt. Do not substitute the
  current cooldown or an assumed 20-tick maximum. No impl currently names COMBO_MODE.
- MeshBlock.getFace returns null for self/nonadjacent, replacing Direction.SELF.
  Direction is currently computed from this minus other; audit Bukkit direction
  orientation before qualifying placement checks. Reference-equality block
  comparisons in upstream-transformed checks also need semantic review.
- Block registry-name matching for BED and metadata-dependent PROCOSMETICS
  exclusions were changed during porting. Their full effects remain unqualified.
- Punch singleton maps to canonical ANIMATION. Distinct attack packet is not
  normalized to animation; test both event streams.
- The unreachable legacy position/look packet branch was removed. The active
  native teleport path remains, but is NOT approved for log-only testing.
- Player weather currently uses ServerLevel rain, not per-player packet state.
  This is NON-PARITY until the consumer is qualified.

Safety blocker before any server rig launch: HeatLadder alone is log-only.
Direct and scheduled kick, teleport, rotation, held-slot and vehicle-ejection
paths remain elsewhere. Audit every direct/transitive effect and scheduled
callback, add central containment, then prove containment with tests.

Outstanding: CollisionState hand-verification; dependency metadata regeneration
and packaged/no-source/indirect dependency qualification; product layer;
client-version resolver; client-acknowledged world cache; runtime tests; veil
config wiring/testing. A successful jar is not a usable release yet.

## 2026-11-13 10:40 - central side-effect containment
- safety/Containment + safety/Effects: every gameplay-visible effect (disconnect,
  teleport, rotation change, held-slot change, vehicle eject) routes through one
  gate. Default LOG_ONLY records + logs, never executes. Headless self-test
  sim/containment/ContainmentSelfTest passes; mutation check (gate bypassed)
  fails as required. Measurement pings and veil chunk packets are protocol IO,
  not punishments, and stay direct. HTTP client disconnect (DiscordWebhook) is
  not gameplay. ENFORCE mode not enabled anywhere yet; wired to config later.
- Covered call sites: CollisionState setback teleport, ConnectionTracker +
  ConfirmationTracker kicks, CheckUtils hotbar shuffle + random rotation,
  5 vehicle-ejection checks. Queued acknowledgedTasks runnables reach effects
  only through these wrapped calls, so scheduled callbacks are contained
  transitively.

## 2026-11-13 10:47 - dependency roots implemented, analyzer de-over-approximated
- ClientVersionResolver: client version now comes from the handshake protocol
  number (ClientIntentionPacket capture mixin, applied at join). Only the
  running server's protocol maps (computed at runtime); anything else stays
  UNKNOWN and stays gated. ViaFabric/multi-version is out of scope.
- WorldBlockTracker: presence cache fed by outbound level-chunk-load,
  forget-chunk and single-block-update packets; reset on login. Chunk payload
  blocks not mirrored - outside tracked single-block updates, block lookups keep
  Vulcan's empty-cache air behavior. ServerLevel is never substituted here.
  ContextGate.worldCacheAvailable() now true; ack-confirmation (chunk batch
  received) not modeled, noted.
- analyze_deps: seeds restricted to call sites (dot-prefixed); declarations and
  comments no longer seed. Regression test scripts/test_analyze_deps.py covers
  seeding rules and stale-metadata freshness. 179/180 needsVersion/needsWorld
  at class granularity is genuine transitive coupling through
  CollisionState/MotionState/ExemptType, not analyzer noise; it is now satisfied
  by the two implementations above rather than suppressed. KillAuraL remains
  explicitly gated (maximum damage window). docs/ELIGIBILITY.md lists per-check
  needs.

## 2026-11-13 10:54 - world readiness claim RETRACTED and tracker made faithful
The 10:47 "satisfied" claim was wrong: chunk presence plus sparse updates is not
a client-acknowledged world view, and returning air for unmirrored loaded
terrain is not established parity. Correction applied:
- ContextGate.worldCacheAvailable() is false again; world-dependent checks stay
  gated until runtime qualification proves the mirrored state.
- Upstream source review (OpenVulcan WorldBlockTracker) showed the real design:
  outbound-packet mirroring with EVERY apply deferred through the confirmation
  tracker, and hasTrackedBlockNearby is a wet-potent-sulfur proximity query
  (26.1+ feature), not generic chunk presence. My presence-based implementation
  was semantically wrong and is replaced.
- Tracker now mirrors upstream: chunk columns (veil-adjusted sulfur scan),
  section multi-block updates (runUpdates), single block updates, chunk forget,
  respawn/login reset, client placements - all confirmation-deferred.
  PotentSulfurBlock.STATE verified in the 26.3 jar (DRY/WET/DORMANT/ERUPTING/
  CONTINUOUS; wet = any non-DRY, matching upstream).
- getBlockState keeps the empty-cache air fallback; it has no current consumer
  and any future consumer re-opens parity review. Dimension handling: reset on
  respawn packet covers dimension change; min/max Y fixed at 26.3 values
  (-64..319) pending a dimension-aware source.
- Version handshake proves protocol match only for vanilla paths; translators
  (ViaFabric) would still surface UNKNOWN and stay gated.

## 2026-11-13 10:57 - product layer v1 ported (unqualified)
- product/: Ladder, Cases, Config, Screens, Discord, Perm, Verdict, Trace,
  MeshCommands ported from main with one adaptation: every removal (kick,
  tempban, ban, staff kick, reconnect-ban disconnect) routes through
  safety.Effects, so containment LOG_ONLY also covers the product ladder.
  /mesh registered via CommandRegistrationCallback; join-time ban enforcement
  active; Verdict heat forgotten on quit.
- NOT wired: Vulcan-port AlertManager -> PunishmentHandler -> HeatLadder stub
  and product Verdict heat ladder are currently two parallel systems. Luke's
  design (IDENTITY.md) is signal->heat->ladder; integrating the 179 Vulcan
  checks into Verdict.signal is product-integration work that must not change
  check detection logic. Open.
- Fingerprints (client brand/channel kicks) not ported; CombatLog, Movement/
  Combat/etc. original check suite superseded by the Vulcan port. GUIs from
  IDENTITY.md remain planned-not-built; building them is post-V1 unless Luke
  re-prioritizes.
- Veil config now honored: Config.applyVeil maps veilXray/veilEsp into Veil.

## 2026-11-13 10:57 - multi-version scope (Luke: 1.21.11 and up)
Grounded deltas from the original tree (versions/*.properties, per-version
source dirs) and 26.3 javap verification:
- 26.1.2/26.2: same Java 25 Mojmap line. Original isolated the delta to one
  Packets.java (renamed accessors: getHitResult/getSequence/getHand/getLines/
  position()) and one AttackMixin. 26.x attack split exists 26.1+, so our
  AttackMixin path carries over; effort = per-version properties + a packets_a
  alias source dir + dependency resolution + build + rig smoke each.
- 1.21.11: heavier. Obfuscated toolchain with remap=1 + build-remap.gradle,
  Java 21, attack inside ServerboundInteractPacket (Handler dispatch), older
  EntityType/MobEffects/permission APIs (original kept a per-family Perm.java).
  Rebuild code must stay Java 21 compatible to share the tree.
- Concrete plan: keep one tree, add versions/*.properties + variant source
  dirs exactly like the original, keep the packet/entity/effect accessor seam
  in one place per version family. Monday V1 on all four targets is the risk:
  26.3 itself is not yet runtime-qualified; each added target needs its own
  build + rig smoke. Grounded choice for Luke: V1 Monday = 26.3 qualified,
  26.2/26.1.2 fast-follow (same generation), 1.21.11 after (toolchain port);
  or V1 date moves to include all four.

## 2026-11-13 11:05 - Independent QA (fc30762) reconciliation, commit pending
Fixed in this commit (source-verified):
- ContextGate was dead code: all four dispatch loops (PacketDispatcher x2, OutboundDispatcher, MoveEventDispatcher) now skip ContextGate.isGated checks. A gated check gets no handleEvent/handlePacket call: no VL, heat, alerts or effects.
- /mesh ban bypass: under LOG_ONLY the whole transaction is simulated (no Cases.add, no disconnect, no webhook; staff sees "[log-only] simulated ban"); under ENFORCE the case persists and Discord.post only fires if Effects.disconnect actually ran.
- Verdict REMOVE under LOG_ONLY is now a SIMULATED audit log line only: no case-store persist, no escalation state, no webhook; Effects.disconnect records intent via containment. Discord.hold posts only in ENFORCE.
- JOIN short-circuit only when the ban disconnect really executed; a contained (simulated) ban no longer leaves a connected player without PlayerData.
- WorldBlockTracker unpack Z/Y sign extension (was unsigned: z=-1 -> 67108863, y=-1 -> 4095).
- Sulfur local-index layout now matches Veil.Mod.idx (x | y<<4 | z<<8; was transposed y<->z).
- Historical 928f010 approach (superseded below): handleChunkColumn froze a live section scan and consumed a global emitted-mods record. That approach did not establish packet-payload parity.
Recorded OPEN (not fixed this round):
- Historical diagnosis corrected below: live LevelChunk scanning was not exact packet capture. The assertion that packet section bytes were unavailable before Netty encode was wrong; native packet-data construction already serializes them. World gate stays CLOSED.
- Checks read world via MeshBlock -> live ServerLevel, not the client-ack cache (arch finding): with the gate wired, world-dependent checks are suppressed; real client-view integration is required before reopening, not a flag toggle.
- handshakeComplete defaults true / setJoinConfirmationId has no callers (dead join anchor) - confirmation join anchoring unproven.
- Upstream chunk-load tracks block entities only vs our full-section sulfur scan: PotentSulfurBlock block-entity status unverified in 26.3 jar.
- Dispatcher gating regression test with mutation (QA proposed) not yet written; analyzer script STALE-still-PASS and git-failure gaps open.
- Veil fixture lane: hidden block-entity NBT unfiltered, cache dimension-blindness (separate from emitted keys), encode swap race window, fake-ore Y plausibility, ESP tick-8 latency (real 400ms vs proposed 250ms bar - requirement unverified, behavior unchanged).

## 2026-11-13 - Safety regression batch after independent 928f010 review

The earlier statement that LOG_ONLY could no longer poison persistent punishment state was too broad: staff kick still persisted a case, and pardon/unban still changed real cases. That claim is superseded by the changes and limits below.

- Staff kick, pardon and unban now identify a simulated action and leave the case store unchanged in LOG_ONLY. Cases.add/pardon/unban also enforce the same rule at the mutation boundary, protecting future callers. Simulated Cases.add returns an unstored object with ID SIMULATED.
- Product Discord.send now checks containment before constructing a request. Empty config defaults were not an egress safety barrier. Nonblank inherited settings no longer bypass LOG_ONLY.
- Config.applyVeil now holds xray/ESP/cullMobs off unconditionally. Reloading an inherited config cannot enable unqualified packet rewriting. Hidden block-entity NBT, concurrent live-world swap and dimension-blind cache findings remain open; no veil runtime use is cleared.
- The real registered commands, store/config/Discord/Effects and Verdict REMOVE now have headless behavioral tests. The production dispatch bodies have synthetic-check behavioral tests on all four evaluation boundaries plus placement/digging callers. All four missing-guard mutations are rejected; five product safety mutations are rejected. See docs/SAFETY-TESTS.md for exact scope and limits. These are compiled fixture-jar tests, NOT full shipping-mod/Minecraft runtime qualification.
- Analyzer --check now fails stale/missing generated metadata without writing it. The regression suite no longer relies on Git status or ignores a Git failure. The initial Containment mode assertion now inspects the fresh class before changing mode.
- No world-context changes in this batch. The 928f010 live-at-send snapshot, lossy emitted-map key, consume-on-read multi-recipient fault, JOIN/confirmation lifecycle, world-reader parity and veil issues remain unresolved. World eligibility stays closed. No launch, no client session, no live deployment.

## 2026-11-13 - 26.x compile-time version seams

Actual baseline build attempts at 2eb56ce failed: 26.2 had 17 compiler errors; 26.1.2 had 39. The earlier "renamed accessors only" description was incomplete for 26.1.2.

Added explicit mc26.1.2/mc26.2/mc26.3 source adapters selected by exact minecraft_version. Common code now delegates only API differences: placement/chunk/entity-removal/vehicle accessors, short-delta vs VecDelta decode, native entity-type constants, potent-sulfur presence, and three newer movement attributes. AnimationMixin uses the exact target packet descriptor (Swing for 26.1.2/26.2; Punch for 26.3). Digging maps the same native enum names via a string switch so a newer optional action need not exist in an older compile target; unknown values throw rather than silently translate.

26.1.2's native jar has no potent-sulfur block or BOUNCINESS/AIR_DRAG_MODIFIER/FRICTION_MODIFIER attributes. Its adapter reports native feature absence, not a synthetic detection result. These branches need independent semantic review. No check threshold, punishment threshold or ContextGate eligibility changed. 1.21.11 is not supported by these adapters and remains unbuilt.

All three 26.x builds now compile/package. Packaged checks validate target-specific fabric.mod.json, Java 25 class major version and all declared mixin class files. processResources tracks mc/java inputs so switching target cannot reuse stale metadata. Output names include the MC version. These checks do not validate mixin application or gameplay.

Independent QA accepted the headless 2eb56ce safety batch and its exact reload-test delta (SHA256 23caceaa5f2c9bd6d3dfd02f08470efa1f405ce4130f9b7575893eb5bdb269b6). The unchanged reload-test delta is included here. No private-boot or client clearance inferred; world gate remains closed and veil off.

## 2026-11-13 - 1.21.11 build/remap route

The isolated baseline probe restored the old remap plugin route against 1.21.11 and used the nearest compiled adapter only to expose missing APIs. It reached compileJava and reported 18 errors. No baseline support or runtime result was implied.

Added an explicit 1.21.11 adapter and Java 21/remap route. Native interact dispatch supplies ATTACK/INTERACT/INTERACT_AT (entity ID via exact accessor); 26.x retains its separate native attack hook. Both feed one common interaction-processing order, preserving raw packets, tracker updates and check dispatch. ChunkPos.toLong, motion packet getId/getMovement and a ServerPlayer-typed message local close the verified older API differences.

All four targets now compile and package: 1.21.11 on the remap route with Java 21 bytecode, and 26.1.2/26.2/26.3 on the native route with Java 25 bytecode. Output metadata, bytecode major, mixin compatibility level and declared mixin class presence are checked per target. The 1.21.11 packaged mixins contain intermediary target references. A RED package check caught a Java-25 mixin compatibility declaration in the Java-21 jar; resource expansion now sets the correct per-target level. Each build route rejects mismatched java_min.

Loom printed "Cannot remap modifiers because it does not exist in any of the targets [] or their parents" while preparing the 1.21.11 build. Build/remap succeeded; the warning is unresolved and retained in evidence, not dismissed as harmless. No mixin application or in-game boot has run. No check availability, threshold, enforcement mode or veil setting changed. Sneak/input feed, packet lifecycle and runtime qualification remain open across targets. Existing headless safety suites/mutations still pass after the shared interaction extraction.


## 2026-11-13 - Packet-owned sulfur snapshot, bounded headless evidence

Replaced live LevelChunk scanning and Veil's global emitted-mods handoff with immediate decoding of the actual outbound packet's section byte array. Detached sections use the target native palette factory. The scan considers emitted block-entity positions, like upstream applyChunkColumn, and freezes only sulfur positions before deferring application. Each recipient independently reads the same packet; no consume-on-read metadata remains. Updates copy mutable positions before deferral. Frozen sets are copied back into mutable cache sets, and reset generations discard queued old-world callbacks.

Native getReadBuffer bytecode constructs a new FriendlyByteBuf around Unpooled.wrappedBuffer(packetByteArray). Releasing that independent wrapper after reading does not consume the packet's byte array or another recipient's reader index. Section bytes already exist at packet-data construction; no new encode-point hook is needed for this feed. This fixes the earlier timing explanation, not the unsafe Veil rewrite itself.

The actual WorldBlockTracker passes a headless packet-buffer fixture and six rejected mutations: delayed capture, missing reset-generation check, mutable section-update positions, unmodifiable live sets, unsigned negative Z, unsigned negative Y. Cases include shared packet/two recipients, post-capture source mutation, negative coordinates, no emitted tile entity, query radius boundaries, and subsequent removal. The packet buffer, palette decoder, native adapters and ConfirmationTracker are boundary doubles, not Minecraft-runtime proof. No native decoder roundtrip, JOIN/network lifecycle, real acknowledgement order, dimension transition or concurrent execution is qualified by this suite.

All four targets compile/package after this change, with per-target metadata/class/mixin-presence checks. The first 26.1.2 invocation used the wrong Fabric API version 0.154.0 and failed on EntityFluidExtension; the corrected previously-cached 0.155.3 build passes. Both logs are retained. Existing product safety/dispatch/analyzer suites and mutations pass. Gradle test remains NO-SOURCE. The prior 1.21.11 remap warning remains unresolved.

Scope limits: full block-state mirroring and placed-block prediction are absent. The sulfur view is not general world-cache parity. Per-consumer authoritative world reads must be compared to upstream; they must not all be replaced with this partial view. Confirmation runAfterConfirmation may attach to the last pending ping or run immediately with no pending ping; JOIN anchor and ordering remain open. Generation checks are not a concurrent collection/thread-ownership proof. World eligibility remains closed, Veil stays unconditionally off, runtime hold unchanged. No server/client was launched.


## 2026-11-13 - JOIN identity and outbound send declaration repair

Native PlayerList.placeNewPlayer sends ClientboundLoginPacket before constructing ClientboundPlayerAbilitiesPacket on all four targets. Cached Fabric networking 26.3 source invokes ServerPlayConnectionEvents.JOIN immediately before the abilities-packet constructor, not at method tail. PlayerData was only created at JOIN; therefore its earlier login branch could not set entityId, leaving -42069. PlayerDataManager.onJoin now reads actual ServerPlayer.getId() before publishing the session. No INIT creation or duplicate session path added. A failing-then-passing test exercises the actual manager with explicit boundary doubles, including no login callback, two players, quit/rejoin. It does not test Fabric callbacks or native sessions.

All four native listener declarations confirm send(Packet) belongs to ServerCommonPacketListenerImpl, not ServerGamePacketListenerImpl. The send mixin now targets the declaring common class, removes the nonexistent shadow, and dispatches only when this is the game listener. The exact descriptor is checked against all four actual native jars; this test first rejected the old subclass target. This is a static target check, not successful mixin application or a connected-client result.

Correction to review hypothesis: native 26.3 PlayerList.respawn explicitly sets the replacement player's ID from the previous player's ID. A new player object alone does not prove changed entity ID. No speculative respawn ID workaround added. PlayerData's stored player reference across replacement and thread ownership still need lifecycle review. No JOIN handshake/confirmation timing policy was changed. The missing initial join-confirmation ping/handshake lifecycle remains open.

All four builds and package checks pass after the repair. World, product, dispatch, analyzer suites and their existing mutations pass; the new lifecycle tests pass. Native declarations, PlayerList bodies, login accessors, cached Fabric source, RED tests and build logs are under docs/build-evidence/lifecycle. Four-target native evidence does not imply Fabric source verification for all four. Gradle tests remain NO-SOURCE; earlier 1.21.11 remap warning remains unresolved. No server/client launched, runtime hold unchanged, world eligibility closed, Veil off.

## 2026-11-13 - Session metadata and copied player reference

Upstream UserDataInitializer explicitly populates UUID and player name before creating checks. The port left both at defaults despite available native player identity. onJoin now sets UUID/name alongside entity ID before checks and publication. Alert interpolation and prefix filters therefore have the real session name.

Native respawn creates a replacement ServerPlayer but copies the old entity ID. Cached Fabric ServerPlayerMixin fires COPY_FROM after restoreFrom, before native respawn sends its state packets. Registered COPY_FROM refreshes the existing PlayerData player reference only if it still points to that previous object. It does not recreate checks, reset buffers, invent an ID, or change confirmation policy. This avoids waiting until AFTER_RESPAWN, which is after respawn state packets. Cached API source evidence is for 26.3; other targets compile against their actual dependencies but event invocation on those targets has not run.

The actual PlayerDataManager fixture first failed unset UUID/name and missing copied-reference behavior. It passes UUID/name, same session across replacement, preserved entity ID, stale callback protection, untracked-copy handling, two users, quit and rejoin. Four mutations are rejected: omitted UUID, omitted name, omitted player refresh, missing old-reference guard. PlayerData/native/Fabric boundaries remain doubled in this headless manager fixture; no game session or event delivery is proven.

All four actual builds/package checks pass after this local-only delta, and existing world/product/dispatch/analyzer suites and mutations pass. Evidence under docs/build-evidence/session-reference. Remote integration remains held separately; no push/rebase/merge/delete was attempted for this delta. Runtime admission requires the independent review and bounded isolated smoke, not this log. World gate closed; Veil off. No server/client launched.

## 2026-11-13 - First isolated 26.3 shipping-artifact boots

Independent review admitted a disposable loopback-only 26.3 boot, not detection qualification. Boot 1 with the unmodified 691d1a9 artifact failed: fabric.mod.json registered a DedicatedServerModInitializer under main. Corrected resource key to server, preserving onInitializeServer. Packaged-entrypoint contract test reproduces RED before repair. Boot 2 then failed actual AnimationMixin application: 26.3 target is handlePunch(ServerboundPunchPacket), not handleAnimate. Corrected only the 26.3 hook method string; native declaration test rejects the prior target and checks all four adapters.

Boot 3 with the rebuilt shipping artifact reached Done on 127.0.0.1:25587, then cleanly stopped. Actual meshac common accessor/pong/send, inbound/animation/attack and chunk-map mixins applied on loaded classes. No player connected, no JOIN/DISCONNECT/self-velocity/respawn/ban-lifecycle or native packet decoder outcome is established. ChunkDataMixin transformation was not observed during this boot, so it remains unproven. No test observer or changed instrumentation was bundled as shipping code.

All three full logs, exact artifact hashes, server properties and inherited product config retained in docs/build-evidence/boot26.3. Config deliberately requests veil booleans true to exercise the unconditional qualification hold; monitorOnly=true, webhook empty. Containment remains LOG_ONLY, world-dependent checks remain gated, Veil remains off. No real account, user host, public listener, live world, Meadow guard bypass or release used. Boot success is not a V1 readiness claim. Next bounded phase requires a disposable test client and direct lifecycle observations.

## 2026-11-13 - Isolated vanilla 26.3 lifecycle with separate observer

Shipping artifact 04fcd24381fbceecd619633c9dbce105128977731ff58426b9e6a70c4599a770 unchanged from boot 3. A separate clearly labeled read-only observer mod logged actual MeshServices/PlayerData and Fabric lifecycle callbacks. No production patch, hook substitute, gate override or observer counters were bundled into meshac. Public vanilla client assets/dependencies were checksum-checked from Mojang's manifest; test-only offline MeshRigA identity, loopback endpoint, no real account/session.

Observed: initial client joined and rendered the disposable flat world. All 13 declared meshac mixins transformed in actual server log, including previously unloaded handshake/chunkdata/entity targets. Native chunk sends produced no logged payload decode failure during this run; this is not a complete codec matrix. Session entity ID matched native ID, UUID/name populated, native protocol resolved V_26_3, stored player reference current, containment LOG_ONLY and Veil false,false,false despite inherited true requests.

CORRECTION: analyzer metadata lists 180 classes, but JumpD is an empty class, not an AbstractCheck or CheckManager entry. Actual instantiated check count is 179, and all 179 were gated. Any prior "JumpD active" statement described metadata only and was not a real active detector. No working detection is established by these runs.

A following run used a pre-existing local test ban case. JOIN logged a contained disconnect, then still published a valid session. Its file remained byte-identical. Vanilla immediate_respawn + kill triggered actual COPY_FROM. That callback initially saw a temporary new native ID before vanilla's setId; the next tick showed the original ID retained and stored player reference updated. The observer did not overwrite that ID. Disconnect while server remained running showed zero native players and zero stored sessions. Rejoin produced a new valid session and another contained ban disconnect. The rejoin screenshot was captured while client still displayed Joining world, so rejoin server-callback success is not claimed as fully rendered second-world readiness. Initial rendered-world evidence is separate.

Still unproven: self-velocity exactly-once hook count, detection outcomes/false positives, full acknowledgement/thread ordering, all legitimate scenarios, Meadow behavior, other-version game sessions. No thresholds, eligibility or Veil settings changed. Dummy-token Realms authentication and absent-audio warnings are retained; they did not prevent the initial world render. Test server/client/display processes stopped, no public listener or user hardware used. Observer source/scripts and logs/hashes under sim/lifecycle/observer and docs/build-evidence/lifecycle26.3. Observer-present results are distinct from bare boot results.

## 2026-11-13 - Private main consolidation

Following the owner's authenticated confirmation, preserved old main as legacy/pre-openvulcan and all 22 original branch tips as archive tags. Verified a complete local recovery bundle, recorded reachability/unique-patch audit, merged current main history without force pushing, and advanced main to the rebuild. Removed 21 obsolete branch heads only after exact remote archive verification, using one atomic push and exact expected-tip leases. Two remote heads remain: main and legacy/pre-openvulcan. Unique old work remains reachable by tags. README, contribution build instructions, documentation map and repository recovery guide now describe the actual private rebuild state. Old detection code was not blindly merged into new logic. No V1 publication, visibility change or release was performed; old-code recovery remains until finished/published V1.

## 2026-11-13 - Native server version initialization and UNKNOWN comparison

ExemptionManager.init had no production caller, leaving its serverVersion at UNKNOWN. Server predicates therefore did not describe the running server. It now initializes from native SharedConstants.getCurrentVersion().name() when the class loads. The native version source is shared with the existing exact-protocol resolver, not guessed from a remembered version number.

Separate defect: MeshVersion placed UNKNOWN last and used raw ordinals, treating an unresolved client as newer than every known release. Exact PacketEvents v2.14.0 ClientVersion source (the version pinned by upstream build.gradle) declares UNKNOWN(-1) and compares protocol numbers. UNKNOWN is therefore older than supported clients, not newer, and not "all predicates false". MeshVersion now uses a comparison order of -1 for UNKNOWN while preserving all known chronology. Earlier comments/ledger claims corrected. ContextGate still explicitly blocks UNKNOWN; no eligibility or threshold was relaxed.

Source references: https://raw.githubusercontent.com/retrooper/packetevents/v2.14.0/api/src/main/java/com/github/retrooper/packetevents/protocol/player/ClientVersion.java and https://raw.githubusercontent.com/retrooper/packetevents/v2.14.0/api/src/main/java/com/github/retrooper/packetevents/manager/server/ServerVersion.java . Retained source excerpts under docs/build-evidence/version-contract. ServerVersion itself compares enum ordinals and has a different sentinel domain; the repaired runtime server initialization uses the actual named supported server, not client UNKNOWN as a server release.

Actual MeshVersion/ExemptionManager headless tests first failed uninitialized native version, then UNKNOWN-as-newest. They pass native 26.1.2 initialization, known chronology and client UNKNOWN protocol -1 ordering. Three mutations are rejected. Native SharedConstants and service boundaries are doubles in this fixture, so it is not a new server boot. All four actual builds and package/entrypoint checks pass. Existing headless lifecycle/world/product/dispatch/analyzer suites and mutations pass. No runtime rerun for this artifact yet; prior runtime results remain attached to their exact earlier jar hash. All179 instantiated checks remain gated, Veil off, LOG_ONLY, JOIN anchor/input/threading/self-velocity proof still open.

## 2026-11-13 12:29 - bounded initial JOIN anchor, runtime pending

Initial handshake now starts false. Login-send HEAD initializes one session before native write; Fabric JOIN still performs its ban check and reuses that session. A login-only native Connection.doSendPacket TAIL sends the upstream random anchor after channel.write, with ID and duplicate/reentrancy guard set before send. Periodic sendConfirmation(true) restores event-loop scheduling with live PLAY/session/connection rechecks. The false argument stays inline. See docs/build-evidence/join-anchor/README.md for exact source pin, RED/GREEN, eleven mutations, native4 descriptor/accessor and packaged-registration evidence, current four-target builds and hashes. No changed-artifact runtime yet; previous real-client claims remain hash-bound to their earlier jar.

This does NOT fix global ownership: ordinary outbound hooks run caller-side, and inbound HEAD hooks precede vanilla server-thread handoff, permitting duplicate dispatch on replay. No gates opened. Native input evidence corrects the earlier 26.3-only note: all four targets lack legacy sneak command constants and expose Input.shift(), but the feed is missing. Unknown-action/legacy-callback exceptions have no local containment catch. The 1.21.11 forced fresh-project-cache build executes compileJava and reproduces the configuration-stage modifiers remap warning; retained Fabric ContextChainAccessor and Brigadier field evidence narrows its source without declaring it harmless. Other versions remain compile-only.

### 12:34 review scope clarification

Initial anchor is one-shot per PlayerData. Upstream schedules after every JOIN_GAME; same-session configuration-to-play reentry would therefore diverge, and has not been qualified. Common-listener send declaration receipts now cover all four targets. Duplicate join-PONG handling is not idempotent in either the current port branch or pinned upstream logic; ingress exactly-once is separate. PacketEvents source also shows sneak EntityAction removed since1.21.6, with separate PlayerInput not consumed by pinned upstream OpenVulcan. Missing modern sneak handling remains unsupported, not an assumed parity repair to synthesize silently.

## 2026-11-13 12:40 - 26.3 native initial-anchor runtime observed

Shipping jarbf0697545885b23bbfb5d271d842a392c5d6de3d0fbd337a3eb0a601c8e729a8, code239033f, now has isolated26.3 runtime evidence. Native login write entity56 preceded anchor-11207, whose ID was assigned before write; matching PONG completed handshake false->true with game-listener/current-connection/player identity checks true and no periodic pending entry. All16 shipping mixins applied. Native serverVersion initializedV_26_3 from26.3. Separate read-only observer present; shipping jar byte-identical. Screenshot inspected as a fully rendered flat world. LOG_ONLY, all179 checks gated and Veil OFF stayed in place; case bytes unchanged. No public route existed in the test network namespace. See docs/build-evidence/anchor26.3 for source, logs, hashes, process exit evidence and retained earlier observer failure/first loading-screen capture caveats. No broader detection/ownership/other-version qualification follows.

### 12:44 native login route contract pinned

All four actual PlayerList initial-login paths invoke the one-argument game-listener send(Packet)V overload. The game-listener constructor sets player.connection first. The native target checker now verifies that first-send descriptor rather than assuming every login reaches the early-publication hook. Thus the confirmed native route publishes PlayerData before the Netty task can perform login write; alternate direct two-argument/Connection login routes remain unqualified. No production change or fallback added.

## 2026-11-13 12:49 - defensive current-listener PONG admission, runtime pending

Separate candidate: common PONG hook verifies its own live native Connection against current PlayerData/player/listener/PLAY state before dispatch. This closes a source-reproduced stale-listener same-UUID/colliding-ID boundary; it is defensive session isolation, not a claim of exact upstream rejection policy. Actual-class RED/GREEN/mutation cover stale/replacement/same-player connections, phase/close, valid copied-player and ordinary replies. All four current builds execute compileJava; headless regressions pass. No changed-artifact runtime yet; bf069754 native anchor evidence does not migrate. Duplicate anchor tolerance, ownership/after-send and stale-quit behavior remain unchanged and open. Gates stay closed.


### Explicit listener admission and isolated normal join

The reviewed PONG follow-up now compares THIS listener to both Connection.getPacketListener and player.connection, retaining the transport/session/PLAY checks. Same-player/same-Connection replacement is fixture/mutation evidence only, with native stale-callback reachability unproven. Source d7ffae8 has normal26.3 changed-artifact join evidence pinned to jar2f7b63892b17c87b5de0111c32e0263b145df6cc95d5f8878ab8f2e9888f0ec8, under docs/build-evidence/session-pong26.3. That positive run does not prove native stale-callback exclusion.

### Finite movement ingress candidate

Only ServerboundMovePlayerPacket tracking now uses the admitted Connection.channelRead0 genericsFtw boundary, before native handler thread handoff, instead of game-handler HEAD replay. Authoritative pre-move capture stays server-side after successful ensureRunningOnSameThread; the existing TAIL event stays separate. Actual hooks/dispatcher/session transport pass RED-to-GREEN and six mutation rejections with modeled native boundaries. Four native descriptors/branch order are retained; four compileJava/build/package routes pass. See docs/build-evidence/move-ingress. No changed-artifact native run yet. Other inbound HEAD hooks, global concurrent ownership, authoritative event/read qualification, velocity/explosion after-write and callback lifetime remain open. Gates closed, Veil off, LOG_ONLY.


### Movement runtime failure and canonical ground sample repair

The first driven26.3movement run of a8ac337 failed, despite anchor-only runner exit0: CollisionState setters populated duplicate subGroundBlockState/deepGroundBlockState fields while readers classified null subGroundMaterial/deepGroundMaterial. Full native stack and failed runs are retained under docs/build-evidence/move26.3. Pinned upstream uses canonical material fields consistently. The repair removes those duplicate aliases, with actualCollisionState/real-native-block RED/GREEN and assignment mutation evidence. No guessed block or exception swallow. Final source-bound jar0566dd2f9619d51525056330e24cddae02d16726af39344cb04ccff867aaa330 passes movement-specific native assertions:62 admittedchannel/62dispatcher/124handlerHEAD/62servercapture/TAIL/event,47 changed positions, no early disconnect. All gates remain closed. This is finite movement qualification, not detector/global ownership or other-family/version readiness.


### Encoded-write completion candidate, not yet runtime qualified

Self-motion/explosion preparation moves to the successful native PacketEncoder encoded-write boundary, through a filtered MessageToByteEncoder operation wrapper. Native encode failure produces no preparation. Tasks attach to the unvoided invocation promise before ctx.write and run on completion without a success predicate. Pinned PE v2.14.0 encoder source SHA d3cafc0dd6a6358416cc49cf1d213f5cba134335bc310802126ac730d22f6008 establishes that finite timing contract. PE cancellation can complete tasks without ctx.write; full cancellation/event policy parity is not claimed. Current-session/PLAY/listener completion rechecks are explicit defensive divergence. Direct Connection/bypass-listener sends deliberately gain this narrow upstream-like tracking. Other outbound routes remain caller-side.

Actual callback/dispatcher/tracker tests use real Netty promises with modeled raw argument capture. Native3site receipts are retained for exact Netty4.2.7 (1.21.11/26.1.2),4.2.15 (26.2),4.2.16 (26.3). This is not actual superclass weaving, native unbundler fanout, arbitrary sync processing throws or full ownership proof. Unused listener/flush loops were removed, not credited as coverage. Fabricloader floor is0.19.5 and MixinExtras>=0.5.5. No gate opened. Candidate stays unlanded pending native changed-artifact and review closure.


### Finite26.3 encoded-write runtime observed

Final metadata-floor artifact50ee984dcc14027464d72021e017806c01097253ce3d9e64f542cdfc4557fd53 has native superclass weaving and a deliberately driven positive packet scenario. Separate test-only driver sends direct self-motion, two-argument self-motion with caller listener, a native bundled self-motion child and an explosion. Read-only observation records actual raw packet classes at the callback factory and exactly3motion/1explosion completion callbacks in the scenario, on Netty. Native caller reports success. Initial anchor and driven movement remain positive,68channel/68dispatcher/136HEAD/68captureTAIL/event,54changedpositions. Client remains connected until deliberate stop; case bytes unchanged; gates179/179, Veiloff, LOG_ONLY. Screenshot inspected rendered flatworld/HUD/hand/tutorial. See docs/build-evidence/outbound26.3. Factory entry for unrelated packets is NOT a created task. Other native target runtimes, failed/nativevoid/cancellation paths, arbitrary concurrent ownership and full detector/false-positive readiness remain unqualified. No publication or gate opening.


### Finite held-item ingress and native replay observation

Held-item tracking now uses its own admitted Connection.channelRead0 genericsFtw hook instead of game-handler HEAD. Other remaining HEAD families are unchanged. Actual-hook/dispatcher modeled replay fixture is RED/GREEN; six mutation probes include exact wrong invocation descriptor and AFTER-shift rejection. Movement fixture also now rejects AFTER shift; its descriptor assertion already existed. All24 headless regression suites pass, with interrupted final batch and bounded retry receipts retained.

Changed26.3 artifact1f777c1f87f6810223bc2b57bec8c7c4f6684b9a0b1a38dbe6794d871c225943 records4driven native hotbar packets, slots1,2,4,0, each admitted->dispatch->conversion-entry->networkHEAD->serverHEAD->TAIL. Movement70/70/140/70captureTAILevent passes. The runner records finite positive outbound callbacks/caller success only; the unchanged strict outbound validator FAILS on the held trace because unrelated motion factory entries contaminate its scenario window. No deterministic self selection exists in these raw logs. Prior outbound trace remains strictGREEN, and all outbound class bytes remain identical; no refreshed strict outbound qualification is claimed. Failed first shutdown-tail validator run is preserved, not credited. Final sender-freeze/observed-drain shutdown avoids truncation without dropping partial packets. See docs/build-evidence/held26.3 for limits, raw logs and separate observer. All gates remain closed, Veiloff, LOG_ONLY. Not malformed-slot policy, wrapper-content, global ownership, all-target runtime or detector readiness. Pending independent closeout; no release.

### Finite player-abilities native-constructed handoff candidate

Only abilities HEAD tracking moves to a separate admitted channel hook with existing session guards. Actual hook/dispatcher modeled replay RED/GREEN and6mutation probes pass;4build/package routes pass. Final26headless suites pass across explicit interrupted/retry logs. Native26.3shippingjara9a1d272ae0f87192b142b400bb53b15db1b6ac5990105acb80cdd9b18496b5d observes2nativeconstructedtrue/falseidentities from real pipeline context immediately before packet_handler through admitted/dispatcher/event/networkHEAD/serverHEAD/TAIL. Event direction/currentplayer validated. This excludes wire/clientcodec/UI/flightpermission/checkeffects, with failed realclient attempts retained. Ordinary movement35/35/70/35allchanged, strict outbound finite validator passes, cases unchanged, gatesclosed/Veiloff/LOG_ONLY. See docs/build-evidence/abilities26.3 for exact scope, failed attempts and validator mutations. Pending independent final closeout, not landed or released.
