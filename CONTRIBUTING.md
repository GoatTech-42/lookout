# contributing

small fixes with clear evidence are welcome. read the code you plan to change and [the project rules](docs/PRINCIPLES.md) first.

## report a bug

search the existing issues before opening one. include the exact minecraft, meshac, fabric loader, fabric api and java versions. if you built the jar yourself, include the commit.

write the steps, what you expected and what happened. a false flag needs the ordinary action that triggered it and the matching log lines. a missed detection needs evidence that the tested action actually happened; no signal by itself does not prove a miss.

remove player names, addresses, tokens, webhook urls and other private details from logs. keep the check name, timing and movement values needed to understand the result. do not upload a whole server folder or player database.

## make a change

- keep one fix per pull request. explain the cause and the smallest change that fixes it.
- reuse the game's movement and collision rules where possible. do not trust client claims.
- avoid new dependencies unless the existing code, java or fabric cannot do the job.
- do not weaken a lag, teleport or knockback exemption just to make one detection fire.
- keep generated files, credentials, worlds, logs and build output out of the diff.

## build

use jdk 25 to run gradle:

```sh
./gradlew build
```

this builds 26.3. the verified alternative build routes are:

```sh
./gradlew build -Pminecraft_version=26.2 -Pfabric_api_version=0.159.0+26.2 -Pmc_range=26.2 -Pjava_min=25
./gradlew build -Pminecraft_version=26.1.2 -Pfabric_api_version=0.155.3+26.1.2 -Pmc_range=26.1.x -Pjava_min=25
./gradlew build -Premap=1 -Pminecraft_version=1.21.11 -Pfabric_api_version=0.141.6+1.21.11 -Pmc_range=1.21.11 -Pjava_min=21
```

`remap=1` makes settings.gradle select build-remap.gradle for 1.21.11 and Java 21 bytecode. 26.x uses build.gradle and Java 25 bytecode. a successful compile is not mixin application, server startup or gameplay proof. the 1.21.11 remap warning remains recorded, not dismissed.

## test the change

for a new or changed check, include both a run that catches the tested action and a clean control that stays unflagged. test lag, packet bunching, teleports and knockback when they affect the code you touched.

use a disposable server you control. set `monitorOnly` to true, leave `egregiousChecks` empty, leave client fingerprinting off and set `combatLog` to false before testing. the last two act even in monitor mode. never use someone else's server as a test rig.

[rig/README.md](rig/README.md) describes the local setup. those scripts are for a prepared rig, not a one-command ci suite. do not run scripts that create containers, change server files or accept the minecraft eula until you have read them and agreed to the eula yourself.

in the pull request, list the commands and versions you tested, the jar or commit tested, and the observed results. say what you did not test. do not call a version supported by the fix just because another version passed.

record the false flag, cause and fix in the pull request, with private details removed. update coverage only for behavior you actually observed.

## license

this rebuild is private internal work. keep the legacy LICENSE and third-party credits, but do not treat that license file as permission to redistribute imported Vulcan code. public distribution and release remain blocked pending rights and readiness review.
