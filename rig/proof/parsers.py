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

envelope rules (the log surface is unauthenticated plain text):
- chat-routed lines (payload starts "[System] [CHAT] " or "[CHAT] ")
  are NEVER scanned for markers or same-line mixin errors: chat
  participants can echo any marker format
- a marker-shaped line (watching/alert/stats/null-gui only) with no
  recognizable header, or whose payload itself starts with a fully
  recognized header (nested), is AMBIGUOUS: recorded in
  ambiguous_markers, counted in no marker collection, and the affected
  scorer row goes UNVERIFIED. a payload starting with a MALFORMED
  header is not detected as nested (known gap, pinned in the envelope
  corpus)
- the same-line mixin scan is not a marker scan: it keeps counting
  headerless and nested lines (stack-trace continuations carry the real
  evidence) and excludes only chat-routed lines
- this filtering does not authenticate anything: a non-chat writer can
  still echo markers
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
# chat-routed payload: real client logs show "[System] [CHAT] ...";
# server-style captures show "[CHAT] <name> ..."
_CHAT_PAYLOAD = re.compile(r"^\[(?:System|CHAT)\] \[CHAT\] |^\[CHAT\] ")
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
    ambiguous_markers: list = field(default_factory=list)  # (line, kind) marker-shaped but unclassifiable envelope

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
        hm = _HEADER_LAUNCHER.match(line) or _HEADER_LOG4J.match(line)
        payload = line[hm.end():].lstrip() if hm else None
        chat_routed = bool(payload and _CHAT_PAYLOAD.match(payload))
        nested = bool(payload and (_HEADER_LAUNCHER.match(payload)
                                   or _HEADER_LOG4J.match(payload)))

        def marker(kind, matched):
            # chat-routed content is never counted (echo-able); a marker
            # without a clean envelope is ambiguous, never counted
            if not matched or chat_routed:
                return False
            if hm is None or nested:
                out.ambiguous_markers.append((i, kind))
                return False
            return True

        if MOD_ENTRY.match(line):
            out.mod_listed = True
        if marker("watching", WATCHING in line):
            out.watching = True
        if is_mixin_error(line) and not chat_routed:
            out.mixin_errors.append((i, line.strip()))
        m = ALERT.search(line)
        if marker("alert", m):
            out.alerts.append(Alert(int(m.group(2)), m.group(3), bool(m.group(1)), i))
        m = STATS.search(line)
        if marker("stats", m):
            ent = int(m.group(1))
            out.stats[ent] = out.stats.get(ent, 0) + 1
        m = NULL_GUI.search(line)
        if marker("null-gui", m):
            if m.group(1) == "begin":
                pending = i
            elif pending is not None:
                out.null_gui_windows.append((pending, i))
                pending = None
        sev = hm.group(2) if hm else None
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

# server receipts, anchored at payload start after a recognized header.
# observed real forms in the committed 26.3 server corpus:
#   join:  System chat: <handle> joined the game
#   login: <handle>[/<ip:port>] logged in with entity id <n> at (...)
# player chat logs as "<handle> ...": the join form requires the
# payload to start with "System chat: ", and the login form requires
# the verified name + bracketed-address shape (\w{3,16}[/...]), which
# a "<handle>" chat prefix cannot satisfy, so a player cannot echo a
# receipt line (bounded claim: the observed corpus formats). a
# headerless receipt-shaped line is not counted: no ambiguity
# machinery on the server side, just excluded.
_SERVER_JOIN = re.compile(r"^System chat: .+ joined the game\s*$")
_SERVER_LOGIN = re.compile(r"^\w{3,16}\[/[^\]]+\] logged in with entity id \d+ at \(")

def parse_server_log(text):
    out = []
    for line in text.splitlines():
        hm = _HEADER_LAUNCHER.match(line)
        if hm is None:
            hm = _HEADER_LOG4J.match(line)
        if hm is None:
            continue
        payload = line[hm.end():].lstrip()
        if _SERVER_JOIN.match(payload) or _SERVER_LOGIN.match(payload):
            out.append(line.strip())
    return out
