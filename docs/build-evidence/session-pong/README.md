# Current-listener PONG admission candidate

This is a separate defensive session-boundary fix. It is not a claim of exact upstream parity, a global ownership fix, or changed-artifact runtime qualification.

The common-listener PONG HEAD previously resolved PlayerData by UUID alone. An old listener could therefore process a PONG into a replacement session with the same UUID; a colliding anchor ID could complete the replacement handshake. The fixture forces that ID collision to expose the boundary, not to claim a real stale client produced it.

Before dispatch, the hook now uses the existing ConfirmationTransport.isPlay contract with its own native Connection. This verifies the live connection, current session, current native player reference, game listener identity, player's current connection and actual outbound PLAY encoder phase. It prevents old-listener/same-UUID adoption, including replacement connections for the same native player object. No fallback, scheduling delay, detector gate, threshold, Veil or enforcement setting changes.

## Evidence

Actual common hook, dispatcher, PlayerDataManager, PlayerData, ConfirmationTracker and transport are compiled with explicit native-channel/service boundaries. RED catches the stale listener completing the replacement handshake. GREEN covers valid current reply, old listener against replacement session, same-player old/new connections, closed/configuration-phase rejection, valid COPY_FROM refresh and current ordinary confirmation. Duplicate ordinary negative confirmation still follows the unchanged rejection policy. A mutation removing current-origin validation fails with the original stale-listener assertion.

The test is `JAVA_HOME=/path/to/jdk25 MESHAC_PREP_OUTPUT=/path/to/output python sim/ownership/run_session_pong.py`, from the repository root. It uses repository-relative fixture definitions and the supplied JDK, not a separate checkout. Exact generated/production compile-input hashes are retained. No random-UUID orphan entry is retained when preparing the replacement-session fixture.

All four source targets execute compileJava and build/package successfully. Existing anchor/session/version/world/product/dispatcher/analyzer regressions and relevant mutations pass; logs are retained. Gradle test itself has NO-SOURCE. No native runtime or Mixin weave of this changed artifact has run. Earlier native anchor proof remains pinned to bf0697545885b23bbfb5d271d842a392c5d6de3d0fbd337a3eb0a601c8e729a8, not the jars produced here.

## Still open

Duplicate initial anchor policy is deliberately unchanged. Pinned upstream and current port both can reject a repeated anchor after handshake. Tolerance would be a defensive policy adaptation, separate from exactly-once delivery of one game packet through native handoff. Ordinary unknown negatives are not globally ignored. Global ingress ownership, caller-side outbound overload coverage, velocity/explosion post-write callbacks, callback failure/lifetime safety and stale-quit removal remain separate work. This fix does not touch quit removal. No detector is qualified or ungated.
