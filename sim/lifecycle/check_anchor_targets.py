#!/usr/bin/env python3
"""Characterize native boundaries our mixins target. Registration checked in each built jar separately."""
from pathlib import Path
import subprocess,re,json,zipfile,sys
r=Path(__file__).resolve().parents[2];jdk=Path.home()/'jdk25/bin';cache=Path.home()/'.gradle-meshac/caches/fabric-loom/minecraftMaven/net/minecraft';outdir=r/'docs/build-evidence/join-anchor'
for v in ['1.21.11','26.1.2','26.2','26.3']:
 jars=list((cache/'minecraft-merged').glob(v+'*/*.jar')) if v=='1.21.11' else list((cache/'minecraft-merged-deobf'/v).glob('*.jar'))
 assert len(jars)==1,(v,jars)
 texts={}
 for cls in ['Connection','PacketEncoder']:
  text=subprocess.check_output([str(jdk/'javap'),'-p','-c','-s','-classpath',str(jars[0]),'net.minecraft.network.'+cls],text=True);texts[cls]=text
  (outdir/(v+'-'+cls+'.txt')).write_text(text)
 c=texts['Connection'];e=texts['PacketEncoder']
 method=re.search(r'private void doSendPacket\(.*?(?=\n  (?:public|private|protected))',c,re.S).group()
 assert 'descriptor: (Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V' in method
 assert method.count(': return')==1 and 'io/netty/channel/Channel.write' in method
 assert 'private io.netty.channel.Channel channel;' in c
 assert 'private final net.minecraft.network.ProtocolInfo<T> protocolInfo;' in e
 send=re.search(r'private void sendPacket\(.*?(?=\n  (?:public|private|protected))',c,re.S).group()
 assert 'io/netty/channel/EventLoop.inEventLoop' in send and 'io/netty/channel/EventLoop.execute' in send and 'doSendPacket' in send
 # Pin the native caller overload too: publication only hooks common send(Packet), not send(Packet, listener).
 playerlist=subprocess.check_output([str(jdk/'javap'),'-p','-c','-classpath',str(jars[0]),'net.minecraft.server.players.PlayerList'],text=True)
 login_start=playerlist.index('class net/minecraft/network/protocol/game/ClientboundLoginPacket')
 after_login=playerlist[login_start:]
 first_send=re.search(r'// Method ([^\n]*\.send:[^\n]+)',after_login)
 assert first_send and first_send.group(1)=='net/minecraft/server/network/ServerGamePacketListenerImpl.send:(Lnet/minecraft/network/protocol/Packet;)V',(v,'initial login no longer reaches early-publication overload',first_send.group(1) if first_send else 'no send')
 game=subprocess.check_output([str(jdk/'javap'),'-p','-c','-classpath',str(jars[0]),'net.minecraft.server.network.ServerGamePacketListenerImpl'],text=True)
 constructor=game[:game.index('public void tick()')]
 assert 'Field net/minecraft/server/level/ServerPlayer.connection:' in constructor,(v,'player.connection constructor initialization moved')
 receipt=after_login[:first_send.end()]+'\n\nNative game listener constructor assigns ServerPlayer.connection before PlayerList login send.\n'
 (outdir/(v+'-login-overload.txt')).write_text(receipt)
 print('Native anchor descriptor/accessors, event-loop handoff, initial login one-arg send path PASS:',v)
# Mixin metadata is executable integration configuration. Check source now; built jar reads below.
needed={'ConnectionMixin','ConnectionAccessor','PacketEncoderAccessor'}
assert needed <= set(json.loads((r/'src/main/resources/meshac.mixins.json').read_text())['mixins'])
for version in sys.argv[1:]:
 jar=r/'build/libs'/('meshac-'+version+'-2.9.7.28-meshac-0.1.0.jar')
 with zipfile.ZipFile(jar) as z:
  cfg=json.loads(z.read('meshac.mixins.json'));assert needed <= set(cfg['mixins']);assert cfg['injectors']['defaultRequire']==1
  for name in needed:assert 'dev/meshac/mixin/'+name+'.class' in z.namelist()
 print('Packaged required anchor mixins/classes PASS:',version)
print('These native boundary and packaged registration checks do not prove Mixin weaving or runtime packet delivery.')
