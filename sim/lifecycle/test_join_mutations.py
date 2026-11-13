#!/usr/bin/env python3
from pathlib import Path
import tempfile,shutil,subprocess,sys
root=Path(__file__).resolve().parents[2]
mutations=[('uuid','data.setUniqueId(player.getUUID());',''),('name','data.setPlayerName(player.getName().getString());',''),('copy','data.setPlayer(replacement);','{}'),('stale-copy','data != null && data.getPlayer() == previous','data != null')]
with tempfile.TemporaryDirectory(prefix='meshac-session-mutation-') as d:
 t=Path(d);shutil.copytree(root/'src',t/'src');shutil.copytree(root/'sim',t/'sim');p=t/'src/main/java/dev/meshac/data/PlayerDataManager.java';original=p.read_text()
 for name,old,new in mutations:
  assert original.count(old)==1
  p.write_text(original.replace(old,new));r=subprocess.run([sys.executable,str(t/'sim/lifecycle/run_join_identity.py')],text=True,capture_output=True)
  assert r.returncode!=0 and 'AssertionError' in r.stderr,(name,r.stderr)
  p.write_text(original);print('REJECTED:',name)
print('Session identity mutation checks PASS')
