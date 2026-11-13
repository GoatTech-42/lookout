#!/usr/bin/env python3
from pathlib import Path
import tempfile,shutil,subprocess,sys
root=Path(__file__).resolve().parents[2]
mutations=[('native-init','check/ExemptionManager.java','MeshVersion.fromName(\n\t\t\tnet.minecraft.SharedConstants.getCurrentVersion().name())','MeshVersion.UNKNOWN'),('unknown-newest','config/MeshVersion.java','this == UNKNOWN ? -1 : ordinal()','ordinal()'),('known-order','config/MeshVersion.java','this.comparisonOrder() > other.comparisonOrder()','this.comparisonOrder() < other.comparisonOrder()')]
with tempfile.TemporaryDirectory(prefix='meshac-version-mutations-') as d:
 t=Path(d);shutil.copytree(root/'src',t/'src');shutil.copytree(root/'sim',t/'sim')
 for name,path,old,new in mutations:
  p=t/'src/main/java/dev/meshac'/path;o=p.read_text();assert o.count(old)==1;p.write_text(o.replace(old,new));r=subprocess.run([sys.executable,str(t/'sim/version/run_version_contract.py')],text=True,capture_output=True);p.write_text(o);assert r.returncode!=0 and 'AssertionError' in r.stderr,(name,r.stderr);print('REJECTED:',name)
print('Version contract mutation checks PASS')
