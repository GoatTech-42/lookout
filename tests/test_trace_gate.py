#!/usr/bin/env python3
"""Mutation checks for the source gate, not Minecraft/runtime validation."""
import argparse
import pathlib
import shutil
import subprocess
import sys
import tempfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
ENG = 'src/main/java/dev/lookout/core/LookoutEngine.java'
AL = 'src/main/java/dev/lookout/mc/ChatAlerter.java'
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--gate', type=pathlib.Path, default=ROOT / 'tests/check_trace_coverage.py')
GATE = parser.parse_args().gate.resolve()
RESULTS = []


def run(root):
    p = subprocess.run([sys.executable, str(GATE)], cwd=root, text=True, capture_output=True)
    failures = {line.split()[1] for line in p.stdout.splitlines() if line.startswith('FAIL ')}
    assert p.returncode in (0, 1), p.stderr
    assert (p.returncode == 1) == bool(failures), (p.returncode, p.stdout, p.stderr)
    return failures


def replace(root, path, old, new):
    path = root / path
    text = path.read_text()
    assert text.count(old) == 1, (path, old)
    path.write_text(text.replace(old, new))


def exercise(name, edit, expected):
    with tempfile.TemporaryDirectory() as d:
        root = pathlib.Path(d)
        shutil.copytree(ROOT / 'src', root / 'src')
        edit(root)
        actual = run(root)
    print('PASS' if actual == expected else 'FAIL', name, 'expected=', sorted(expected), 'actual=', sorted(actual))
    RESULTS.append(actual == expected)


def second_variant(root):
    target = root / 'src/mc1.21.11/java/dev/lookout/mc/ChatAlerter.java'
    target.parent.mkdir(parents=True, exist_ok=True)
    text = (root / AL).read_text()
    start = text.index('        if (TRACE) {')
    end = text.index('        Minecraft client', start)
    target.write_text(text[:start] + text[end:])


def main():
    exercise('baseline', lambda r: None, set())
    exercise('M1-engine-trace-always-on', lambda r: replace(r, ENG, 'if (TRACE)', 'if (true)'), {'trace-env-gated'})
    exercise('M2-r9-constant-arguments', lambda r: replace(r, AL,
             'LOGGER.info("[lookout] trace alert entity={} check={}", entityId, checkId);', 'LOGGER.info("alert");'),
             {'R9-trace-id-fields-main'})
    exercise('M3-trace-only-when-disabled', lambda r: replace(r, ENG,
             'trace("onSwing id=" + entityId);\n        if (!config.enabled) return;',
             'if (!config.enabled) { trace("onSwing id=" + entityId); return; }'), {'trace-onSwing'})
    exercise('F-A-silent-second-variant', second_variant,
             {'R9-alert-logged-before-gui-mc1.21.11', 'R9-trace-id-fields-mc1.21.11', 'R9-private-default-mc1.21.11'})
    exercise('F-B-named-default', lambda r: replace(r, AL, 'if (TRACE)', 'if (true)'), {'R9-private-default-main'})
    exercise('F-B-private-branch-name', lambda r: replace(r, AL,
             'LOGGER.info("[lookout] alert entity={} check={}", entityId, checkId);', 'LOGGER.info("[lookout] alert entity={} check={}", entityId, name);'), {'R9-private-default-main'})
    exercise('comment-return-not-code', lambda r: replace(r, ENG,
             'trace("onSwing id=" + entityId);', '// return is only a comment\n        trace("onSwing id=" + entityId);'), set())
    exercise('multiline-trace', lambda r: replace(r, ENG,
             'trace("onSwing id=" + entityId);', 'trace(\n            "onSwing id=" +\n            entityId\n        );'), set())
    exercise('missing-handler', lambda r: replace(r, ENG, 'public void onSwing(int entityId)',
             'public void absentSwing(int entityId)'), {'trace-onSwing'})
    exercise('missing-key-field', lambda r: replace(r, ENG,
             'trace("onSwing id=" + entityId);', 'trace("onSwing id=constant");'), {'trace-onSwing'})
    print(f'SUITE: {sum(RESULTS)}/{len(RESULTS)} source-gate fixtures pass')
    sys.exit(0 if all(RESULTS) else 1)


if __name__ == '__main__':
    main()
