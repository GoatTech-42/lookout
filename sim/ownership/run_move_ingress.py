#!/usr/bin/env python3
"""Actual movement hooks/dispatcher, modeled native admission and thread replay, not Mixin weaving."""
from pathlib import Path
import subprocess,tempfile,os,shutil,json,hashlib
r=Path(__file__).resolve().parents[2];jdk=Path(os.environ.get('JAVA_HOME',str(Path.home()/'jdk25')))/'bin'
ctx={'__file__':str(r/'sim/lifecycle/run_join_anchor.py')}
exec((r/'sim/lifecycle/run_join_anchor.py').read_text().split("with tempfile.TemporaryDirectory(prefix='meshac-anchor-')")[0],ctx)
s=ctx['s']
def put(n,b):s[n+'.java']='package '+n.rsplit('/',1)[0].replace('/','.')+'; '+b
put('org/spongepowered/asm/mixin/injection/At','@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME) public @interface At {String value();String target() default "";Shift shift() default Shift.NONE;enum Shift{NONE,BEFORE,AFTER}}')
put('org/spongepowered/asm/mixin/injection/Inject','@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME) public @interface Inject {String method();At at();}')
put('net/minecraft/network/protocol/game/ServerboundPlayerAbilitiesPacket','public class ServerboundPlayerAbilitiesPacket {}')
put('io/netty/channel/ChannelHandlerContext','public class ChannelHandlerContext {}')
s['net/minecraft/server/network/ServerGamePacketListenerImpl.java']='''package net.minecraft.server.network;public class ServerGamePacketListenerImpl extends dev.meshac.mixin.ServerGamePacketListenerImplMixin implements net.minecraft.network.PacketListener,dev.meshac.mixin.ServerCommonPacketListenerImplAccessor {public net.minecraft.network.Connection c;public net.minecraft.network.Connection meshac$getConnection(){return c;}public net.minecraft.server.level.ServerPlayer getPlayer(){return player;}public void send(net.minecraft.network.protocol.Packet<?> p){c.send(p);}}'''
s['net/minecraft/network/protocol/game/ServerboundMovePlayerPacket.java']='package net.minecraft.network.protocol.game;public class ServerboundMovePlayerPacket implements net.minecraft.network.protocol.Packet<Object>{}'
put('net/minecraft/network/protocol/game/ServerboundClientCommandPacket','public class ServerboundClientCommandPacket {public enum Action{PERFORM_RESPAWN}public Action getAction(){return Action.PERFORM_RESPAWN;}}')
s['dev/meshac/packet/Packets.java']=s['dev/meshac/packet/Packets.java'].replace('public class Packets {','public class Packets {public static int moves;').replace('toFlying(Object p,Object pl){return','toFlying(Object p,Object pl){moves++;return')
s['net/minecraft/server/level/ServerPlayer.java']=s['net/minecraft/server/level/ServerPlayer.java'].replace('public double getX(){return 1;}','public double x=1;public double getX(){return x;}')
put('dev/meshac/event/MoveEventDispatcher','public class MoveEventDispatcher {public static int events;public static double from;public static void fireMove(net.minecraft.server.level.ServerPlayer p,double x,double y,double z){events++;from=x;}}')
test='''public class MoveIngressTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static void hooks(Class<?> cls,Object self,String method,String point,Object...args)throws Exception{
  for(var m:cls.getDeclaredMethods()){var i=m.getAnnotation(org.spongepowered.asm.mixin.injection.Inject.class);if(i==null||!i.method().split("\\\\(")[0].equals(method))continue;var a=i.at();if(a.value().equals("INVOKE")){String target=method.equals("channelRead0")?"Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V":"Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V";check(a.target().equals(target),"unexpected native target: "+a.target());}String actual=a.value().equals("INVOKE")?(a.shift()==org.spongepowered.asm.mixin.injection.At.Shift.AFTER?"AFTER_HANDOFF":"INGRESS"):a.value();if(!actual.equals(point))continue;m.setAccessible(true);m.invoke(self,args);}
 }
 static net.minecraft.server.level.ServerPlayer player(){var p=new net.minecraft.server.level.ServerPlayer();var g=new net.minecraft.server.network.ServerGamePacketListenerImpl();var c=new net.minecraft.network.Connection();p.connection=g;g.player=p;g.c=c;c.listener=g;dev.meshac.data.PlayerDataManager.onJoin(p);return p;}
 static void read(net.minecraft.network.Connection c,net.minecraft.network.protocol.game.ServerboundMovePlayerPacket packet,boolean nativeAccept,boolean tracked)throws Exception{
  // Native channel is-open and shouldHandleMessage gates precede its sole genericsFtw invocation.
  if(!c.connected||!nativeAccept)return;
  int before=dev.meshac.packet.Packets.moves;
  hooks(dev.meshac.mixin.ConnectionMixin.class,c,"channelRead0","INGRESS",null,packet,null);
  check(dev.meshac.packet.Packets.moves==before+(tracked?1:0),"movement must dispatch at admitted channel boundary before any native handler/replay");
  var g=c.listener;
  hooks(dev.meshac.mixin.ServerGamePacketListenerImplMixin.class,g,"handleMovePlayer","HEAD",packet,null);
  // First native ensureRunningOnSameThread schedules and throws; no successful TAIL on this pass.
  // Another server-side change before queued replay must be the authoritative from coordinate.
  g.player.x=8;
  hooks(dev.meshac.mixin.ServerGamePacketListenerImplMixin.class,g,"handleMovePlayer","HEAD",packet,null);
  hooks(dev.meshac.mixin.ServerGamePacketListenerImplMixin.class,g,"handleMovePlayer","AFTER_HANDOFF",packet,null);
  // A later channel packet can enter and schedule while this server body is still running.
  // Its network-side HEAD must not overwrite this invocation's authoritative from coordinate.
  g.player.x=10;
  hooks(dev.meshac.mixin.ServerGamePacketListenerImplMixin.class,g,"handleMovePlayer","HEAD",packet,null);
  g.player.x=9;
  hooks(dev.meshac.mixin.ServerGamePacketListenerImplMixin.class,g,"handleMovePlayer","TAIL",packet,null);
 }
 public static void main(String[]args)throws Exception{
  var p=player();var c=p.connection.c;var packet=new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket();read(c,packet,true,true);
  check(dev.meshac.packet.Packets.moves==1,"one admitted network move reached real PacketDispatcher "+dev.meshac.packet.Packets.moves+" times");check(dev.meshac.event.MoveEventDispatcher.events==1&&dev.meshac.event.MoveEventDispatcher.from==8,"authoritative move event must capture successful server replay from coordinate once");
  read(c,packet,false,false);check(dev.meshac.packet.Packets.moves==1,"native rejected message dispatched");c.connected=false;read(c,packet,true,false);check(dev.meshac.packet.Packets.moves==1,"closed channel dispatched");
  var q=player();var old=q.connection;var replacement=new net.minecraft.server.network.ServerGamePacketListenerImpl();replacement.player=q;replacement.c=old.c;q.connection=replacement;old.c.listener=old;read(old.c,packet,true,false);check(dev.meshac.packet.Packets.moves==1,"stale player listener ownership dispatched movement");
  var phase=player();phase.connection.c.ch.encoder.play=false;read(phase.connection.c,packet,true,false);check(dev.meshac.packet.Packets.moves==1,"non-PLAY connection dispatched movement");
  var next=player();read(next.connection.c,packet,true,true);check(dev.meshac.packet.Packets.moves==2,"next current connection movement lost");
  System.out.println("MoveIngressTest PASS: real dispatcher once per admitted channel read, zero on native rejection/close/stale/phase; server replay capture separate (modeled boundary, not weave)");
 }
}'''
with tempfile.TemporaryDirectory(prefix='meshac-move-ingress-') as d:
 t=Path(d);files=[]
 for n,b in s.items():
  p=t/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(b);files.append(str(p))
 actual=['Meshac','check/ContextGate','check/CheckDependencies','data/PlayerData','data/PlayerDataManager','config/MeshVersion','packet/PacketDispatcher','packet/OutboundDispatcher','tracker/ConfirmationTracker','tracker/PendingConfirmation','mixin/ServerCommonPacketListenerImplAccessor','mixin/ServerGamePacketListenerSendMixin','mixin/ConnectionMixin','mixin/ConnectionAccessor','mixin/PacketEncoderAccessor','tracker/ConfirmationTransport','mixin/ServerGamePacketListenerImplMixin']
 files += [str(r/'src/main/java/dev/meshac'/(n+'.java')) for n in actual]
 p=t/'MoveIngressTest.java';p.write_text(test);files.append(str(p))
 output=Path(os.environ.get('MESHAC_PREP_OUTPUT','/tmp/meshac-move-ingress'));output.mkdir(parents=True,exist_ok=True)
 (output/'compile-inputs.json').write_text(json.dumps({str(Path(f).relative_to(r)) if Path(f).is_relative_to(r) else 'boundary/'+str(Path(f).relative_to(t)):hashlib.sha256(Path(f).read_bytes()).hexdigest() for f in files},indent=2)+'\n')
 subprocess.run([str(jdk/'javac'),'-d',str(t/'classes'),*files],check=True)
 subprocess.run([str(jdk/'java'),'-cp',str(t/'classes'),'MoveIngressTest'],check=True)
