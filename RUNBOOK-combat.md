# combat rig runbook (iteration 3+)

RAM gate first, in its OWN step: `free -m` available must be >= 1600MB (700 footprint + 900 headroom for goatllm). Launch only in a later step. Never `source` the pid files (keys contain dashes). After a sandbox restart the whole rig is gone; relaunch fresh. If a real client ever joins from the launcher, 26.1.2 dropped --server/--port: use `--quickPlayMultiplayer 127.0.0.1:25565` (bindings packet E).

## procedure

1. server: resilient fifo bridge `(while :; do cat in.fifo; sleep 0.2; done) | exec ~/jdk/jdk-25.0.5.1+2/bin/java -Xmx700M -jar server.jar nogui` + fifo holder. Record pids manually.
2. victims: `BOTNAME=VictimA STILL=1 ... victim_bot.py`, same for VictimB (spec names them sam/ivy in fixtures; VictimA/VictimB here).
3. freeze + stage + static-verify: `python3 bot/choreo.py prepare server VictimA VictimB` - stone platform at (190,60,190), slowness 255 + gravity 0 + knockback_resistance 1, tp both, aborts unless both static within 0.05 over 3s AT the expected coordinates.
4. attacker: `COMBAT_ONLY=1 DESCEND=1 ARM_PAUSE=12 HOLD_AFTER=20 ... bot2.py` (SERVERDIR env overrides the server dir). every arm runs choreo.run_arm: STAGE the intended geometry first (walk_to the arm's distance), server-verify the band SECOND (killaura 3.0, reach [4.6..6.7], multiaura 3.0), then attack - verifying before staging aborts every combined sequence (QA 8.0). fail-closed: no victims, a stage that cannot place, or an out-of-band verify all abort nonzero; a silently shortened arm fails the exact per-arm DELTA sent counts (counters are cumulative across the run; guard_begin snapshots, guard_end compares deltas via choreo.ArmGuard); zero server-side Health delta fails the receipt. ARM_PAUSE still leaves the console window between arms.
5. per-arm verification below, then teardown: TERM exact owned PID/PGID only (attacker, victims, holder, server pgid), verify `ps` clean, `free -m` recovered.

## verdict mapping (arch black-box spec, folded)

an arm is EXERCISED only when ALL of these hold; anything less is UNVERIFIED, never PASS.

gates before any verdict:
- G1 weave proof - watcher client log: lookout mod id present, zero dev.lookout mixin apply errors, watching line. REQUIRES the real watcher client (frontend lane's boot); a bot-only session cannot satisfy G1, so bot-only sessions calibrate the rig and prove nothing about lookout.
- G2 opt-in proof - config/lookout.json: killaura, reach, timer, multiAura all true. Same watcher dependency.
- G3 server receipt - server log shows the target connected and provoke traffic arrived; choreo.py's server-side Pos queries + victim Health deltas are our receipt evidence in bot-only mode.
- G4 coverage counts - per-arm counts must exceed the check's own threshold (below).

per arm (choreo.py verify-arm / health provides the server-side records):
- killaura facing (spec P1): 6 rounds at 2.5 blocks, ~100 deg off-facing, 4 ticks apart. Exercised: 6 attack packets received within 4.6 blocks per server Pos (bot2 walk_to + choreo verify-arm 3.0), expected watcher note `hits landing while facing away` on round 5. Traps: short coverage (<6 vacuous), wrong-family note, cooldown masking (gap >10s or fresh target). Controls stay silent: C1 duel, C8 aligned 2.8, C9 mob fight. Mutants: M1a facingDegrees 180 (provoke must still alert), M1b threshold 0 (controls must trip it).
- rough reach (P2): 6 rounds at exactly 5.2 blocks, aligned. Exercised: 6 hits logged >4.6 by logged positions, expected note `hits landing from <d> blocks away` on round 5, and NO facing note (family separation). Traps: short coverage, distance drift inside 4.6, knockback contamination in window. Controls C1/C8 silent. Mutants: M2a reachBlocks 99, M2b reachBlocks 2.0.
- multiaura (P4): 4 bursts, both victims attacked within 2 ticks per burst, one burst every 3 ticks (gaps <= 8). Exercised: >=3 bursts received server-side with TWO DISTINCT victims each (Health deltas on both), expected note `hitting several players at once over and over` on burst 3. Traps: short coverage (<4 sent), burst fragmentation (gaps >8 reset the streak silently), duplicate-notification inflation. Controls C2-C7 silent. Mutants: M3a count 99, M3b window 20, M3c gap 99.
- timer (P3, noted): pure Pos-check arm; needs the watcher tps inside 19-21 the whole window - watcher-run only, same shape.

bot-only session verdicts: G3/G4-side calibration only ("lane-run"). The watcher-note half of every arm's proof waits for the frontend lane's real-client boot under RAM coordination.

parser/verification hardening: bot/choreo.py guards G1-G9 (entity-name match, strict payloads, missing-reply abort, freshest-wins, expected-coords, drift abort, reach-band compare+abort, line-anchored answers, damage receipt) plus run_arm sequencing and bot2 wiring/count guards, pinned by tests/test_choreo.py (29/29) and tests/mut_choreo.py (19 mutants, each killed exactly). attack counting lives in choreo.ArmGuard (shared by bot2 and the tests): hit() on every send, begin() snapshots, end() compares per-arm deltas + receipt. G4 stays UNQUALIFIED in bot-only mode: sent counts and health deltas are not per-hit server receipt counts (not observable at 4-tick cadence). sliced mutant commands for the 120s cap:
  MUT_ONLY=M-G1-ignore-entity-name,M-G2-loose-payload,M-G3-timeout-fallback,M-G4-first-match-wins,M-G5-no-coord-check,M-G6-always-static python3 tests/mut_choreo.py
  MUT_ONLY=M-G7-no-max-abort,M-G7b-no-min-abort,M-G8-unanchored-ans,M-G9-no-receipt-abort python3 tests/mut_choreo.py
  MUT_ONLY=M-arm-stage-optional,M-arm-verify-after-attacks,M-arm-cumulative-counts,M-arm-shortcount-ok python3 tests/mut_choreo.py
  MUT_ONLY=M-arm-no-receipt-loop,M-bot2-ka-dist-drift,M-bot2-no-prearm-guard,M-bot2-no-count-guard python3 tests/mut_choreo.py
  MUT_ONLY=M-bot2-wiring-killaura-late,M-bot2-open-exit python3 tests/mut_choreo.py documented limits, unguarded: "freshest" is freshest complete line in the read buffer; the log file is pinned per query but a newly rotated log is followed only from the next query. test/mutant paths default to the repo layout (tests/../bot) and are overridable via CHOREO/BOT2 env.
