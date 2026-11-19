#!/usr/bin/env python3
"""Compile and run the lookout sim: legit movement must stay silent, cheat movement must alert."""
import pathlib, subprocess, sys

root = pathlib.Path(__file__).resolve().parents[2]
classes = root / "build" / "classes" / "java" / "main"
out = root / "build" / "sim-lookout"
if not classes.exists():
    sys.exit("run ./gradlew build first (build/classes/java/main missing)")
out.mkdir(parents=True, exist_ok=True)
subprocess.run(["javac", "-cp", str(classes), "-d", str(out), str(root / "sim" / "lookout" / "LookoutSim.java")], check=True)
subprocess.run(["java", "-cp", f"{classes}:{out}", "LookoutSim"], check=True)
