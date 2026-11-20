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
- stats line:       "[lookout-stats] entity=<id> samples=<n>" (LookoutEngine)
- null-gui window:  "[lookout-proof] null-gui-window begin|end" markers the
                    rig wrapper injects (see the runbook); the lookout code
                    itself logs nothing when the GUI is absent
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
STATS = re.compile(r"\[lookout-stats\] entity=(\d+) samples=(\d+)")
NULL_GUI = re.compile(r"\[lookout-proof\] null-gui-window (begin|end)")

@dataclass
class Alert:
    entity: int
    check: str
    trace: bool
    line: int

@dataclass
class ClientLog:
    mod_listed: bool = False
    watching: bool = False
    mixin_errors: list = field(default_factory=list)
    alerts: list = field(default_factory=list)
    stats: dict = field(default_factory=dict)          # entity -> max samples
    null_gui_windows: list = field(default_factory=list)  # (begin_line, end_line)

def parse_client_log(text):
    out = ClientLog()
    pending = None
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
            ent, n = int(m.group(1)), int(m.group(2))
            out.stats[ent] = max(out.stats.get(ent, 0), n)
        m = NULL_GUI.search(line)
        if m:
            if m.group(1) == "begin":
                pending = i
            elif pending is not None:
                out.null_gui_windows.append((pending, i))
                pending = None
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
