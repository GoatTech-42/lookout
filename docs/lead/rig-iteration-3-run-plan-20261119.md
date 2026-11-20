# rig iteration 3 run plan (combat arms, bot-only or full rig)

gates, in order, each in its OWN step:
1. RAM: `free -m` available must be >= 1600 MB for the full rig (server 700 MB
   + watcher client + Xvfb + bots) or >= 900 MB for a bot-only calibration
   session (server 700 MB + python bots). bot-only only with main's explicit
   confirm. under the cap: stand down, report the numbers, retry next window.
2. lane clear: main confirms no other heavy lane (goatllm) is running.
3. sandbox fresh: if uptime is minutes-old, all rig processes are gone;
   relaunch everything fresh.

launch (never reuse pids across restarts; never `source` pid files - keys
contain dashes; record each PID/PGID at launch in shell vars):
1. server: resilient fifo bridge `(while :; do cat in.fifo; sleep 0.2; done) |
   exec "$JAVA25" -Xmx700M -jar server.jar nogui` + fifo
   holder. set JAVA25 to the local JDK 25 executable before launch and verify
   `"$JAVA25" -version`; the path is host-specific and must be substituted.
2. victims: BOTNAME=VictimA STILL=1 victim_bot.py; same for VictimB.
3. stage + verify: python3 bot/choreo.py prepare server VictimA VictimB
   (stone platform, slowness 255 + gravity 0 + kb-resist 1, static-verified
   at the expected coordinates or abort).
4. full rig only: watcher client FIRST, before any stimulus: owned Xvfb plus
   watcher via --quickPlayMultiplayer 127.0.0.1:25565 (26.1.2 dropped
   --server/--port), MESHAC_TRACE=1. Prove loaded/watching and effective
   opt-ins (G1/G2 greps) before the attacker joins, or provokes can land
   before capture is live. Same ordering as the HOST-RUN GO packet.
5. attacker: COMBAT_ONLY=1 DESCEND=1 ARM_PAUSE=12 HOLD_AFTER=20 SERVERDIR=server
   bot2.py - run_arm stages, server-verifies the band, attacks, count+receipt
   guards; any guard failure aborts nonzero and the session is invalid.

verdicts: per RUNBOOK-combat.md mapping. bot-only = lane-run calibration
(G3/G4-side; G4 UNQUALIFIED). watcher-note halves need the real client.
teardown: TERM exact owned PID/PGID only (attacker, victims, fifo holder,
server PGID) - never pkill/pattern kills; verify ps clean; free -m recovered.
time-box: the whole session finishes and reports inside one run (~35 min).
