#!/usr/bin/env python3
"""Remove each real dispatcher guard in an isolated tree and require observable handler failure."""
from pathlib import Path
import tempfile,shutil,subprocess,sys
root=Path(__file__).resolve().parents[2]
guard='if (dev.meshac.check.ContextGate.isGated(check.getClass().getSimpleName(), data)) continue; '
with tempfile.TemporaryDirectory(prefix='meshac-dispatch-mutation-') as tmp:
 t=Path(tmp);shutil.copytree(root/'src',t/'src');shutil.copytree(root/'sim',t/'sim')
 for route,path,occurrence in [('inbound','packet/PacketDispatcher.java',1),('event','packet/PacketDispatcher.java',0),('outbound','packet/OutboundDispatcher.java',0),('move','event/MoveEventDispatcher.java',0)]:
  p=t/'src/main/java/dev/meshac'/path; original=p.read_text();parts=original.split(guard);assert len(parts)>occurrence+1
  start=-1
  for _ in range(occurrence+1):start=original.index(guard,start+1)
  p.write_text(original[:start]+original[start+len(guard):])
  r=subprocess.run([sys.executable,str(t/'sim/dispatch/run_dispatch_safety.py')],capture_output=True,text=True)
  p.write_text(original)
  assert r.returncode!=0 and 'FAIL gated check evaluated' in r.stderr,(route,r.stderr)
  print('REJECTED missing production guard:',route)
 print('Dispatcher mutation checks PASS')
