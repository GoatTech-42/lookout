# empty-trace follow-up

Local QA candidate, based on 99408eb (the docs caveat atop pushed d58a0d5).
No jar build, game/client runtime, rig launch or push.

## behavior

Both LookoutEngine and ChatAlerter now use the same nonempty environment-value
check. Unset and empty MESHAC_TRACE disable engine traces and named alert logs.
Any nonempty value enables them, including "0" or whitespace. This is not a
boolean-value parser. Default R9 logging still emits entity/check IDs before
the GUI-null exit. Enabled capture logs remain sensitive.

## regression and mutation evidence

Run with JDK 25 on PATH:

    python3 tests/test_trace_env.py
    python3 tests/test_trace_env.py --mutants

The test compiles fresh, real core and ChatAlerter source into an owned temporary
directory. Engine output is exercised through onSwing, and alert output through
alert with a null GUI. Test-only Minecraft and SLF4J boundaries replace the game
and logger backend. Each unset/empty/nonempty case runs in a separate JVM to
avoid static-final state carrying between cases. It checks exact output and
stderr, not a copy of the environment expression.

Value: protects=empty/unset tracing stays off while nonempty still works;
fails_when=either consumer enables tracing for empty or ignores the flag;
why_new=source gate could only validate text shape; seam=none;
no_claim=Minecraft boot, real logger persistence and GUI rendering use test doubles.

Test-first red: before production changes, six cases ran. Exactly engine-empty
and alerter-empty failed. Unset/nonempty passed for both consumers. The retained
red log records exit 1.

Green: all six base cases pass. Four independently compiled mutants are killed:

| mutant | changed production behavior only | exact failing cases |
|---|---|---|
| engine guard | if (TRACE) becomes if (true), output call retained | engine-unset, engine-empty |
| alerter guard | if (TRACE) becomes if (true), both log branches retained | alerter-unset, alerter-empty |
| engine empty guard | initializer reverts to non-null check, trace call unchanged | engine-empty |
| alerter empty guard | initializer reverts to non-null check, log calls unchanged | alerter-empty |

The FAIL lines inside --mutants output are expected mutant observations. Each
is followed by the exact expected-set comparison; the overall command exits 0
only when the unmodified six cases pass and all four mutant sets match exactly.

The source gate's accepted initializer shape was updated for the new expression;
no thresholds or negative checks were relaxed. Source gate remains a source-shape
check, not control-flow proof. Its original 11-fixture suite still passes.

## other fresh checks

- compileJava succeeds with a 512 MiB Gradle heap; pre-existing deprecation and
  unchecked warnings remain. Gradle test reports NO-SOURCE, not a test pass.
- Source gate 14/14; source-gate fixtures 11/11.
- Dependency gate 4/4 against previously built jars; not a new-jar check.
- R4 loudness check passes; choreography suite 32/32.
- LookoutSim PASS; LookoutGuardSim 28/28 red-controlled, PASS.
- Choreography's unchanged 20-mutant suite was not rerun for this environment
  edit. Its previous replay is not claimed as fresh evidence here.

G4 remains UNQUALIFIED. Runtime/client, 26.1.2 delivery, real log persistence and
watcher-chat proof remain unverified. Rig stays held for explicit clearance.
