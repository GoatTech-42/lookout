# lookout false-positive notes

every check in lookout watches other players from the outside, using only what the
server sends our client. that data is interpolated (positions smooth over 3 ticks),
quantized, and sometimes delayed, so every check errs toward silence. an alert means
"worth a look", never "confirmed cheating".

engine-wide: a master enabled flag gates every event path and every tick, and a
per-player, per-check cooldown sits in front of every alert, so a burst of suspicious
packets from one check produces one note, not a wall (different checks keep their own
clocks).

## flight

- legit jump arcs, including jump boost, end inside the grace window.
- the long-airtime rule only engages while the player is not falling at speed
  (dy above -1.0), so a real drop from any height passes it, while a slow-fall fly
  that sinks gently instead of accelerating still flags. the sim covers a 300-block
  fall as a negative fixture and a 0.5/tick descent as a positive.
- knockback, riptide and explosions arrive as velocity packets and exempt 20 ticks.
- elytra glide, swimming pose, water, ladders/vines, levitation and slow falling
  all exempt.
- a lag spike arrives as a teleport/position-sync and resets the air clock.
- unloaded chunks read as support, so watching someone at the edge of render
  distance cannot flag.
- known hole: fly that mimics a real fall at terminal-velocity-like speed (a block
  per tick or faster) passes the long-airtime rule. deliberate; telling that from a
  legit drop from outside is guesswork.

## jesus

- support is probed across the whole player footprint (center, four edges and four
  diagonal corners), so standing with any corner over solid ground at the water's
  edge does not flag.
- gliding (elytra), recent velocity and recent teleports exempt, the same as
  flight. a low elytra glide skimming the surface stays quiet.
- frost walker freezes the surface into frosted ice, which the probe reads as
  solid ground (and not liquid), so it self-exempts.
- boats and other vehicles exempt. swimming pose and being in water exempt.
- needs ten straight ticks of fast surface travel; one step on water does not flag.

## step

- stairs, slabs and climbables are probed at every cell of the 3-tick window
  (start, middle and end), so a staircase climb cannot flag.
- measured ground-to-ground over 3 ticks so client smoothing cannot hide it.
- known FP risk: two legit 0.6 ledges inside 3 ticks without stairs/slabs is rare
  but possible; the buffer needs it to repeat before alerting.
- jumps never read, because a jump leaves the ground.

## speed

- only grounded ticks count, and bhop chains are mostly airborne, so good legit
  movement stays quiet.
- ice, slipperiness, soul ground and the visible speed effect are applied per
  sample across the whole window, not just at the latest tick, so sliding off an
  ice patch onto normal ground does not retroactively flag the ice ticks.
- a window with a long sample gap is treated as stale and skipped, so resumed
  movement after a stall cannot average in old deltas.
- deliberate miss: subtle speed (5-15%) stays under the cap. catching that from
  outside is not reliable; meshac on the server owns that fight.

## totem

- needs three distinct pops, each followed by a near-instant restock, inside the
  memory window. a single clutch swap, even a fast one, never flags.
- each pop is consumed by the first restock that follows it, so two pops cannot
  share one restock, and duplicate equipment notifications for the same restock
  count once.
- unqualified FP risk: packet-timing compression. the sim sees pop and restock
  events at exact ticks; on a real connection, packets can arrive bunched, which
  compresses the observed pop-to-restock gap and could make a legit fast restock
  look instant. not yet measured against real traffic.

## killaura facing (off by default, opt-in)

- evidence comes from damage packets: they name the hurt player and the player who
  caused it. no damage packet, no judgment, so mob fights, mining and bystanders
  never enter the picture. only player-attack damage counts; thorns reuses the same
  packet shape (equal cause and direct ids) and is filtered out by the damage type.
- the angle tolerance is wide and positions are lag-lerped; single odd hits decay,
  and the buffer also fades over time so spaced-out incidents never add up.
- still opt-in: the judgment is heuristic and the alert stays hedged.

## rough reach (off by default, opt-in)

- same damage-packet evidence: only player-attack-typed hits on a tracked player
  are judged (thorns and every other type are skipped), at both ends' smoothed
  positions.
- the distance bar sits well past vanilla's roughly-3-block melee reach plus a
  lag-lerp margin, single long hits decay, and the buffer fades over time.
- still the roughest check in lookout; the bar is heuristic. treat its notes with
  the most skepticism.

## fastbow

- the shooter comes from the add-entity packet's owner field, not from proximity,
  so bystanders are never blamed. arrows with an unknown or untracked owner are
  skipped: skeleton fire, other players' shots and your own arrows never count
  against someone drawing a bow nearby.
- crossbow arrows belong to their real owner too; since the check also needs an
  observed bow draw before that release, crossbow fire never counts.
- one draw fuels one counted release: a multi-arrow burst on a single draw counts
  once, and duplicate spawn packets are deduped by entity id.
- weak spam-click arrows are slow and never count; honest full draws never count.
- the full-power speed line is a heuristic, not a sourced vanilla maximum.

## noslow (experimental configurable heuristic)

- the speed line is a rough, configurable line, not a sourced vanilla maximum.
- only grounded ticks count (hopping keeps momentum legitimately), and the line
  scales with the visible speed effect.
- a grace covers the momentum carried into the use (sprint, then eat), and slippery
  ground exempts outright: sliding on ice while eating is legit and the difference
  is not readable from outside. that exemption is also a known hole.
- vehicles, elytra, water, climbables, knockback and teleports exempt.
- unqualified FP risk: momentum sources the sim does not model (slopes, slime,
  water exits, wind charges) could push a legit using-player over the line.

## elytra overspeed (experimental configurable heuristic)

- only level-or-climbing glides count; dives trade height for speed legitimately
  and exempt, and pullouts decay inside the sustain window.
- the cap approximates unpowered and firework-boosted flight from outside; it is
  not a sourced vanilla maximum, and borderline notes are weak.

## timer-like movement (off by default, opt-in)

- honest scope: from another client there is no separate timer signal in positions,
  because per-tick and per-second math use the same samples. this check only says
  that real-time ground speed stays past what sprint, effects and terrain explain.
  it overlaps with the speed check on purpose; its edge is that wall-clock math is
  robust to packet bunching and server lag spikes.
- the cap scales with the strongest speed effect seen in the window, and ice or
  soul-speed ground disqualifies samples, same as the speed check. effect-expiry
  and terrain transitions are covered by sim negatives.
- the cadence gate measures our own client's sampling rate, using a monotonic
  clock (nanoTime), never the adjustable system clock. the window must be one
  unbroken run of consecutive samples: wall-clock pauses over half a second and
  tick gaps (player left render) reset it, and the local rate must sit between 19
  and 21 tps. a slow local client makes wall-clock math unreadable; a fast one
  means catch-up processing after a stall. both go quiet instead of guessing.
- a laggy-client negative runs over the bare cap and stays silent behind the gate,
  and the sim re-runs that exact window with the gate floored to 0 and asserts it
  goes red, so the negative is not vacuous.
- low multipliers are invisible to it. that is a known, stated miss.

## multi-aura (off by default, opt-in)

- typed evidence only: player-attack damage packets, so attacker and victims are
  all real. no packet, no judgment.
- a vanilla sweep attack can hit two or more players in one tick and looks
  identical per event, so one burst counts ONCE no matter how many victims it
  carries: hits inside the aggregation window extend the current burst instead of
  stacking the count. duplicates and late-arriving extra victims in the same burst
  never inflate it.
- what separates aura is cadence across distinct bursts: sweeps need a charged
  cooldown and slow movement, so legit bursts come seconds apart. a burst after a
  long gap resets the streak to 1; only rapid back-to-back bursts alert.
- sim negatives cover a single-target duel, sweep doubles at a legit cadence, a
  lone 4-victim burst, re-notified hits inside one burst, charged sweeps with
  growing victim counts, and rapid pairs broken by a long gap.
