#!/usr/bin/env python3
"""Actual outbound hooks/dispatch/velocity/confirmation classes. Deterministic channel, not runtime weave."""
from pathlib import Path
import subprocess,tempfile,sys,os,shutil,json,hashlib
r=Path(__file__).resolve().parents[2];jdk=Path(os.environ['JAVA_HOME'])/'bin' if 'JAVA_HOME' in os.environ else Path(shutil.which('javac')).resolve().parent
ctx={'__file__':str(r/'sim/lifecycle/run_join_anchor.py')}
exec((r/'sim/lifecycle/run_join_anchor.py').read_text().split("with tempfile.TemporaryDirectory(prefix='meshac-anchor-')")[0],ctx)
s=ctx['s']
for n in ['dev/meshac/tracker/VelocityTracker.java','dev/meshac/util/Vec3d.java']:s.pop(n)
s['dev/meshac/player/CollisionState.java']=s['dev/meshac/player/CollisionState.java'].replace('public CollisionState(Object d){}','public CollisionState(Object d){}public void queueAcknowledgedTask(Runnable r){}')
s['dev/meshac/tracker/WorldBlockTracker.java']=s['dev/meshac/tracker/WorldBlockTracker.java'].replace('public class WorldBlockTracker {','public class WorldBlockTracker {public boolean hasTrackedBlockNearby(double x,double y,double z){return false;}')
s['dev/meshac/packet/Flying.java']=s['dev/meshac/packet/Flying.java'].replace('public class Flying {','public class Flying {public boolean hasPositionChanged(){return true;}public boolean isOnGround(){return false;}')
s['net/minecraft/network/protocol/game/ClientboundSetEntityMotionPacket.java']=s['net/minecraft/network/protocol/game/ClientboundSetEntityMotionPacket.java'].replace('return 1;','return 37;').replace('new net.minecraft.world.phys.Vec3(0,0,0)','new net.minecraft.world.phys.Vec3(1,2,3)')
s['net/minecraft/network/protocol/game/ClientboundExplodePacket.java']=s['net/minecraft/network/protocol/game/ClientboundExplodePacket.java'].replace('java.util.Optional.empty()','java.util.Optional.of(new net.minecraft.world.phys.Vec3(1,2,3))')
s['net/minecraft/network/Connection.java']=s['net/minecraft/network/Connection.java'].replace('writes.add(p instanceof net.minecraft.network.protocol.game.ClientboundLoginPacket?"login":"ping");','writes.add(p instanceof net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket?"velocity":p instanceof net.minecraft.network.protocol.game.ClientboundExplodePacket?"explosion":p instanceof net.minecraft.network.protocol.game.ClientboundLoginPacket?"login":"ping");')
# The real common-listener PONG hook is in this fixture's class hierarchy.
s['net/minecraft/server/network/ServerGamePacketListenerImpl.java']='package net.minecraft.server.network; public class ServerGamePacketListenerImpl extends dev.meshac.mixin.ServerCommonPacketListenerImplMixin implements net.minecraft.network.PacketListener,dev.meshac.mixin.ServerCommonPacketListenerImplAccessor {public net.minecraft.server.level.ServerPlayer player;public net.minecraft.network.Connection c;public net.minecraft.server.level.ServerPlayer getPlayer(){return player;}public net.minecraft.network.Connection meshac$getConnection(){return c;}public void send(net.minecraft.network.protocol.Packet<?> p){c.send(p);}}'
s['net/minecraft/network/protocol/common/ServerboundPongPacket.java']='package net.minecraft.network.protocol.common;public record ServerboundPongPacket(int id){public int getId(){return id;}}'
s['net/minecraft/network/protocol/common/ServerboundKeepAlivePacket.java']='package net.minecraft.network.protocol.common;public class ServerboundKeepAlivePacket {}'
test='''public class SessionPongTest {
 static net.minecraft.server.level.ServerPlayer player(){var p=new net.minecraft.server.level.ServerPlayer();var g=new net.minecraft.server.network.ServerGamePacketListenerImpl();var c=new net.minecraft.network.Connection();g.player=p;g.c=c;c.listener=g;p.connection=g;dev.meshac.data.PlayerDataManager.onJoin(p);return p;}
 static void pong(Object listener,int id)throws Exception{var m=dev.meshac.mixin.ServerCommonPacketListenerImplMixin.class.getDeclaredMethod("meshac$pong",net.minecraft.network.protocol.common.ServerboundPongPacket.class,org.spongepowered.asm.mixin.injection.callback.CallbackInfo.class);m.setAccessible(true);m.invoke(listener,new net.minecraft.network.protocol.common.ServerboundPongPacket(id),null);}
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception{
  var p=player();var old=p.connection;var d=dev.meshac.data.PlayerDataManager.get(p);d.getConfirmationTracker().setJoinConfirmationId(-11207);pong(old,-11207);check(d.isHandshakeComplete(),"current listener valid anchor failed");
  var q=player();dev.meshac.data.PlayerDataManager.onQuit(q);q.uuid=p.getUUID();dev.meshac.data.PlayerDataManager.onQuit(p);dev.meshac.data.PlayerDataManager.onJoin(q);var replacement=dev.meshac.data.PlayerDataManager.get(q);replacement.getConfirmationTracker().setJoinConfirmationId(-11207);pong(old,-11207);
  check(!replacement.isHandshakeComplete()&&!replacement.isConnectionKickQueued()&&replacement.getRawConfirmationIds().isEmpty(),"stale listener PONG mutated replacement session on colliding anchor ID");
  pong(q.connection,-11207);check(replacement.isHandshakeComplete(),"current replacement listener failed");
  var phase=player();var pd=dev.meshac.data.PlayerDataManager.get(phase);pd.getConfirmationTracker().setJoinConfirmationId(-11207);phase.connection.c.ch.encoder.play=false;pong(phase.connection,-11207);check(!pd.isHandshakeComplete(),"configuration-phase PONG completed handshake");
  phase.connection.c.ch.encoder.play=true;phase.connection.c.connected=false;pong(phase.connection,-11207);check(!pd.isHandshakeComplete(),"closed connection PONG completed handshake");
  var same=player();var stale=same.connection;var current=new net.minecraft.server.network.ServerGamePacketListenerImpl();current.player=same;current.c=new net.minecraft.network.Connection();current.c.listener=current;same.connection=current;var sd=dev.meshac.data.PlayerDataManager.get(same);sd.getConfirmationTracker().setJoinConfirmationId(-11207);pong(stale,-11207);check(!sd.isHandshakeComplete(),"old connection for same native player accepted PONG");pong(current,-11207);check(sd.isHandshakeComplete(),"new connection for same player not accepted");
  var prior=player();var copy=new net.minecraft.server.level.ServerPlayer();copy.uuid=prior.getUUID();copy.connection=prior.connection;var copied=dev.meshac.data.PlayerDataManager.get(prior);dev.meshac.data.PlayerDataManager.onPlayerCopy(prior,copy);copy.connection.player=copy;copied.getConfirmationTracker().setJoinConfirmationId(-11207);pong(copy.connection,-11207);check(copied.isHandshakeComplete()&&copied.getPlayer()==copy,"valid copied-player listener PONG rejected");
  replacement.getConfirmationTracker().getPendingConfirmations().add(new dev.meshac.tracker.PendingConfirmation(-32347,System.currentTimeMillis()));pong(q.connection,-32347);check(replacement.getConfirmationTracker().getPendingConfirmations().isEmpty()&&!replacement.isConnectionKickQueued(),"current ordinary confirmation failed");pong(q.connection,-32347);check(replacement.isConnectionKickQueued(),"duplicate ordinary negative policy silently relaxed");
  System.out.println("SessionPongTest PASS (real common hook, session/listener/phase/close boundary, ordinary policy retained)");
 }
}'''
with tempfile.TemporaryDirectory(prefix='meshac-velocity-order-') as d:
 t=Path(d);files=[]
 for n,b in s.items():
  p=t/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(b);files.append(str(p))
 actual=['util/Vec3d','tracker/VelocityTracker','Meshac','check/ContextGate','check/CheckDependencies','data/PlayerData','data/PlayerDataManager','config/MeshVersion','packet/PacketDispatcher','packet/OutboundDispatcher','tracker/ConfirmationTracker','tracker/PendingConfirmation','mixin/ServerCommonPacketListenerImplMixin','mixin/ServerCommonPacketListenerImplAccessor','mixin/ServerGamePacketListenerSendMixin','mixin/ConnectionMixin','mixin/ConnectionAccessor','mixin/PacketEncoderAccessor','tracker/ConfirmationTransport']
 files += [str(r/'src/main/java/dev/meshac'/(n+'.java')) for n in actual]
 p=t/'SessionPongTest.java';p.write_text(test);files.append(str(p))
 manifest={str(Path(f).relative_to(r)) if Path(f).is_relative_to(r) else 'boundary/'+str(Path(f).relative_to(t)):hashlib.sha256(Path(f).read_bytes()).hexdigest() for f in files}
 output=Path(os.environ.get('MESHAC_PREP_OUTPUT',str(Path.cwd())));output.mkdir(parents=True,exist_ok=True);(output/(Path(__file__).stem+'-compile-inputs.json')).write_text(json.dumps(manifest,indent=2)+'\n')
 subprocess.run([str(jdk/'javac'),'-d',str(t/'classes'),*files],check=True)
 subprocess.run([str(jdk/'java'),'-cp',str(t/'classes'),'SessionPongTest'],check=True)
