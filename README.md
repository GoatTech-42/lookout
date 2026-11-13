# meshac

a private Fabric anticheat rebuild based on OpenVulcan 2.9.7.28, with meshac's commands, screens and case system kept around it.

## current status

this is work in progress, not a published v1.

- 1.21.11, 26.1.2, 26.2 and 26.3 compile and package with explicit version adapters.
- 26.3 has booted with the shipping jar and joined a disposable local world with a vanilla test client. identity, respawn reference refresh, disconnect cleanup and server-side rejoin were observed with a separate test observer.
- all 179 instantiated checks remain gated. the 180-class dependency inventory includes an empty JumpD placeholder; it is not an active detector.
- containment stays LOG_ONLY. Veil stays off, including when an inherited config requests it.
- Meadow, detection outcomes and the broad legitimate-play matrix are not qualified. a clean boot is not proof that this catches cheats or avoids false flags.

read [PARITY.md](PARITY.md) for the evidence and remaining gaps. [docs/README.md](docs/README.md) is the documentation map.

## layout

| path | purpose |
| --- | --- |
| `src/main/java/dev/meshac/` | shared port, packet routing, checks and product layer |
| `src/mc*/java/` | exact native API and packet hooks for each target |
| `src/main/resources/` | Fabric metadata, mixins and bundled settings |
| `sim/` | bounded headless tests and the separately labeled lifecycle observer |
| `scripts/` | dependency analysis and packaged-artifact checks |
| `docs/build-evidence/` | retained RED/GREEN/build/isolated-runtime receipts |
| `rig/` | historical rig scripts; read before use, not portable by default |

## build

use JDK 25 to run Gradle. the default target is 26.3:

```sh
./gradlew build
python3 scripts/verify_built_jar.py 26.3 25
python3 sim/lifecycle/check_entrypoint.py 26.3
```

1.21.11 uses the separate remap route and Java 21 bytecode. see [CONTRIBUTING.md](CONTRIBUTING.md) for exact target commands and test limits. Gradle's test task has no tests; the headless suites run separately.

## branches and recovery

`main` is the rebuild line after consolidation. `legacy/pre-openvulcan` preserves the old main code until v1 is finished and published. the pre-cleanup branch tips are archived as tags, with their exact hashes in [docs/REPOSITORY.md](docs/REPOSITORY.md). unique old work is preserved, not mixed into the new detection logic without review.

## credits and use

Vulcan AntiCheat 2.9.7.28 is by frap and the Vulcan developers. OpenVulcan supplied the deobfuscated source used for this port. meshac's branding and surrounding systems are by GoatTech. see [CREDITS.md](CREDITS.md).

private internal work only. the legacy LICENSE file is retained as historical project material, not a claim that it grants rights to distribute the imported Vulcan code. no public release or redistribution is authorized by this repository state.
