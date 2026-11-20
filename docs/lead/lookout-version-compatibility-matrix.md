# lookout compatibility matrix, source inventory

HISTORICAL, AS OF THE TAG EXPORT inspected 2026-11-19 (local tree 34e436c).
Current-fix annotations below are bound to main 7c15f68; the dependency
inspection row is bound to main 5ee7ad7 plus this slice's checker
'<version>.x' range fix. Source-compile seam
fixes landed since 271474a5: b510e25 (26.1.2, 26.2) and 4bd648b (1.21.11),
each with per-version src/mc*/java/dev/lookout overrides. No game run.
V = verified-from-source declaration/relationship, NOT verified compatibility.
U = unverified. K = known-mismatch in the inspected source. Empty value = unknown.

## version inventory and limits
The local tag export contains v0.1.0-beta
(d80dd2b27d948010a67b5fbdae7edf1679457b3e) and
archive/pre-openvulcan-20261113/* history tags, not extra Minecraft targets.
The beta README install table and exported versions/*.properties name the same
four targets below. This is an inventory as of that export, not a live tag list
or any claim about GitHub releases/assets completeness. The repository is private;
unauthenticated API 404 responses establish nothing about release availability.
Historical declarations below do not establish current compatibility.

## meshac target to lookout work
| Minecraft target | inventory | mappings/build route needed | lookout mixin/API binding | trace env | protocol number / stimulus bot |
|---|---|---|---|---|---|
| 1.21.11 | V: beta + properties | V: remap=1, officialMojangMappings, Java 21 | V: template fix accepted; V: src/mc1.21.11 overrides compile green on 4bd648b (EntityType constants, getEntityIds(), SetEntityMotionPacket.getId(), Gui.getChat().addMessage; javap-verified vs the mapped jar); U: Java 21 generated resource/client targets | V: shared unset/empty off, nonempty on; U: game load | U: / U: no verified bot adapter |
| 26.1.2 | V: beta + 26.1.properties | V: standard Loom, Java 25; no explicit mappings block | V: src/mc26.1.2 overrides compile green on b510e25 (EntityType constants, getEntityIds(), Gui.getChat().addClientSystemMessage); the former K on EntityTypes.ARROW is resolved; U: remaining client targets | V: shared unset/empty off, nonempty on; U: game load | U: / U: no verified bot adapter |
| 26.2 | V: beta + properties | V: standard Loom, Java 25; no explicit mappings block | V: src/mc26.2 override compiles green on b510e25 (EntityTypes holder, getEntityIds()); U: remaining target names/accessors need per-target validation | V: shared unset/empty off, nonempty on; U: game load | U: / U: no verified bot adapter |
| 26.3 | V: beta + properties/default | V: standard Loom, Java 25; no explicit mappings block | V: shared client source present; U: live mixin binding | V: shared unset/empty off, nonempty on; U: game load | V: bot hardcodes 777, labels 26.3; U: fresh negotiated proof |

Protocol cells intentionally leave unknown numbers blank. The protocol-777
client cannot qualify another target without an explicit verified adapter.
"No explicit mappings block" describes build.gradle only, not a claim that
mapping work is unnecessary or that namespaces are compatible across versions.
The 26.1.2 EntityType mismatch is a cross-source conflict, not a fresh compiler
error from that target. Do not call its entire API surface classified from this.

## exact client seams to validate for every target
Lookout glue has a shared src/main reference (26.3-shaped) plus per-version
overrides under src/mc26.1.2, src/mc26.2 and src/mc1.21.11 for
ClientPacketListenerMixin and ChatAlerter (compileJava source filter plus
fail-closed sourcesJar duplicate handling; other files still shared).
Meshac's VersionBridge adapters are in dev.meshac, not an automatic lookout
client adapter.
- ClientPacketListener injections: handleAddEntity, handleRemoveEntities,
  handleDamageEvent, handleAnimate, handleEntityEvent, handleSetEntityMotion,
  handleTeleportEntity, handleEntityPositionSync and handleSetEquipment.
- Verify packet accessors, descriptors and callback timing per target. The
  src/main reference uses EntityTypes, entityIds(), motion/teleport id(),
  sourceType() unwrapKey(), key.identifier(), event 35, OFFHAND and totem
  item matching; the per-version overrides carry the renamed equivalents
  verified per target jar.
- Accessor audit 2026-11-20, v2 (scripts/audit_accessors.py gate, exit 0;
  sources at this commit; each mapped jar bound by path and sha256 in the
  run output; javap dumps pinned per target): every required member the
  effective per-variant lookout sources reference resolves on its required
  targets, with the declaring class and inheritance chain recorded per row
  (340 required rows, 0 absences). Receiver coverage is derived from the
  sources' net.minecraft/com.mojang imports (fail-closed); the member list
  per receiver is curated from the call sites, exactly as CHECKS lists.
  Matching is anchored on the declaration token (SPECTRAL_ARROW never
  satisfies ARROW; negatives pinned in tests/test_audit_accessors.py).
  Packet accessors, probe and client sampling members, and the chat route
  (Gui.chatListener on 26.2/26.3 vs Gui.getChat with
  addClientSystemMessage on 26.1.2 / addMessage on 1.21.11;
  Component.literal; Minecraft.getInstance/gui/level/player/getConnection)
  are covered. The only per-target differences are the three known seams
  the overrides carry: entityIds() (26.3) vs getEntityIds() (other
  targets), SetEntityMotionPacket.id() (26.x) vs getId() (1.21.11),
  EntityTypes holder (26.2/26.3) vs EntityType constants (26.1.2/1.21.11).
  Library surface (authlib GameProfile.name, datafixers Pair accessors) is
  verified per cached library jar (path + sha256 in the run output), not
  per-target bound. No new mismatch found. This is member presence only:
  bytecode descriptors, callback timing and game load remain U.
- The 1.21.11 source conflict is resolved on 4bd648b: its override uses
  getId()/getEntityIds(), matching the mapped 1.21.11 jar (javap). Other
  affected lookout packet classes per target remain U beyond compile.
- The src/main ChatAlerter reference uses gui.chatListener().handleSystemMessage
  (26.3). Overrides: 26.1.2 uses Gui.getChat().addClientSystemMessage, 1.21.11
  uses Gui.getChat().addMessage. Game-load chat route and null-GUI evidence
  remain U for every target.
- LookoutClient/ClientWorldProbe client world sampling also need target checks.

## per-version acceptance still needed
| evidence | 1.21.11 | 26.1.2 | 26.2 | 26.3 |
|---|---|---|---|---|
| fresh target build + exact jar/dependency inspection | fresh online build green on 4bd648b; sources-jar override bytes and override class presence verified; dependency inspection exit 0 on fresh online build 2026-11-20, meshac-1.21.11-2.9.7.28-meshac-0.1.0.jar sha256 885c7490775db2333a394e57ef744f82ad6f20b5e64a48c50efead748a86f9b2, route build-remap.gradle | fresh online build 2026-11-20; check_deps exit 0 on meshac-26.1.2-2.9.7.28-meshac-0.1.0.jar sha256 40ce14f1ef9d87552d66845a271d07f2088bc9fbf67a95f25f4a4c2380f8251f, route build.gradle | fresh online build 2026-11-20; check_deps exit 0 on meshac-26.2-2.9.7.28-meshac-0.1.0.jar sha256 61085bbc7d0457fe3f9f49ba013067095e873f7da80225b25cec2c48c217c09d, route build.gradle | fresh online build 2026-11-20; check_deps exit 0 on meshac-26.3-2.9.7.28-meshac-0.1.0.jar sha256 796e9c8f8bb90706588c872f0f9745b9ef017f8bcd1573f195fc3a894466acac, route build.gradle |
| generated/shipped required:true + correct compatibilityLevel | U | U | U | U |
| real watcher boot, target delivery, chat/log capture | U | U | U | U |
| protocol-matched stimuli + G1-G4 proof | U | U | U | U |
Dependency inspection covers the known mixinextras include route,
registration and nested archive version; platform constraints
(fabricloader, minecraft, java, fabric-api) are syntax-checked for the
four forms the metadata ships ('>=<version>', exact pin, '<version>.x',
'*') but never resolved - '<version>.x' is platform metadata syntax only
and version_satisfies runs only for bundled deps. This is not general
Fabric version-range support or full dependency discovery. G4 remains UNQUALIFIED. Trace semantics have Java/stub tests, not game proof.
Main 271474a5 supersedes named-capture logging: alert/stats records use IDs, not
player names/messages. IDs and chat captures remain private and correlatable.
Any nonempty trace value, including "0", is on; unset/empty is off.
The accepted Java-level template fix replaces the old JAVA_25 hardcode and expands
both build routes from java_min. Per-target generated-resource proof is still U.

## inspected sources
Tag README install table; versions/{1.21.11,26.1,26.2,26.3}.properties;
settings.gradle; build.gradle; build-remap.gradle; gradle.properties;
src/main/resources/{fabric.mod.json,lookout.mixins.json};
src/main/java/dev/lookout/{core/LookoutEngine,mc/ChatAlerter,mixin/ClientPacketListenerMixin}.java;
src/mc{26.1.2,26.2,1.21.11}/java/dev/lookout/{mixin/ClientPacketListenerMixin,mc/ChatAlerter}.java (26.2: mixin only);
src/mc*/java/dev/meshac/compat/VersionBridge.java; bot/{bot2,victim_bot}.py.
This historical inventory is not compatibility evidence. Rig remains HELD.
