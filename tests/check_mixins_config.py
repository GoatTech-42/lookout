#!/usr/bin/env python3
"""Static gate: shipped lookout.mixins.json in a built jar.

Caller-bound like check_deps: --jar picks the exact artifact, --java-min
is the java_min the build route used. No defaults guessed from the jar
name. Asserts:
  - lookout.mixins.json exists, parses, is a JSON object
  - required is exactly true
  - compatibilityLevel is exactly JAVA_<java-min>
  - client array is a list containing ClientPacketListenerMixin
  - package is dev.lookout.mixin
Covers the lookout mixin config only; meshac.mixins.json (server engine)
is not inspected here. Structured FAILs, never a traceback."""
import argparse, json, sys, zipfile

fails = []
def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ((" " + detail) if (not ok and detail) else ""))
    if not ok:
        fails.append(name)

ap = argparse.ArgumentParser()
ap.add_argument("--jar", required=True)
ap.add_argument("--java-min", required=True, type=int)
args = ap.parse_args()

raw = None
try:
    with zipfile.ZipFile(args.jar) as z:
        raw = z.read("lookout.mixins.json")
except KeyError:
    check("mixins-config-present", False, "lookout.mixins.json missing from jar")
except (zipfile.BadZipFile, OSError) as e:
    check("mixins-config-present", False, f"jar unreadable: {e}")
cfg = None
if raw is not None:
    check("mixins-config-present", True)
    parsed = False
    try:
        cfg = json.loads(raw)
        parsed = True
    except (json.JSONDecodeError, UnicodeDecodeError):
        check("mixins-config-parseable", False, "lookout.mixins.json is not valid JSON")
    if parsed and not isinstance(cfg, dict):
        # JSON null/false/0 parse fine but are not a config; a None
        # sentinel here would conflate parsed-null with unparseable
        check("mixins-config-parseable", False, "lookout.mixins.json is not a JSON object")
        parsed = False
    if parsed:
        check("mixins-config-parseable", True)
        check("mixins-required", cfg.get("required") is True,
              f"required={cfg.get('required')!r}")
        want_level = f"JAVA_{args.java_min}"
        check("mixins-compatibility-level", cfg.get("compatibilityLevel") == want_level,
              f"compatibilityLevel={cfg.get('compatibilityLevel')!r} want {want_level}")
        client = cfg.get("client")
        check("mixins-client-array",
              isinstance(client, list) and "ClientPacketListenerMixin" in client,
              f"client={client!r}")
        check("mixins-package", cfg.get("package") == "dev.lookout.mixin",
              f"package={cfg.get('package')!r}")

print("SUITE:", "all pass" if not fails else f"failures {fails}")
sys.exit(0 if not fails else 1)
