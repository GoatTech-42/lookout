# Initial JOIN confirmation boundary

This is a bounded initial anchor and periodic-send scheduling change, not proof of global tracker thread ownership. No changed-artifact runtime has run. All detector gates stay closed, LOG_ONLY remains in force, and Veil stays off.

## Source contracts

OpenVulcan source pin `f5640d1fbd19710bd398cb3945dedf605eadc225`, fetched again and byte-compared with the retained source:

- https://raw.githubusercontent.com/Skid-Technology/OpenVulcan/f5640d1fbd19710bd398cb3945dedf605eadc225/src/main/java/me/frep/vulcan/spigot/tracker/entity/EntityMetadataTracker.java
- https://raw.githubusercontent.com/Skid-Technology/OpenVulcan/f5640d1fbd19710bd398cb3945dedf605eadc225/src/main/java/me/frep/vulcan/spigot/tracker/confirmation/ConfirmationTracker.java
- https://raw.githubusercontent.com/Skid-Technology/OpenVulcan/f5640d1fbd19710bd398cb3945dedf605eadc225/src/main/java/me/frep/vulcan/spigot/packet/listener/PacketListener.java

Upstream starts handshake false, assigns an initial random ID in [-11500,-11000) after JOIN_GAME send, then accepts the matching reply without adding it to the periodic pending queue. Tick sends request channel-event-loop execution. The false scheduling argument stays inline. Existing latest-pending/immediate `runAfterConfirmation` behavior is unchanged.

## Native adaptation

Fabric JOIN is after the login packet. The existing send HEAD now initializes the session before login dispatch; the later real Fabric JOIN callback still runs its inherited-ban check and reuses the session/checks. Native identity initialization is unchanged. The native game listener constructor sets player.connection before login send, as recorded in the retained listener bytecode.

A login-only Connection.doSendPacket TAIL hook sends the anchor after the native channel write. All four native targets have the exact declared descriptor, a single normal return, and sendPacket's channel-event-loop handoff. Connection.channel and PacketEncoder.protocolInfo accessors are checked against all four native jars. The encoder's actual current protocol, live connection, listener/player identity and current session are checked again at queued execution. The anchor ID and sent guard are assigned before send, including reentrant callbacks. Duplicate login callbacks do not restart the same session. New sessions on replacement connections get their own anchor.

`check_anchor_targets.py` retains native javap evidence and reads all four packaged mixin configurations/classes, including required registration. This is more than compile evidence but is not a Mixin weave test. Native runtime application is pending independent review.

## Tests and limits

`run_join_anchor.py` compiles actual PlayerData, PlayerDataManager, ConfirmationTracker, PendingConfirmation, dispatchers, initializer and hook classes. Minecraft, Netty queues, services and unrelated trackers are explicit headless boundaries. It invokes the real registered Fabric JOIN callback. It does not load Minecraft or apply Mixin transformation.

Retained REDs catch handshake default, missing pre-write session, and missing post-write anchor. GREEN covers login HEAD publication then channel write then anchor, matching synchronous reply, unrelated reply, duplicate and reentrant login, inherited-ban containment on Fabric JOIN reuse, identity/check initialization, periodic true scheduling, false inline generation, normal/support-mode queue behavior, phase change before queued execution, close before execution, stale old connection and replacement session. Eleven production mutations fail with assertions. Existing session, version, world, product, dispatch and analyzer regressions pass. Gradle test itself has NO-SOURCE.

All four current source targets compile and package. The 1.21.11 compile/remap was explicitly forced with --rerun-tasks, then repeated with a fresh --project-cache-dir. Both execute compileJava. Exact jar hashes are in jar-hashes.txt. No prior real-client evidence is attributed to these jars.

## Remaining thread and input blockers

The ordinary outbound hook still runs on its caller thread. Inbound game HEAD hooks run before vanilla ensureRunningOnSameThread; native dispatch can invoke them once on Netty then again on server-thread replay. Pong has no native handoff body. This delta does not fix that architecture or qualify callbacks/authoritative world reads. Native evidence is retained rather than treating concurrent deques as an ownership proof.

Targeted input follow-up: all four actual native PlayerCommand.Action enums lack the old sneak constants, including 1.21.11. Input.shift() exists in all four. No input hook is present, so sneak feed remains missing across all targets, not only 26.3. Native digging enums are fully covered by current converter names (1.21.11/26.1.2/26.2 include STAB; 26.3 adds CHANGE_DESTROY_DIRECTION). Future unknown-action and missing legacy interaction callback throws have no local dispatcher catch. They can reach native connection error handling, outside Effects containment. Current-native coverage is not a safe future-version promise; runtime failure containment remains unqualified. No changes made to those paths in this anchor delta.

## 1.21.11 remap warning

The fresh-project-cache build reproduces `Cannot remap modifiers because it does not exist in any of the targets [] or their parents.` before compileJava. Cached forced builds do not reproduce that configuration-stage warning. A dependency scan finds the matching accessor in Fabric API fabric-entity-events-v1 3.1.1+1d0ab4303e, ContextChainAccessor.getModifiers(), targeting com.mojang.brigadier.context.ContextChain with @Accessor("modifiers"). The actual Brigadier class has a private modifiers field; both bytecode records are retained. This identifies a concrete matching dependency entry, not a proof that it is the sole warning emitter or harmless at runtime. No warning was suppressed and no dependency was changed. No 1.21.11 runtime is admitted or claimed.
