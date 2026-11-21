"""Row scoring for the lookout rig proof harness.

Verdicts: PASS, FAIL, or UNVERIFIED (missing inputs or insufficient
coverage; never pass-by-absence). Each row is a function over parsed
evidence returning (verdict, detail). Rows map to the compatibility
matrix's rig-gated cells; PASS here is still only log-level proof - see
the runbook for what each row does not prove.
"""
from rig.proof import parsers

MIN_STATS_LINES = 5      # stats lines per entity for target delivery
G4_MIN_COUNTS = {"killaura": 6, "reach": 6, "multiaura": 3, "timer": 6}

def _ambiguous(client, kind):
    # marker-shaped lines whose envelope could not be classified
    # (bare, nested or malformed header); never counted, never silent
    hits = [i + 1 for i, k in client.ambiguous_markers if k == kind]
    if hits:
        return ("UNVERIFIED", f"{len(hits)} {kind}-marker lines with "
                f"unclassified envelope (lines {', '.join(map(str, hits))}); "
                "not counted")
    return None


def row_boot(client):
    # precedence per row, checked across all rows: definitive FAIL
    # evidence (real mixin error lines - boot is the only row with any)
    # is never masked by ambiguity; ambiguity in turn blocks
    # absence-of-evidence FAILs and the PASS
    if client is None:
        return "UNVERIFIED", "no client log supplied"
    # parsed indices are 0-based; details render 1-based for human review
    review = ([f"lines {f.start + 1}-{f.end + 1} ({f.severity} entry)" for f in client.window_findings]
              + [f"lines {s + 1}-{e + 1}+ (entry truncated at "
                 f"{parsers.MAX_CONTINUATION} continuations; omitted content not qualified)"
                 for s, e in client.truncated_error_entries])
    if client.mixin_errors:
        detail = f"{len(client.mixin_errors)} dev.lookout mixin error lines"
        if review:
            detail += "; plus unreviewed window candidates: " + "; ".join(review)
        return "FAIL", detail
    amb = _ambiguous(client, "watching")
    if amb:
        return amb
    problems = []
    if not client.mod_listed:
        problems.append("lookout not in the loader mod list")
    if not client.watching:
        problems.append("watching line absent")
    if problems:
        detail = "; ".join(problems)
        if review:
            detail += "; plus unreviewed window candidates: " + "; ".join(review)
        return "FAIL", detail
    if review:
        return "UNVERIFIED", ("mixin error window candidates need human block review: "
                              + "; ".join(review)
                              + " (see docs/lead/mixin-window-detector-design.md)")
    return "PASS", ("mod listed, watching line present, no same-line or in-entry "
                    "mixin error match, no truncated error entries "
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
    amb = _ambiguous(client, "stats")
    if amb:
        return amb
    delivered = {e: n for e, n in client.stats.items() if n >= MIN_STATS_LINES}
    if not delivered:
        return "FAIL", (f"no entity reached {MIN_STATS_LINES} stats lines "
                        "(teleportJumpStats off in the config, per-tick "
                        "horizontal deltas never over the configured floor, "
                        "or no packet flow)")
    ents = ", ".join(f"entity {e} ({n} lines)" for e, n in sorted(delivered.items()))
    return "PASS", (f"stats lines observed: {ents}; narrow sampled-stats "
                    "smoke heuristic, not intended-target or general "
                    "packet delivery proof")

def row_chat_capture(client):
    if client is None:
        return "UNVERIFIED", "no client log supplied"
    amb = _ambiguous(client, "alert")
    if amb:
        return amb
    if not client.alerts:
        return "FAIL", "no alert lines captured"
    checks = sorted({a.check for a in client.alerts})
    return "PASS", (f"{len(client.alerts)} alert lines on the log surface "
                    f"(checks: {', '.join(checks)}); visual chat rendering not scored")

def row_null_gui(client):
    if client is None:
        return "UNVERIFIED", "no client log supplied"
    amb = _ambiguous(client, "null-gui")
    if amb:
        return amb
    if not client.null_gui_windows:
        return "UNVERIFIED", "no null-gui window markers (rig wrapper must inject them)"
    # the row's positive evidence is in-window alerts; an ambiguous
    # alert line inside a valid window is unverifiable evidence
    amb_alerts = [i + 1 for i, k in client.ambiguous_markers
                  if k == "alert"
                  and any(b <= i <= e for b, e in client.null_gui_windows)]
    if amb_alerts:
        return ("UNVERIFIED", f"ambiguous alert lines inside a null-gui window "
                f"(lines {', '.join(map(str, amb_alerts))}); not counted")
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
