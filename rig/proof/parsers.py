"""Parsers for lookout rig proof evidence.

Every parser takes text and returns plain data; no file or process I/O.
Recognized line formats (kept in sync with the lookout sources):

- mod list entry:   Fabric loader table line, e.g. "\t- lookout 2.9.7.28"
- watching line:    "[lookout] watching (client-side companion to meshac)"
                    (LookoutClient.onInitializeClient)
- mixin error:      any ERROR/WARN line naming dev.lookout together with
                    mixin/apply/injection vocabulary
- alert line:       "[lookout] alert entity=<id> check=<check>" or, under
                    MESHAC_TRACE, "[lookout] trace alert entity=<id> check=<check>"
                    (ChatAlerter; names never appear in logs)
- stats line:       "[lookout-stats] entity=<id> d=<f> dy=<f> ground=<b>
                    glide=<b> water=<b> vehicle=<b> exempt=<b> vel=<n>
                    tp=<n> grace=<b>" - matched with the full field layout
                    bounded to end-of-line. decimals accept dot or comma
                    (the emitter uses String.format without a Locale, so
                    the JVM default locale decides). LookoutEngine emits a
                    line every engine tick while config.teleportJumpStats
                    is on and the per-tick horizontal delta exceeds
                    teleportJumpStatsFloor - per-tick samples, not
                    discrete teleport events, and NOT gated on
                    MESHAC_TRACE - so the parser COUNTS lines per entity
- null-gui window:  "[lookout-proof] null-gui-window begin|end" markers the
                    rig wrapper injects (see the runbook); the lookout code
                    itself logs nothing when the GUI is absent
- window finding:  per log ENTRY (header + continuations, capped),
                    severity on the header + dev.lookout + mixin vocabulary
                    anywhere in the entry (multi-line forms)
"""
import re
from dataclasses import dataclass, field

WATCHING = "[lookout] watching (client-side companion to meshac)"
MOD_ENTRY = re.compile(r"^\s*-\s+lookout\s+\S")
# mixin error = severity AND package AND vocabulary, each matched
# independently and order-insensitively ("Mixin apply failed:
# dev.lookout.x" and "ERROR ... dev.lookout ... apply" both count)
_SEVERITY = re.compile(r"\b(ERROR|WARN)\b")
_PACKAGE = re.compile(r"dev\.lookout")
_VOCAB = re.compile(r"mixin|apply|inject|callback", re.I)

def is_mixin_error(line):
    return bool(_SEVERITY.search(line) and _PACKAGE.search(line) and _VOCAB.search(line))
ALERT = re.compile(r"\[lookout\] (trace )?alert entity=(\d+) check=([a-z0-9_]+)")
STATS = re.compile(r"\[lookout-stats\] entity=(\d+)"
                  r" d=\d+[.,]\d+ dy=-?\d+[.,]\d+"
                  r" ground=(?:true|false) glide=(?:true|false) water=(?:true|false)"
                  r" vehicle=(?:true|false) exempt=(?:true|false)"
                  r" vel=\d+ tp=\d+ grace=(?:true|false)\s*$")
NULL_GUI = re.compile(r"\[lookout-proof\] null-gui-window (begin|end)")

# --- window detector (multi-line mixin error forms) -------------------
# spec: docs/lead/mixin-window-detector-design.md. an entry is a header
# line plus its continuation lines (no header prefix), up to the next
# header, EOF, or a cap of MAX_CONTINUATION continuation lines. window
# findings are per entry; the same-line scan above runs over ALL lines
# independently and is never bounded by the cap.
MAX_CONTINUATION = 30
# line indices in parsed data (mixin_errors, window_findings,
# truncated_error_entries, alerts, null_gui_windows) are 0-based; the
# CLI/row details render them 1-based for human review
_HEADER_LAUNCHER = re.compile(r"^\[\d{1,2}:\d{2}:\d{2}\] \[([^]/]+)/([A-Z]+)\]:")
_HEADER_LOG4J = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?Z (.+?) ([A-Z]+) ")
def _header_severity(line):
    # any bare-word LEVEL opens an entry (unknown levels included, so a
    # FATAL line resets the window instead of merging into the open
    # entry); the ERROR/WARN gate in flush_entry decides what scores
    m = _HEADER_LAUNCHER.match(line) or _HEADER_LOG4J.match(line)
    return m.group(2) if m else None

@dataclass
class Alert:
    entity: int
    check: str
    trace: bool
    line: int

@dataclass
class WindowFinding:
    start: int            # header line index
    end: int              # last continuation line inside the window
    severity: str
    package_lines: list
    vocab_lines: list

@dataclass
class ClientLog:
    mod_listed: bool = False
    watching: bool = False
    mixin_errors: list = field(default_factory=list)
    alerts: list = field(default_factory=list)
    stats: dict = field(default_factory=dict)          # entity -> stats line count
    null_gui_windows: list = field(default_factory=list)  # (begin_line, end_line)
    window_findings: list = field(default_factory=list)   # WindowFinding, window-only (deduped)
    truncated_error_entries: list = field(default_factory=list)  # (header_line, capped_last_line)

class _Entry:
    __slots__ = ("severity", "lines", "truncated")

    def __init__(self, severity, first):
        self.severity = severity
        self.lines = [first]
        self.truncated = False


def parse_client_log(text):
    out = ClientLog()
    pending = None
    entry = None  # open _Entry, or None between entries

    def flush_entry():
        nonlocal entry
        if entry is None:
            return
        if entry.severity in ("ERROR", "WARN"):
            if entry.truncated:
                out.truncated_error_entries.append((entry.lines[0][0], entry.lines[-1][0]))
            pkg = [i for i, l in entry.lines if _PACKAGE.search(l)]
            voc = [i for i, l in entry.lines if _VOCAB.search(l)]
            if pkg and voc and not any(is_mixin_error(l) for _, l in entry.lines):
                # window-only finding: a same-line match in this entry is
                # already FAIL evidence in mixin_errors, never counted twice
                out.window_findings.append(WindowFinding(entry.lines[0][0], entry.lines[-1][0],
                                                         entry.severity, pkg, voc))
        entry = None

    for i, line in enumerate(text.splitlines()):
        if MOD_ENTRY.match(line):
            out.mod_listed = True
        if WATCHING in line:
            out.watching = True
        if is_mixin_error(line):
            out.mixin_errors.append((i, line.strip()))
        m = ALERT.search(line)
        if m:
            out.alerts.append(Alert(int(m.group(2)), m.group(3), bool(m.group(1)), i))
        m = STATS.search(line)
        if m:
            ent = int(m.group(1))
            out.stats[ent] = out.stats.get(ent, 0) + 1
        m = NULL_GUI.search(line)
        if m:
            if m.group(1) == "begin":
                pending = i
            elif pending is not None:
                out.null_gui_windows.append((pending, i))
                pending = None
        sev = _header_severity(line)
        if sev is not None:
            flush_entry()
            entry = _Entry(sev, (i, line))
        elif entry is not None and not entry.truncated:
            if len(entry.lines) <= MAX_CONTINUATION:
                entry.lines.append((i, line))
            else:
                # the entry continues past the cap: truncated. later lines
                # are orphans for the window pass only (the same-line scan
                # above still sees them)
                entry.truncated = True
        # else: orphan line (no open entry, or entry already capped):
        # no window membership
    flush_entry()
    return out

OPT_INS = ("killaura", "reach", "timer", "multiAura")

def parse_config(text):
    """config/lookout.json -> dict of the four opt-in flags (missing = False)."""
    import json
    try:
        cfg = json.loads(text)
    except json.JSONDecodeError:
        return None
    if not isinstance(cfg, dict):
        return None
    # master enabled must be exactly true; any other type reads as off
    return {"enabled": cfg.get("enabled") is True,
            **{k: cfg.get(k) is True for k in OPT_INS}}

KNOWN_ARMS = ("killaura", "reach", "multiaura", "timer")
_ARM_LINE = re.compile(r"\s*arm=(killaura|reach|multiaura|timer)\s+sent=(\d+)\s*$")

def parse_arm_counts(text):
    """Operator-supplied per-arm coverage: exactly 'arm=<known> sent=<n>'
    per non-blank line. Any corrupt line, unknown arm, or duplicate arm
    invalidates the whole file (None) - nothing is silently ignored or
    summed."""
    counts = {}
    for line in text.splitlines():
        if not line.strip():
            continue
        m = _ARM_LINE.match(line)
        if not m:
            return None
        arm = m.group(1)
        if arm in counts:
            return None
        counts[arm] = int(m.group(2))
    return counts

SERVER_RECEIPT = re.compile(r"joined the game|logged in with entity id")

def parse_server_log(text):
    return [line.strip() for line in text.splitlines() if SERVER_RECEIPT.search(line)]
