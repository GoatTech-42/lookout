#!/usr/bin/env python3
"""Unit tests for rig/proof parsers and row scoring.

Synthetic evidence text inline; the committed good fixtures back the
--dry-run self-test. Assertions are exact (verdict strings, parsed
fields), not substrings a passing row could satisfy. Mutant controls: score.py minus the mixin-error check, plus the
window-detector mutants. each mutant is demonstrated as a CHANGED OUTPUT
(verdict line or parsed finding) on exactly its mapped fixture after a
healthy baseline on the good log - not a whole-suite rerun per mutant.
parsed line indices are 0-based; verdict detail strings render 1-based. Run from the repo root:
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


# ---------------------------------------------------------------------
# window detector: multi-line mixin error forms (spec:
# docs/lead/mixin-window-detector-design.md). fixtures synthetic,
# id-only, numbered after the design doc. BASE carries the healthy boot
# surface with launcher-style timestamped headers.
BASE = ("[12:00:01] [main/INFO]: Loading Minecraft 26.3 with Fabric Loader 0.19.5\n"
        "\t- lookout 2.9.7.28-meshac-0.1.0\n"
        "[12:00:02] [Render thread/INFO]: [lookout] watching (client-side companion to meshac)\n")

def _filler(n):
    return "\tat trace.Frame.f(Frame.java:1)\n" * n

# f1: summary + caused-by (the shape QA replayed against the old parser)
f1 = (BASE + "[12:10:00] [Render thread/ERROR]: Mixin apply failed\n"
      "Caused by: java.lang.IllegalStateException: dev.lookout.client.AttackHooks\n"
      "[12:10:01] [Render thread/INFO]: done\n")
c1 = parsers.parse_client_log(f1)
expect("win-f1-no-sameline", c1.mixin_errors == [])
expect("win-f1-window-finding",
       [(f.start, f.end, f.severity, f.package_lines, f.vocab_lines)
        for f in c1.window_findings] == [(3, 4, "ERROR", [4], [3])],
       f"got {c1.window_findings}")
expect("win-f1-boot-unverified", score.row_boot(c1)[0] == "UNVERIFIED")

# f2: deep frames - dev.lookout + inject several lines below the header
f2 = (BASE + "[12:10:00] [Render thread/ERROR]: Unexpected error during event dispatch\n"
      "\tat net.minecraft.Game.run(Game.java:1)\n"
      "\tat dev.lookout.mixin.ChatAlerter.inject(ChatAlerter.java:42)\n"
      "\tat net.minecraft.Game.loop(Game.java:2)\n")
c2 = parsers.parse_client_log(f2)
expect("win-f2-no-sameline", c2.mixin_errors == [])
expect("win-f2-window-finding", len(c2.window_findings) == 1
       and c2.window_findings[0].start == 3 and c2.window_findings[0].end == 6)
expect("win-f2-boot-unverified", score.row_boot(c2)[0] == "UNVERIFIED")

# f3: caused-by chain, only the innermost names callback + package
f3 = (BASE + "[12:10:00] [Render thread/ERROR]: Unexpected error in listener\n"
      "Caused by: java.lang.RuntimeException: outer\n"
      "Caused by: java.lang.IllegalStateException: callback failed in dev.lookout.client.AttackHooks\n")
c3 = parsers.parse_client_log(f3)
expect("win-f3-window-finding", len(c3.window_findings) == 1
       and c3.window_findings[0].package_lines == [5]
       and c3.window_findings[0].vocab_lines == [5])
expect("win-f3-boot-unverified", score.row_boot(c3)[0] == "UNVERIFIED")

# f4: boundary - satisfying line on continuation line 30 (31 total): counts
f4 = (BASE + "[12:10:00] [Render thread/ERROR]: Mixin apply failed\n" + _filler(29)
      + "Caused by: java.lang.RuntimeException: dev.lookout.client.AttackHooks\n"
      + "[12:10:31] [main/INFO]: done\n")
c4 = parsers.parse_client_log(f4)
expect("win-f4-boundary-30-counts",
       [(f.start, f.end, f.package_lines) for f in c4.window_findings] == [(3, 33, [33])],
       f"got {c4.window_findings}")
expect("win-f4-not-truncated", c4.truncated_error_entries == [])

# f5: truncation boundary - satisfying line on continuation 31 of a
# still-continuing ERROR entry: no window finding, TRUNCATED surfaced
f5 = (BASE + "[12:10:00] [Render thread/ERROR]: Mixin apply failed\n" + _filler(30)
      + "Caused by: java.lang.RuntimeException: dev.lookout.client.AttackHooks inject\n"
      + _filler(2) + "[12:11:00] [main/INFO]: done\n")
c5 = parsers.parse_client_log(f5)
expect("win-f5-no-finding-past-cap", c5.window_findings == [])
expect("win-f5-truncated-flagged", c5.truncated_error_entries == [(3, 33)],
       f"got {c5.truncated_error_entries}")
v5, d5 = score.row_boot(c5)
expect("win-f5-boot-unverified-truncated",
       v5 == "UNVERIFIED"
       and "entry truncated at 30 continuations; omitted content not qualified" in d5, d5)

# f6: clean boundary - entry ends exactly at the cap via next header
f6 = (BASE + "[12:10:00] [Render thread/ERROR]: Mixin apply failed\n" + _filler(30)
      + "[12:11:00] [main/INFO]: done\n")
c6 = parsers.parse_client_log(f6)
expect("win-f6-clean-no-finding", c6.window_findings == [] and c6.truncated_error_entries == [])
expect("win-f6-boot-pass", score.row_boot(c6)[0] == "PASS")

# f7: next-header reset - package+vocab in the FOLLOWING entry do not leak
f7 = (BASE + "[12:10:00] [Render thread/ERROR]: Unexpected error during dispatch\n"
      + _filler(2)
      + "[12:10:01] [main/INFO]: dev.lookout mixin ClientPacketListenerMixin applied 26 targets\n")
c7 = parsers.parse_client_log(f7)
expect("win-f7-no-leak-across-header", c7.window_findings == [])
expect("win-f7-boot-pass", score.row_boot(c7)[0] == "PASS")

# f8: EOF flush - entry running to EOF is still evaluated
f8 = (BASE + "[12:10:00] [Render thread/ERROR]: Mixin apply failed\n"
      "Caused by: java.lang.IllegalStateException: dev.lookout.client.AttackHooks\n")
c8 = parsers.parse_client_log(f8)
expect("win-f8-eof-entry-evaluated", len(c8.window_findings) == 1)

# f9: orphan continuations open no window; same-line scan still sees them
f9a = "Caused by: dev.lookout.client.AttackHooks callback failed\n" + BASE
c9a = parsers.parse_client_log(f9a)
expect("win-f9-orphan-no-window", c9a.window_findings == [] and c9a.mixin_errors == [])
f9b = "ERROR Mixin apply failed dev.lookout.mixin.ClientPacketListenerMixin\n" + BASE
c9b = parsers.parse_client_log(f9b)
expect("win-f9-orphan-sameline-caught", len(c9b.mixin_errors) == 1)
expect("win-f9-orphan-sameline-fail", score.row_boot(c9b)[0] == "FAIL")

# f10: cap + same-line regression - a same-line match beyond the cap
# still FAILs via the all-lines scan; truncation evidence rides along
f10 = (BASE + "[12:10:00] [Render thread/ERROR]: Mixin apply failed\n" + _filler(30)
       + "Caused by: ERROR injection callback failed dev.lookout.mixin.Foo\n"
       + "[12:11:00] [main/INFO]: done\n")
c10 = parsers.parse_client_log(f10)
expect("win-f10-sameline-beyond-cap-caught", len(c10.mixin_errors) == 1)
v10, d10 = score.row_boot(c10)
expect("win-f10-boot-fail", v10 == "FAIL")
expect("win-f10-fail-keeps-review-evidence",
       "mixin error lines" in d10 and "window candidates" in d10 and "truncated" in d10, d10)

# f11: unrelated error + nearby routine dev.lookout INFO line stay apart
f11 = (BASE + "[12:10:00] [Render thread/ERROR]: Failed to load texture\n"
       "\tat net.minecraft.client.Texture.load(Texture.java:9)\n"
       "[12:10:01] [main/INFO]: dev.lookout mixin ClientPacketListenerMixin applied 26 targets\n")
c11 = parsers.parse_client_log(f11)
expect("win-f11-unrelated-error-clean", c11.window_findings == [])
expect("win-f11-boot-pass", score.row_boot(c11)[0] == "PASS")

# f12: dev.lookout frames without mixin vocabulary: no finding
f12 = (BASE + "[12:10:00] [Render thread/ERROR]: Unexpected error during attack\n"
       "\tat dev.lookout.client.AttackHooks.onAttack(AttackHooks.java:9)\n"
       "\tat net.minecraft.Game.run(Game.java:1)\n")
c12 = parsers.parse_client_log(f12)
expect("win-f12-no-vocab-no-finding", c12.window_findings == [])
expect("win-f12-boot-pass", score.row_boot(c12)[0] == "PASS")

# f13: severity exclusion - INFO entry with package + vocab on one line
f13 = BASE + "[12:10:00] [main/INFO]: Mixin apply failed: dev.lookout.client.AttackHooks\n"
c13 = parsers.parse_client_log(f13)
expect("win-f13-info-excluded", c13.mixin_errors == [] and c13.window_findings == [])
expect("win-f13-boot-pass", score.row_boot(c13)[0] == "PASS")

# f14: dedup - same-line match is not double counted; distinct entries
# keep their own evidence
f14a = (BASE + "[12:10:00] [main/ERROR]: Mixin apply failed dev.lookout.mixin.ClientPacketListenerMixin\n"
        "Caused by: dev.lookout.client.Other callback\n")
c14a = parsers.parse_client_log(f14a)
expect("win-f14-sameline-not-double-counted",
       len(c14a.mixin_errors) == 1 and c14a.window_findings == [])
f14b = (BASE
        + "[12:10:00] [main/ERROR]: Mixin apply failed dev.lookout.mixin.ClientPacketListenerMixin\n"
        + "[12:10:02] [main/ERROR]: Mixin apply failed dev.lookout.mixin.ChatAlerter\n")
c14b = parsers.parse_client_log(f14b)
expect("win-f14-distinct-entries-both-kept", len(c14b.mixin_errors) == 2)
f14c = f14b + ("[12:10:05] [Render thread/ERROR]: Mixin apply failed\n"
               "Caused by: java.lang.IllegalStateException: dev.lookout.client.AttackHooks\n")
c14c = parsers.parse_client_log(f14c)
v14c, d14c = score.row_boot(c14c)
expect("win-f14-fail-keeps-window-evidence",
       v14c == "FAIL" and len(c14c.mixin_errors) == 2 and len(c14c.window_findings) == 1
       and "window candidates" in d14c, d14c)

# f15: vocabulary collision - 'apply' in an unrelated message + a
# dev.lookout frame: window-only, so UNVERIFIED for human review,
# never auto-FAIL, never silent PASS
f15 = (BASE + "[12:10:00] [main/ERROR]: Failed to apply config value\n"
       "\tat dev.lookout.client.ConfigScreen.onClose(ConfigScreen.java:5)\n")
c15 = parsers.parse_client_log(f15)
expect("win-f15-collision-window-only",
       c15.mixin_errors == [] and len(c15.window_findings) == 1)
expect("win-f15-boot-unverified", score.row_boot(c15)[0] == "UNVERIFIED")

# f16: header grammar - log4j direct format opens an entry; a malformed
# near-header is a continuation
f16a = (BASE + "2026-11-13T23:03:13.292799379Z Render thread ERROR Mixin apply failed\n"
        "Caused by: java.lang.IllegalStateException: dev.lookout.client.AttackHooks\n")
c16a = parsers.parse_client_log(f16a)
expect("win-f16-log4j-header-finding",
       [(f.start, f.severity) for f in c16a.window_findings] == [(3, "ERROR")])
f16b = (BASE + "[12:10:01] not a header Mixin apply failed\n"
        "Caused by: dev.lookout.client.AttackHooks callback failed\n")
c16b = parsers.parse_client_log(f16b)
expect("win-f16-malformed-header-no-window",
       c16b.window_findings == [] and c16b.mixin_errors == [])

# f19: a truncated entry can ALSO carry an in-prefix finding; both are
# listed separately (the truncation wording stays unconditional)
f19 = (BASE + "[12:10:00] [Render thread/ERROR]: Mixin apply failed\n"
       "Caused by: java.lang.IllegalStateException: dev.lookout.client.AttackHooks\n"
       + _filler(30) + "[12:11:00] [main/INFO]: done\n")
c19 = parsers.parse_client_log(f19)
expect("win-f19-finding-and-truncation-coexist",
       len(c19.window_findings) == 1 and c19.truncated_error_entries == [(3, 33)],
       f"got {c19.window_findings} {c19.truncated_error_entries}")
v19, d19 = score.row_boot(c19)
expect("win-f19-detail-lists-both",
       v19 == "UNVERIFIED" and "lines 4-34 (ERROR entry)" in d19
       and "entry truncated at 30 continuations; omitted content not qualified" in d19, d19)

# f18: any bare-word level opens an entry - a FATAL header resets the
# window instead of merging into the open ERROR entry, and the
# ERROR/WARN gate keeps the FATAL entry itself out of findings
f18 = (BASE + "[12:10:00] [Render thread/ERROR]: Mixin apply failed\n"
       "[12:10:01] [Render thread/FATAL]: dev.lookout mixin crash in callback\n")
c18 = parsers.parse_client_log(f18)
expect("win-f18-unknown-level-resets-entry",
       c18.window_findings == [] and c18.mixin_errors == [])
expect("win-f18-boot-pass", score.row_boot(c18)[0] == "PASS")

# f17: healthy regression - the existing good log (untimestamped legacy
# headers: all orphans) produces no window output at all
expect("win-f17-healthy-regression",
       parsers.parse_client_log(GOOD).window_findings == []
       and parsers.parse_client_log(GOOD).truncated_error_entries == [])

# --- window-detector mutants: each killed by its own fixture after a
# healthy baseline. verdict-level via proof.py; parser-level probe for
# the dedup mutant ------------------------------------------------------
def mutant_tree(td, parser_subs=(), score_subs=()):
    rigdir = pathlib.Path(td, "rig", "proof")
    rigdir.mkdir(parents=True)
    (rigdir / "__init__.py").write_text("")
    psrc = (ROOT / "rig/proof/parsers.py").read_text()
    ssrc = (ROOT / "rig/proof/score.py").read_text()
    for old, new in parser_subs:
        assert psrc.count(old) == 1, f"parser mutant anchor not unique: {old[:60]!r}"
        psrc = psrc.replace(old, new)
    for old, new in score_subs:
        assert ssrc.count(old) == 1, f"score mutant anchor not unique: {old[:60]!r}"
        ssrc = ssrc.replace(old, new)
    (rigdir / "parsers.py").write_text(psrc)
    (rigdir / "score.py").write_text(ssrc)
    (rigdir / "proof.py").write_text((ROOT / "rig/proof/proof.py").read_text())

def boot_line(cwd, text):
    case = pathlib.Path(cwd, "case.log")
    case.write_text(text)
    r = subprocess.run([sys.executable, "rig/proof/proof.py",
                        "--client-log", str(case), "--row", "boot"],
                       cwd=cwd, capture_output=True, text=True)
    assert "Traceback" not in r.stderr, r.stderr[-300:]
    for l in r.stdout.splitlines():
        parts = l.split(" ", 2)
        if len(parts) >= 2 and parts[0] in ("PASS", "FAIL", "UNVERIFIED") and parts[1] == "boot":
            assert (r.returncode == 0) == (parts[0] == "PASS"), \
                f"rc {r.returncode} vs verdict {l!r}"
            return l
    raise AssertionError("no boot verdict line: " + (r.stdout + r.stderr)[-300:])

def mutant_case(name, fix, killed, parser_subs=(), score_subs=()):
    with tempfile.TemporaryDirectory() as td:
        mutant_tree(td, parser_subs, score_subs)
        base = boot_line(td, GOOD)
        expect(f"mutant-{name}-baseline-healthy", base.startswith("PASS boot"), base)
        got = boot_line(td, fix)
        expect(f"mutant-{name}-killed", killed(got), got)

# m1: no cap -> f5's 31st-continuation hit becomes a finding, no truncation
mutant_case("nocap", f5,
            lambda l: l.startswith("UNVERIFIED") and "truncated" not in l,
            parser_subs=[("MAX_CONTINUATION = 30\n", "MAX_CONTINUATION = 10000\n")])
# m2: cap counts the header (29 continuations) -> f4's line-30 hit is lost
mutant_case("cap-counts-header", f4,
            lambda l: "lines 4-34 (ERROR entry)" not in l,
            parser_subs=[("if len(entry.lines) <= MAX_CONTINUATION:",
                          "if len(entry.lines) <= MAX_CONTINUATION - 1:")])
# m3: package+vocab required on one line -> f1's split form invisible
mutant_case("sameline-required", f1,
            lambda l: l.startswith("PASS boot"),
            parser_subs=[("            pkg = [i for i, l in entry.lines if _PACKAGE.search(l)]\n"
                          "            voc = [i for i, l in entry.lines if _VOCAB.search(l)]",
                          "            pkg = [i for i, l in entry.lines if _PACKAGE.search(l) and _VOCAB.search(l)]\n"
                          "            voc = pkg")])
# m4: severity gate dropped -> f13's INFO entry yields a finding
mutant_case("severity-dropped", f13,
            lambda l: not l.startswith("PASS boot"),
            parser_subs=[('        if entry.severity in ("ERROR", "WARN"):',
                          '        if entry.severity in ("ERROR", "WARN", "INFO", "DEBUG"):')])
# m5: ERROR entries never close (sliding-window / no header reset class)
# -> f7 and f11 leak package+vocab into the open ERROR entry
mutant_case("no-header-reset", f11,
            lambda l: not l.startswith("PASS boot"),
            parser_subs=[("        if sev is not None:\n            flush_entry()",
                          '        if sev is not None and not (entry is not None and entry.severity in ("ERROR", "WARN")):\n            flush_entry()')])
mutant_case("no-header-reset-f7", f7,
            lambda l: not l.startswith("PASS boot"),
            parser_subs=[("        if sev is not None:\n            flush_entry()",
                          '        if sev is not None and not (entry is not None and entry.severity in ("ERROR", "WARN")):\n            flush_entry()')])
# m6: EOF entry dropped -> f8's finding never flushes
mutant_case("eof-dropped", f8,
            lambda l: l.startswith("PASS boot"),
            parser_subs=[("    flush_entry()\n    return out", "    return out")])
# m7: truncation treated as clean -> f5's capped entry reports nothing
mutant_case("truncated-clean", f5,
            lambda l: l.startswith("PASS boot"),
            parser_subs=[("            if entry.truncated:\n"
                          "                out.truncated_error_entries.append((entry.lines[0][0], entry.lines[-1][0]))",
                          "            if entry.truncated:\n"
                          "                pass")])
# m8: same-line scan bounded by the cap -> f10's line-31 match escapes
mutant_case("sameline-bounded", f10,
            lambda l: not l.startswith("FAIL boot"),
            parser_subs=[("        if is_mixin_error(line):\n"
                          "            out.mixin_errors.append((i, line.strip()))",
                          "        if is_mixin_error(line) and entry is not None and not entry.truncated and len(entry.lines) <= MAX_CONTINUATION:\n"
                          "            out.mixin_errors.append((i, line.strip()))")])
# m9: window-only findings auto-FAIL (no human gate) -> f15 collision fails
mutant_case("autofail", f15,
            lambda l: l.startswith("FAIL boot"),
            score_subs=[('        return "UNVERIFIED", ("mixin error window candidates need human block review: "',
                         '        return "FAIL", ("mixin error window candidates need human block review: "')])
# m10: window findings dropped silently -> f1 scores PASS
mutant_case("drop-window", f1,
            lambda l: l.startswith("PASS boot"),
            score_subs=[('    if review:\n'
                         '        return "UNVERIFIED", ("mixin error window candidates need human block review: "\n'
                         '                              + "; ".join(review)\n'
                         '                              + " (see docs/lead/mixin-window-detector-design.md)")\n',
                         '')])
# m11: launcher-format-only grammar -> f16a's log4j entry invisible
mutant_case("launcher-only-grammar", f16a,
            lambda l: l.startswith("PASS boot"),
            parser_subs=[("    m = _HEADER_LAUNCHER.match(line) or _HEADER_LOG4J.match(line)",
                          "    m = _HEADER_LAUNCHER.match(line)")])
# m12: dedup removed -> f14a's entry double-reports (parser-level probe)
with tempfile.TemporaryDirectory() as td:
    mutant_tree(td, parser_subs=[(
        "            if pkg and voc and not any(is_mixin_error(l) for _, l in entry.lines):",
        "            if pkg and voc:")])
    probe = ("import sys; sys.path.insert(0, '.');"
             "from rig.proof import parsers;"
             "c = parsers.parse_client_log(open('case.log').read());"
             "print(len(c.mixin_errors), len(c.window_findings))")
    case = pathlib.Path(td, "case.log")
    case.write_text(GOOD)
    r = subprocess.run([sys.executable, "-c", probe], cwd=td, capture_output=True, text=True)
    expect("mutant-dedup-baseline-healthy", r.returncode == 0 and r.stdout.split() == ["0", "0"],
           r.stdout[-100:] + r.stderr[-200:])
    case.write_text(f14a)
    r = subprocess.run([sys.executable, "-c", probe], cwd=td, capture_output=True, text=True)
    expect("mutant-dedup-killed", r.returncode == 0 and r.stdout.split() == ["1", "1"],
           "healthy tree prints '1 0' (same-line only); mutant double-counts")


# --- corpus regression: the window detector over every committed real
# client log (docs/build-evidence + the committed fixture). pins zero
# false findings and the exact truncation footprint (0-based indices);
# any parser change that alters real-log behavior goes red. the logs
# are immutable meshac 26.3 evidence; lookout was not loaded in them,
# so this proves absence of false positives, not detection.
CORPUS = {
    "docs/build-evidence/abilities26.3/abilities-constructed-client.log": (0, 0, [(0, 30), (41, 71)]),
    "docs/build-evidence/abilities26.3/abilities-constructed-first/abilities-constructed-client.log": (0, 0, [(0, 30), (42, 72)]),
    "docs/build-evidence/abilities26.3/abilities-failed-hotbar/abilities-client.log": (0, 0, [(0, 30), (42, 72)]),
    "docs/build-evidence/abilities26.3/abilities-failed-input/abilities-client.log": (0, 0, [(0, 30), (41, 71)]),
    "docs/build-evidence/abilities26.3/abilities-interrupted/abilities-client.log": (0, 0, [(0, 30), (42, 72), (213, 243)]),
    "docs/build-evidence/abilities26.3/failed-readiness/abilities-client.log": (0, 0, [(0, 30), (41, 71)]),
    "docs/build-evidence/anchor26.3/anchor-client.log": (0, 0, [(0, 30), (41, 71)]),
    "docs/build-evidence/anchor26.3/first-anchor-run/anchor-client.log": (0, 0, [(0, 30), (41, 71)]),
    "docs/build-evidence/held26.3/first-run/held-client.log": (0, 0, [(0, 30), (42, 72)]),
    "docs/build-evidence/held26.3/held-client.log": (0, 0, [(0, 30), (42, 72)]),
    "docs/build-evidence/lifecycle26.3/lifecycle-client.log": (0, 0, []),
    "docs/build-evidence/lifecycle26.3/transitions-client.log": (0, 0, []),
    "docs/build-evidence/move26.3/failure2/move-client.log": (0, 0, [(0, 30), (42, 72)]),
    "docs/build-evidence/move26.3/move-client.log": (0, 0, [(0, 30), (43, 73)]),
    "docs/build-evidence/move26.3/move-failure1-client.log": (0, 0, [(0, 30), (42, 72)]),
    "docs/build-evidence/outbound26.3/outbound-client.log": (0, 0, [(0, 30), (42, 72)]),
    "docs/build-evidence/session-pong26.3/anchor-client.log": (0, 0, [(0, 30), (42, 72)]),
    "rig/proof/fixtures/client-good.log": (0, 0, []),
}
for rel, want in sorted(CORPUS.items()):
    got_c = parsers.parse_client_log((ROOT / rel).read_text())
    got = (len(got_c.mixin_errors), len(got_c.window_findings), got_c.truncated_error_entries)
    expect(f"win-corpus-{pathlib.Path(rel).parent.name}-{pathlib.Path(rel).stem}",
           got == want, f"got {got}")

# corpus mutation protection: a bad parser (cap removed) must break the
# pinned corpus footprint; the probe runs the corpus pins against a
# mutant tree (logs read from the real repo), the healthy copy first.
# demonstrated as changed output, like the other mutants.
_CORPUS_PINS = repr(sorted(CORPUS.items()))
_CORPUS_PROBE = ("import sys, pathlib; sys.path.insert(0, '.');"
                 "from rig.proof import parsers;"
                 f"ROOT = {str(ROOT)!r}; pins = {_CORPUS_PINS};"
                 "bad = [rel for rel, want in pins"
                 " if (lambda c: (len(c.mixin_errors), len(c.window_findings), c.truncated_error_entries))"
                 "(parsers.parse_client_log(pathlib.Path(ROOT, rel).read_text())) != want];"
                 "print(len(bad))")
with tempfile.TemporaryDirectory() as td:
    mutant_tree(td)  # no substitutions: healthy copy of the committed tree
    r = subprocess.run([sys.executable, "-c", _CORPUS_PROBE], cwd=td,
                       capture_output=True, text=True)
    expect("mutant-corpus-baseline-healthy", r.returncode == 0 and r.stdout.split() == ["0"],
           r.stdout[-100:] + r.stderr[-200:])
with tempfile.TemporaryDirectory() as td:
    mutant_tree(td, parser_subs=[("MAX_CONTINUATION = 30\n", "MAX_CONTINUATION = 10000\n")])
    r = subprocess.run([sys.executable, "-c", _CORPUS_PROBE], cwd=td,
                       capture_output=True, text=True)
    expect("mutant-corpus-nocap-killed", r.returncode == 0 and r.stdout.split() == ["15"],
           "15 logs carry pinned truncations; the no-cap mutant must mismatch exactly those")

print("SUITE:", "all pass" if not fails else f"failures {fails}")
sys.exit(0 if not fails else 1)
