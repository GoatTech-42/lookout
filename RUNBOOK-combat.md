# combat rig runbook (iteration 3+)

RAM gate first, in its OWN step: `free -m` available must be >= 1600MB (700 footprint + 900 headroom for goatllm). Launch only in a later step. Never `source` the pid files (keys contain dashes). After a sandbox restart the whole rig is gone; relaunch fresh. If a real client ever joins from the launcher, 26.1.2 dropped --server/--port: use `--quickPlayMultiplayer 127.0.0.1:25565` (bindings packet E).

## procedure

1. server: resilient fifo bridge `(while :; do cat in.fifo; sleep 0.2; done) | exec ~/jdk/jdk-25.0.5.1+2/bin/java -Xmx700M -jar server.jar nogui` + fifo holder. Record pids manually.
2. victims: `BOTNAME=VictimA STILL=1 ... victim_bot.py`, same for VictimB (spec names them sam/ivy in fixtures; VictimA/VictimB here).
3. freeze + stage + static-verify: `python3 bot/choreo.py prepare server VictimA VictimB` - stone platform at (190,60,190), slowness 255 + gravity 0 + knockback_resistance 1, tp both, aborts unless both static within 0.05 over 3s AT the expected coordinates.
4. attacker: `COMBAT_ONLY=1 DESCEND=1 ARM_PAUSE=12 HOLD_AFTER=20 ... bot2.py`. ARM_PAUSE leaves the console window between arms.
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

parser/verification hardening: bot/choreo.py guards G1-G6 (entity-name match, strict payloads, missing-reply abort, freshest-wins, expected-coords, drift abort), pinned by tests/test_choreo.py (12/12) and tests/mut_choreo.py (6 mutants, each killed exactly by its guard).
