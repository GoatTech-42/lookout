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
