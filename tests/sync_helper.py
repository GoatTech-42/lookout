"""Format sync checks shared by test_rig_proof.py and its mutant probes.

stats is bound to its emission statement: the source is comment-stripped
string-aware, split into semicolon-terminated statements string-aware,
and the single statement containing the stats literal must BE a
System.out.println call (an unused assignment, a comment, or any other
dead site keeping the concatenation is rejected). Its field order must
equal the canonical layout and a rendered canonical line must match the
parser's STATS regex.

watching and alert are EXISTENCE PINS only: they fail when the exact
literal vanishes from the source file (or the parser no longer matches a
rendered line), but they do not bind the emission statement the way
stats does.

Returns the list of failed check names; empty means in sync.
"""
import re

_STATS_FIELDS = ["entity", "d", "dy", "ground", "glide", "water",
                 "vehicle", "exempt", "vel", "tp", "grace"]
_STATS_LINE = ("[lookout-stats] entity=7 d=1.20 dy=0.42 ground=true glide=false"
               " water=false vehicle=false exempt=false vel=100 tp=200 grace=false")


def _strip_comments(src):
    """Remove // and /* */ comments without touching string/char literals."""
    out = []
    i, n = 0, len(src)
    in_str = in_chr = esc = in_line = in_block = False
    while i < n:
        ch = src[i]
        nxt = src[i + 1] if i + 1 < n else ""
        if in_line:
            if ch == "\n":
                in_line = False
                out.append(ch)
            i += 1
            continue
        if in_block:
            if ch == "*" and nxt == "/":
                in_block = False
                i += 2
            else:
                i += 1
            continue
        if in_str or in_chr:
            out.append(ch)
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif in_str and ch == '"':
                in_str = False
            elif in_chr and ch == "'":
                in_chr = False
            i += 1
            continue
        if ch == "/" and nxt == "/":
            in_line = True
            i += 2
            continue
        if ch == "/" and nxt == "*":
            in_block = True
            i += 2
            continue
        if ch == '"':
            in_str = True
        elif ch == "'":
            in_chr = True
        out.append(ch)
        i += 1
    return "".join(out)


def _live_statements(src):
    """Semicolon-terminated statements of comment-stripped source,
    string/char aware, split only at paren-depth 0."""
    stmts, buf = [], []
    in_str = in_chr = esc = False
    paren = 0
    for ch in _strip_comments(src):
        buf.append(ch)
        if esc:
            esc = False
            continue
        if in_str:
            if ch == "\\":
                esc = True
            elif ch == '"':
                in_str = False
            continue
        if in_chr:
            if ch == "\\":
                esc = True
            elif ch == "'":
                in_chr = False
            continue
        if ch == '"':
            in_str = True
        elif ch == "'":
            in_chr = True
        elif ch == "(":
            paren += 1
        elif ch == ")":
            paren -= 1
        elif ch == ";" and paren == 0:
            stmts.append("".join(buf))
            buf = []
    return stmts


def _println_arg_span(stmt, start):
    """(open, close) indices of the System.out.println argument span
    starting at start, string/char aware; None if unbalanced."""
    open_i = stmt.find("(", start)
    if open_i < 0:
        return None
    depth = 0
    in_str = in_chr = esc = False
    for i in range(open_i, len(stmt)):
        ch = stmt[i]
        if esc:
            esc = False
            continue
        if in_str:
            if ch == "\\":
                esc = True
            elif ch == '"':
                in_str = False
            continue
        if in_chr:
            if ch == "\\":
                esc = True
            elif ch == "'":
                in_chr = False
            continue
        if ch == '"':
            in_str = True
        elif ch == "'":
            in_chr = True
        elif ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0:
                return (open_i, i)
    return None


def _stats_emitter_fields(engine_src):
    """Field order of the single live stats println emission, or None.

    The statement containing the stats literal must contain a
    System.out.println call whose own argument span holds the literal;
    an assignment, comment, or other dead site is rejected."""
    hits = [s for s in _live_statements(engine_src)
            if '"[lookout-stats] entity="' in s]
    if len(hits) != 1:
        return None
    stmt = hits[0]
    lit_i = stmt.find('"[lookout-stats] entity="')
    for m in re.finditer(r"System\.out\.println\(", stmt):
        span = _println_arg_span(stmt, m.start())
        if span and span[0] < lit_i < span[1]:
            return re.findall(r'([a-z]+)="', stmt[span[0]:span[1] + 1])
    return None


def sync_findings(root, pp):
    bad = []
    watch_src = (root / "src/main/java/dev/lookout/LookoutClient.java").read_text()
    if ('"[lookout] watching (client-side companion to meshac)"' not in watch_src
            or pp.WATCHING != "[lookout] watching (client-side companion to meshac)"):
        bad.append("watching")
    eng = (root / "src/main/java/dev/lookout/core/LookoutEngine.java").read_text()
    if _stats_emitter_fields(eng) != _STATS_FIELDS or not pp.STATS.search(_STATS_LINE):
        bad.append("stats")
    for key, path in (("alert-main", "src/main/java/dev/lookout/mc/ChatAlerter.java"),
                      ("alert-1.21.11", "src/mc1.21.11/java/dev/lookout/mc/ChatAlerter.java"),
                      ("alert-26.1.2", "src/mc26.1.2/java/dev/lookout/mc/ChatAlerter.java")):
        src = (root / path).read_text()
        for literal, trace in (("[lookout] alert entity={} check={}", False),
                               ("[lookout] trace alert entity={} check={}", True)):
            if '"' + literal + '"' not in src:
                bad.append(key)
                continue
            rendered = literal.replace("{}", "7", 1).replace("{}", "killaura_facing", 1)
            m = pp.ALERT.search(rendered)
            if not (m and m.group(2) == "7" and m.group(3) == "killaura_facing"
                    and bool(m.group(1)) == trace):
                bad.append(key)
    return bad
