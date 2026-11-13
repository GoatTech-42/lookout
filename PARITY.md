# Meshac port parity matrix (Vulcan 2.9.7.28 -> dev.meshac)

Legend: PORTED = behavior matches upstream; DIFF = deliberate platform adaptation, behavior-preserving; BLOCKER = unresolved parity gap, path must fail safe; UNSUPPORTED = upstream feature not ported.

## Blockers (must fail safe until implemented)
- client-version resolution: PlayerData.clientVersion is UNKNOWN. Vulcan used packetevents per-client protocol detection; a 26.3 Fabric server cannot assume client == server (Via/Bedrock translators). Version-gated check branches take the UNKNOWN path (all isNewerThan/isAtLeast predicates false), matching packetevents ClientVersion.UNKNOWN. Resolution hook point: PlayerData#setClientVersion, to be fed by a ViaVersion/Geyser soft hook. (raised 09:12, confirmed by parent)
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
