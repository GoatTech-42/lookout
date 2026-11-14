#!/usr/bin/env python3
"""REAL PlayerDataManager two-stage join flow (provisional login-send HEAD -> definitive Fabric JOIN)
with the REAL TranslatorHooks/ClientVersionResolver/MeshVersion against the real Via/Geyser/Floodgate
jars; only the Minecraft/Fabric classes and the PlayerData record are stubs.

TWO runtime classpaths, because the v3 review (B103) showed an all-vendor classpath masks the
Via-only lifecycle bug: bedrock class presence accidentally supplied the provisional deferral.
- Mode "all" (default): every vendor jar. Scenarios: early false-native -> late-Bedrock veto;
  native handshake control; TRUE late Via (Via.init AFTER the provisional onJoin, fixing the v3
  harness ordering critique); direct Fabric JOIN without any HEAD (definitive fall-through);
  initialize-once and final-known guards.
- Mode "via-only": runtime classpath WITHOUT geyser/floodgate/base/cumulus/events jars. Scenarios:
  Via class present but runtime not loaded must still defer provisionally (B103 v3 flaw 1) and the
  handshake must survive; definitive then resolves Via 47; direct JOIN without HEAD resolves
  definitively (B103 v3 flaw 2).
Fixture style matches sim/safety (stubs + real product class)."""
from pathlib import Path
import tempfile,subprocess
r=Path(__file__).resolve().parents[2];jdk=Path.home()/'jdk25/bin'
lib=r/'sim/version/lib'
alljars=['viaversion-api-5.12.2.jar','geyser-api-2.12.1-SNAPSHOT.jar','floodgate-api-2.2.4-SNAPSHOT.jar','base-api-1.0.3.jar','immutable-2.0.jar','annotations-24.0.0.jar','guava-33.4.8-jre.jar','netty-buffer-5.0.0.Alpha2.jar','netty-common-5.0.0.Alpha2.jar','cumulus-1.1.2.jar','events-1.2-20260725.205006-4.jar']
viaonly=['viaversion-api-5.12.2.jar','immutable-2.0.jar','annotations-24.0.0.jar','guava-33.4.8-jre.jar','netty-buffer-5.0.0.Alpha2.jar','netty-common-5.0.0.Alpha2.jar']
cp=lambda names: ':'.join(str(lib/n) for n in names)
s={
'net/minecraft/SharedConstants':'public class SharedConstants {public static SharedConstants getCurrentVersion(){return new SharedConstants();}public String name(){return "26.3";}public static int getProtocolVersion(){return 777;}}',
'net/minecraft/network/Connection':'public class Connection {}',
'net/minecraft/world/entity/player/Player':'public class Player {public java.util.UUID getUUID(){return null;}}',
'net/minecraft/server/MinecraftServer':'public class MinecraftServer {public int getTickCount(){return 0;}}',
'net/minecraft/server/level/ServerLevel':'public class ServerLevel {public net.minecraft.server.MinecraftServer getServer(){return new net.minecraft.server.MinecraftServer();}}',
'net/minecraft/server/network/ServerCommonPacketListenerImpl':'public class ServerCommonPacketListenerImpl implements dev.meshac.mixin.ServerCommonPacketListenerImplAccessor {private net.minecraft.network.Connection c;public void setConnection(net.minecraft.network.Connection c){this.c=c;}public net.minecraft.network.Connection meshac$getConnection(){return c;}}',
'net/minecraft/server/level/ServerPlayer':'public class ServerPlayer extends net.minecraft.world.entity.player.Player {public net.minecraft.server.network.ServerCommonPacketListenerImpl connection;private final java.util.UUID id=java.util.UUID.randomUUID();public int getId(){return 1;}public java.util.UUID getUUID(){return id;}public Name getName(){return new Name();}public ServerLevel level(){return new ServerLevel();}public static class Name{public String getString(){return "stub";}}}',
'dev/meshac/mixin/ServerCommonPacketListenerImplAccessor':'public interface ServerCommonPacketListenerImplAccessor {net.minecraft.network.Connection meshac$getConnection();}',
'dev/meshac/MeshServices':'public class MeshServices {static final java.util.Map<java.util.UUID,dev.meshac.data.PlayerData> P=new java.util.concurrent.ConcurrentHashMap<>();public static java.util.Map<java.util.UUID,dev.meshac.data.PlayerData> players(){return P;}public static void log(String s){System.out.println(s);}}',
'dev/meshac/check/manager/CheckManager':'public class CheckManager {public static Object createChecks(dev.meshac.data.PlayerData d){return new Object();}}',
'dev/meshac/veil/Veil':'public class Veil {public static void grace(net.minecraft.server.level.ServerPlayer p,long t){}public static void forgetPlayer(net.minecraft.server.level.ServerPlayer p){}}',
'dev/meshac/data/PlayerData':'public class PlayerData {private final net.minecraft.server.level.ServerPlayer player;private dev.meshac.config.MeshVersion v=dev.meshac.config.MeshVersion.UNKNOWN;public int setVersionCalls;public PlayerData(net.minecraft.server.level.ServerPlayer p){this.player=p;}public net.minecraft.server.level.ServerPlayer getPlayer(){return player;}public void setEntityId(int i){}public void setUniqueId(java.util.UUID u){}public void setPlayerName(String s){}public void setClientVersion(dev.meshac.config.MeshVersion nv){this.v=nv;setVersionCalls++;}public dev.meshac.config.MeshVersion getClientVersion(){return v;}public void setChecks(Object o){}public void setPlayer(net.minecraft.server.level.ServerPlayer np){}}'
}
test='''package dev.meshac.data;
import dev.meshac.config.MeshVersion;
import dev.meshac.MeshServices;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
public class JoinStageFlowTest {
  static void eq(Object a, Object b, String w) { if (a != b) throw new AssertionError(w + ": expected " + b + " got " + a); }
  static void truth(boolean b, String w) { if (!b) throw new AssertionError(w); }
  static ServerPlayer player() {
    ServerPlayer p = new ServerPlayer();
    p.connection = new net.minecraft.server.network.ServerCommonPacketListenerImpl();
    p.connection.setConnection(new net.minecraft.network.Connection());
    ClientVersionResolver.capture(p.connection.meshac$getConnection(), 777);
    return p;
  }
  static PlayerData data(ServerPlayer p) { return MeshServices.players().get(p.getUUID()); }
  static final Set<UUID> via47 = new HashSet<>();
  static void initVia() {
    Object viaApi = Proxy.newProxyInstance(JoinStageFlowTest.class.getClassLoader(), new Class<?>[]{com.viaversion.viaversion.api.ViaAPI.class}, (o, m, a2) -> {
      if (m.getName().equals("getPlayerVersion")) return via47.contains(a2[0]) ? 47 : 777;
      throw new UnsupportedOperationException(m.getName()); });
    Object viaPlatform = Proxy.newProxyInstance(JoinStageFlowTest.class.getClassLoader(), new Class<?>[]{com.viaversion.viaversion.api.platform.ViaPlatform.class}, (o, m, a2) -> {
      if (m.getName().equals("getApi")) return viaApi;
      throw new UnsupportedOperationException(m.getName()); });
    com.viaversion.viaversion.api.Via.init((com.viaversion.viaversion.api.ViaManager) Proxy.newProxyInstance(JoinStageFlowTest.class.getClassLoader(), new Class<?>[]{com.viaversion.viaversion.api.ViaManager.class}, (o, m, a2) -> {
      if (m.getName().equals("getPlatform")) return viaPlatform;
      throw new UnsupportedOperationException(m.getName()); }));
  }
  public static void main(String[] args) {
    if (args.length > 0 && args[0].equals("viaonly")) { viaOnlyRun(); return; }
    allJarsRun();
  }
  static void allJarsRun() {
    // A: provisional would have been false-native (handshake 777) -> late Bedrock registration.
    ServerPlayer A = player();
    PlayerDataManager.onJoin(A);
    eq(data(A).getClientVersion(), MeshVersion.UNKNOWN, "A provisional defers to UNKNOWN (translator classes present), no false native");
    final UUID aid = A.getUUID();
    Object floodApi = Proxy.newProxyInstance(JoinStageFlowTest.class.getClassLoader(), new Class<?>[]{org.geysermc.floodgate.api.FloodgateApi.class}, (o, m, a2) -> {
      if (m.getName().equals("isFloodgatePlayer")) return aid.equals(a2[0]);
      throw new UnsupportedOperationException(m.getName()); });
    truth(org.geysermc.floodgate.api.InstanceHolder.set((org.geysermc.floodgate.api.FloodgateApi) floodApi, null, null, null, null, null, null), "floodgate registered");
    PlayerDataManager.onFabricJoin(A);
    eq(data(A).getClientVersion(), MeshVersion.UNKNOWN, "A definitive: Bedrock veto, false native corrected");

    // C: native control, nothing registered for it, Via not initialized yet.
    ServerPlayer C = player();
    PlayerDataManager.onJoin(C);
    eq(data(C).getClientVersion(), MeshVersion.UNKNOWN, "C provisional defers");
    PlayerDataManager.onFabricJoin(C);
    eq(data(C).getClientVersion(), MeshVersion.V_26_3, "C definitive: native handshake control V_26_3");

    // B: TRUE late Via: Via.init AFTER the provisional onJoin (v3 harness initialized it before, masking).
    ServerPlayer B = player();
    PlayerDataManager.onJoin(B);
    eq(data(B).getClientVersion(), MeshVersion.UNKNOWN, "B provisional defers while Via not loaded");
    initVia();
    via47.add(B.getUUID());
    PlayerDataManager.onFabricJoin(B);
    eq(data(B).getClientVersion(), MeshVersion.V_1_8, "B definitive: late Via 47 -> V_1_8");

    // E: direct Fabric JOIN without any login-send HEAD (B103 v3 flaw 2): initialize then fall
    // through to the definitive resolution; must NOT stay UNKNOWN.
    ServerPlayer E = player();
    via47.add(E.getUUID());
    PlayerDataManager.onFabricJoin(E);
    eq(data(E).getClientVersion(), MeshVersion.V_1_8, "E direct JOIN without HEAD resolves definitively V_1_8");
    eq(data(E).setVersionCalls, 2, "E wrote provisional then definitive exactly once each");

    int calls = data(A).setVersionCalls;
    PlayerDataManager.onJoin(A);
    truth(data(A).setVersionCalls == calls, "initialize-once guard: repeat onJoin never re-derives");
    int callsB = data(B).setVersionCalls;
    PlayerDataManager.onFabricJoin(B);
    truth(data(B).setVersionCalls == callsB, "definitive stage skips non-UNKNOWN (final known V_1_8 stays)");
    PlayerDataManager.onFabricJoin(A);
    truth(data(A).setVersionCalls == calls + 1, "definitive stage retries UNKNOWN (Bedrock veto stays UNKNOWN)");
    System.out.println("JoinStageFlowTest(all) PASS (9 stage-flow + 3 guard asserts)");
  }
  static void viaOnlyRun() {
    // V: Via class present but runtime NOT loaded; no bedrock classes on this classpath at all.
    // B103 v3 flaw 1: the provisional stage must still defer and preserve the handshake.
    ServerPlayer V = player();
    PlayerDataManager.onJoin(V);
    eq(data(V).getClientVersion(), MeshVersion.UNKNOWN, "V via-only provisional defers on Via CLASS presence, not loaded state");
    eq(ClientVersionResolver.resolve(V.connection.meshac$getConnection()), MeshVersion.V_26_3,
        "V handshake preserved through provisional deferral (consumed only by this assert)");
    ClientVersionResolver.capture(V.connection.meshac$getConnection(), 777); // restore for the definitive stage
    initVia();
    via47.add(V.getUUID());
    PlayerDataManager.onFabricJoin(V);
    eq(data(V).getClientVersion(), MeshVersion.V_1_8, "V via-only definitive: late Via 47 -> V_1_8");

    // W: direct Fabric JOIN without HEAD on the via-only classpath (B103 v3 flaw 2).
    ServerPlayer W = player();
    via47.add(W.getUUID());
    PlayerDataManager.onFabricJoin(W);
    eq(data(W).getClientVersion(), MeshVersion.V_1_8, "W via-only direct JOIN without HEAD resolves definitively V_1_8");
    System.out.println("JoinStageFlowTest(via-only) PASS (5 asserts, isolated classpath: no geyser/floodgate/base jars)");
  }
}
'''
with tempfile.TemporaryDirectory(prefix='meshac-stageflow-') as d:
 t=Path(d);files=[]
 for n,b in s.items():
  p=t/(n+'.java');p.parent.mkdir(parents=True,exist_ok=True);p.write_text('package '+n.rsplit('/',1)[0].replace('/','.')+'; '+b);files.append(str(p))
 p=t/'dev/meshac/data/JoinStageFlowTest.java';p.write_text(test);files.append(str(p))
 for f in ['src/main/java/dev/meshac/config/MeshVersion.java','src/main/java/dev/meshac/data/ClientVersionResolver.java','src/main/java/dev/meshac/data/TranslatorHooks.java','src/main/java/dev/meshac/data/PlayerDataManager.java']:
  files.append(str(r/f))
 subprocess.run([str(jdk/'javac'),'-cp',cp(alljars),'-d',str(t/'classes'),*files],check=True)
 subprocess.run([str(jdk/'java'),'-cp',str(t/'classes')+':'+cp(alljars),'dev.meshac.data.JoinStageFlowTest'],check=True)
 subprocess.run([str(jdk/'java'),'-cp',str(t/'classes')+':'+cp(viaonly),'dev.meshac.data.JoinStageFlowTest','viaonly'],check=True)
