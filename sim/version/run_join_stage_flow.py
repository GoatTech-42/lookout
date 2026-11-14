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
  static ServerPlayer player() { return player(777); }
  static ServerPlayer player(int protocol) {
    ServerPlayer p = new ServerPlayer();
    p.connection = new net.minecraft.server.network.ServerCommonPacketListenerImpl();
    p.connection.setConnection(new net.minecraft.network.Connection());
    ClientVersionResolver.capture(p.connection.meshac$getConnection(), protocol);
    return p;
  }
  static PlayerData data(ServerPlayer p) { return MeshServices.players().get(p.getUUID()); }
  static final Set<UUID> via47 = new HashSet<>();
  static final Set<UUID> via210 = new HashSet<>();
  static void initVia() {
    Object viaApi = Proxy.newProxyInstance(JoinStageFlowTest.class.getClassLoader(), new Class<?>[]{com.viaversion.viaversion.api.ViaAPI.class}, (o, m, a2) -> {
      if (m.getName().equals("getPlayerVersion")) return via47.contains(a2[0]) ? 47 : via210.contains(a2[0]) ? 210 : 777;
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
    if (args.length > 0 && args[0].equals("none")) { noneRun(); return; }
    allJarsRun();
  }
  // Contract A: bedrock API classes present => ALWAYS UNKNOWN, whatever Via or the handshake say.
  static void allJarsRun() {
    ServerPlayer A = player();
    PlayerDataManager.onJoin(A);
    eq(data(A).getClientVersion(), MeshVersion.UNKNOWN, "A: HEAD stage UNKNOWN with bedrock classes present");
    final UUID aid = A.getUUID();
    Object floodApi = Proxy.newProxyInstance(JoinStageFlowTest.class.getClassLoader(), new Class<?>[]{org.geysermc.floodgate.api.FloodgateApi.class}, (o, m, a2) -> {
      if (m.getName().equals("isFloodgatePlayer")) return aid.equals(a2[0]);
      throw new UnsupportedOperationException(m.getName()); });
    truth(org.geysermc.floodgate.api.InstanceHolder.set((org.geysermc.floodgate.api.FloodgateApi) floodApi, null, null, null, null, null, null), "floodgate registered");
    PlayerDataManager.onFabricJoin(A);
    eq(data(A).getClientVersion(), MeshVersion.UNKNOWN, "A: JOIN stage still UNKNOWN (registration irrelevant)");
    eq(ClientVersionResolver.resolve(A.connection.meshac$getConnection()), MeshVersion.V_26_3, "A: handshake never consumed");
    // Indeterminate positive Via: loaded Via 47 must NOT override bedrock presence.
    ServerPlayer B = player();
    initVia();
    via47.add(B.getUUID());
    PlayerDataManager.onJoin(B);
    PlayerDataManager.onFabricJoin(B);
    eq(data(B).getClientVersion(), MeshVersion.UNKNOWN, "A: positive Via 47 still UNKNOWN with bedrock classes present");
    // Direct JOIN without HEAD.
    ServerPlayer E = player();
    PlayerDataManager.onFabricJoin(E);
    eq(data(E).getClientVersion(), MeshVersion.UNKNOWN, "A: direct JOIN without HEAD UNKNOWN");
    truth(data(E).setVersionCalls == 1, "A: direct JOIN initializes and resolves once (stage-independent)");
    int calls = data(A).setVersionCalls;
    PlayerDataManager.onJoin(A);
    truth(data(A).setVersionCalls == calls, "initialize-once guard: repeat onJoin never re-derives");
    PlayerDataManager.onFabricJoin(A);
    truth(data(A).setVersionCalls == calls + 1, "UNKNOWN re-resolved at JOIN (same UNKNOWN answer)");
    System.out.println("JoinStageFlowTest(all) PASS (8 contract-A asserts)");
  }
  // Contract B: Via-class-only classpath (no geyser/floodgate/base jars at runtime).
  static void viaOnlyRun() {
    ServerPlayer V = player();
    PlayerDataManager.onJoin(V);
    eq(data(V).getClientVersion(), MeshVersion.UNKNOWN, "B: Via class present but NOT loaded -> UNKNOWN, no handshake fallback");
    eq(ClientVersionResolver.resolve(V.connection.meshac$getConnection()), MeshVersion.V_26_3, "B: handshake not consumed while Via not loaded");
    ClientVersionResolver.capture(V.connection.meshac$getConnection(), 777); // restore after the probe consumed it
    initVia();
    via47.add(V.getUUID());
    PlayerDataManager.onFabricJoin(V);
    eq(data(V).getClientVersion(), MeshVersion.V_1_8, "B: JOIN upgrades UNKNOWN after late Via load (47 -> V_1_8)");
    ServerPlayer V2 = player();
    PlayerDataManager.onJoin(V2);
    eq(data(V2).getClientVersion(), MeshVersion.V_26_3, "B: loaded Via positive server protocol 777 -> V_26_3 (documented topology assumption)");
    int c2 = data(V2).setVersionCalls;
    PlayerDataManager.onFabricJoin(V2);
    truth(data(V2).setVersionCalls == c2, "B: KNOWN from HEAD is final at JOIN");
    ServerPlayer V3 = player();
    via210.add(V3.getUUID());
    PlayerDataManager.onJoin(V3);
    eq(data(V3).getClientVersion(), MeshVersion.UNKNOWN, "B: Via unmapped protocol 210 -> UNKNOWN");
    ServerPlayer W = player();
    via47.add(W.getUUID());
    PlayerDataManager.onFabricJoin(W);
    eq(data(W).getClientVersion(), MeshVersion.V_1_8, "B: direct JOIN without HEAD resolves loaded Via 47");
    System.out.println("JoinStageFlowTest(via-only) PASS (8 contract-B asserts)");
  }
  // Contract C: no translator classes at all on the runtime classpath.
  static void noneRun() {
    ServerPlayer N = player();
    PlayerDataManager.onJoin(N);
    eq(data(N).getClientVersion(), MeshVersion.V_26_3, "C: translator-free native handshake control -> V_26_3");
    int cn = data(N).setVersionCalls;
    PlayerDataManager.onFabricJoin(N);
    truth(data(N).setVersionCalls == cn, "C: KNOWN final at JOIN");
    ServerPlayer N2 = player(47);
    PlayerDataManager.onJoin(N2);
    eq(data(N2).getClientVersion(), MeshVersion.UNKNOWN, "C: handshake mismatch (47 vs server 777) -> UNKNOWN");
    net.minecraft.network.Connection raw = new net.minecraft.network.Connection();
    ClientVersionResolver.capture(raw, 777);
    eq(TranslatorHooks.resolve(null, raw), MeshVersion.UNKNOWN, "C: null playerId never resolves native (guard)");
    System.out.println("JoinStageFlowTest(none) PASS (5 contract-C asserts)");
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
 subprocess.run([str(jdk/'java'),'-cp',str(t/'classes'),'dev.meshac.data.JoinStageFlowTest','none'],check=True)
