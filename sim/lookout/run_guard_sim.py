#!/usr/bin/env python3
"""Compile and run the lookout false-positive guard fixtures against the built core classes."""
import pathlib, subprocess, sys

root = pathlib.Path(__file__).resolve().parents[2]
classes = root / "build" / "classes" / "java" / "main"
out = root / "build" / "sim-lookout-guard"
if not classes.exists():
    sys.exit("run ./gradlew build first (build/classes/java/main missing)")
out.mkdir(parents=True, exist_ok=True)
subprocess.run(["javac", "-cp", str(classes), "-d", str(out), str(root / "sim" / "lookout" / "LookoutGuardSim.java")], check=True)
subprocess.run(["java", "-cp", f"{classes}:{out}", "LookoutGuardSim"], check=True)
