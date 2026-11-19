# local-chat-only player names

Local QA candidate based on ffeda56. No jar build, push, game or rig run.
This note supersedes earlier named-capture guidance: diagnostic mode is no longer
an exception to local-chat-only accused names. Raw captures still stay private.

## changes

ChatAlerter TRACE and default records now contain entity ID and check ID only,
never the playerName or message. The trace record keeps a trace marker so its
unset/empty guard remains observable. Local chat still displays name and message.
The message is excluded because suppression summaries themselves include names.
R9 now correlates chat pixels with log ID/check/time, not duplicate named text.

teleportJumpStats remains false by default. When explicitly enabled its existing
numeric diagnostics use entityId, not p.name. Engine entry traces already use
IDs/numeric event fields; no new player-name trace was introduced. MESHAC_TRACE
unset/empty remains off, any nonempty value remains on, including "0"/whitespace.
This is no anonymity guarantee: IDs can correlate with other logs, screenshots
contain names, and unrelated Minecraft/mod logging is outside these call sites.

## regression evidence

Before production edits the real-source Java/stub harness ran 12 cases; exactly
alerter-nonempty and stats-on-{unset,empty,nonempty} failed. Default stats and all
unset/empty trace behavior passed. Captured red log retained, exit 1.

After: 15/15 cases pass, adding the actual ChatAlerter present-GUI route. Every
unset/empty/nonempty value is tested independently for engine trace, null-GUI
alert, default stats, enabled stats and local named chat. Fresh source is compiled
into temporary directories; each case is a separate JVM. Minecraft chat/logger
and world boundaries are test doubles, not a real client/logger persistence test.

Seven independently compiled mutants have exact expected failures:
- engine trace branch guard removed: engine-unset and engine-empty only;
- alerter trace branch guard removed: alerter/chat unset and empty only;
- engine empty check reverted: engine-empty only;
- alerter empty check reverted: alerter-empty and chat-empty only;
- named alert logging restored: alerter-nonempty and chat-nonempty only;
- stats p.name restored: all three stats-on cases only;
- stats flag guard alone removed, output retained: all three stats-default cases.
Each leaves the relevant output call intact. Expected FAIL lines in mutant runs
are observations; final PASS means exact-set comparisons all matched.

Value: protects=no accused names/messages in alert/stats output, local chat intact;
fails_when=name or default-on regressions return; why_new=prior tests expected names;
seam=none; no_claim=real logger persistence, client rendering or third-party logs.

## gate changes and fresh verification

Source gate's R9 named-fields check is replaced by a trace-ID-fields check;
constant-argument mutant still dies in its own renamed check, and privacy is now
exercised behaviorally. This is an intentional requirement change, not the old
named-evidence contract silently discarded. Trace fixtures stay 11/11, gate 14/14.

compileJava succeeds with bounded 512 MiB heap, existing warnings remain. Fresh
LookoutSim PASS and guard sim 28/28 red-controlled. Mixin Java source gate 3/3,
fixtures 5/5 plus meta-mutant; dependency gate 4/4 on old jars; R4 pass; choreography
32/32. Unchanged choreography mutation suite not rerun. No resource/jar or boot
proof inferred. G4 remains UNQUALIFIED. Rig remains HELD.

Replay: python3 tests/test_trace_env.py --mutants, with JDK 25 on PATH.
Source/docs inspected; README remains under 60 lines. The broader compatibility
and runtime tasks remain separate. No detector thresholds or parity code changed.
