# lookout false-positive notes

every check in lookout watches other players from the outside, using only what the
server sends our client. that data is interpolated (positions smooth over 3 ticks),
quantized, and sometimes delayed, so every check errs toward silence. an alert means
"worth a look", never "confirmed cheating".

## flight

- legit jump arcs, including jump boost, end inside the grace window.
- knockback, riptide and explosions arrive as velocity packets and exempt 20 ticks.
- elytra glide, swimming pose, water, ladders/vines, levitation and slow falling all exempt.
- a lag spike arrives as a teleport/position-sync and resets the air clock.
- unloaded chunks read as support, so watching someone at the edge of render distance
  cannot flag.
- known hole: slow, gravity-mimicking fly that keeps falling slightly will not flag.

## jesus

- frost walker freezes the surface; ice reads solid, so it self-exempts.
- boats and other vehicles exempt. swimming pose and being in water exempt.
- needs ten straight ticks of fast surface travel; one step on water does not flag.

## step

- stairs, slabs and climbables anywhere in the window exempt it.
- measured ground-to-ground over 3 ticks so client smoothing cannot hide it.
- known FP risk: two legit 0.6 ledges inside 3 ticks without stairs/slabs is rare but
  possible; the buffer needs it to repeat before alerting.
- jumps never read, because a jump leaves the ground.

## speed

- only grounded ticks count, and bhop chains are mostly airborne, so good legit
  movement stays quiet. ice and soul-speed (glinting boots on soul ground) exempt.
- cap scales with the visible speed effect amplifier.
- deliberate miss: subtle speed (5-15%) stays under the cap. catching that from
  outside is not reliable; meshac on the server owns that fight.

## totem

- needs three near-instant restocks inside the memory window. a single clutch swap,
  even a fast one, never flags.
- latency only delays what we see; it cannot invent an instant restock.
