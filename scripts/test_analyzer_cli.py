#!/usr/bin/env python3
"""Catch a check mode that writes stale output, returns success on stale data, or requires Git."""
from pathlib import Path
import subprocess,sys,tempfile,shutil,os
root=Path(__file__).resolve().parents[1]
with tempfile.TemporaryDirectory() as d:
 t=Path(d); (t/'scripts').mkdir(); (t/'src/main/java/dev/meshac/check/impl').mkdir(parents=True)
 shutil.copy(root/'scripts/analyze_deps.py',t/'scripts/analyze_deps.py')
 (t/'src/main/java/dev/meshac/check/impl/Fake.java').write_text('class Fake {}')
 target=t/'src/main/java/dev/meshac/check/CheckDependencies.java'; target.write_text('stale-fixture')
 cmd=[sys.executable,str(t/'scripts/analyze_deps.py')]
 stale=subprocess.run(cmd+['--check'],capture_output=True,text=True)
 assert stale.returncode!=0,'stale --check falsely passed'
 assert target.read_text()=='stale-fixture','--check changed stale evidence'
 subprocess.run(cmd,check=True)
 before=target.read_bytes()
 fresh=subprocess.run(cmd+['--check'],capture_output=True,text=True,env={**os.environ,'PATH':''})
 assert fresh.returncode==0, fresh.stderr
 assert target.read_bytes()==before,'--check rewrote fresh evidence'
 target.unlink()
 missing=subprocess.run(cmd+['--check'],capture_output=True,text=True)
 assert missing.returncode!=0 and not target.exists(),'missing metadata falsely passed or was silently created'
 print('test_analyzer_cli: PASS (stale/missing fail, check does not write, no Git required)')
