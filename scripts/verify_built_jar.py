#!/usr/bin/env python3
"""Verify one target's packaged metadata/class version/mixin files, not runtime functionality."""
import json,sys,zipfile,struct,pathlib
mc=sys.argv[1];minimum=int(sys.argv[2]);root=pathlib.Path(__file__).resolve().parents[1]
jar=root/'build/libs'/('meshac-'+mc+'-2.9.7.28-meshac-0.1.0.jar')
with zipfile.ZipFile(jar) as z:
 m=json.loads(z.read('fabric.mod.json')); expected='26.1.x' if mc=='26.1.2' else mc
 assert m['depends']['minecraft']==expected,(mc,m['depends'])
 assert str(minimum) in m['depends']['java'],m['depends']
 for path in ['dev/meshac/compat/VersionBridge.class','dev/meshac/mixin/AnimationMixin.class','dev/meshac/check/ContextGate.class','dev/meshac/packet/PacketDispatcher.class']:
  data=z.read(path);assert struct.unpack('>H',data[6:8])[0]==minimum+44,(path,'wrong bytecode target')
 mix=json.loads(z.read('meshac.mixins.json'))
 for name in mix['mixins']:assert 'dev/meshac/mixin/'+name+'.class' in z.namelist(),name
 assert 'AnimationMixin' in mix['mixins']
 print('Packaged target metadata/class/mixin presence PASS:',mc,jar.name)
