# lookout

a client-side companion to meshac. it watches other players from what your own
client can see and drops you a quiet chat note when someone keeps moving like a
cheat. no server side, no punishments, no public reports: the notes show up only
in your own chat.

## status

experimental. as of main fbe6e10 (2026-11-20), proven per target:

- source compiles green on 26.3 (shared source), 26.1.2, 26.2 and
  1.21.11 (per-version overrides under src/mc*/java)
- dependency and shipped lookout.mixins.json gates pass on fresh online
  builds of all four targets (exact jars and shas in
  docs/lead/lookout-version-compatibility-matrix.md)
- detector logic validated at the source level with a headless
  simulation of scripted scenarios (see sim/lookout/)

not proven for any target: game load, real watcher boot, chat delivery,
protocol-matched stimuli, G1-G4 runtime proof. it has not been
smoke-tested on a real client or server yet. treat every alert as a
hint, not a signal. the compatibility matrix is the detailed record;
this paragraph names only the rows above, nothing more.

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
- killaura facing (opt-in): hits landing while the attacker looks elsewhere
- fastbow: near-full-power arrows off almost no draw
- noslow: moving at full speed while eating, drinking, drawing or blocking
- rough reach (opt-in): hits landing from too far away
- elytra overspeed: gliding level far past ordinary elytra flight
- timer-like movement (opt-in): real-time ground speed past legit play
- multi-aura (opt-in): one player damaging several players at once, over and over

each alert is hedged on purpose ("flight-like movement"), rate-limited per player
and check, and means "worth a look", never a verdict. see docs/lookout-false-positives.md for
the notes per check, including the deliberate misses.

## install

drop the jar in your mods folder with fabric for 26.3. done.

## config

`config/lookout.json` after first launch: per-check toggles, thresholds, and the
alert cooldown. killaura facing, rough reach, timer and multi-aura are off by
default: each is heuristic, so they ship opt-in. noslow and elytra are experimental
heuristics with configurable lines.

## build

use JDK 25 to run Gradle:

```sh
./gradlew build
python3 scripts/verify_built_jar.py 26.3 25
python3 sim/lookout/run_lookout_sim.py
```

## what is left for a real client

everything so far is validated at the source level with synthetic fixtures. before
trusting any of it in play, run a real smoke pass:

1. drop the 26.3 jar (the one handed to you, or `./gradlew build`) in a fabric
   26.3 mods folder.
2. join a disposable local world with a second account (the "target").
3. legit pass first: 10 minutes of normal play on the target (sprinting, jumping,
   climbing stairs, swimming, riding a boat, eating, drawing a bow, trading hits,
   elytra with fireworks, dying with a totem). expect zero notes in your chat.
4. then provoke each check on the target: hover/fly, run across water, climb
   without jumping, high ground speed, rapid totem restocks, full-power bow taps,
   sprinting while eating, long elytra level flight. each should produce one
   hedged note, then respect the cooldown.
5. opt-in checks: enable killaura, reach, timer and multiaura in lookout.json and
   repeat with two targets for the multi-victim cases. expect silence during
   legit duels and sweep attacks.
6. toggle checks off in lookout.json and confirm the notes stop.

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
