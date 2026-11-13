#!/usr/bin/env python3
"""Actual callback factory/dispatch/velocity/confirmation. Deterministic transport model, not runtime weave."""
from pathlib import Path
import subprocess,tempfile,sys,os,shutil,json,hashlib
netty=sorted((Path.home()/'.gradle-meshac/caches/modules-2/files-2.1/io.netty').glob('*/4.2.16.Final/*/*.jar'))
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
s['net/minecraft/network/Connection.java']=s['net/minecraft/network/Connection.java'].replace('writes.add(p instanceof net.minecraft.network.protocol.game.ClientboundLoginPacket?"login":"ping");','writes.add(p instanceof net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket?"velocity":p instanceof net.minecraft.network.protocol.game.ClientboundExplodePacket?"explosion":p instanceof net.minecraft.network.protocol.game.ClientboundLoginPacket?"login":p instanceof net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket?"position":"ping");')
s['net/minecraft/network/protocol/game/ClientboundSetEntityMotionPacket.java']=s['net/minecraft/network/protocol/game/ClientboundSetEntityMotionPacket.java'].replace('public class ClientboundSetEntityMotionPacket','public class ClientboundSetEntityMotionPacket').replace('{public int', '{public int fixtureEntity=37;public int').replace('return 37;','return fixtureEntity;')
s['net/minecraft/network/protocol/game/ClientboundExplodePacket.java']=s['net/minecraft/network/protocol/game/ClientboundExplodePacket.java'].replace('public class ClientboundExplodePacket', 'public class ClientboundExplodePacket').replace('{public java.util.Optional', '{public net.minecraft.world.phys.Vec3 fixtureKnockback=new net.minecraft.world.phys.Vec3(1,2,3);public java.util.Optional').replace('java.util.Optional.of(new net.minecraft.world.phys.Vec3(1,2,3))','java.util.Optional.ofNullable(fixtureKnockback)')
s['net/minecraft/server/network/ServerCommonPacketListenerImpl.java']=s['net/minecraft/server/network/ServerCommonPacketListenerImpl.java'].replace('c.send(p);}}','c.send(p);}public void send(net.minecraft.network.protocol.Packet<?> p,io.netty.channel.ChannelFutureListener listener){c.send(p);}}')
s['net/minecraft/network/Connection.java']=s['net/minecraft/network/Connection.java'].replace('public boolean connected=true,synchronousPong;','public boolean failNextWrite;public Runnable beforeWrite;public boolean connected=true,synchronousPong;').replace('Runnable r=()->{writes.add','Runnable r=()->fixture.OutboundOrderTest.wrap(this,p,()->{if(beforeWrite!=null){var b=beforeWrite;beforeWrite=null;b.run();}if(failNextWrite){failNextWrite=false;throw new IllegalStateException("native-write-fixture-failure");}writes.add').replace('invokeTail(this,p);};','invokeTail(this,p);});')
test='''package fixture;public class OutboundOrderTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void wrap(net.minecraft.network.Connection c,Object packet,Runnable original){wrapArgs(c,packet,null,true,original);}
 public static void wrapArgs(net.minecraft.network.Connection c,Object packet,io.netty.channel.ChannelFutureListener expectedListener,boolean expectedFlush,Runnable original){
  try{var type=bridge.loadClass("bridge.RealWrite");type.getMethod("write",java.util.function.Supplier.class,Runnable.class).invoke(null,(java.util.function.Supplier<Runnable>)()->{
   try{var m=dev.meshac.packet.OutboundDispatcher.class.getDeclaredMethod("beforeVelocityWrite",net.minecraft.network.Connection.class,Object.class);return (Runnable)m.invoke(null,c,packet);}catch(ReflectiveOperationException e){throw new RuntimeException(e);}},original);
  }catch(java.lang.reflect.InvocationTargetException e){if(e.getCause() instanceof RuntimeException x)throw x;throw new RuntimeException(e.getCause());}catch(ReflectiveOperationException e){throw new RuntimeException(e);}
 }
 static ClassLoader bridge;
 static Runnable deferred(net.minecraft.network.Connection c,Object packet,boolean cancel)throws Exception{
  return (Runnable)bridge.loadClass("bridge.RealWrite").getMethod("defer",java.util.function.Supplier.class,boolean.class).invoke(null,(java.util.function.Supplier<Runnable>)()->dev.meshac.packet.OutboundDispatcher.beforeVelocityWrite(c,packet),cancel);
 }

 static net.minecraft.server.level.ServerPlayer player(){var p=new net.minecraft.server.level.ServerPlayer();var g=new net.minecraft.server.network.ServerGamePacketListenerImpl();var c=new net.minecraft.network.Connection();p.connection=g;g.player=p;g.c=c;c.listener=g;dev.meshac.data.PlayerDataManager.onJoin(p);dev.meshac.data.PlayerDataManager.get(p).setHandshakeComplete(true);return p;}
 static net.minecraft.network.protocol.Packet<?> packet(boolean explosion){return explosion?new net.minecraft.network.protocol.game.ClientboundExplodePacket():new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket();}
 public static void main(String[] args)throws Exception{
  bridge=new java.net.URLClassLoader(java.util.Arrays.stream(args[0].split(java.io.File.pathSeparator)).map(x->{try{return java.nio.file.Path.of(x).toUri().toURL();}catch(Exception e){throw new RuntimeException(e);}}).toArray(java.net.URL[]::new),null);
  for(boolean explosion:new boolean[]{false,true})for(boolean two:new boolean[]{false,true}){var p=player();var c=p.connection.c;if(two)p.connection.send(packet(explosion),null);else p.connection.send(packet(explosion));c.ch.loop.drain();var expected=java.util.List.of(explosion?"explosion":"velocity","ping");check(c.writes.equals(expected),"afterwrite order/overload expected "+expected+" twoarg="+two+" actual="+c.writes);check(dev.meshac.data.PlayerDataManager.get(p).getConfirmationTracker().getPendingConfirmations().size()==1,"generated negative ping not recorded exactly once by unchanged caller-side admission");}

  var stale=player();var sc=stale.connection.c;sc.beforeWrite=()->{var replacement=player();dev.meshac.data.PlayerDataManager.onQuit(replacement);replacement.uuid=stale.getUUID();dev.meshac.data.PlayerDataManager.onQuit(stale);dev.meshac.data.PlayerDataManager.onJoin(replacement);dev.meshac.data.PlayerDataManager.get(replacement).setHandshakeComplete(true);};stale.connection.send(packet(false));sc.ch.loop.drain();check(sc.writes.equals(java.util.List.of("velocity")),"stale session emitted afterwrite confirmation");
  var phase=player();var pc=phase.connection.c;pc.beforeWrite=()->pc.ch.encoder.play=false;phase.connection.send(packet(false));pc.ch.loop.drain();check(pc.writes.equals(java.util.List.of("velocity")),"phase change emitted afterwrite confirmation");
  var identity=player();var ic=identity.connection.c;ic.beforeWrite=()->{var next=new net.minecraft.server.network.ServerGamePacketListenerImpl();next.player=identity;next.c=ic;identity.connection=next;};identity.connection.send(packet(false));ic.ch.loop.drain();check(ic.writes.equals(java.util.List.of("velocity")),"replaced listener ownership emitted old invocation afterwrite confirmation");
  var other=player();var otherMotion=new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket();otherMotion.fixtureEntity=999;other.connection.send(otherMotion);other.connection.c.ch.loop.drain();check(other.connection.c.writes.equals(java.util.List.of("velocity")),"non-self motion emitted confirmation");
  for(boolean absent:new boolean[]{false,true}){var p=player();var explosion=new net.minecraft.network.protocol.game.ClientboundExplodePacket();explosion.fixtureKnockback=absent?null:new net.minecraft.world.phys.Vec3(0,0,0);p.connection.send(explosion);p.connection.c.ch.loop.drain();check(p.connection.c.writes.equals(java.util.List.of("explosion")),"empty/zero explosion emitted confirmation");}
  var failed=player();var fc=failed.connection.c;fc.failNextWrite=true;failed.connection.send(packet(false));try{fc.ch.loop.drain();throw new AssertionError("native failure swallowed");}catch(IllegalStateException expected){check(expected.getMessage().equals("native-write-fixture-failure"),"different native failure");}check(fc.writes.equals(java.util.List.of("ping")),"failed native promise did not run upstream after-send task");failed.connection.send(new net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket());fc.ch.loop.drain();check(fc.writes.equals(java.util.List.of("ping","position")),"failed invocation callback leaked to later unrelated packet");
  var nested=player();var nc=nested.connection.c;nc.beforeWrite=()->nested.connection.send(packet(true));nested.connection.send(packet(false));nc.ch.loop.drain();check(nc.writes.equals(java.util.List.of("explosion","ping","velocity","ping")),"reentrant invocation lost callback ownership: "+nc.writes);
  for(int condition=0;condition<4;condition++){var p=player();var c=p.connection.c;var completion=deferred(c,packet(false),false);check(c.writes.isEmpty(),"deferred write emitted ping early");switch(condition){case 0->c.connected=false;case 1->c.ch.encoder.play=false;case 2->dev.meshac.data.PlayerDataManager.onQuit(p);case 3->{var g=new net.minecraft.server.network.ServerGamePacketListenerImpl();g.player=p;g.c=c;p.connection=g;}}completion.run();c.ch.loop.drain();check(c.writes.isEmpty(),"delayed retired/closed/phase/listener completion escaped policy "+condition);}
  var cancelled=player();var cancelTask=deferred(cancelled.connection.c,packet(false),true);cancelTask.run();cancelled.connection.c.ch.loop.drain();check(cancelled.connection.c.writes.equals(java.util.List.of("ping")),"cancelled-promise callback was incorrectly success-filtered");
  var recipient1=player();var recipient2=player();var sharedPacket=packet(true);var complete1=deferred(recipient1.connection.c,sharedPacket,false);var complete2=deferred(recipient2.connection.c,sharedPacket,false);complete2.run();recipient2.connection.c.ch.loop.drain();check(recipient1.connection.c.writes.isEmpty()&&recipient2.connection.c.writes.equals(java.util.List.of("ping")),"shared packet recipient callback crossed");complete1.run();recipient1.connection.c.ch.loop.drain();check(recipient1.connection.c.writes.equals(java.util.List.of("ping")),"shared packet first recipient callback lost");
  System.out.println("OutboundOrderTest PASS actual callback factory/dispatcher/velocity/confirmation; actual encoded-write callback and real Netty promise through isolated loader; native invocation capture and Minecraft API modeled");
 }
}'''
with tempfile.TemporaryDirectory(prefix='meshac-velocity-order-') as d:
 t=Path(d);files=[]
 for n,b in s.items():
  p=t/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(b);files.append(str(p))
 actual=['util/Vec3d','tracker/VelocityTracker','Meshac','check/ContextGate','check/CheckDependencies','data/PlayerData','data/PlayerDataManager','config/MeshVersion','packet/PacketDispatcher','packet/OutboundDispatcher','tracker/ConfirmationTracker','tracker/PendingConfirmation','mixin/ServerCommonPacketListenerImplAccessor','mixin/ServerGamePacketListenerSendMixin','mixin/ConnectionMixin','mixin/ConnectionAccessor','mixin/PacketEncoderAccessor','tracker/ConfirmationTransport']
 files += [str(r/'src/main/java/dev/meshac'/(n+'.java')) for n in actual]
 p=t/'OutboundOrderTest.java';p.write_text(test);files.append(str(p))
 manifest={str(Path(f).relative_to(r)) if Path(f).is_relative_to(r) else 'boundary/'+str(Path(f).relative_to(t)):hashlib.sha256(Path(f).read_bytes()).hexdigest() for f in files}
 output=Path(os.environ.get('MESHAC_PREP_OUTPUT',str(Path.cwd())));output.mkdir(parents=True,exist_ok=True);(output/(Path(__file__).stem+'-compile-inputs.json')).write_text(json.dumps(manifest,indent=2)+'\n')
 subprocess.run([str(jdk/'javac'),'-d',str(t/'classes'),*files],check=True)
 encoded={'__file__':str(r/'sim/ownership/run_outbound_encoded.py')}
 exec((r/'sim/ownership/run_outbound_encoded.py').read_text().split("test=r'"+"''")[0],encoded)
 bridgefiles=[]
 for n,b in encoded['s'].items():
  if n=='dev/meshac/packet/OutboundDispatcher.java':b='package dev.meshac.packet;public class OutboundDispatcher{public static java.util.function.Supplier<Runnable> source;public static Runnable beforeVelocityWrite(net.minecraft.network.Connection c,Object p){return source.get();}}'
  p=t/'nettysrc'/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(b);bridgefiles.append(str(p))
 bridge=t/'RealWrite.java';bridge.write_text('''package bridge;public class RealWrite {
  public static Runnable defer(java.util.function.Supplier<Runnable> prepare,boolean cancel){
   io.netty.channel.ChannelPromise[] pending={null};var c=channel(prepare,()->{},pending);
   c.write(io.netty.buffer.Unpooled.buffer().writeByte(7));
   return ()->{try{if(cancel)pending[0].cancel(false);else pending[0].setSuccess();}finally{c.finishAndReleaseAll();}};
  }
  public static void write(java.util.function.Supplier<Runnable> prepare,Runnable original){var c=channel(prepare,original,null);try{c.write(io.netty.buffer.Unpooled.buffer().writeByte(7)).syncUninterruptibly();}finally{c.finishAndReleaseAll();}}
  private static io.netty.channel.embedded.EmbeddedChannel channel(java.util.function.Supplier<Runnable> prepare,Runnable original,io.netty.channel.ChannelPromise[] pending){
   var c=new io.netty.channel.embedded.EmbeddedChannel();
   c.pipeline().addLast("sink",new io.netty.channel.ChannelOutboundHandlerAdapter(){public void write(io.netty.channel.ChannelHandlerContext ctx,Object m,io.netty.channel.ChannelPromise p){original.run();io.netty.util.ReferenceCountUtil.release(m);if(pending==null)p.setSuccess();else pending[0]=p;}});
   c.pipeline().addLast("operation-model",new io.netty.channel.ChannelOutboundHandlerAdapter(){public void write(io.netty.channel.ChannelHandlerContext ctx,Object msg,io.netty.channel.ChannelPromise p)throws Exception{
    var prior=dev.meshac.packet.OutboundDispatcher.source;dev.meshac.packet.OutboundDispatcher.source=prepare;
    try{var method=dev.meshac.mixin.ByteEncoderCompletionMixin.class.getDeclaredMethod("meshac$encodedWrite",io.netty.channel.ChannelHandlerContext.class,Object.class,io.netty.channel.ChannelPromise.class,com.llamalad7.mixinextras.injector.wrapoperation.Operation.class,Object.class);method.setAccessible(true);method.invoke(new net.minecraft.network.PacketEncoder<>(),ctx,msg,p,(com.llamalad7.mixinextras.injector.wrapoperation.Operation<io.netty.channel.ChannelFuture>)(args)->ctx.write(args[1],(io.netty.channel.ChannelPromise)args[2]),new Object());}
    finally{dev.meshac.packet.OutboundDispatcher.source=prior;}
   }});
   c.pipeline().addLast("connection",new net.minecraft.network.Connection());
   return c;
  }
 }''')
 nettycp=os.pathsep.join(map(str,netty));bridgeclasses=t/'nettyclasses'
 (output/'run_outbound_write-bridge-inputs.json').write_text(json.dumps({str(Path(f).relative_to(t)) if Path(f).is_relative_to(t) else str(Path(f)):hashlib.sha256(Path(f).read_bytes()).hexdigest() for f in [str(bridge),str(r/'src/main/java/dev/meshac/mixin/ByteEncoderCompletionMixin.java'),*bridgefiles,*map(str,netty)]},indent=2)+'\n')
 subprocess.run([str(jdk/'javac'),'-cp',nettycp,'-d',str(bridgeclasses),str(bridge),str(r/'src/main/java/dev/meshac/mixin/ByteEncoderCompletionMixin.java'),*bridgefiles],check=True)
 subprocess.run([str(jdk/'java'),'-cp',str(t/'classes'),'fixture.OutboundOrderTest',str(bridgeclasses)+os.pathsep+nettycp],check=True)
