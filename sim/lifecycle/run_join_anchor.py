#!/usr/bin/env python3
"""Actual session/tracker/dispatcher/hooks with deterministic native/Netty boundaries, not a weave test."""
from pathlib import Path
import tempfile,subprocess
ROOT=Path(__file__).resolve().parents[2]; JDK=Path.home()/'jdk25/bin'
# Reuse data-only packet/world boundaries, not the prior suite's execution or assertions.
scope={'__file__':str(ROOT/'sim/dispatch/run_dispatch_safety.py')}
exec((ROOT/'sim/dispatch/run_dispatch_safety.py').read_text().split("with tempfile.TemporaryDirectory(")[0],scope)
s=scope['stubs']
def put(n,b):s[n+'.java']='package '+n.rsplit('/',1)[0].replace('/','.')+'; '+b
for n in ['dev/meshac/data/PlayerData','dev/meshac/data/PlayerDataManager']:s.pop(n+'.java')
put('dev/meshac/MeshServices','public class MeshServices {static final java.util.Map<java.util.UUID,dev.meshac.data.PlayerData> p=new java.util.HashMap<>();public static java.util.Map<java.util.UUID,dev.meshac.data.PlayerData> players(){return p;} public static int ticks(){return 0;} public static void log(String s){} public static MeshServices alerts(){return new MeshServices();} public void broadcastAlert(String s){} public static void mainThread(Runnable r){r.run();}}')
put('dev/meshac/check/manager/CheckManager','public class CheckManager {public static int count;public static java.util.List<dev.meshac.check.AbstractCheck> createChecks(Object d){count++;return new java.util.ArrayList<>();}}')
put('dev/meshac/check/CheckUtils','public class CheckUtils {public static boolean isAtLeast1_17(Object d){return true;}public static boolean isAtLeast1_13(Object d){return true;}public static boolean isNewerThan1_9(Object d){return true;}}')
put('dev/meshac/check/ExemptType','public enum ExemptType {FIXTURE}')
put('dev/meshac/check/ExemptionManager','public class ExemptionManager {public static void logColored(String s){}public static boolean isAtLeast1_21_2(){return true;}}')
put('dev/meshac/config/MeshSettings','''public class MeshSettings {public static boolean connectionOtherAnticheatSupport,connectionDebugMessages,connectionNoResponseCodeOfConductFix;public static int connectionNoResponseDelayInSeconds=30,connectionMaxSizeAmount=1000; public static String messagesFrozen="fixture",connectionInvalidStaffAlertMessage="invalid",connectionInvalidConsoleAlertMessage="invalid",connectionInvalidKickMessage="invalid",connectionOutOfOrderStaffAlertMessage="order",connectionOutOfOrderConsoleAlertMessage="order",connectionOutOfOrderKickMessage="order",connectionNoResponseStaffAlertMessage="timeout",connectionNoResponseConsoleAlertMessage="timeout",connectionNoResponseKickMessage="timeout",connectionMaxSizeStaffAlertMessage="size",connectionMaxSizeConsoleAlertMessage="size",connectionMaxSizeKickMessage="size";}''')
put('dev/meshac/collection/EvictingList','public class EvictingList extends java.util.ArrayList<Integer> {public EvictingList(int n){}}')
put('dev/meshac/util/MathUtils','public class MathUtils {public static double length3(double a,double b,double c){return Math.sqrt(a*a+b*b+c*c);}}')
put('dev/meshac/data/ClientVersionResolver','public class ClientVersionResolver {public static dev.meshac.config.MeshVersion resolve(Object c){return dev.meshac.config.MeshVersion.V_26_3;}}')
put('dev/meshac/veil/Veil','public class Veil {public static void attacked(Object a,Object b,int t){}public static void grace(Object p,int t){}public static void forgetPlayer(Object p){}}')
# Typed tracker boundaries required by the real PlayerData and dispatcher.
body=s['dev/meshac/data/Support.java'].split('; ',1)[1].replace('public class Support','public class NAME').replace('public float getYaw()', 'public NAME(dev.meshac.data.PlayerData d){} public float getYaw()')
for name in ['VehicleTracker','WorldBlockTracker','EntityTracker','RelativeMoveTracker','RotationTracker','VelocityTracker','ConnectionTracker']:
 extra='public interface ClientboundSectionUpdatesConsumer {void consume(java.util.function.BiConsumer<Object,Object> c);}' if name=='WorldBlockTracker' else ''
 put('dev/meshac/tracker/'+name,body.replace('NAME',name).replace('\n}',extra+'\n}'))
for name in ['MotionState','ClickTimingData']:
 put('dev/meshac/player/'+name,body.replace('NAME',name))
put('dev/meshac/player/PlayerToggles','public class PlayerToggles {public PlayerToggles(Object d){}public boolean isExempt(Object t){return false;}}')
a=s['dev/meshac/player/CollisionState.java'];s['dev/meshac/player/CollisionState.java']=a.replace('public record ServerAbilities','public CollisionState(Object d){} public double getX(){return 0;}public double getY(){return 0;}public double getZ(){return 0;}public double getJoinX(){return 0;}public double getJoinY(){return 0;}public double getJoinZ(){return 0;}public record ServerAbilities')
for name in ['Placement','HeldItemChange']:put('dev/meshac/packet/'+name,'public class '+name+' {}')
s['dev/meshac/packet/Packets.java']=s['dev/meshac/packet/Packets.java'].replace('public static Object toPlacement(Object p){return p;}','public static Placement toPlacement(Object p){return new Placement();}').replace('public static Object toHeldItemChange(Object p){return p;}','public static HeldItemChange toHeldItemChange(Object p){return new HeldItemChange();}')
s['dev/meshac/packet/MeshPacketType.java']=s['dev/meshac/packet/MeshPacketType.java'].replace('SERVER_POSITION,','PONG,SERVER_POSITION,')
put('net/minecraft/world/entity/player/Player','public class Player {public java.util.UUID uuid=java.util.UUID.randomUUID();public java.util.UUID getUUID(){return uuid;}}')
a=s['net/minecraft/server/level/ServerPlayer.java'];s['net/minecraft/server/level/ServerPlayer.java']=a.replace('class ServerPlayer {','class ServerPlayer extends net.minecraft.world.entity.player.Player {public net.minecraft.server.network.ServerGamePacketListenerImpl connection;public int id=37;public boolean hasDisconnected(){return !connection.c.isConnected();}public int getId(){return id;}public Name getName(){return new Name();}public class Name {public String getString(){return "fixture";}}')
put('dev/meshac/safety/Effects','public class Effects {public static boolean disconnect(Object p,Object c){throw new AssertionError("unexpected disconnect");}}')
put('io/netty/channel/EventLoop','public class EventLoop {public boolean in;public java.util.ArrayDeque<Runnable> tasks=new java.util.ArrayDeque<>();public boolean inEventLoop(){return in;}public void execute(Runnable r){tasks.add(r);}public void drain(){in=true;try{while(!tasks.isEmpty())tasks.remove().run();}finally{in=false;}}}')
put('io/netty/channel/Channel','public class Channel {public final EventLoop loop=new EventLoop();public final net.minecraft.network.PacketEncoder<?> encoder=new net.minecraft.network.PacketEncoder<>();public EventLoop eventLoop(){return loop;}public Channel pipeline(){return this;}public <T> T get(Class<T> c){return c.cast(encoder);}}')
put('io/netty/channel/ChannelFutureListener','public interface ChannelFutureListener {}')
put('net/minecraft/network/ConnectionProtocol','public enum ConnectionProtocol {PLAY,CONFIGURATION}')
put('net/minecraft/network/ProtocolInfo','public class ProtocolInfo<T> {public ConnectionProtocol id(){return ConnectionProtocol.PLAY;}}')
put('net/minecraft/network/PacketEncoder','public class PacketEncoder<T> implements dev.meshac.mixin.PacketEncoderAccessor {public boolean play=true;public ProtocolInfo<?> meshac$getProtocolInfo(){return new ProtocolInfo<>(){public ConnectionProtocol id(){return play?ConnectionProtocol.PLAY:ConnectionProtocol.CONFIGURATION;}};}}')
put('net/minecraft/network/PacketListener','public interface PacketListener {}')
put('net/minecraft/network/protocol/game/ClientboundLoginPacket','public class ClientboundLoginPacket implements net.minecraft.network.protocol.Packet<Object> {public int playerId(){return 37;}}')
put('net/minecraft/network/protocol/common/ClientboundPingPacket','public record ClientboundPingPacket(int id) implements net.minecraft.network.protocol.Packet<Object> {public int getId(){return id;}}')
put('net/minecraft/network/Connection','''public class Connection extends dev.meshac.mixin.ConnectionMixin implements dev.meshac.mixin.ConnectionAccessor {public boolean connected=true,synchronousPong;public Runnable onPing;public io.netty.channel.Channel ch=new io.netty.channel.Channel();public net.minecraft.server.network.ServerGamePacketListenerImpl listener;public java.util.List<String> writes=new java.util.ArrayList<>();public net.minecraft.network.PacketListener getPacketListener(){return listener;}public boolean isConnected(){return connected;}public io.netty.channel.Channel meshac$getChannel(){return ch;}public void send(net.minecraft.network.protocol.Packet<?> p){Runnable r=()->{writes.add(p instanceof net.minecraft.network.protocol.game.ClientboundLoginPacket?"login":"ping");if(p instanceof net.minecraft.network.protocol.common.ClientboundPingPacket ping){var d=dev.meshac.data.PlayerDataManager.get(listener.player);if(!d.isHandshakeComplete()&&ping.getId()!=d.getConfirmationTracker().getJoinConfirmationId())throw new AssertionError("anchor ID assigned after send");if(onPing!=null){var callback=onPing;onPing=null;callback.run();}if(synchronousPong)dev.meshac.packet.PacketDispatcher.onConfirmation(listener.player,ping.getId(),dev.meshac.packet.MeshPacketType.PONG,ping);}invokeTail(this,p);};if(ch.loop.inEventLoop())r.run();else ch.loop.execute(r);}static void invokeTail(Object o,Object p){try{var m=dev.meshac.mixin.ConnectionMixin.class.getDeclaredMethod("meshac$afterSend",net.minecraft.network.protocol.Packet.class,io.netty.channel.ChannelFutureListener.class,boolean.class,org.spongepowered.asm.mixin.injection.callback.CallbackInfo.class);m.setAccessible(true);m.invoke(o,p,null,true,null);}catch(ReflectiveOperationException e){throw new RuntimeException(e);}}}''')
put('net/minecraft/server/network/ServerCommonPacketListenerImpl','''public class ServerCommonPacketListenerImpl extends dev.meshac.mixin.ServerGamePacketListenerSendMixin implements dev.meshac.mixin.ServerCommonPacketListenerImplAccessor {public net.minecraft.network.Connection c;public net.minecraft.network.Connection meshac$getConnection(){return c;}public void send(net.minecraft.network.protocol.Packet<?> p){try{var m=dev.meshac.mixin.ServerGamePacketListenerSendMixin.class.getDeclaredMethod("meshac$send",net.minecraft.network.protocol.Packet.class,org.spongepowered.asm.mixin.injection.callback.CallbackInfo.class);m.setAccessible(true);m.invoke(this,p,null);}catch(ReflectiveOperationException e){throw new RuntimeException(e);}c.send(p);}}''')
put('net/minecraft/server/network/ServerGamePacketListenerImpl','public class ServerGamePacketListenerImpl extends ServerCommonPacketListenerImpl implements net.minecraft.network.PacketListener {public net.minecraft.server.level.ServerPlayer player;}')
for n,b in {'Mixin':'Class<?>[] value();','Shadow':'','Final':'','Unique':''}.items():put('org/spongepowered/asm/mixin/'+n,'public @interface '+n+' {'+b+'}')
put('org/spongepowered/asm/mixin/gen/Accessor','public @interface Accessor {String value();}')
put('org/spongepowered/asm/mixin/injection/At','public @interface At {String value();}')
put('org/spongepowered/asm/mixin/injection/Inject','public @interface Inject {String method();At at();}')
put('org/spongepowered/asm/mixin/injection/callback/CallbackInfo','public class CallbackInfo {}')
# Compile the real initializer to invoke its registered Fabric JOIN callback after early publication.
put('net/fabricmc/api/DedicatedServerModInitializer','public interface DedicatedServerModInitializer {void onInitializeServer();}')
put('org/slf4j/Logger','public class Logger {public void info(String s){}}')
put('org/slf4j/LoggerFactory','public class LoggerFactory {public static Logger getLogger(String s){return new Logger();}}')
put('net/fabricmc/fabric/api/networking/v1/ServerPlayConnectionEvents','''public class ServerPlayConnectionEvents {public interface Join {void call(net.minecraft.server.network.ServerGamePacketListenerImpl h,Object sender,Object server);}public interface Quit {void call(net.minecraft.server.network.ServerGamePacketListenerImpl h,Object server);}public static class JoinEvent {public Join callback;public void register(Join j){callback=j;}}public static class QuitEvent {public void register(Quit q){}}public static final JoinEvent JOIN=new JoinEvent();public static final QuitEvent DISCONNECT=new QuitEvent();}''')
put('net/fabricmc/fabric/api/event/lifecycle/v1/ServerLifecycleEvents','public class ServerLifecycleEvents {public static class Event {public void register(java.util.function.Consumer<Object> c){}}public static final Event SERVER_STARTED=new Event();}')
put('net/fabricmc/fabric/api/event/lifecycle/v1/ServerTickEvents','public class ServerTickEvents {public static class Event {public void register(java.util.function.Consumer<Object> c){}}public static final Event END_SERVER_TICK=new Event();}')
put('net/fabricmc/fabric/api/entity/event/v1/ServerPlayerEvents','public class ServerPlayerEvents {public interface Copy {void call(net.minecraft.server.level.ServerPlayer a,net.minecraft.server.level.ServerPlayer b,boolean alive);}public static class Event {public void register(Copy c){}}public static final Event COPY_FROM=new Event();}')
put('net/fabricmc/fabric/api/command/v2/CommandRegistrationCallback','public class CommandRegistrationCallback {public interface Call {void call(Object a,Object b,Object c);}public static class Event {public void register(Call c){}}public static final Event EVENT=new Event();}')
put('dev/meshac/product/Cases','public class Cases {public static class Case {}public static void load(){}public static Case activeBan(java.util.UUID id){return new Case();}}')
put('dev/meshac/product/Screens','public class Screens {public static Object ban(Cases.Case c){return c;}}')
put('dev/meshac/product/Config','public class Config {public static void load(){}public static void applyVeil(){}}')
put('dev/meshac/product/MeshCommands','public class MeshCommands {public static void register(Object o){}}')
put('dev/meshac/product/Verdict','public class Verdict {public static void forget(Object o){}}')
put('dev/meshac/config/MeshConfig','public class MeshConfig {public static void load(){}}')
put('dev/meshac/safety/Containment','public class Containment {public static void setLogger(java.util.function.Consumer<String> c){}}')
put('dev/meshac/safety/Effects','public class Effects {public static int bans;public static boolean disconnect(Object p,Object c){bans++;return false;}}')
s['dev/meshac/config/MeshSettings.java']=s['dev/meshac/config/MeshSettings.java'].replace('public class MeshSettings {','public class MeshSettings {public static void loadSettings(){}')
s['dev/meshac/MeshServices.java']=s['dev/meshac/MeshServices.java'].replace('public static int ticks()', 'public static void init(Object s){}public static void tick(){}public static int ticks()')
s['dev/meshac/veil/Veil.java']=s['dev/meshac/veil/Veil.java'].replace('public class Veil {','public class Veil {public static void tick(Object s){}')
s['net/minecraft/server/network/ServerGamePacketListenerImpl.java']=s['net/minecraft/server/network/ServerGamePacketListenerImpl.java'].replace('public net.minecraft.server.level.ServerPlayer player;','public net.minecraft.server.level.ServerPlayer player;public net.minecraft.server.level.ServerPlayer getPlayer(){return player;}')
with tempfile.TemporaryDirectory(prefix='meshac-anchor-') as d:
 t=Path(d);files=[]
 for n,b in s.items():
  p=t/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(b);files.append(str(p))
 actual=['Meshac','check/ContextGate','check/CheckDependencies','data/PlayerData','data/PlayerDataManager','config/MeshVersion','packet/PacketDispatcher','packet/OutboundDispatcher','tracker/ConfirmationTracker','tracker/PendingConfirmation','mixin/ServerCommonPacketListenerImplAccessor','mixin/ServerGamePacketListenerSendMixin','mixin/ConnectionMixin','mixin/ConnectionAccessor','mixin/PacketEncoderAccessor','tracker/ConfirmationTransport']
 files.extend(str(ROOT/'src/main/java/dev/meshac'/(n+'.java')) for n in actual)
 files.append(str(ROOT/'sim/lifecycle/JoinAnchorTest.java'))
 subprocess.run([str(JDK/'javac'),'-d',str(t/'classes'),*files],check=True)
 subprocess.run([str(JDK/'java'),'-cp',str(t/'classes'),'JoinAnchorTest'],check=True)
