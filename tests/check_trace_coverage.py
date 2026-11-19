#!/usr/bin/env python3
"""First-capture SOURCE gate for shim risks R1-R9, not runtime validation.

Checks the current Java source shape: entry trace calls, trace guard, R9 log
fields/privacy before GUI access, and required mixins. Not a Java parser or a
proof of control flow. Run test_trace_gate.py for negative/format fixtures.
"""
import glob
import json
import re
import sys

fails = []


def check(name, ok, detail=''):
    print(('PASS ' if ok else 'FAIL ') + name + (' ' + detail if detail else ''))
    if not ok:
        fails.append(name)


# Preserve quoted strings as single tokens, discard comments and whitespace.
TOKEN = re.compile(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|[A-Za-z_$][\w$]*|\S', re.S)


def tokens(text):
    return [m.group() for m in TOKEN.finditer(text)
            if not m.group().startswith(('//', '/*'))]


def body_of(text, signature):
    ts, sig = tokens(text), tokens(signature)
    for i in range(len(ts) - len(sig) + 1):
        if ts[i:i + len(sig)] == sig and ts[i + len(sig):i + len(sig) + 1] == ['{']:
            start = i + len(sig) + 1
            depth = 1
            for j in range(start, len(ts)):
                if ts[j] == '{':
                    depth += 1
                elif ts[j] == '}':
                    depth -= 1
                if depth == 0:
                    return ts[start:j]
    return []


def contains(ts, sequence):
    return any(ts[i:i + len(sequence)] == sequence for i in range(len(ts) - len(sequence) + 1))


def first_statement(ts):
    return ts[:ts.index(';') + 1] if ';' in ts else []


src = open('src/main/java/dev/lookout/core/LookoutEngine.java').read()
HANDLERS = {
    'onVelocity(int entityId)': ['entityId'],
    'onTeleport(int entityId)': ['entityId'],
    'onSwing(int entityId)': ['entityId'],
    'onDamage(int victimId, int causeId, int directId, String typeKey)': ['typeKey'],
    'onProjectileSpawn(double x, double y, double z, double speed, int projectileId, int ownerId)': ['speed'],
    'onTotemPop(int entityId)': ['entityId'],
    'onTotemEquip(int entityId)': ['entityId'],
}
for sig, fields in HANDLERS.items():
    stmt = first_statement(body_of(src, 'public void ' + sig))
    check('trace-' + sig.split('(')[0],
          stmt[:2] == ['trace', '('] and stmt[-2:] == [')', ';']
          and all(f in stmt for f in fields), 'first statement and key fields')

helper = body_of(src, 'private static void trace(String msg)')
check('trace-env-gated',
      contains(tokens(src), tokens('private static final boolean TRACE = !System.getenv().getOrDefault("MESHAC_TRACE", "").isEmpty();'))
      and helper == tokens('if (TRACE) System.out.println("[lookout-trace] " + msg);'))

alerters = sorted(glob.glob('src/*/java/dev/lookout/mc/ChatAlerter.java'))
check('R9-alerter-present', bool(alerters))
for path in alerters:
    variant = path.split('/')[1]
    text = open(path).read()
    body = body_of(text, 'public void alert(int entityId, String playerName, String checkId, String message)')
    gui = tokens('if (client.gui == null)')
    pos = next((i for i in range(len(body)) if body[i:i + len(gui)] == gui), -1)
    prefix = body[:pos] if pos >= 0 else []
    check('R9-alert-logged-before-gui-' + variant,
          pos >= 0 and contains(prefix, tokens('LOGGER.info(')))
    named = tokens('LOGGER.info("[lookout] {}: {}", name, message);')
    check('R9-named-fields-' + variant, contains(prefix, named))
    # Deliberately narrow accepted branch shape. Changes need reviewed fixtures.
    log_start = next((i for i in range(len(prefix)) if prefix[i:i + 4] == ['LOGGER', '.', 'info', '(']), -1)
    named_call = first_statement(prefix[log_start:]) if log_start >= 0 else []
    branch = tokens('if (TRACE) {') + named_call + tokens('} else { LOGGER.info("[lookout] alert entity={} check={}", entityId, checkId); }')
    check('R9-private-default-' + variant,
          contains(tokens(text), tokens('private static final boolean TRACE = !System.getenv().getOrDefault("MESHAC_TRACE", "").isEmpty();'))
          and contains(prefix, branch) and prefix.count('LOGGER') == 2)

for path in sorted(glob.glob('src/main/resources/*mixins*.json')):
    data = json.load(open(path))
    check('R8-required-' + path.split('/')[-1], data.get('required') is True)

print('SUITE:', 'all pass' if not fails else f'failures {fails}')
sys.exit(0 if not fails else 1)
