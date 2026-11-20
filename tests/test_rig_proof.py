#!/usr/bin/env python3
"""Unit tests for rig/proof parsers and row scoring.

Synthetic evidence text inline; the committed good fixtures back the
--dry-run self-test. Assertions are exact (verdict strings, parsed
fields), not substrings a passing row could satisfy. One mutant control:
score.py minus the mixin-error check must let a boot log with mixin
errors pass, after a healthy baseline. Run from the repo root:
python3 tests/test_rig_proof.py"""
import json, os, pathlib, subprocess, sys, tempfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from rig.proof import parsers, score

fails = []
def expect(name, cond, detail=""):
    print(("PASS " if ok_str(cond) else "FAIL ") + name + (f" ({detail})" if detail and not cond else ""))
    if not cond:
        fails.append(name)

def ok_str(cond):
    return cond

GOOD = """[main/INFO]: Loading Minecraft 26.3 with Fabric Loader 0.19.5
\t- lookout 2.9.7.28-meshac-0.1.0
[Render thread/INFO]: [lookout] watching (client-side companion to meshac)
[Render thread/INFO]: [lookout-stats] entity=7 samples=3
[Render thread/INFO]: [lookout-stats] entity=7 samples=9
[Render thread/INFO]: [lookout] alert entity=7 check=killaura_facing
"""

# parser: mod entry needs the loader table line, not just the watching line
c = parsers.parse_client_log(GOOD)
expect("parse-mod-listed", c.mod_listed)
expect("parse-watching", c.watching)
expect("parse-mixin-clean", c.mixin_errors == [])
expect("parse-stats-max", c.stats == {7: 9}, f"got {c.stats}")
expect("parse-alert-fields",
       [(a.entity, a.check, a.trace) for a in c.alerts] == [(7, "killaura_facing", False)])

no_mod = parsers.parse_client_log(GOOD.replace("\t- lookout 2.9.7.28-meshac-0.1.0\n", ""))
expect("parse-mod-absent", not no_mod.mod_listed)

err = parsers.parse_client_log(
    "[main/ERROR]: Mixin apply failed dev.lookout.mixin.ClientPacketListenerMixin\n" + GOOD)
expect("parse-mixin-error-caught", len(err.mixin_errors) == 1)
err_rev = parsers.parse_client_log(
    "[main/ERROR]: Mixin apply failed: dev.lookout.client.AttackHooks\n" + GOOD)
expect("parse-mixin-error-vocab-first", len(err_rev.mixin_errors) == 1)
err_cb = parsers.parse_client_log(
    "[Render thread/WARN]: injection callback failed in dev.lookout.mixin.ChatAlerter\n")
expect("parse-mixin-error-warn-injection", len(err_cb.mixin_errors) == 1)
no_sev = parsers.parse_client_log("[main/INFO]: Mixin apply failed: dev.lookout.client.AttackHooks\n")
expect("parse-mixin-no-severity-not-error", no_sev.mixin_errors == [])
no_pkg = parsers.parse_client_log("[main/ERROR]: Mixin apply failed: com.example.Other\n")
expect("parse-mixin-no-package-not-error", no_pkg.mixin_errors == [])
info_only = parsers.parse_client_log(
    "[main/INFO]: dev.lookout mixin ClientPacketListenerMixin applied 26 targets\n" + GOOD)
expect("parse-mixin-info-not-error", info_only.mixin_errors == [])

traced = parsers.parse_client_log("[lookout] trace alert entity=9 check=reach_rough\n")
expect("parse-alert-trace-flag", traced.alerts and traced.alerts[0].trace)

ng = parsers.parse_client_log(
    "[lookout-proof] null-gui-window begin\n[lookout] alert entity=1 check=x\n"
    "[lookout-proof] null-gui-window end\n[lookout-proof] null-gui-window begin\n")
expect("parse-nullgui-window-paired", len(ng.null_gui_windows) == 1)
expect("parse-nullgui-unclosed-dropped", True)  # the unclosed begin above is ignored

expect("parse-config-good",
       parsers.parse_config('{"enabled": true, "killaura": true, "reach": true, "timer": true, "multiAura": true}')
       == {"enabled": True, **{k: True for k in parsers.OPT_INS}})
expect("parse-config-missing-false",
       parsers.parse_config('{"enabled": true, "killaura": true}')["reach"] is False)
expect("parse-config-enabled-missing-false",
       (parsers.parse_config('{"killaura": true}') or {}).get("enabled") is False)
expect("parse-config-malformed", parsers.parse_config("{nope") is None)
expect("parse-config-non-dict", parsers.parse_config("[1]") is None)
expect("parse-arm-counts", parsers.parse_arm_counts("arm=reach sent=2\narm=killaura sent=6\n")
       == {"reach": 2, "killaura": 6})
expect("parse-arm-counts-corrupt-none", parsers.parse_arm_counts("arm=reach sent=2\ngarbage line\n") is None)
expect("parse-arm-counts-duplicate-none", parsers.parse_arm_counts("arm=reach sent=2\narm=reach sent=4\n") is None)
expect("parse-arm-counts-unknown-arm-none", parsers.parse_arm_counts("arm=fly sent=9\n") is None)
expect("score-g4-invalid-counts-unverified",
       score.row_g4_coverage(parsers.parse_arm_counts("arm=reach sent=2\narm=reach sent=4\n"))[0] == "UNVERIFIED")
expect("parse-server-receipt",
       len(parsers.parse_server_log("[INFO]: VictimA joined the game\n[INFO]: noise\n")) == 1)

# scorer: verdict paths per row
good = parsers.parse_client_log(GOOD)
expect("score-boot-pass", score.row_boot(good)[0] == "PASS")
expect("score-boot-unverified", score.row_boot(None)[0] == "UNVERIFIED")
expect("score-boot-fail-mixin",
       score.row_boot(parsers.parse_client_log(
           "[main/ERROR]: Mixin apply failed dev.lookout.mixin.ClientPacketListenerMixin\n" + GOOD))[0] == "FAIL")
expect("score-boot-fail-no-watch",
       score.row_boot(parsers.parse_client_log(GOOD.replace(parsers.WATCHING, "x")))[0] == "FAIL")

ALL_ON = {"enabled": True, "killaura": True, "reach": True, "timer": True, "multiAura": True}
expect("score-g2-pass", score.row_g2_optin(dict(ALL_ON))[0] == "PASS")
expect("score-g2-fail", score.row_g2_optin({**ALL_ON, "reach": False})[0] == "FAIL")
expect("score-g2-fail-master-off",
       score.row_g2_optin({**ALL_ON, "enabled": False})[0] == "FAIL")
expect("score-g2-unverified", score.row_g2_optin(None)[0] == "UNVERIFIED")
expect("parse-config-enabled-wrong-type",
       (parsers.parse_config('{"enabled": "yes", "killaura": true}') or {}).get("enabled") is False)

expect("score-delivery-pass", score.row_target_delivery(good)[0] == "PASS")
thin = parsers.parse_client_log("[lookout-stats] entity=1 samples=2\n")
expect("score-delivery-fail-thin", score.row_target_delivery(thin)[0] == "FAIL")

expect("score-chat-pass", score.row_chat_capture(good)[0] == "PASS")
expect("score-chat-fail-none",
       score.row_chat_capture(parsers.parse_client_log("[lookout-stats] entity=1 samples=9\n"))[0] == "FAIL")

ng_client = parsers.parse_client_log(
    GOOD + "[lookout-proof] null-gui-window begin\n[lookout] alert entity=7 check=reach_rough\n"
           "[lookout-proof] null-gui-window end\n")
expect("score-nullgui-pass", score.row_null_gui(ng_client)[0] == "PASS")
expect("score-nullgui-unverified-no-markers", score.row_null_gui(good)[0] == "UNVERIFIED")
quiet_window = parsers.parse_client_log(
    GOOD + "[lookout-proof] null-gui-window begin\n[lookout-proof] null-gui-window end\n")
expect("score-nullgui-fail-quiet-window", score.row_null_gui(quiet_window)[0] == "FAIL")

expect("score-g3-pass", score.row_g3_receipt(["VictimA joined the game"])[0] == "PASS")
expect("score-g3-fail", score.row_g3_receipt([])[0] == "FAIL")
expect("score-g3-unverified", score.row_g3_receipt(None)[0] == "UNVERIFIED")
expect("score-g4-pass", score.row_g4_coverage({"killaura": 6, "reach": 6, "multiaura": 3, "timer": 6})[0] == "PASS")
expect("score-g4-fail-short",
       score.row_g4_coverage({"killaura": 6, "reach": 5, "multiaura": 3, "timer": 6})[0] == "FAIL")
expect("score-g4-unverified", score.row_g4_coverage(None)[0] == "UNVERIFIED")

# dry-run self-test against the committed fixtures
r = subprocess.run([sys.executable, "rig/proof/proof.py", "--dry-run"],
                   cwd=ROOT, capture_output=True, text=True)
expect("dry-run-all-rows-pass",
       r.returncode == 0 and "SUITE: all pass" in r.stdout and "Traceback" not in r.stderr,
       r.stdout[-200:] + r.stderr[-200:])

# live-mode negative: a boot log with mixin errors must fail the boot row
with tempfile.TemporaryDirectory() as td:
    bad = pathlib.Path(td, "bad.log")
    bad.write_text("[main/ERROR]: Mixin apply failed dev.lookout.mixin.ClientPacketListenerMixin\n"
                   "\t- lookout 2.9.7.28-meshac-0.1.0\n" + parsers.WATCHING + "\n")
    r = subprocess.run([sys.executable, "rig/proof/proof.py",
                        "--client-log", str(bad), "--row", "boot"],
                       cwd=ROOT, capture_output=True, text=True)
    expect("live-boot-fail-exit1",
           r.returncode == 1 and any(l.startswith("FAIL boot") for l in r.stdout.splitlines()))

    # mutant: score.py without the mixin-error check must pass that bad log
    src = (ROOT / "rig/proof/score.py").read_text()
    block = '''    if client.mixin_errors:
        problems.append(f"{len(client.mixin_errors)} dev.lookout mixin error lines")
'''
    assert block in src, "mixin-error check missing from committed scorer"
    mut = pathlib.Path(td, "score_mut.py")
    rigdir = pathlib.Path(td, "rig", "proof"); rigdir.mkdir(parents=True)
    (rigdir / "__init__.py").write_text("")
    (rigdir / "parsers.py").write_text((ROOT / "rig/proof/parsers.py").read_text())
    (rigdir / "score.py").write_text(src.replace(block, ""))
    (rigdir / "proof.py").write_text((ROOT / "rig/proof/proof.py").read_text())
    base = subprocess.run([sys.executable, "rig/proof/proof.py",
                           "--client-log", str(bad), "--row", "boot"],
                          cwd=td, capture_output=True, text=True)
    # baseline: mutant tree runs at all (row scored, no import/startup error)
    ok = "boot" in base.stdout and "Traceback" not in base.stderr
    expect("mutant-no-mixincheck-baseline-healthy", ok, base.stderr[-200:])
    ok = (base.returncode == 0
          and any(l.startswith("PASS boot") for l in base.stdout.splitlines()))
    expect("boot-mutant-no-mixincheck-killed", ok,
           "want PASS boot: mixin errors slip through the stripped scorer")

# unreadable/missing evidence must score UNVERIFIED, never traceback
with tempfile.TemporaryDirectory() as td2:
    missing = os.path.join(td2, "nope.log")
    r = subprocess.run([sys.executable, "rig/proof/proof.py",
                        "--client-log", missing, "--row", "boot"],
                       cwd=ROOT, capture_output=True, text=True)
    expect("live-missing-file-unverified",
           r.returncode == 1 and "Traceback" not in (r.stdout + r.stderr)
           and any(l.startswith("UNVERIFIED boot") for l in r.stdout.splitlines()),
           r.stderr[-200:])
    bad_utf = os.path.join(td2, "bad.log")
    with open(bad_utf, "wb") as fh:
        fh.write(b"\xff\xfe not utf-8 \x80\x81")
    r = subprocess.run([sys.executable, "rig/proof/proof.py",
                        "--client-log", bad_utf, "--row", "boot"],
                       cwd=ROOT, capture_output=True, text=True)
    expect("live-invalid-utf8-unverified",
           r.returncode == 1 and "Traceback" not in (r.stdout + r.stderr)
           and any(l.startswith("UNVERIFIED boot") for l in r.stdout.splitlines()),
           r.stderr[-200:])

print("SUITE:", "all pass" if not fails else f"failures {fails}")
sys.exit(0 if not fails else 1)
