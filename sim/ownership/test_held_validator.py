#!/usr/bin/env python3
"""Reject missing, duplicated or reordered events in the retained native trace."""
from pathlib import Path
from verify_held_trace import verify

r = Path(__file__).resolve().parents[2] / 'docs/build-evidence/held26.3'
s, c, t = ((r / f'held-{part}.log').read_text() for part in ['server', 'client', 'runner'])
verify(s, c, t)
line = next(x for x in s.splitlines(True) if '[HELD-OBSERVER] DISPATCH packet=1 ' in x)
checks = [
    ('duplicate-dispatch', s.replace(line, line + line), c, t),
    ('missing-dispatch', s.replace(line, ''), c, t),
    ('wrong-slot', s.replace('CONVERT packet=1 slot=1', 'CONVERT packet=1 slot=8'), c, t),
    ('reordered-stage', s.replace('ADMITTED_CHANNEL packet=1', 'TEMP packet=1').replace('DISPATCH packet=1', 'ADMITTED_CHANNEL packet=1').replace('TEMP packet=1', 'DISPATCH packet=1'), c, t),
    ('missing-shutdown', s.replace('Stopping the server', ''), c, t),
    ('missing-client-close', s, c.replace('Server closed', ''), t),
    ('truncated-runner', s, c, t.split('HELD FINITE NATIVE ASSERTIONS PASS')[0]),
    ('failed-first-run', *((r / 'first-run' / f'held-{x}.log').read_text() for x in ['server', 'client', 'runner'])),
]
for name, a, b, d in checks:
    try:
        verify(a, b, d)
    except AssertionError:
        print('REJECTED:', name)
    else:
        raise AssertionError('mutation survived ' + name)
print('Held native validator mutation checks PASS')
