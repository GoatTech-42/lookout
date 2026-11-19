# lookout

a client-side companion to meshac. it watches other players from what your own
client can see and drops you a quiet chat note when someone keeps moving like a
cheat. no server side, no punishments, no public reports: the notes show up only
in your own chat.

## status

experimental. 26.3 only. validated at the source level with a headless
simulation of scripted scenarios (see sim/lookout/); it has not been smoke-tested
on a real client or server yet. treat every alert as a hint, not a signal.

## two parts in this repo

- the meshac server engine (dev.meshac.*): kept from the fork, untouched. its
  mixins now live in the "server" array of meshac.mixins.json, so they only load
  on a physical server. lookout does not call into it.
- the lookout detector (dev.lookout.*): new client-only code. it reads the S2C
  entity packets your client already receives (positions, velocities, animations,
  status effects, equipment) plus the world your client has loaded, and it never
  sends anything to the server. its one mixin sits in the "client" array of
  lookout.mixins.json.

## what it watches for

- flight / hover: airborne with no support past what a jump explains
- jesus: running across water
- step: gaining more than 0.6 without jumping
- speed: sustained ground speed past sprint + visible effects
- autototem: instant restocks, three in a row
- killaura facing: swinging while looking nowhere near the closest player in range
- fastbow: near-full-power arrows off almost no draw
- noslow: moving at full speed while eating, drinking, drawing or blocking
- rough reach: repeated swings lined up on someone too far away to hit
- elytra overspeed: gliding level far past elytra + firework physics

each alert is hedged on purpose ("flight-like movement"), rate-limited per player
and check, and means "worth a look", never a verdict. see docs/lookout-false-positives.md for
the notes per check, including the deliberate misses.

## install

drop the jar in your mods folder with fabric for 26.3. done.

## config

`config/lookout.json` after first launch: per-check toggles, thresholds, and the
alert cooldown.

## build

use JDK 25 to run Gradle:

```sh
./gradlew build
python3 scripts/verify_built_jar.py 26.3 25
python3 sim/lookout/run_lookout_sim.py
```

## meshac build and evidence

this repo is a fork of meshac; its build instructions and evidence still apply to
the retained engine. see [PARITY.md](PARITY.md) for the engine's qualification
status, [docs/README.md](docs/README.md) for the documentation map,
[docs/build-evidence/](docs/build-evidence/) for the retained build receipts, and
[CONTRIBUTING.md](CONTRIBUTING.md) for per-target build commands.

## credits

detection engine: Vulcan AntiCheat 2.9.7.28 by frap and the Vulcan developers,
ported to fabric for meshac (see CREDITS.md). the client-side observation layer
and checks are by GoatTech.

private internal build. not for redistribution.
