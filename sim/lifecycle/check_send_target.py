#!/usr/bin/env python3
"""Check native declaration of the configured send mixin target. Not a weave test."""
from pathlib import Path
import subprocess,re,os
root=Path(__file__).resolve().parents[2];jdk=Path(os.environ.get('JAVA_HOME',str(Path.home()/'jdk25')))/'bin'
source=(root/'src/main/java/dev/meshac/mixin/ServerGamePacketListenerSendMixin.java').read_text()
owner=re.search(r'@Mixin\((\w+)\.class\)',source).group(1)
cache=Path.home()/'.gradle-meshac/caches/fabric-loom/minecraftMaven/net/minecraft'
for v in ['1.21.11','26.1.2','26.2','26.3']:
 jars=list((cache/'minecraft-merged').glob(v+'*/*.jar')) if v=='1.21.11' else list((cache/'minecraft-merged-deobf'/v).glob('*.jar'))
 assert len(jars)==1,(v,jars)
 out=subprocess.check_output([str(jdk/'javap'),'-p','-s','-classpath',str(jars[0]),'net.minecraft.server.network.'+owner],text=True)
 assert 'descriptor: (Lnet/minecraft/network/protocol/Packet;)V' in out,(v,owner,'send(Packet) not declared by mixin target')
 print('Native declared send target PASS:',v,owner)
