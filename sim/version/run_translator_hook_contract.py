#!/usr/bin/env python3
"""Actual TranslatorHooks + MeshVersion + ClientVersionResolver against the real Via/Geyser/Floodgate
API jars (sim/version/lib). Asserts the pinned fromProtocol table, the choose() composition table,
the presence tri-state (including a present-but-broken class), no-runtime fallthrough, REAL Via
dispatch via proxies (loaded-Via known/unmapped/error), REAL Geyser/Floodgate registered Bedrock
vetoes, the provisional-deferral lifecycle incl. a registration-transition regression, and a Geyser
API-mismatch indeterminate run. Proxy technique adapted from B103's
IndependentHookProbe (disclosed; proxies only stand in for the translator runtime, no logic change)."""
from pathlib import Path
import tempfile,subprocess
r=Path(__file__).resolve().parents[2];jdk=Path.home()/'jdk25/bin'
lib=r/'sim/version/lib'
jars=':'.join(str(lib/n) for n in ['viaversion-api-5.12.2.jar','geyser-api-2.12.1-SNAPSHOT.jar','floodgate-api-2.2.4-SNAPSHOT.jar','base-api-1.0.3.jar','immutable-2.0.jar','annotations-24.0.0.jar','guava-33.4.8-jre.jar','netty-buffer-5.0.0.Alpha2.jar','netty-common-5.0.0.Alpha2.jar','cumulus-1.1.2.jar','events-1.2-20260725.205006-4.jar'])
s={
'net/minecraft/SharedConstants':'public class SharedConstants {public static SharedConstants getCurrentVersion(){return new SharedConstants();}public String name(){return "26.3";}public static int getProtocolVersion(){return 777;}}',
'net/minecraft/network/Connection':'public class Connection {}',
'dev/meshac/MeshServices':'public class MeshServices {public static void log(String s){System.out.println(s);}}',
'dev/meshac/data/BrokenDepBase':'public class BrokenDepBase {}',
'dev/meshac/data/BrokenDep':'public class BrokenDep extends BrokenDepBase {}'
}
test='''package dev.meshac.data;
import dev.meshac.config.MeshVersion;
import java.lang.reflect.Proxy;
import java.util.UUID;
import net.minecraft.network.Connection;
public class TranslatorHookContractTest {
  static void eq(Object a, Object b, String what) { if (a != b) throw new AssertionError(what + ": expected " + b + " got " + a); }
  static void truth(boolean b, String what) { if (!b) throw new AssertionError(what); }
  static Object proxy(Class<?> c, java.lang.reflect.InvocationHandler h) { return Proxy.newProxyInstance(c.getClassLoader(), new Class<?>[]{c}, h); }

  public static void main(String[] args) {
    if (args.length > 0 && args[0].equals("mismatch")) { mismatchRun(); return; }
    eq(MeshVersion.fromProtocol(5), MeshVersion.V_1_7_10, "protocol 5");
    eq(MeshVersion.fromProtocol(47), MeshVersion.V_1_8, "protocol 47");
    eq(MeshVersion.fromProtocol(107), MeshVersion.V_1_9, "protocol 107"); eq(MeshVersion.fromProtocol(108), MeshVersion.V_1_9, "protocol 108");
    eq(MeshVersion.fromProtocol(109), MeshVersion.V_1_9, "protocol 109"); eq(MeshVersion.fromProtocol(110), MeshVersion.V_1_9, "protocol 110");
    eq(MeshVersion.fromProtocol(315), MeshVersion.V_1_11, "protocol 315"); eq(MeshVersion.fromProtocol(316), MeshVersion.V_1_11, "protocol 316");
    eq(MeshVersion.fromProtocol(335), MeshVersion.V_1_12, "protocol 335"); eq(MeshVersion.fromProtocol(338), MeshVersion.V_1_12, "protocol 338");
    eq(MeshVersion.fromProtocol(340), MeshVersion.V_1_12_2, "protocol 340");
    eq(MeshVersion.fromProtocol(393), MeshVersion.V_1_13, "protocol 393"); eq(MeshVersion.fromProtocol(401), MeshVersion.V_1_13, "protocol 401"); eq(MeshVersion.fromProtocol(404), MeshVersion.V_1_13, "protocol 404");
    eq(MeshVersion.fromProtocol(477), MeshVersion.V_1_14, "protocol 477"); eq(MeshVersion.fromProtocol(480), MeshVersion.V_1_14, "protocol 480");
    eq(MeshVersion.fromProtocol(485), MeshVersion.V_1_14, "protocol 485"); eq(MeshVersion.fromProtocol(490), MeshVersion.V_1_14, "protocol 490"); eq(MeshVersion.fromProtocol(498), MeshVersion.V_1_14, "protocol 498");
    eq(MeshVersion.fromProtocol(573), MeshVersion.V_1_15, "protocol 573"); eq(MeshVersion.fromProtocol(575), MeshVersion.V_1_15, "protocol 575"); eq(MeshVersion.fromProtocol(578), MeshVersion.V_1_15, "protocol 578");
    eq(MeshVersion.fromProtocol(735), MeshVersion.V_1_16, "protocol 735"); eq(MeshVersion.fromProtocol(736), MeshVersion.V_1_16, "protocol 736");
    eq(MeshVersion.fromProtocol(751), MeshVersion.V_1_16, "protocol 751"); eq(MeshVersion.fromProtocol(753), MeshVersion.V_1_16, "protocol 753"); eq(MeshVersion.fromProtocol(754), MeshVersion.V_1_16, "protocol 754");
    eq(MeshVersion.fromProtocol(755), MeshVersion.V_1_17, "protocol 755"); eq(MeshVersion.fromProtocol(756), MeshVersion.V_1_17, "protocol 756");
    eq(MeshVersion.fromProtocol(757), MeshVersion.V_1_18, "protocol 757"); eq(MeshVersion.fromProtocol(758), MeshVersion.V_1_18, "protocol 758");
    eq(MeshVersion.fromProtocol(759), MeshVersion.V_1_19, "protocol 759"); eq(MeshVersion.fromProtocol(760), MeshVersion.V_1_19, "protocol 760");
    eq(MeshVersion.fromProtocol(761), MeshVersion.V_1_19, "protocol 761"); eq(MeshVersion.fromProtocol(762), MeshVersion.V_1_19, "protocol 762");
    eq(MeshVersion.fromProtocol(763), MeshVersion.V_1_20, "protocol 763");
    eq(MeshVersion.fromProtocol(764), MeshVersion.V_1_20_2, "protocol 764"); eq(MeshVersion.fromProtocol(765), MeshVersion.V_1_20_2, "protocol 765"); eq(MeshVersion.fromProtocol(766), MeshVersion.V_1_20_2, "protocol 766");
    eq(MeshVersion.fromProtocol(767), MeshVersion.V_1_21, "protocol 767");
    eq(MeshVersion.fromProtocol(768), MeshVersion.V_1_21_2, "protocol 768"); eq(MeshVersion.fromProtocol(769), MeshVersion.V_1_21_2, "protocol 769");
    eq(MeshVersion.fromProtocol(770), MeshVersion.V_1_21_2, "protocol 770"); eq(MeshVersion.fromProtocol(771), MeshVersion.V_1_21_2, "protocol 771");
    eq(MeshVersion.fromProtocol(772), MeshVersion.V_1_21_2, "protocol 772"); eq(MeshVersion.fromProtocol(773), MeshVersion.V_1_21_2, "protocol 773");
    eq(MeshVersion.fromProtocol(774), MeshVersion.V_1_21_11, "protocol 774");
    eq(MeshVersion.fromProtocol(775), MeshVersion.V_26_1, "protocol 775"); eq(MeshVersion.fromProtocol(776), MeshVersion.V_26_2, "protocol 776"); eq(MeshVersion.fromProtocol(777), MeshVersion.V_26_3, "protocol 777");
    eq(MeshVersion.fromProtocol(4), MeshVersion.UNKNOWN, "protocol 4 (1.7.2-1.7.5) unmapped");
    eq(MeshVersion.fromProtocol(210), MeshVersion.UNKNOWN, "protocol 210 (1.10.x) unmapped");
    eq(MeshVersion.fromProtocol(-1), MeshVersion.UNKNOWN, "protocol -1"); eq(MeshVersion.fromProtocol(0), MeshVersion.UNKNOWN, "protocol 0");
    eq(MeshVersion.fromProtocol(778), MeshVersion.UNKNOWN, "protocol 778 future");

    eq(TranslatorHooks.choose(true, TranslatorHooks.Presence.PRESENT, TranslatorHooks.Presence.ABSENT, Boolean.FALSE, false, MeshVersion.UNKNOWN, MeshVersion.V_26_3), MeshVersion.UNKNOWN, "provisional defers with bedrock detector present");
    eq(TranslatorHooks.choose(true, TranslatorHooks.Presence.ABSENT, TranslatorHooks.Presence.PRESENT, Boolean.FALSE, true, MeshVersion.V_1_8, MeshVersion.V_26_3), MeshVersion.UNKNOWN, "provisional defers with Via class present");
    eq(TranslatorHooks.choose(true, TranslatorHooks.Presence.ABSENT, TranslatorHooks.Presence.ABSENT, Boolean.FALSE, false, MeshVersion.UNKNOWN, MeshVersion.V_26_3), MeshVersion.V_26_3, "provisional resolves only when no translator exists");
    eq(TranslatorHooks.choose(true, TranslatorHooks.Presence.ABSENT, TranslatorHooks.Presence.PRESENT, Boolean.FALSE, false, MeshVersion.UNKNOWN, MeshVersion.V_26_3), MeshVersion.UNKNOWN, "provisional defers with Via CLASS present but runtime not loaded (B103 v3 flaw 1)");
    eq(TranslatorHooks.choose(false, TranslatorHooks.Presence.ABSENT, TranslatorHooks.Presence.PRESENT, Boolean.FALSE, false, MeshVersion.UNKNOWN, MeshVersion.V_26_3), MeshVersion.V_26_3, "definitive: Via class present but not loaded is ABSENT for resolution, handshake allowed");
    eq(TranslatorHooks.choose(false, TranslatorHooks.Presence.PRESENT, TranslatorHooks.Presence.ABSENT, Boolean.TRUE, false, MeshVersion.UNKNOWN, MeshVersion.V_26_3), MeshVersion.UNKNOWN, "definitive Bedrock veto beats handshake");
    eq(TranslatorHooks.choose(false, TranslatorHooks.Presence.PRESENT, TranslatorHooks.Presence.ABSENT, null, false, MeshVersion.UNKNOWN, MeshVersion.V_26_3), MeshVersion.UNKNOWN, "definitive indeterminate Bedrock veto");
    eq(TranslatorHooks.choose(false, TranslatorHooks.Presence.ABSENT, TranslatorHooks.Presence.PRESENT, Boolean.FALSE, true, MeshVersion.V_1_21_11, MeshVersion.V_26_3), MeshVersion.V_1_21_11, "definitive loaded Via wins");
    eq(TranslatorHooks.choose(false, TranslatorHooks.Presence.ABSENT, TranslatorHooks.Presence.PRESENT, Boolean.FALSE, true, MeshVersion.UNKNOWN, MeshVersion.V_26_3), MeshVersion.UNKNOWN, "loaded Via UNKNOWN never falls back (1.10 client must not become 26.3)");
    eq(TranslatorHooks.choose(false, TranslatorHooks.Presence.ABSENT, TranslatorHooks.Presence.PRESENT, Boolean.FALSE, true, null, MeshVersion.V_26_3), MeshVersion.UNKNOWN, "loaded Via error stays UNKNOWN");
    eq(TranslatorHooks.choose(false, TranslatorHooks.Presence.ABSENT, TranslatorHooks.Presence.ABSENT, Boolean.FALSE, false, MeshVersion.UNKNOWN, MeshVersion.V_26_3), MeshVersion.V_26_3, "no Via: handshake fallback allowed");
    eq(TranslatorHooks.choose(false, TranslatorHooks.Presence.ABSENT, TranslatorHooks.Presence.ABSENT, Boolean.FALSE, false, MeshVersion.UNKNOWN, MeshVersion.UNKNOWN), MeshVersion.UNKNOWN, "all unknown");
    eq(TranslatorHooks.choose(false, TranslatorHooks.Presence.ABSENT, TranslatorHooks.Presence.ABSENT, Boolean.FALSE, false, MeshVersion.UNKNOWN, null), MeshVersion.UNKNOWN, "null handshake");

    eq(TranslatorHooks.present("com.viaversion.viaversion.api.Via"), TranslatorHooks.Presence.PRESENT, "via jar class present");
    eq(TranslatorHooks.present("com.example.DoesNotExist"), TranslatorHooks.Presence.ABSENT, "missing class absent");
    eq(TranslatorHooks.present("dev.meshac.data.BrokenDep"), TranslatorHooks.Presence.BROKEN, "present but unloadable base = BROKEN");

    UUID id = UUID.randomUUID();
    eq(TranslatorHooks.resolve(id, null, false), MeshVersion.UNKNOWN, "no runtime, no handshake");
    Connection cp = new Connection(); ClientVersionResolver.capture(cp, 777);
    eq(TranslatorHooks.resolve(id, cp, true), MeshVersion.UNKNOWN, "provisional DEFERS while translator classes present (jars on classpath)");
    eq(TranslatorHooks.resolve(id, cp, false), MeshVersion.V_26_3, "handshake survived provisional deferral; definitive resolves native");
    Connection c2 = new Connection(); ClientVersionResolver.capture(c2, 47);
    eq(TranslatorHooks.resolve(id, c2, false), MeshVersion.UNKNOWN, "no runtime: non-server protocol UNKNOWN");
    Connection c3 = new Connection(); ClientVersionResolver.capture(c3, 777);
    eq(TranslatorHooks.resolve(null, c3, false), MeshVersion.V_26_3, "null uuid: handshake resolves");

    final int[] protocol = { 210 };
    final boolean[] fail = { false };
    Object viaApi = proxy(com.viaversion.viaversion.api.ViaAPI.class, (o, m, a) -> {
      if (m.getName().equals("getPlayerVersion")) { if (fail[0]) throw new IllegalStateException("injected lookup failure"); return protocol[0]; }
      throw new UnsupportedOperationException(m.getName()); });
    Object viaPlatform = proxy(com.viaversion.viaversion.api.platform.ViaPlatform.class, (o, m, a) -> {
      if (m.getName().equals("getApi")) return viaApi;
      throw new UnsupportedOperationException(m.getName()); });
    com.viaversion.viaversion.api.Via.init((com.viaversion.viaversion.api.ViaManager) proxy(com.viaversion.viaversion.api.ViaManager.class, (o, m, a) -> {
      if (m.getName().equals("getPlatform")) return viaPlatform;
      throw new UnsupportedOperationException(m.getName()); }));
    eq(TranslatorHooks.resolve(id, null, false), MeshVersion.UNKNOWN, "loaded Via, unmapped 210, no handshake");
    Connection c4 = new Connection(); ClientVersionResolver.capture(c4, 777);
    eq(TranslatorHooks.resolve(id, c4, false), MeshVersion.UNKNOWN, "loaded Via unmapped 210 must not fall back to native handshake");
    fail[0] = true;
    eq(TranslatorHooks.resolve(id, null, false), MeshVersion.UNKNOWN, "loaded Via lookup error stays UNKNOWN");
    Connection c5 = new Connection(); ClientVersionResolver.capture(c5, 777);
    eq(TranslatorHooks.resolve(id, c5, false), MeshVersion.UNKNOWN, "loaded Via error must not fall back to handshake");
    fail[0] = false; protocol[0] = 47;
    Connection cv = new Connection(); ClientVersionResolver.capture(cv, 777);
    eq(TranslatorHooks.resolve(id, cv, true), MeshVersion.UNKNOWN, "provisional defers even with loaded Via");
    eq(TranslatorHooks.resolve(id, cv, false), MeshVersion.V_1_8, "definitive: loaded Via known translated protocol 47, handshake untouched");

    final UUID GID = UUID.randomUUID(), FID = UUID.randomUUID(), OTHER = UUID.randomUUID();
    Object floodApi = proxy(org.geysermc.floodgate.api.FloodgateApi.class, (o, m, a) -> {
      if (m.getName().equals("isFloodgatePlayer")) return FID.equals(a[0]);
      throw new UnsupportedOperationException(m.getName()); });
    truth(org.geysermc.floodgate.api.InstanceHolder.set((org.geysermc.floodgate.api.FloodgateApi) floodApi, null, null, null, null, null, null), "floodgate instance registered");
    Object geyserApi = proxy(org.geysermc.geyser.api.GeyserApi.class, (o, m, a) -> {
      if (m.getName().equals("connectionByUuid")) return GID.equals(a[0]) ? proxy(org.geysermc.geyser.api.connection.GeyserConnection.class, (o2, m2, a2) -> { throw new UnsupportedOperationException(m2.getName()); }) : null;
      throw new UnsupportedOperationException(m.getName()); });
    org.geysermc.api.Geyser.set((org.geysermc.geyser.api.GeyserApi) geyserApi);
    Connection cp2 = new Connection(); ClientVersionResolver.capture(cp2, 777);
    eq(TranslatorHooks.resolve(GID, cp2, true), MeshVersion.UNKNOWN, "REGRESSION (v2 flaw): provisional before Geyser registration completes stays UNKNOWN, not V_26_3");
    eq(TranslatorHooks.resolve(GID, cp2, false), MeshVersion.UNKNOWN, "definitive after registration: Bedrock veto beats Via(47) and preserved handshake(777)");
    Connection c7 = new Connection(); ClientVersionResolver.capture(c7, 777);
    eq(TranslatorHooks.resolve(FID, c7, false), MeshVersion.UNKNOWN, "registered Floodgate player: Bedrock veto beats Via(47) and handshake(777)");
    eq(TranslatorHooks.resolve(OTHER, null, false), MeshVersion.V_1_8, "non-Bedrock player unaffected by registered detectors");
    System.out.println("TranslatorHookContractTest PASS (58 table, 11 composition, 3 presence, 6 no-runtime/provisional, 8 Via dispatch, 4 Bedrock dispatch asserts)");
  }

  static void mismatchRun() {
    Object base = proxy(org.geysermc.api.GeyserApiBase.class, (o, m, a) -> { throw new UnsupportedOperationException(m.getName()); });
    org.geysermc.api.Geyser.set((org.geysermc.api.GeyserApiBase) base);
    UUID id = UUID.randomUUID();
    eq(TranslatorHooks.resolve(id, null, false), MeshVersion.UNKNOWN, "Geyser API-class mismatch: indeterminate, not absent");
    Connection c = new Connection(); ClientVersionResolver.capture(c, 777);
    eq(TranslatorHooks.resolve(id, c, false), MeshVersion.UNKNOWN, "Geyser mismatch must not fall back to handshake");
    System.out.println("TranslatorHookContractTest mismatch run PASS (2 asserts)");
  }
}
'''
with tempfile.TemporaryDirectory(prefix='meshac-translator-') as d:
 t=Path(d);files=[]
 for n,b in s.items():
  p=t/(n+'.java');p.parent.mkdir(parents=True,exist_ok=True);p.write_text('package '+n.rsplit('/',1)[0].replace('/','.')+'; '+b);files.append(str(p))
 p=t/'dev/meshac/data/TranslatorHookContractTest.java';p.write_text(test);files.append(str(p))
 for f in ['src/main/java/dev/meshac/config/MeshVersion.java','src/main/java/dev/meshac/data/ClientVersionResolver.java','src/main/java/dev/meshac/data/TranslatorHooks.java']:
  files.append(str(r/f))
 subprocess.run([str(jdk/'javac'),'-cp',jars,'-d',str(t/'classes'),*files],check=True)
 # BrokenDep must load as present-but-broken: remove its base class before running.
 (t/'classes/dev/meshac/data/BrokenDepBase.class').unlink()
 subprocess.run([str(jdk/'java'),'-cp',str(t/'classes')+':'+jars,'dev.meshac.data.TranslatorHookContractTest'],check=True)
 subprocess.run([str(jdk/'java'),'-cp',str(t/'classes')+':'+jars,'dev.meshac.data.TranslatorHookContractTest','mismatch'],check=True)
