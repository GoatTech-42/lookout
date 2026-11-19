# 26.1.2 capture operator README

Status: rig HELD until main's lane-clear word and explicit mode approval.
Planning only. Source checks and stub tests are not client/runtime proof.

## before launch
- Bind accepted source, jar SHA256, Minecraft/Fabric/dependencies and config.
- Use a matching 26.1.2 server, real watcher and verified stimulus clients.
  Existing combat bots send protocol 777/26.3; do not assume 26.1.2 support.
- No other heavy lane may run. Check free -m in its OWN step; launch later.
  Full rig: >=1600 MB available. Bot-only: >=900 MB plus explicit approval;
  bot-only cannot qualify this watcher capture. Below the gate: stand down.
- Verify JAVA25 executable/version, paths, console FIFO and fresh log directory.
  Record each owned PID/PGID at launch; discard old PIDs after any restart.
- Reserve capture, teardown and reporting time inside one roughly 35-minute run.

## launch order
1. Start matching server (-Xmx700M), resilient FIFO bridge and holder; record IDs.
   Verify authentic server-ready output and version, not just an open port.
2. Start owned Xvfb if needed, then real watcher with MESHAC_TRACE=1 and
   --quickPlayMultiplayer 127.0.0.1:25565. Record owned PIDs/PGIDs/lock.
3. Verify lookout loaded/watching, no lookout mixin errors, and effective config.
   Prove required opt-ins from that watcher's config, not a template.
4. Join verified stimulus clients; record target names/IDs and stimulus times.
   Watcher must be observing before any stimulus. Perform smoke below first.

## capture before scoring any row
| risk | stimulus | watcher.log evidence |
|---|---|---|
| R1 | bow arrow + crossbow spectral arrow | two onProjectileSpawn, nonzero speed, projectile/owner IDs |
| R2 | punch target for knockback | onVelocity target ID within two ticks of damage; shared tick basis required |
| R3 | one target teleport of 50 blocks | exactly one onTeleport target ID |
| R4 | melee + fall damage | two onDamage, distinct nonempty typeKey; no empty-unwrap warning |
| R5 | one totem pop | onTotemPop target ID at pop, not actual player death |
| R6 | totem into offhand | onTotemEquip target ID within about one second |
| R7 | empty-hand swing | onSwing target ID |
| R8 source/jar | inspect shipped config | lookout.mixins.json required:true; retain jar/hash evidence |
| R8 runtime | watcher boot | zero lookout mixin apply errors; retain boot log |
| R9 | known alerting provoke | same note in watcher chat screenshot AND watcher.log |
| R10 | boot/config | compatibilityLevel matches launched Java and target version |
README is an operator checklist, not standalone evidence. Retain actual captures.
All seven engine lines need [lookout-trace] prefix and the correct target/window.
Save version/hash/config metadata, raw server/client logs, stimulus times and chat
pixels. Trace arrival proves that event only, not every event or detector correctness.
G4 remains UNQUALIFIED without independent per-hit server receipt counts.

## trace privacy
Unset or empty MESHAC_TRACE is off after the accepted empty-trace fix. Any nonempty
value, including "0" or whitespace, is on. Enabled named capture logs are sensitive;
keep raw logs private and scrub names before sharing. Default logs retain IDs only.

## stop and teardown
Stop on failed RAM/lane/version/boot/config proof, missing/wrong trace or duplicate
R3 teleport, empty damage key, disconnect, contention, or too little cleanup time.
Do not score a starved check or infer pass from silence; preserve partial evidence.
Use TERM only on exact owned recorded PID/PGID, never pkill/killall/patterns or KILL.
Verify process death, port release and RAM recovery. If ownership/death is uncertain,
report it to main; never claim cleanup complete. Send actual evidence and gaps.
