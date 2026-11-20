"""Row scoring for the lookout rig proof harness.

Verdicts: PASS, FAIL, or UNVERIFIED (missing inputs or insufficient
coverage; never pass-by-absence). Each row is a function over parsed
evidence returning (verdict, detail). Rows map to the compatibility
matrix's rig-gated cells; PASS here is still only log-level proof - see
the runbook for what each row does not prove.
"""
from rig.proof import parsers

MIN_SAMPLES = 5          # stats lines per entity for target delivery
G4_MIN_COUNTS = {"killaura": 6, "reach": 6, "multiaura": 3, "timer": 6}

def row_boot(client):
    if client is None:
        return "UNVERIFIED", "no client log supplied"
    problems = []
    if not client.mod_listed:
        problems.append("lookout not in the loader mod list")
    if client.mixin_errors:
        problems.append(f"{len(client.mixin_errors)} dev.lookout mixin error lines")
    if not client.watching:
        problems.append("watching line absent")
    if problems:
        return "FAIL", "; ".join(problems)
    return "PASS", ("mod listed, watching line present, no detected mixin errors "
                    "(surface check only; not full G1 weave proof)")

def row_g2_optin(config):
    if config is None:
        return "UNVERIFIED", "no lookout.json supplied or unreadable"
    off = [k for k, v in config.items() if not v]
    if off:
        return "FAIL", f"not active: {', '.join(off)}"
    return "PASS", "master enabled and killaura, reach, timer, multiAura all true"

def row_target_delivery(client):
    if client is None:
        return "UNVERIFIED", "no client log supplied (trace mode required on the rig)"
    delivered = {e: n for e, n in client.stats.items() if n >= MIN_SAMPLES}
    if not delivered:
        return "FAIL", (f"no entity reached {MIN_SAMPLES} stats samples "
                        "(trace off, no packets, or wrong target)")
    ents = ", ".join(f"entity {e} ({n} samples)" for e, n in sorted(delivered.items()))
    return "PASS", f"packet flow observed: {ents}"

def row_chat_capture(client):
    if client is None:
        return "UNVERIFIED", "no client log supplied"
    if not client.alerts:
        return "FAIL", "no alert lines captured"
    checks = sorted({a.check for a in client.alerts})
    return "PASS", (f"{len(client.alerts)} alert lines on the log surface "
                    f"(checks: {', '.join(checks)}); visual chat rendering not scored")

def row_null_gui(client):
    if client is None:
        return "UNVERIFIED", "no client log supplied"
    if not client.null_gui_windows:
        return "UNVERIFIED", "no null-gui window markers (rig wrapper must inject them)"
    in_window = [a for a in client.alerts
                 if any(b <= a.line <= e for b, e in client.null_gui_windows)]
    if not in_window:
        return "FAIL", "no alert lines inside any null-gui window"
    return "PASS", (f"{len(in_window)} alert lines inside operator-marked windows "
                    "(R9 log surface; markers are the operator's claim, "
                    "this does not prove the GUI was null)")

def row_g3_receipt(server_lines):
    if server_lines is None:
        return "UNVERIFIED", "no server log supplied"
    if not server_lines:
        return "FAIL", "no target join/receipt lines in the server log"
    return "PASS", (f"{len(server_lines)} join/receipt lines (any player; "
                    "not a per-target receipt)")

def row_g4_coverage(counts):
    if counts is None:
        return "UNVERIFIED", ("no per-arm counts supplied, or counts file invalid "
                              "(schema: one 'arm=<known> sent=<n>' per line, no duplicates)")
    short = [f"{arm}={counts.get(arm, 0)}<{want}" for arm, want in G4_MIN_COUNTS.items()
             if counts.get(arm, 0) < want]
    if short:
        return "FAIL", "coverage short: " + ", ".join(short)
    return "PASS", "per-arm counts exceed check thresholds"

ROWS = {
    "boot": ("watcher boot surface check", row_boot),
    "g2-optin": ("opt-in proof (G2)", row_g2_optin),
    "target-delivery": ("packet flow to the watcher", row_target_delivery),
    "chat-capture": ("alert log-surface capture", row_chat_capture),
    "null-gui": ("R9 log surface without GUI", row_null_gui),
    "g3-receipt": ("server receipt (G3)", row_g3_receipt),
    "g4-coverage": ("per-arm coverage counts (G4)", row_g4_coverage),
}
