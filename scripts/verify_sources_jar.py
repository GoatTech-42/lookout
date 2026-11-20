#!/usr/bin/env python3
"""Assert a built sources jar carries exactly one copy of each variant-overridden
source path, with bytes equal to the variant's override file, not the reference.

Usage: verify_sources_jar.py <sources-jar> <variant-dir> <path> [<path>...]
  <sources-jar>  e.g. build/libs/meshac-26.1.2-...-sources.jar
  <variant-dir>  e.g. src/mc26.1.2/java
  <path>         jar-relative source path, e.g. dev/lookout/mc/ChatAlerter.java

Exit 0 when every listed path appears exactly once in the jar and its bytes
match <variant-dir>/<path> on disk. Any deviation is a loud FAIL, never silent.
"""
import sys
import zipfile
from pathlib import Path


def main() -> int:
    if len(sys.argv) < 4:
        print(__doc__)
        return 2
    jar_path, variant_dir, paths = Path(sys.argv[1]), Path(sys.argv[2]), sys.argv[3:]
    failures = []
    with zipfile.ZipFile(jar_path) as jar:
        names = jar.namelist()
        for rel in paths:
            count = names.count(rel)
            if count != 1:
                failures.append(f"{rel}: {count} entries in sources jar, expected exactly 1")
                continue
            expected = (variant_dir / rel).read_bytes()
            actual = jar.read(rel)
            if actual != expected:
                failures.append(f"{rel}: bytes differ from the variant override in {variant_dir}")
    for failure in failures:
        print("FAIL " + failure)
    if failures:
        return 1
    for rel in paths:
        print(f"PASS {rel}: exactly one entry, bytes match {variant_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
