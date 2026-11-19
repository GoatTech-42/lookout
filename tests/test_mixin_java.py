#!/usr/bin/env python3
"""Negative fixtures for the mixin Java source gate. No game/jar execution."""
from pathlib import Path
import shutil
import tempfile
from check_mixin_java import check

ROOT = Path(__file__).resolve().parents[1]
RESOURCE = 'src/main/resources/lookout.mixins.json'
CASES = [
    ('baseline', None, None, None, []),
    ('hardcode-25', RESOURCE, 'JAVA_${java}', 'JAVA_25', ['lookout-java-template']),
    ('omit-standard-expansion', 'build.gradle',
     '["meshac.mixins.json", "lookout.mixins.json"]', '"meshac.mixins.json"', ['lookout-java-expand-build.gradle']),
    ('omit-remap-expansion', 'build-remap.gradle',
     '["meshac.mixins.json", "lookout.mixins.json"]', '"meshac.mixins.json"', ['lookout-java-expand-build-remap.gradle']),
    ('wrong-java-source', 'build-remap.gradle',
     'filesMatching(["meshac.mixins.json", "lookout.mixins.json"]) { expand "java": project.java_min }',
     'filesMatching(["meshac.mixins.json", "lookout.mixins.json"]) { expand "java": 25 }', ['lookout-java-expand-build-remap.gradle']),
]


def main():
    for name, target, old, new, expected in CASES:
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            for path in (RESOURCE, 'build.gradle', 'build-remap.gradle'):
                p = root / path
                p.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(ROOT / path, p)
            if target:
                p = root / target
                text = p.read_text()
                assert text.count(old) == 1
                p.write_text(text.replace(old, new))
            actual = check(root)
            assert actual == expected, (name, expected, actual)
            print('PASS', name, 'exact failures=', actual)
    # Remove only the gate's template guard: hardcode mutant must then survive.
    # This meta-mutant proves the hardcode fixture detects loss of its guard.
    import inspect
    ns = {}
    source = inspect.getsource(check)
    assert source.count("resource.get('compatibilityLevel') != 'JAVA_${java}'") == 1
    source = source.replace("resource.get('compatibilityLevel') != 'JAVA_${java}'", 'False')
    exec(compile(source, '<template-guard-mutant>', 'exec'), check.__globals__, ns)
    with tempfile.TemporaryDirectory() as d:
        root = Path(d)
        for path in (RESOURCE, 'build.gradle', 'build-remap.gradle'):
            p = root / path
            p.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(ROOT / path, p)
        p = root / RESOURCE
        p.write_text(p.read_text().replace('JAVA_${java}', 'JAVA_25'))
        assert ns['check'](root) != ['lookout-java-template']
    print('PASS guard-only meta-mutant killed by hardcode fixture')
    print('SUITE: 5/5 fixtures and guard-only meta-mutant pass')


if __name__ == '__main__':
    main()
