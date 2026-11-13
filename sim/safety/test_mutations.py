#!/usr/bin/env python3
"""Run each unsafe production mutation only in a temporary copy; require tests to reject it."""
from pathlib import Path
import subprocess,tempfile,shutil,sys
root=Path(__file__).resolve().parents[2]
mutations=[
 ('kick-command-guard','product/MeshCommands.java','if (simulate(c.getSource(), "kick " + t.getGameProfile().name())) return 1;',''),
 ('case-add-guard','product/Cases.java','if (dev.meshac.safety.Containment.mode() != dev.meshac.safety.Containment.Mode.ENFORCE) {','if (false) {'),
 ('case-pardon-guard','product/Cases.java','if (dev.meshac.safety.Containment.mode() != dev.meshac.safety.Containment.Mode.ENFORCE) return false;',''),
 ('discord-egress-guard','product/Discord.java','if (dev.meshac.safety.Containment.mode() != dev.meshac.safety.Containment.Mode.ENFORCE) return;',''),
 ('veil-qualification-hold','product/Config.java','dev.meshac.veil.Veil.xrayOn = false;','dev.meshac.veil.Veil.xrayOn = get().veilXray != null && get().veilXray;'),
]
with tempfile.TemporaryDirectory(prefix='meshac-mutations-') as tmp:
 t=Path(tmp); shutil.copytree(root/'src',t/'src'); shutil.copytree(root/'sim',t/'sim')
 for name,path,old,new in mutations:
  p=t/'src/main/java/dev/meshac'/path; original=p.read_text(); assert original.count(old)==1,name
  p.write_text(original.replace(old,new))
  result=subprocess.run([sys.executable,str(t/'sim/safety/run_product_safety.py')],capture_output=True,text=True)
  p.write_text(original)
  assert result.returncode!=0 and 'FAIL:' in result.stderr, name+' not caught or harness compilation failed: '+result.stderr
  print('REJECTED:',name)
 print('Product mutation checks PASS (temporary copies only)')
