# lookout false-positive notes

every check in lookout watches other players from the outside, using only what the
server sends our client. that data is interpolated (positions smooth over 3 ticks),
quantized, and sometimes delayed, so every check errs toward silence. an alert means
"worth a look", never "confirmed cheating".

engine-wide: a master enabled flag gates every event path and every tick, and one
shared per-player cooldown sits in front of every alert, so bursts of suspicious
packets produce one note, not a wall.

## flight

- legit jump arcs, including jump boost, end inside the grace window.
- the long-airtime rule only engages while the player is not falling at speed
  (dy above -1.0), so a real drop from any height passes it. the sim covers a
  300-block fall as a negative fixture.
- knockback, riptide and explosions arrive as velocity packets and exempt 20 ticks.
- elytra glide, swimming pose, water, ladders/vines, levitation and slow falling
  all exempt.
- a lag spike arrives as a teleport/position-sync and resets the air clock.
- unloaded chunks read as support, so watching someone at the edge of render
  distance cannot flag.
- known hole: slow, gravity-mimicking fly that keeps falling slightly will not
  flag. deliberate; catching it from outside is guesswork.

## jesus

- support is probed across the whole player footprint (five sample points), so
  standing with one corner over solid ground at the water's edge does not flag.
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
