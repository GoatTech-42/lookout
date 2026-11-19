#!/usr/bin/env python3
"""Check every @Inject target of the lookout mixin exists (name + descriptor) in the given Minecraft client jar.
usage: verify_mixin_targets.py MIXIN.java CLIENT_JAR JAVAP"""
import re, subprocess, sys
mixin, jar, javap = sys.argv[1:4]
src = open(mixin).read()
cls = re.search(r"@Mixin\((\w+)\.class\)", src).group(1)
imports = dict((m.group(2), m.group(1) + "." + m.group(2)) for m in re.finditer(r"import ([\w.]+)\.(\w+);", src))
target = imports[cls]
sig = subprocess.run([javap, "-p", "-cp", jar, target], capture_output=True, text=True, check=True).stdout
bad = 0
for m in re.finditer(r'@Inject\(method = "(\w+)".*?\n\s*private void [\w$]+\((\w+) ', src, re.S):
    name, ptype = m.group(1), m.group(2)
    full = imports[ptype]
    ok = re.search(rf"\b{name}\({re.escape(full)}\);", sig) is not None
    print(("OK   " if ok else "MISS ") + f"{target}.{name}({full})")
    bad += not ok
sf = re.search(r"@Shadow private (\w+) (\w+);", src)
if sf:
    ok = re.search(rf"{re.escape(imports[sf.group(1)])} {sf.group(2)};", sig) is not None
    print(("OK   " if ok else "MISS ") + f"field {sf.group(2)} : {imports[sf.group(1)]}"); bad += not ok
sys.exit(1 if bad else 0)
