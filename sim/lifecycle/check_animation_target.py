#!/usr/bin/env python3
"""Native target existence for the per-version animation hook; not a weave proof."""
from pathlib import Path
import subprocess,re
root=Path(__file__).resolve().parents[2];cache=Path.home()/'.gradle-meshac/caches/fabric-loom/minecraftMaven/net/minecraft'
for v in ['1.21.11','26.1.2','26.2','26.3']:
 s=(root/('src/mc'+v+'/java/dev/meshac/mixin/AnimationMixin.java')).read_text()
 method=re.search(r'@Inject\(method\s*=\s*"([^"]+)"',s).group(1)
 packet=re.search(r'import net.minecraft.network.protocol.game.(Serverbound\w+Packet);',s).group(1)
 jars=list((cache/'minecraft-merged').glob(v+'*/*.jar')) if v=='1.21.11' else list((cache/'minecraft-merged-deobf'/v).glob('*.jar'))
 out=subprocess.check_output([str(Path.home()/'jdk25/bin/javap'),'-p','-classpath',str(jars[0]),'net.minecraft.server.network.ServerGamePacketListenerImpl'],text=True)
 assert ' '+method+'(net.minecraft.network.protocol.game.'+packet+');' in out,(v,method,packet,'target missing')
 print('Native animation hook PASS:',v,method,packet)
