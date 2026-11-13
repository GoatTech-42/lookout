# Isolated 26.3 initial-anchor runtime

Shipping source239033f, documentation at launchf6a1d51. Shipping and installed jar SHA256 both bf0697545885b23bbfb5d271d842a392c5d6de3d0fbd337a3eb0a601c8e729a8. A separately labeled, read-only observer was present; source and resource metadata are retained. No shipping bytecode was patched for observation.

The existing disposable offline server and test client ran together inside a fresh user/network namespace. Only its loopback interface was brought up; the retained route table has no external route. Cached assets were used. Endpoint127.0.0.1:25587, test identityMeshRigA, no real account. Server remained LOG_ONLY, Veil false/false/false, all179 actual checks gated. Inherited ban disconnect was contained; persistent case bytes before/after are identical SHA25637c53c19d9eb419e442a41d2c731673ff7765805729002c494d89033c082bc96. Manifest includes only relevant non-secret server property values, not the generated management secret.

## Observed native sequence, second successful run

All16 shipping mixins applied, including the new ConnectionMixin, ConnectionAccessor and PacketEncoderAccessor. Native source-level targeting and packaged registration proof preceded this run; this run adds actual native application.

-12:38:56 Netty thread: LOGIN_PRE_WRITE and LOGIN_POST_WRITE entity56. Game listener/current connection identity true, PlayerData's current player reference true, stored/native IDs both56. Handshake false, initial sentinel joinID-69, no periodic pending confirmation.
-12:38:56, after native login write: initial PING_PRE_WRITE/PING_POST_WRITE ID-11207. joinID already-11207 before write, handshake false, pending0, same native listener/connection/player identities.
-12:38:57 Netty thread: PONG_POST_HANDLE ID-11207, matching joinID, handshake true, pending0, same identities. Tick observer also records false then true, raw anchor reply present, serverVersionV_26_3 from native version26.3.

The initial positive configuration-phase ping is unrelated and explicitly logs listenerGame=false; no port anchor was attributed to it. The raw ID history later evicts the old anchor as its bounded list fills with normal replies; handshake remains true. That eviction is not a second handshake transition.

Second-run screenshot was inspected directly: fully rendered disposable flat world, first-person hand, HUD and movement tutorial visible. It is visual join evidence only, not a legitimate-behavior/check-safety matrix.

## Retained earlier attempts and warnings

First observer boot failed before any client joined because the observer entrypoint/helper shared the declared mixin package. Only the separate observer package was corrected; shipping artifact stayed byte-identical. Failure log is retained.

First successful handshake run observed anchor-11258 and entity60. Its screenshot still showed Loading terrain, so no rendered-world claim comes from that screenshot. Python runner completed its assertions and orderly server stop, but the enclosing execution detected detached children and returned exit1 after cleanup. That result is not presented as a pristine pass. The second runner adds process-group cleanup and child reaping, waits longer only for the visual capture, and exits0 with no remaining server/client/Xvfb processes. The production timing policy is unchanged.

Offline service discovery, host-name lookup, authentication-service and unavailable audio-device errors are retained. The namespace has no external route, so public service access could not succeed. These warnings were not hidden or called detector failures.

## Limits

This qualifies only the observed26.3 boot, native initial login/write/anchor/pong transition, native server-version initialization, containment state and rendered join. It does not qualify global tracker ownership, duplicate-PONG policy, ordinary outbound overload coverage, velocity/explosion after-send ordering, any detector, Meadow behavior, full lifecycle after this new artifact, reconfiguration/transfer, other target runtimes or distribution rights. One-shot-per-PlayerData scope divergence remains documented in the anchor source evidence. No gates opened.
