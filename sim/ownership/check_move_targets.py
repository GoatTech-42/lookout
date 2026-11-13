#!/usr/bin/env python3
"""Assert native admitted-ingress and replay capture boundaries in all four cached native jars."""
from pathlib import Path
import subprocess,re,json,hashlib,os
r=Path(__file__).resolve().parents[2];jdk=Path(os.environ.get('JAVA_HOME',str(Path.home()/'jdk25')))/'bin';cache=Path.home()/'.gradle-meshac/caches/fabric-loom/minecraftMaven/net/minecraft';out=r/'docs/build-evidence/move-ingress/native';out.mkdir(parents=True,exist_ok=True);manifest={}
for v in ['1.21.11','26.1.2','26.2','26.3']:
 jars=list((cache/'minecraft-merged').glob(v+'*/*.jar')) if v=='1.21.11' else list((cache/'minecraft-merged-deobf'/v).glob('*.jar'))
 assert len(jars)==1,(v,jars)
 texts={}
 for cls in ['net.minecraft.network.Connection','net.minecraft.server.network.ServerGamePacketListenerImpl','net.minecraft.network.protocol.PacketUtils','net.minecraft.network.PacketProcessor$ListenerAndPacket']:
  text=subprocess.check_output([str(jdk/'javap'),'-p','-c','-s','-classpath',str(jars[0]),cls],text=True);texts[cls]=text
 c=texts['net.minecraft.network.Connection'];body=re.search(r'protected void channelRead0\(io.netty.channel.ChannelHandlerContext, net.minecraft.network.protocol.Packet<\?>\);.*?(?=\n  (?:public|private|protected))',c,re.S).group()
 assert 'descriptor: (Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V' in body
 assert body.count('// Method genericsFtw:')==1
 assert body.index('Channel.isOpen')<body.index('PacketListener.shouldHandleMessage')<body.index('// Method genericsFtw:')
 game=texts['net.minecraft.server.network.ServerGamePacketListenerImpl'];move=re.search(r'public void handleMovePlayer\(.*?(?=\n  (?:public|private|protected))',game,re.S).group()
 handoff='net/minecraft/network/protocol/PacketUtils.ensureRunningOnSameThread:(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V'
 assert move.count(handoff)==1 and ': return' not in move[:move.index(handoff)]
 utils=texts['net.minecraft.network.protocol.PacketUtils'];processor=texts['net.minecraft.network.PacketProcessor$ListenerAndPacket']
 assert 'RunningOnDifferentThreadException' in utils and 'scheduleIfPossible' in utils
 assert 'protocol/Packet.handle:' in processor
 receipt=body+'\n'+move[:move.index(handoff)+len(handoff)]+'\n\n'+utils+'\n\n'+processor
 (out/(v+'.txt')).write_text(receipt);manifest[v]={'jar':str(jars[0]),'jarSHA256':hashlib.sha256(jars[0].read_bytes()).hexdigest(),'receiptSHA256':hashlib.sha256(receipt.encode()).hexdigest()}
 print(v,'native movement channel admission/replay capture target PASS')
(out/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
