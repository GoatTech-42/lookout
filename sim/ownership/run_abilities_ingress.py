#!/usr/bin/env python3
"""Actual abilities hooks/dispatcher, modeled native admission and thread replay, not Mixin weaving."""
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
s['net/minecraft/network/protocol/game/ServerboundPlayerAbilitiesPacket.java']='package net.minecraft.network.protocol.game;public class ServerboundPlayerAbilitiesPacket implements net.minecraft.network.protocol.Packet<Object>{public final boolean flying;public ServerboundPlayerAbilitiesPacket(){this(false);}public ServerboundPlayerAbilitiesPacket(boolean flying){this.flying=flying;}}'
s['dev/meshac/packet/Packets.java']=s['dev/meshac/packet/Packets.java'].replace('public class Packets {','public class Packets {public static int held;').replace('toHeldItemChange(Object p){return','toHeldItemChange(Object p){held++;return')
put('dev/meshac/packet/MeshPacketEvent','public record MeshPacketEvent(MeshPacketType type,Object player,Object packet,boolean inbound) {public static int abilities;public static Object raw,recipient;public static boolean direction;public MeshPacketEvent {if(type==MeshPacketType.PLAYER_ABILITIES){abilities++;raw=packet;recipient=player;direction=inbound;}}}')
s['net/minecraft/network/Connection.java']=s['net/minecraft/network/Connection.java'].replace('public net.minecraft.network.PacketListener getPacketListener(){return listener;}', 'public boolean overrideListener;public net.minecraft.network.PacketListener observedListener;public net.minecraft.network.PacketListener getPacketListener(){return overrideListener?observedListener:listener;}')
test='''public class AbilitiesIngressTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static void hooks(Class<?> cls,Object self,String method,String point,Object...args)throws Exception{
  for(var m:cls.getDeclaredMethods()){var i=m.getAnnotation(org.spongepowered.asm.mixin.injection.Inject.class);if(i==null||!(i.method().equals(method)||i.method().startsWith(method+"(")))continue;var a=i.at();if(method.equals("channelRead0")){check(a.value().equals("INVOKE")&&a.target().equals("Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"),"wrong native ingress target");check(a.shift()==org.spongepowered.asm.mixin.injection.At.Shift.NONE||a.shift()==org.spongepowered.asm.mixin.injection.At.Shift.BEFORE,"ingress must precede native invocation");}String actual=a.value().equals("INVOKE")?"INGRESS":a.value();if(actual.equals(point)){m.setAccessible(true);m.invoke(self,args);}}
 }
 static net.minecraft.server.level.ServerPlayer player(){var p=new net.minecraft.server.level.ServerPlayer();var g=new net.minecraft.server.network.ServerGamePacketListenerImpl();var c=new net.minecraft.network.Connection();p.connection=g;g.player=p;g.c=c;c.listener=g;dev.meshac.data.PlayerDataManager.onJoin(p);return p;}
 static void read(net.minecraft.network.Connection c,net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket p,boolean accepted,boolean tracked)throws Exception{
  if(!c.connected||!accepted)return;int count=dev.meshac.packet.MeshPacketEvent.abilities;
  hooks(dev.meshac.mixin.ConnectionMixin.class,c,"channelRead0","INGRESS",null,p,null);
  check(dev.meshac.packet.MeshPacketEvent.abilities==count+(tracked?1:0),"abilities packet must enter real dispatcher exactly once at admitted channel boundary");if(tracked){check(dev.meshac.packet.MeshPacketEvent.raw==p,"raw identity lost");check(dev.meshac.packet.MeshPacketEvent.recipient==c.listener.player,"wrong player");check(dev.meshac.packet.MeshPacketEvent.direction,"wrong event direction");}
  hooks(dev.meshac.mixin.ServerGamePacketListenerImplMixin.class,c.listener,"handlePlayerAbilities","HEAD",p,null);
  hooks(dev.meshac.mixin.ServerGamePacketListenerImplMixin.class,c.listener,"handlePlayerAbilities","HEAD",p,null);
  check(dev.meshac.packet.MeshPacketEvent.abilities==count+(tracked?1:0),"native network/server replay duplicated abilities event delivery");
 }
 public static void main(String[]args)throws Exception{
  var p=player();var c=p.connection.c;var packet=new net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket();read(c,packet,true,true);read(c,packet,true,true);check(dev.meshac.packet.MeshPacketEvent.abilities==2,"two legitimate identical-packet reads must remain distinct");read(c,packet,false,false);c.connected=false;read(c,packet,true,false);
  var q=player();var old=q.connection;var next=new net.minecraft.server.network.ServerGamePacketListenerImpl();next.player=q;next.c=old.c;q.connection=next;read(old.c,packet,true,false);
  var noListener=player();noListener.connection.c.overrideListener=true;read(noListener.connection.c,packet,true,false);noListener.connection.c.observedListener=new net.minecraft.network.PacketListener(){};read(noListener.connection.c,packet,true,false);
  var stalePlayer=player();var replacement=player();replacement.uuid=stalePlayer.uuid;dev.meshac.data.PlayerDataManager.onJoin(replacement);read(stalePlayer.connection.c,packet,true,false);
  var retired=player();dev.meshac.data.PlayerDataManager.onQuit(retired);read(retired.connection.c,packet,true,false);
  var phase=player();phase.connection.c.ch.encoder.play=false;read(phase.connection.c,packet,true,false);
  System.out.println("AbilitiesIngressTest PASS finite abilities actual-hook/dispatcher replay boundary; modeled native acceptance/handoff, not global ownership or runtime weave");
 }
}'''
with tempfile.TemporaryDirectory(prefix='meshac-abilities-ingress-') as d:
 t=Path(d);files=[]
 for n,b in s.items():
  p=t/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(b);files.append(str(p))
 actual=['Meshac','check/ContextGate','check/CheckDependencies','data/PlayerData','data/PlayerDataManager','config/MeshVersion','packet/PacketDispatcher','packet/OutboundDispatcher','tracker/ConfirmationTracker','tracker/PendingConfirmation','mixin/ServerCommonPacketListenerImplAccessor','mixin/ServerGamePacketListenerSendMixin','mixin/ConnectionMixin','mixin/ConnectionAccessor','mixin/PacketEncoderAccessor','tracker/ConfirmationTransport','mixin/ServerGamePacketListenerImplMixin']
 files += [str(r/'src/main/java/dev/meshac'/(n+'.java')) for n in actual]
 p=t/'AbilitiesIngressTest.java';p.write_text(test);files.append(str(p))
 output=Path(os.environ.get('MESHAC_PREP_OUTPUT','/tmp/meshac-move-ingress'));output.mkdir(parents=True,exist_ok=True)
 (output/'compile-inputs.json').write_text(json.dumps({str(Path(f).relative_to(r)) if Path(f).is_relative_to(r) else 'boundary/'+str(Path(f).relative_to(t)):hashlib.sha256(Path(f).read_bytes()).hexdigest() for f in files},indent=2)+'\n')
 subprocess.run([str(jdk/'javac'),'-d',str(t/'classes'),*files],check=True)
 subprocess.run([str(jdk/'java'),'-cp',str(t/'classes'),'AbilitiesIngressTest'],check=True)
