# lookout

a client-side companion to meshac. it watches other players from what your own
client can see and drops you a quiet chat note when someone keeps moving like a
cheat. no server side, no punishments, no public reports: the notes show up only
in your own chat.

built as a fork of meshac, so the detection engine under it is the same vulcan port.
what's new here is the outside view: everything is computed from S2C entity traffic
(positions, velocities, animations, effects, equipment) and the world your client
has loaded.

## what it watches for

- flight / hover: airborne with no support past what a jump explains
- jesus: running across water
- step: gaining more than 0.6 without jumping
- speed: sustained ground speed past sprint + visible effects
- autototem: instant restocks, three in a row

each alert is hedged on purpose ("flight-like movement"), rate-limited per player,
and means "worth a look", never a verdict. see docs/lookout-false-positives.md for
the full notes per check, including the deliberate misses.

## install

drop the jar in your mods folder with fabric for 26.3. done. it works on any server
because it never asks the server for anything.

## config

`config/lookout.json` after first launch: per-check toggles, thresholds, and the
alert cooldown. defaults are tuned to stay quiet for legit players.

## credits

detection engine: Vulcan AntiCheat 2.9.7.28 by frap and the Vulcan developers,
ported to fabric for meshac (see CREDITS.md). the client-side observation layer
and checks are by GoatTech.

private internal build. not for redistribution.
