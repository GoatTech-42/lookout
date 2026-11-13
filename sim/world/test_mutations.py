#!/usr/bin/env python3
from pathlib import Path
import tempfile,shutil,subprocess,sys
root=Path(__file__).resolve().parents[2]
mutations=[
 ('late-payload-read','''int chunkX = dev.meshac.compat.VersionBridge.chunkX(packet);''','''if (lateFixture) { lateFixture=false; afterConfirmation(() -> handleChunkPacket(level,packet)); return; }
        int chunkX = dev.meshac.compat.VersionBridge.chunkX(packet);''','FAIL:'),
 ('reset-generation','if (generation == capturedGeneration) action.run();','action.run();','FAIL:'),
 ('mutable-update-pos','java.util.Map.entry(pos.immutable(), state)','java.util.Map.entry(pos, state)','FAIL:'),
 ('unmodifiable-live-cache','new HashSet<>(positions)','Set.copyOf(positions)','UnsupportedOperationException'),
 ('unsigned-negative-z','(int) ((key << 26) >> 38)','(int) ((key >> 12) & 0x3FFFFFFL)','FAIL:'),
 ('unsigned-negative-y','(int) ((key << 52) >> 52)','(int) (key & 0xFFFL)','FAIL:'),
]
with tempfile.TemporaryDirectory(prefix='meshac-world-mutation-') as tmp:
 t=Path(tmp);shutil.copytree(root/'src',t/'src');shutil.copytree(root/'sim',t/'sim');p=t/'src/main/java/dev/meshac/tracker/WorldBlockTracker.java';original=p.read_text()
 for name,old,new,want in mutations:
  assert original.count(old)==1,(name,original.count(old));changed=original.replace(old,new)
  if name=='late-payload-read':changed=changed.replace('public class WorldBlockTracker {','public class WorldBlockTracker { private boolean lateFixture=true;')
  p.write_text(changed)
  r=subprocess.run([sys.executable,str(t/'sim/world/run_world_snapshot.py')],capture_output=True,text=True);p.write_text(original)
  assert r.returncode!=0 and want in r.stderr,(name,r.stderr)
  print('REJECTED:',name)
 print('World snapshot mutation checks PASS')
