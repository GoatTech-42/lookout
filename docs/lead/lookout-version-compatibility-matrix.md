# lookout compatibility matrix, source inventory

HISTORICAL, AS OF THE TAG EXPORT inspected 2026-11-19 (local tree 34e436c).
Current-fix annotations below are bound to main 271474a5. No build/jar/game run.
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
| 1.21.11 | V: beta + properties | V: remap=1, officialMojangMappings, Java 21 | V: template fix accepted; U: Java 21 generated resource/client targets | V: shared unset/empty off, nonempty on; U: game load | U: / U: no verified bot adapter |
| 26.1.2 | V: beta + 26.1.properties | V: standard Loom, Java 25; no explicit mappings block | K: shared EntityTypes.ARROW vs version bridge's singular EntityType; U: remaining client targets | V: shared unset/empty off, nonempty on; U: game load | U: / U: no verified bot adapter |
| 26.2 | V: beta + properties | V: standard Loom, Java 25; no explicit mappings block | U: shared target names/accessors need per-target validation | V: shared unset/empty off, nonempty on; U: game load | U: / U: no verified bot adapter |
| 26.3 | V: beta + properties/default | V: standard Loom, Java 25; no explicit mappings block | V: shared client source present; U: live mixin binding | V: shared unset/empty off, nonempty on; U: game load | V: bot hardcodes 777, labels 26.3; U: fresh negotiated proof |

Protocol cells intentionally leave unknown numbers blank. The protocol-777
client cannot qualify another target without an explicit verified adapter.
"No explicit mappings block" describes build.gradle only, not a claim that
mapping work is unnecessary or that namespaces are compatible across versions.
The 26.1.2 EntityType mismatch is a cross-source conflict, not a fresh compiler
error from that target. Do not call its entire API surface classified from this.

## exact client seams to validate for every target
All lookout glue is currently shared under src/main/java/dev/lookout; no
src/mc*/java/dev/lookout replacement was found. Meshac's VersionBridge adapters
are in dev.meshac, not an automatic lookout client adapter.
- ClientPacketListener injections: handleAddEntity, handleRemoveEntities,
  handleDamageEvent, handleAnimate, handleEntityEvent, handleSetEntityMotion,
  handleTeleportEntity, handleEntityPositionSync and handleSetEquipment.
- Verify packet accessors, descriptors and callback timing per target. Current
  shared code uses EntityTypes, entityIds(), motion/teleport id(), sourceType()
  unwrapKey(), key.identifier(), event 35, OFFHAND and totem item matching.
- Compare 1.21.11 meshac bridge's getId()/getEntityIds() with shared lookout's
  id()/entityIds(); this is a source conflict requiring target-specific checks,
  not proof that all affected lookout packet classes expose identical methods.
- ChatAlerter currently uses gui.chatListener().handleSystemMessage; verify the
  client chat route and null-GUI evidence for each target, not just compilation.
- LookoutClient/ClientWorldProbe client world sampling also need target checks.

## per-version acceptance still needed
| evidence | 1.21.11 | 26.1.2 | 26.2 | 26.3 |
|---|---|---|---|---|
| fresh target build + exact jar/dependency inspection | U | U | U | U |
| generated/shipped required:true + correct compatibilityLevel | U | U | U | U |
| real watcher boot, target delivery, chat/log capture | U | U | U | U |
| protocol-matched stimuli + G1-G4 proof | U | U | U | U |
G4 remains UNQUALIFIED. Trace semantics have Java/stub tests, not game proof.
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
src/mc*/java/dev/meshac/compat/VersionBridge.java; bot/{bot2,victim_bot}.py.
This historical inventory is not compatibility evidence. Rig remains HELD.
