#!/usr/bin/env python3
"""Check packaged Fabric main entrypoint's declared interface. Boot remains separate proof."""
from pathlib import Path
import json,zipfile,subprocess,os,sys
root=Path(__file__).resolve().parents[2];jdk=Path(os.environ.get('JAVA_HOME',str(Path.home()/'jdk25')))/'bin'
mc=sys.argv[1] if len(sys.argv)>1 else '26.3'
jar=root/'build/libs'/('meshac-'+mc+'-2.9.7.28-meshac-0.1.0.jar')
with zipfile.ZipFile(jar) as z:
 d=json.loads(z.read('fabric.mod.json'))
 for kind,names in d['entrypoints'].items():
  contract={'main':'ModInitializer','server':'DedicatedServerModInitializer','client':'ClientModInitializer'}[kind]
  for name in names:
   out=subprocess.check_output([str(jdk/'javap'),'-p','-classpath',str(jar),name],text=True)
   assert 'implements net.fabricmc.api.'+contract in out,(name,kind,'Fabric entrypoint contract missing')
print('Packaged Fabric entrypoint contract PASS:',mc)
