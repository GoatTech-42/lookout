#!/usr/bin/env python3
"""Source gate only: target-independent lookout compatibility template + expansion.

No jar or Gradle execution. Does not prove generated resource bytes or client boot.
"""
import json
from pathlib import Path
import re
import sys


def check(root):
    failures = []
    resource = json.loads((root / 'src/main/resources/lookout.mixins.json').read_text())
    if resource.get('compatibilityLevel') != 'JAVA_${java}':
        failures.append('lookout-java-template')
    for name in ('build.gradle', 'build-remap.gradle'):
        text = (root / name).read_text()
        # Current route shape is intentionally narrow, not a Groovy parser.
        text = re.sub(r'//[^\n]*|/\*.*?\*/', '', text, flags=re.S)
        pattern = r'filesMatching\(\["meshac\.mixins\.json",\s*"lookout\.mixins\.json"\]\)\s*\{\s*expand "java": project\.java_min\s*\}'
        start = text.find('processResources {')
        end = text.find('tasks.withType', start)
        body = text[start:end] if start >= 0 and end >= 0 else ''
        if not re.search(pattern, body):
            failures.append('lookout-java-expand-' + name)
    return failures


if __name__ == '__main__':
    failures = check(Path(__file__).resolve().parents[1])
    for name in failures:
        print('FAIL ' + name)
    print('SUITE:', 'all pass (3 source checks)' if not failures else repr(failures))
    sys.exit(bool(failures))
