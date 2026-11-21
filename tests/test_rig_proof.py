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

GOOD = """[12:00:00] [main/INFO]: Loading Minecraft 26.3 with Fabric Loader 0.19.5
\t- lookout 2.9.7.28-meshac-0.1.0
[12:00:01] [Render thread/INFO]: [lookout] watching (client-side companion to meshac)
[12:00:02] [Render thread/INFO]: [lookout-stats] entity=7 d=1.20 dy=0.42 ground=true glide=false water=false vehicle=false exempt=false vel=100 tp=200 grace=false
[12:00:03] [Render thread/INFO]: [lookout-stats] entity=7 d=0.80 dy=0.00 ground=true glide=false water=false vehicle=false exempt=false vel=90 tp=200 grace=false
[12:00:04] [Render thread/INFO]: [lookout-stats] entity=7 d=2.10 dy=0.42 ground=true glide=false water=false vehicle=false exempt=false vel=110 tp=200 grace=false
[12:00:05] [Render thread/INFO]: [lookout-stats] entity=7 d=0.50 dy=0.00 ground=true glide=false water=false vehicle=false exempt=false vel=95 tp=200 grace=false
[12:00:06] [Render thread/INFO]: [lookout-stats] entity=7 d=1.00 dy=0.42 ground=true glide=false water=false vehicle=false exempt=false vel=105 tp=200 grace=false
[12:00:07] [Render thread/INFO]: [lookout] alert entity=7 check=killaura_facing
"""

# parser: mod entry needs the loader table line, not just the watching line
c = parsers.parse_client_log(GOOD)
expect("parse-mod-listed", c.mod_listed)
expect("parse-watching", c.watching)
expect("parse-mixin-clean", c.mixin_errors == [])
expect("parse-stats-count", c.stats == {7: 5}, f"got {c.stats}")
_real_stats = ("[12:00:02] [Render thread/INFO]: [lookout-stats] entity=7 d=1.20 dy=0.42"
               " ground=true glide=false water=false vehicle=false"
               " exempt=false vel=100 tp=200 grace=false")
expect("parse-stats-negative-old-form",
       parsers.parse_client_log("[lookout-stats] entity=7 samples=9\n").stats == {})
expect("parse-stats-negative-incomplete",
       parsers.parse_client_log(_real_stats.rsplit(" grace=false", 1)[0] + "\n").stats == {})
expect("parse-stats-negative-suffix",
       parsers.parse_client_log(_real_stats + " extra=1\n").stats == {})
expect("parse-stats-entity-separation",
       parsers.parse_client_log(
           ("[12:00:02] [Render thread/INFO]: [lookout-stats] entity=7 d=1.20 dy=0.42 ground=true glide=false water=false"
            " vehicle=false exempt=false vel=100 tp=200 grace=false\n") * 3 +
           ("[12:00:03] [Render thread/INFO]: [lookout-stats] entity=9 d=0.50 dy=-0.10 ground=false glide=true water=false"
            " vehicle=false exempt=false vel=90 tp=200 grace=false\n") * 2).stats == {7: 3, 9: 2})
expect("score-delivery-boundary-4",
       score.row_target_delivery(parsers.parse_client_log((_real_stats + "\n") * 4))[0] == "FAIL")
expect("score-delivery-boundary-5",
       score.row_target_delivery(parsers.parse_client_log((_real_stats + "\n") * 5))[0] == "PASS")
expect("parse-stats-comma-decimal",
       parsers.parse_client_log(
           "[12:00:02] [Render thread/INFO]: [lookout-stats] entity=7 d=1,20 dy=-0,10 ground=true glide=false"
           " water=false vehicle=false exempt=false vel=100 tp=200 grace=false\n"
       ).stats == {7: 1})
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

traced = parsers.parse_client_log("[12:00:02] [Render thread/INFO]: [lookout] trace alert entity=9 check=reach_rough\n")
expect("parse-alert-trace-flag", traced.alerts and traced.alerts[0].trace)

ng = parsers.parse_client_log(
    "[12:00:10] [Render thread/INFO]: [lookout-proof] null-gui-window begin\n"
    "[12:00:11] [Render thread/INFO]: [lookout] alert entity=1 check=x\n"
    "[12:00:12] [Render thread/INFO]: [lookout-proof] null-gui-window end\n"
    "[12:00:13] [Render thread/INFO]: [lookout-proof] null-gui-window begin\n")
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
       len(parsers.parse_server_log(
           "[12:00:20] [Server thread/INFO]: System chat: entity 1042 joined the game\n"
           "[12:00:21] [Server thread/INFO]: noise\n")) == 1)

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
expect("score-delivery-pass-detail",
       "entity 7 (5 lines)" in score.row_target_delivery(good)[1]
       and "sampled-stats smoke heuristic" in score.row_target_delivery(good)[1]
       and "packet flow observed" not in score.row_target_delivery(good)[1],
       score.row_target_delivery(good)[1])
thin = parsers.parse_client_log("[12:00:02] [Render thread/INFO]: [lookout-stats] entity=1 d=0.50 dy=0.00 ground=true glide=false water=false vehicle=false exempt=false vel=90 tp=200 grace=false\n")
expect("score-delivery-fail-thin", score.row_target_delivery(thin)[0] == "FAIL")
expect("score-delivery-fail-detail",
       "stats lines" in score.row_target_delivery(thin)[1]
       and "teleportJumpStats" in score.row_target_delivery(thin)[1]
       and "trace off" not in score.row_target_delivery(thin)[1],
       score.row_target_delivery(thin)[1])

expect("score-chat-pass", score.row_chat_capture(good)[0] == "PASS")
expect("score-chat-fail-none",
       score.row_chat_capture(parsers.parse_client_log("[12:00:02] [Render thread/INFO]: [lookout-stats] entity=1 d=0.50 dy=0.00 ground=true glide=false water=false vehicle=false exempt=false vel=90 tp=200 grace=false\n"))[0] == "FAIL")

ng_client = parsers.parse_client_log(
    GOOD + "[12:00:10] [Render thread/INFO]: [lookout-proof] null-gui-window begin\n"
           "[12:00:11] [Render thread/INFO]: [lookout] alert entity=7 check=reach_rough\n"
           "[12:00:12] [Render thread/INFO]: [lookout-proof] null-gui-window end\n")
expect("score-nullgui-pass", score.row_null_gui(ng_client)[0] == "PASS")
expect("score-nullgui-unverified-no-markers", score.row_null_gui(good)[0] == "UNVERIFIED")
quiet_window = parsers.parse_client_log(
    GOOD + "[12:00:10] [Render thread/INFO]: [lookout-proof] null-gui-window begin\n"
           "[12:00:12] [Render thread/INFO]: [lookout-proof] null-gui-window end\n")
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
    bad.write_text("[12:00:00] [main/ERROR]: Mixin apply failed dev.lookout.mixin.ClientPacketListenerMixin\n"
                   "\t- lookout 2.9.7.28-meshac-0.1.0\n"
                   "[12:00:01] [Render thread/INFO]: " + parsers.WATCHING + "\n")
    r = subprocess.run([sys.executable, "rig/proof/proof.py",
                        "--client-log", str(bad), "--row", "boot"],
                       cwd=ROOT, capture_output=True, text=True)
    expect("live-boot-fail-exit1",
           r.returncode == 1 and any(l.startswith("FAIL boot") for l in r.stdout.splitlines()))

    # mutant: score.py without the mixin-error check must pass that bad log
    src = (ROOT / "rig/proof/score.py").read_text()
    block = '''    if client.mixin_errors:
        detail = f"{len(client.mixin_errors)} dev.lookout mixin error lines"
        if review:
            detail += "; plus unreviewed window candidates: " + "; ".join(review)
        return "FAIL", detail
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

# f20: frozen blind spot (docs/lead/mixin-error-multiline-limits.md) -
# package-less summary: an ERROR entry whose only failure signal is
# mixin vocabulary, with dev.lookout nowhere in the log, is invisible
# to the same-line scan AND the window detector. boot PASS here means
# "no same-line match", never "no mixin errors"; the PASS detail says
# surface-check-only. characterization pin: a detection change must
# edit this pin explicitly
f20 = (BASE + "[12:10:00] [Render thread/ERROR]: Mixin apply failed\n"
       "[12:10:01] [main/INFO]: done\n")
c20 = parsers.parse_client_log(f20)
expect("win-f20-packageless-invisible",
       c20.mixin_errors == [] and c20.window_findings == [])
expect("win-f20-boot-pass", score.row_boot(c20)[0] == "PASS")

# f21: frozen blind spot - config-name-only error: names lookout.json
# and carries vocabulary but never the dev.lookout package form;
# invisible to both scans. same PASS caveat as f20
f21 = (BASE + "[12:10:00] [main/ERROR]: Failed to apply lookout.json"
       " config - watcher disabled\n"
       "[12:10:01] [main/INFO]: done\n")
c21 = parsers.parse_client_log(f21)
expect("win-f21-configname-invisible",
       c21.mixin_errors == [] and c21.window_findings == [])
expect("win-f21-boot-pass", score.row_boot(c21)[0] == "PASS")

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
            parser_subs=[("        if is_mixin_error(line) and not chat_routed:\n"
                          "            out.mixin_errors.append((i, line.strip()))",
                          "        if is_mixin_error(line) and entry is not None and not entry.truncated and len(entry.lines) <= MAX_CONTINUATION:\n"
                          "            out.mixin_errors.append((i, line.strip()))")])
# m9: window-only findings auto-FAIL (no human gate) -> f15 collision fails
mutant_case("autofail", f15,
            lambda l: l.startswith("FAIL boot"),
            score_subs=[('        return "UNVERIFIED", ("mixin error window candidates need human block review: "',
                         '        return "FAIL", ("mixin error window candidates need human block review: "')])
# m10: package requirement dropped from the same-line scan -> f20's
# package-less summary becomes a mixin error and boot FAILs
mutant_case("package-dropped", f20,
            lambda l: l.startswith("FAIL boot"),
            parser_subs=[("    return bool(_SEVERITY.search(line) and _PACKAGE.search(line) and _VOCAB.search(line))",
                          "    return bool(_SEVERITY.search(line) and _VOCAB.search(line))")])
# m11: package pattern widened from dev.lookout to the bare name ->
# f21's config-name-only error becomes a mixin error and boot FAILs
mutant_case("package-widened", f21,
            lambda l: l.startswith("FAIL boot"),
            parser_subs=[('_PACKAGE = re.compile(r"dev\.lookout")',
                          '_PACKAGE = re.compile(r"lookout")')])

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
            parser_subs=[("        hm = _HEADER_LAUNCHER.match(line) or _HEADER_LOG4J.match(line)",
                          "        hm = _HEADER_LAUNCHER.match(line)")])
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


# --- server receipt corpus: parse_server_log over every committed real
# server log. pins exact receipt counts (0-based content stays out of
# the pins: real logs carry a player handle, so counts only). bounded
# corpus: receipts are any-player join lines, never per-target proof.
SERVER_CORPUS = {
    "docs/build-evidence/abilities26.3/abilities-constructed-first/abilities-constructed-server.log": 2,
    "docs/build-evidence/abilities26.3/abilities-constructed-server.log": 2,
    "docs/build-evidence/abilities26.3/abilities-failed-hotbar/abilities-server.log": 2,
    "docs/build-evidence/abilities26.3/abilities-failed-input/abilities-server.log": 2,
    "docs/build-evidence/abilities26.3/abilities-interrupted/abilities-server.log": 2,
    "docs/build-evidence/abilities26.3/failed-readiness/abilities-server.log": 0,
    "docs/build-evidence/anchor26.3/anchor-server.log": 2,
    "docs/build-evidence/anchor26.3/first-anchor-run/anchor-server.log": 2,
    "docs/build-evidence/anchor26.3/observer-boot-failure/anchor-server.log": 0,
    "docs/build-evidence/held26.3/first-run/held-server.log": 2,
    "docs/build-evidence/held26.3/held-server.log": 2,
    "docs/build-evidence/lifecycle26.3/lifecycle-server.log": 2,
    "docs/build-evidence/lifecycle26.3/transitions-server.log": 4,
    "docs/build-evidence/move26.3/failure2/move-server.log": 2,
    "docs/build-evidence/move26.3/move-failure1-server.log": 2,
    "docs/build-evidence/move26.3/move-server.log": 2,
    "docs/build-evidence/outbound26.3/outbound-server.log": 2,
    "docs/build-evidence/session-pong26.3/anchor-server.log": 2,
    "rig/proof/fixtures/server-good.log": 2,
}
for rel, want in sorted(SERVER_CORPUS.items()):
    got = len(parsers.parse_server_log((ROOT / rel).read_text()))
    expect(f"srv-corpus-{pathlib.Path(rel).parent.name}-{pathlib.Path(rel).stem}",
           got == want, f"got {got}")

# server receipt echo hardening: player chat logs as "<handle> ..." and
# cannot echo a receipt; a headerless receipt-shaped line is excluded
# (no ambiguity machinery on the server side). synthetic id-only fixture
_ECHO = parsers.parse_server_log(
    (ROOT / "rig/proof/fixtures/server-echoes.log").read_text())
expect("srv-echo-real-forms-counted",
       _ECHO == ["[12:00:19] [Server thread/INFO]: entity1042[/127.0.0.1:25565]"
                 " logged in with entity id 1042 at (0.5, -60.0, 3.5)",
                 "[12:00:20] [Server thread/INFO]: System chat: entity 1042 joined the game"],
       f"got {_ECHO}")
expect("srv-echo-chat-and-headerless-filtered", len(_ECHO) == 2)
# isolated minimal bypass negative (QA 8.2): a chat prefix directly
# followed by the bare login phrase must NOT count
expect("srv-echo-login-minimal-negative",
       parsers.parse_server_log(
           "[12:00:32] [Server thread/INFO]: <entity1043> logged in with"
           " entity id 1042 at (0.5, -60.0, 3.5)\n") == [])
# genuine login positive: verified name + bracketed-address shape counts
expect("srv-echo-login-genuine-positive",
       parsers.parse_server_log(
           "[12:00:19] [Server thread/INFO]: entity1042[/127.0.0.1:25565]"
           " logged in with entity id 1042 at (0.5, -60.0, 3.5)\n")
       == ["[12:00:19] [Server thread/INFO]: entity1042[/127.0.0.1:25565]"
           " logged in with entity id 1042 at (0.5, -60.0, 3.5)"])

# server echo-hardening mutants, changed output on the echo fixture
# after a healthy baseline
_ECHO_PROBE = ("import sys, pathlib; sys.path.insert(0, '.');"
               "from rig.proof import parsers;"
               f"R = {str(ROOT)!r};"
               "import pathlib as pl;"
               "print(len(parsers.parse_server_log(pl.Path(R,"
               " 'rig/proof/fixtures/server-echoes.log').read_text())))")
def srv_mutant(name, subs, want):
    with tempfile.TemporaryDirectory() as td:
        mutant_tree(td, parser_subs=subs)
        r = subprocess.run([sys.executable, "-c", _ECHO_PROBE], cwd=td,
                           capture_output=True, text=True)
        expect(name, r.returncode == 0 and r.stdout.split() == want,
               r.stdout[-100:] + r.stderr[-200:])
srv_mutant("mutant-srv-echo-baseline-healthy", [], ["2"])
# payload-start anchoring dropped (unanchored pattern + search): the
# two chat join echoes re-count
srv_mutant("mutant-srv-anchor-dropped-killed",
           [('_SERVER_JOIN = re.compile(r"^System chat: .+ joined the game\\s*$")',
             '_SERVER_JOIN = re.compile(r"joined the game")'),
            ("        if _SERVER_JOIN.match(payload) or _SERVER_LOGIN.match(payload):",
             "        if _SERVER_JOIN.search(payload) or _SERVER_LOGIN.search(payload):")],
           ["4"])
# login shape relaxed to a bare token: the minimal chat echo re-counts
# (2 real + the <entity1043> minimal echo = 3; the spoofed-address echo
# still fails: its second token is not the login phrase)
srv_mutant("mutant-srv-login-shape-relaxed-killed",
           [('_SERVER_LOGIN = re.compile(r"^\\w{3,16}\\[/[^\\]]+\\] logged in with entity id \\d+ at \\(")',
             '_SERVER_LOGIN = re.compile(r"^\\S+ logged in with entity id \\d+ at \\(")')],
           ["3"])
# header requirement dropped (headerless line scanned as its own
# payload): the headerless receipt-shaped line counts
srv_mutant("mutant-srv-header-required-dropped-killed",
           [("        if hm is None:\n            continue\n        payload = line[hm.end():].lstrip()",
             "        payload = line[hm.end():].lstrip() if hm else line")],
           ["3"])

# server corpus mutation protection: both receipt patterns disabled ->
# zero receipts anywhere, so exactly the 17 receipt-bearing pins break
# (the 2 zero-receipt evidence logs still match and are not part of
# the kill count)
_SERVER_PINS = repr(sorted(SERVER_CORPUS.items()))
_SERVER_PROBE = ("import sys, pathlib; sys.path.insert(0, '.');"
                 "from rig.proof import parsers;"
                 f"ROOT = {str(ROOT)!r}; pins = {_SERVER_PINS};"
                 "bad = [rel for rel, want in pins"
                 " if len(parsers.parse_server_log(pathlib.Path(ROOT, rel).read_text())) != want];"
                 "print(len(bad))")
with tempfile.TemporaryDirectory() as td:
    mutant_tree(td)  # healthy copy of the committed tree
    r = subprocess.run([sys.executable, "-c", _SERVER_PROBE], cwd=td,
                       capture_output=True, text=True)
    expect("mutant-srv-corpus-baseline-healthy", r.returncode == 0 and r.stdout.split() == ["0"],
           r.stdout[-100:] + r.stderr[-200:])
with tempfile.TemporaryDirectory() as td:
    mutant_tree(td, parser_subs=[(
        '_SERVER_JOIN = re.compile(r"^System chat: .+ joined the game\\s*$")',
        '_SERVER_JOIN = re.compile(r"a-string-that-never-appears")'), (
        '_SERVER_LOGIN = re.compile(r"^\\w{3,16}\\[/[^\\]]+\\] logged in with entity id \\d+ at \\(")',
        '_SERVER_LOGIN = re.compile(r"another-string-that-never-appears")')])
    r = subprocess.run([sys.executable, "-c", _SERVER_PROBE], cwd=td,
                       capture_output=True, text=True)
    expect("mutant-srv-corpus-killed", r.returncode == 0 and r.stdout.split() == ["17"],
           "17 files carry receipts; the never-match mutant must mismatch exactly those")


# --- lookalike corpus + hardening: chat-routed echoes are FILTERED
# (negatives), headerless/nested/malformed-header marker lines are
# AMBIGUOUS (counted nowhere, rows go UNVERIFIED). this block replaces
# the r2 frozen characterization explicitly: the echoes that were
# pinned as counted are now pinned as filtered.
_LOOKALIKE = parsers.parse_client_log(
    (ROOT / "rig/proof/fixtures/client-lookalikes.log").read_text())
expect("lookalike-alerts-filtered", _LOOKALIKE.alerts == [],
       f"got {[(a.entity, a.check, a.trace) for a in _LOOKALIKE.alerts]}")
expect("lookalike-watching-real-still-counted", _LOOKALIKE.watching is True)
expect("lookalike-stats-filtered", _LOOKALIKE.stats == {}, f"got {_LOOKALIKE.stats}")
expect("lookalike-null-gui-filtered", _LOOKALIKE.null_gui_windows == [],
       f"got {_LOOKALIKE.null_gui_windows}")
expect("lookalike-mixin-error-filtered", _LOOKALIKE.mixin_errors == [])
expect("lookalike-no-window-findings", _LOOKALIKE.window_findings == [])
_WATCH_ECHO = parsers.parse_client_log(
    (ROOT / "rig/proof/fixtures/client-lookalikes-watching-echo.log").read_text())
expect("lookalike-watching-echo-filtered", _WATCH_ECHO.watching is False)
expect("lookalike-watching-real-control",
       parsers.parse_client_log(
           "[12:00:00] [Render thread/INFO]: [lookout] watching"
           " (client-side companion to meshac)\n").watching is True)

# scorer rows on filtered echoes: echoes must not satisfy any row
echo_stats5 = parsers.parse_client_log(
    "".join(("[12:00:0%d] [Render thread/INFO]: [CHAT] <1057>"
             " [lookout-stats] entity=21 d=1.20 dy=0.42 ground=true glide=false"
             " water=false vehicle=false exempt=false vel=100 tp=200 grace=false\n")
            % i for i in range(5)))
expect("lookalike-scorer-delivery-echo-filtered",
       score.row_target_delivery(echo_stats5)[0] == "FAIL",
       score.row_target_delivery(echo_stats5)[1])
expect("lookalike-scorer-chat-capture-echo-filtered",
       score.row_chat_capture(parsers.parse_client_log(
           "[12:00:01] [Render thread/INFO]: [CHAT] <1057>"
           " [lookout] alert entity=21 check=reach\n"))[0] == "FAIL")
expect("lookalike-scorer-null-gui-echo-filtered",
       score.row_null_gui(parsers.parse_client_log(
           "[12:00:05] [Render thread/INFO]: [CHAT] <1057> [lookout-proof] null-gui-window begin\n"
           "[12:00:06] [Render thread/INFO]: [CHAT] <1057> [lookout] alert entity=23 check=timer\n"
           "[12:00:07] [Render thread/INFO]: [CHAT] <1057> [lookout-proof] null-gui-window end\n"
       ))[0] == "UNVERIFIED")
_spoof_boot = parsers.parse_client_log(
    "\t- lookout 2.9.7.28\n"
    "[12:00:00] [Render thread/INFO]: [lookout] watching (client-side companion to meshac)\n"
    "[12:00:08] [Render thread/INFO]: [CHAT] <1057> ERROR Mixin apply failed dev.lookout.inject\n")
expect("lookalike-scorer-boot-echo-filtered",
       score.row_boot(_spoof_boot)[0] == "PASS", score.row_boot(_spoof_boot)[1])

# --- envelope + ambiguity corpus: real-envelope positives, chat/system
# negatives, bare/nested/malformed-header ambiguities
_ENVELOPE = parsers.parse_client_log(
    (ROOT / "rig/proof/fixtures/client-hardening-envelopes.log").read_text())
expect("envelope-watching-log4j-counted", _ENVELOPE.watching is True)
expect("envelope-real-alert-counted",
       [(a.entity, a.check) for a in _ENVELOPE.alerts] == [(31, "reach"), (36, "reach")],
       f"got {[(a.entity, a.check) for a in _ENVELOPE.alerts]}")
# line 10 (0-based): valid outer header, MALFORMED inner header + alert -
# nested detection covers only fully recognized inner headers, so this
# counts today. frozen known-gap pin, not a qualification.
expect("envelope-malformed-inner-counts-today",
       any(a.entity == 36 for a in _ENVELOPE.alerts))
expect("envelope-stats-echo-filtered", _ENVELOPE.stats == {})
expect("envelope-ambiguous-alerts",
       [(i, k) for i, k in _ENVELOPE.ambiguous_markers]
       == [(5, "alert"), (6, "alert"), (7, "alert")],
       f"got {_ENVELOPE.ambiguous_markers}")
expect("envelope-no-mixin-errors", _ENVELOPE.mixin_errors == [])
expect("envelope-no-window-findings", _ENVELOPE.window_findings == [])
expect("envelope-scorer-chat-capture-unverified",
       score.row_chat_capture(_ENVELOPE)[0] == "UNVERIFIED"
       and "unclassified envelope" in score.row_chat_capture(_ENVELOPE)[1],
       score.row_chat_capture(_ENVELOPE)[1])
expect("envelope-scorer-delivery-fail-no-ambiguity",
       score.row_target_delivery(_ENVELOPE)[0] == "FAIL")
expect("envelope-scorer-delivery-ambiguous-unverified",
       score.row_target_delivery(parsers.parse_client_log(
           "[lookout-stats] entity=31 d=1.20 dy=0.42 ground=true glide=false"
           " water=false vehicle=false exempt=false vel=100 tp=200 grace=false\n"))[0]
       == "UNVERIFIED")
expect("envelope-scorer-null-gui-ambiguous-unverified",
       score.row_null_gui(parsers.parse_client_log(
           "[lookout-proof] null-gui-window begin\n"))[0] == "UNVERIFIED")
expect("envelope-scorer-boot-ambiguous-watching-unverified",
       score.row_boot(parsers.parse_client_log(
           "\t- lookout 2.9.7.28\n"
           "[lookout] watching (client-side companion to meshac)\n"))[0] == "UNVERIFIED")
# a chat-routed line always carries its own INFO header, so it closes
# any open entry and opens an INFO entry: it cannot join an open ERROR
# entry to manufacture a window finding
expect("envelope-chat-cannot-join-error-entry",
       parsers.parse_client_log(
           "[12:00:10] [main/ERROR]: some unrelated failure\n"
           "[12:00:11] [Render thread/INFO]: [System] [CHAT] dev.lookout inject failed\n"
       ).window_findings == [])
# precedence: definitive FAIL evidence is never masked by ambiguity
_prec = parsers.parse_client_log(
    "\t- lookout 2.9.7.28\n"
    "[12:00:00] [Render thread/INFO]: [lookout] watching (client-side companion to meshac)\n"
    "[12:00:01] [main/ERROR]: Mixin apply failed dev.lookout.mixin.ClientPacketListenerMixin\n"
    "[lookout] watching (client-side companion to meshac)\n")
expect("boot-precedence-fail-beats-ambiguity",
       score.row_boot(_prec)[0] == "FAIL" and "mixin error" in score.row_boot(_prec)[1],
       score.row_boot(_prec)[1])
# null-gui: an ambiguous alert inside a valid window is unverifiable
# evidence for the row
_ng_amb = parsers.parse_client_log(
    "[12:00:10] [Render thread/INFO]: [lookout-proof] null-gui-window begin\n"
    "[lookout] alert entity=40 check=reach\n"
    "[12:00:12] [Render thread/INFO]: [lookout-proof] null-gui-window end\n")
expect("null-gui-ambiguous-alert-in-window-unverified",
       score.row_null_gui(_ng_amb)[0] == "UNVERIFIED"
       and "inside a null-gui window" in score.row_null_gui(_ng_amb)[1],
       score.row_null_gui(_ng_amb)[1])
expect("envelope-scorer-boot-clean-pass",
       score.row_boot(parsers.parse_client_log(
           "\t- lookout 2.9.7.28\n"
           "[12:00:00] [Render thread/INFO]: [lookout] watching"
           " (client-side companion to meshac)\n"))[0] == "PASS")

# hardening mutation protection, changed output on the two fixtures
# after a healthy baseline: removing the chat filter must re-count the
# echoes, dropping ambiguity recording must erase the UNVERIFIED basis,
# and disabling the nested-header check must count the nested line.
_HARDEN_PROBE = ("import sys, pathlib; sys.path.insert(0, '.');"
                 "from rig.proof import parsers;"
                 f"R = {str(ROOT)!r};"
                 "import pathlib as pl;"
                 "a = parsers.parse_client_log(pl.Path(R,"
                 " 'rig/proof/fixtures/client-lookalikes.log').read_text());"
                 "b = parsers.parse_client_log(pl.Path(R,"
                 " 'rig/proof/fixtures/client-hardening-envelopes.log').read_text());"
                 "print(len(a.alerts), int(a.watching), sum(a.stats.values()),"
                 " len(a.null_gui_windows), len(a.mixin_errors), len(a.ambiguous_markers));"
                 "print(len(b.alerts), int(b.watching), sum(b.stats.values()),"
                 " len(b.null_gui_windows), len(b.mixin_errors), len(b.ambiguous_markers))")
def hardening_mutant(name, subs, want):
    with tempfile.TemporaryDirectory() as td:
        mutant_tree(td, parser_subs=subs)
        r = subprocess.run([sys.executable, "-c", _HARDEN_PROBE], cwd=td,
                           capture_output=True, text=True)
        got = r.stdout.split()
        expect(name, r.returncode == 0 and got == want,
               "got " + " ".join(got) + r.stderr[-200:])
hardening_mutant("mutant-hardening-baseline-healthy", [],
                 ["0", "1", "0", "0", "0", "0", "2", "1", "0", "0", "0", "3"])
hardening_mutant("mutant-hardening-chatfilter-removed-killed",
                 [("            if not matched or chat_routed:",
                   "            if not matched:")],
                 ["3", "1", "1", "1", "0", "0", "3", "1", "1", "0", "0", "3"])
hardening_mutant("mutant-hardening-ambiguity-dropped-killed",
                 [("                out.ambiguous_markers.append((i, kind))",
                   "                pass")],
                 ["0", "1", "0", "0", "0", "0", "2", "1", "0", "0", "0", "0"])
# precedence mutant: ambiguity checked BEFORE the definitive FAIL
# evidence must turn the precedence regression log UNVERIFIED
_PREC_PROBE = ("import sys, pathlib; sys.path.insert(0, '.');"
               "from rig.proof import parsers, score;"
               "c = parsers.parse_client_log("
               "'\\t- lookout 2.9.7.28\\n"
               "[12:00:00] [Render thread/INFO]: [lookout] watching"
               " (client-side companion to meshac)\\n"
               "[12:00:01] [main/ERROR]: Mixin apply failed"
               " dev.lookout.mixin.ClientPacketListenerMixin\\n"
               "[lookout] watching (client-side companion to meshac)\\n');"
               "print(score.row_boot(c)[0])")
def precedence_mutant(name, subs, want):
    with tempfile.TemporaryDirectory() as td:
        mutant_tree(td, score_subs=subs)
        r = subprocess.run([sys.executable, "-c", _PREC_PROBE], cwd=td,
                           capture_output=True, text=True)
        expect(name, r.returncode == 0 and r.stdout.strip() == want,
               r.stdout[-120:] + r.stderr[-200:])
precedence_mutant("mutant-precedence-baseline-healthy", [], "FAIL")
precedence_mutant("mutant-precedence-ambiguity-first-killed",
                  [("    if client.mixin_errors:",
                    "    amb = _ambiguous(client, \"watching\")\n"
                    "    if amb:\n        return amb\n"
                    "    if client.mixin_errors:")],
                  "UNVERIFIED")
hardening_mutant("mutant-hardening-nested-disabled-killed",
                 [("        nested = bool(payload and (_HEADER_LAUNCHER.match(payload)\n"
                   "                                   or _HEADER_LOG4J.match(payload)))",
                   "        nested = False")],
                 ["0", "1", "0", "0", "0", "0", "3", "1", "0", "0", "0", "2"])

# --- format sync gate: parser formats pinned against the exact source
# literals that emit them (a format change in lookout source breaks the
# harness visibly here, not silently in a scored row). alert formats are
# checked in all three variant sources. checks live in
# tests/sync_helper.py so mutant probes can reuse them.
sys.path.insert(0, str(ROOT / "tests"))
import sync_helper
expect("format-sync-source-literals", sync_helper.sync_findings(ROOT, parsers) == [])

# sync-gate mutation protection, shown as changed output: the pre-fix
# drifted stats regex and a broken alert regex must each fail the gate
# inside a mutant tree, healthy copy first.
_SYNC_PROBE = ("import sys, pathlib; sys.path.insert(0, '.');"
               "from rig.proof import parsers; import sync_helper;"
               "print(','.join(sync_helper.sync_findings(pathlib.Path('.'), parsers)) or 'clean')")
_SYNC_SOURCES = ("src/main/java/dev/lookout/LookoutClient.java",
                 "src/main/java/dev/lookout/core/LookoutEngine.java",
                 "src/main/java/dev/lookout/mc/ChatAlerter.java",
                 "src/mc1.21.11/java/dev/lookout/mc/ChatAlerter.java",
                 "src/mc26.1.2/java/dev/lookout/mc/ChatAlerter.java",
                 "docs/lead/rig-proof-runbook.md")
def sync_mutant(name, want_in_output, parser_subs=None, java_subs=None):
    # parser_subs: (line_prefix, replacement) swaps one regex definition
    # line in the mutant parser. java_subs: (relpath, old, new) literal
    # substitutions on the copied emission sources. None = healthy copy.
    with tempfile.TemporaryDirectory() as td:
        mutant_tree(td)
        if parser_subs is not None:
            line_prefix, replacement = parser_subs
            p = pathlib.Path(td, "rig/proof/parsers.py")
            lines = p.read_text().splitlines()
            hit = [i for i, ln in enumerate(lines) if ln.startswith(line_prefix)]
            assert len(hit) == 1, f"sync mutant prefix not unique: {line_prefix!r}"
            # consume the whole (possibly multi-line) regex statement
            end = hit[0]
            while lines[end].count("(") > lines[end].count(")") or \
                    (end > hit[0] and
                     "\n".join(lines[hit[0]:end + 1]).count("(") >
                     "\n".join(lines[hit[0]:end + 1]).count(")")):
                end += 1
            lines[hit[0]:end + 1] = [replacement]
            p.write_text("\n".join(lines) + "\n")
        for rel in _SYNC_SOURCES:
            dst = pathlib.Path(td, rel)
            dst.parent.mkdir(parents=True, exist_ok=True)
            dst.write_text((ROOT / rel).read_text())
        for rel, old, new in (java_subs or []):
            f = pathlib.Path(td, rel)
            body = f.read_text()
            assert body.count(old) == 1, f"java mutant anchor not unique: {old[:60]!r}"
            f.write_text(body.replace(old, new))
        (pathlib.Path(td) / "sync_helper.py").write_text(
            (ROOT / "tests/sync_helper.py").read_text())
        r = subprocess.run([sys.executable, "-c", _SYNC_PROBE], cwd=td,
                           capture_output=True, text=True)
        expect(name, r.returncode == 0 and want_in_output in r.stdout,
               r.stdout[-120:] + r.stderr[-200:])
_ENG = "src/main/java/dev/lookout/core/LookoutEngine.java"
sync_mutant("mutant-sync-baseline-healthy", "clean")
sync_mutant("mutant-sync-drifted-stats-killed", "stats",
            parser_subs=("STATS = re.compile",
                         'STATS = re.compile(r"\\[lookout-stats\\] entity=(\\d+) samples=(\\d+)")'))
sync_mutant("mutant-sync-broken-alert-killed", "alert-main",
            parser_subs=("ALERT = re.compile",
                         'ALERT = re.compile(r"\\[lookout\\] (trace )?alert entity=(\\d+) check=(zzz)")'))
sync_mutant("mutant-sync-emitter-prefix-killed", "stats",
            java_subs=[(_ENG, '"[lookout-stats] entity="', '"[lookout-stats] id="')])
sync_mutant("mutant-sync-emitter-field-killed", "stats",
            java_subs=[(_ENG, '" ground="', '" gnd="')])
sync_mutant("mutant-sync-emitter-commented-killed", "stats",
            java_subs=[(_ENG, 'System.out.println("[lookout-stats] entity="',
                        '// System.out.println("[lookout-stats] entity="')])
sync_mutant("mutant-sync-emitter-assignment-killed", "stats",
            java_subs=[(_ENG, 'System.out.println("[lookout-stats] entity="',
                        'String lookoutStatsUnused = "[lookout-stats] entity="')])
_CLIENT = "src/main/java/dev/lookout/LookoutClient.java"
_RB = "docs/lead/rig-proof-runbook.md"
sync_mutant("mutant-sync-watching-commented-killed", "watching",
            java_subs=[(_CLIENT, 'LOGGER.info("[lookout] watching',
                        '// LOGGER.info("[lookout] watching')])
sync_mutant("mutant-sync-watching-assignment-killed", "watching",
            java_subs=[(_CLIENT, 'LOGGER.info("[lookout] watching',
                        'String lookoutWatchUnused = "[lookout] watching')])
sync_mutant("mutant-sync-watching-literal-killed", "watching",
            java_subs=[(_CLIENT, 'companion to meshac)"',
                        'companion to meshac!)"')])
sync_mutant("mutant-sync-nullgui-doc-killed", "null-gui",
            java_subs=[(_RB, 'null-gui-window begin', 'null-gui-window start')])
sync_mutant("mutant-sync-alert-commented-killed", "alert-main",
            java_subs=[("src/main/java/dev/lookout/mc/ChatAlerter.java",
                        'LOGGER.info("[lookout] alert entity={} check={}"',
                        '// LOGGER.info("[lookout] alert entity={} check={}"')])
sync_mutant("mutant-sync-alert-assignment-killed", "alert-1.21.11",
            java_subs=[("src/mc1.21.11/java/dev/lookout/mc/ChatAlerter.java",
                        'LOGGER.info("[lookout] alert entity={} check={}"',
                        'String lookoutAlertUnused = "[lookout] alert entity={} check={}"')])
sync_mutant("mutant-sync-alert-literal-killed", "alert-26.1.2",
            java_subs=[("src/mc26.1.2/java/dev/lookout/mc/ChatAlerter.java",
                        '"[lookout] alert entity={} check={}"',
                        '"[lookout] alert ent={} check={}"')])
sync_mutant("mutant-sync-nullgui-parser-killed", "null-gui",
            parser_subs=("NULL_GUI = re.compile",
                         'NULL_GUI = re.compile(r"\\[lookout-proof\\] null-gui-window (start|stop)")'))

print("SUITE:", "all pass" if not fails else f"failures {fails}")
sys.exit(0 if not fails else 1)
