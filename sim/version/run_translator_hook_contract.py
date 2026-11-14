#!/usr/bin/env python3
"""Actual TranslatorHooks + MeshVersion + ClientVersionResolver against the real Via/Geyser/Floodgate
API jars (sim/version/lib). v5 contract A/B/C: KNOWN never depends on translator readiness.
A: any local Bedrock API class present/broken => always UNKNOWN (even with positive Via or a clean
handshake). B: Via-class-only => loaded positive known protocol or UNKNOWN, never the handshake.
C: translator-free => handshake protocol observation. Asserts the pinned fromProtocol table, the
choose() A/B/C table, the presence tri-state (incl. present-but-broken class), and resolve()
composition on the all-vendor classpath (always UNKNOWN, handshake not even consumed)."""
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
import java.util.UUID;
import net.minecraft.network.Connection;
public class TranslatorHookContractTest {
  static void eq(Object a, Object b, String what) { if (a != b) throw new AssertionError(what + ": expected " + b + " got " + a); }
  static void truth(boolean b, String what) { if (!b) throw new AssertionError(what); }
  static final TranslatorHooks.Presence A = TranslatorHooks.Presence.ABSENT;
  static final TranslatorHooks.Presence P = TranslatorHooks.Presence.PRESENT;
  static final TranslatorHooks.Presence B = TranslatorHooks.Presence.BROKEN;

  public static void main(String[] args) {
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
    eq(MeshVersion.fromProtocol(-1), MeshVersion.UNKNOWN, "protocol -1 (Via not-connected answer)");
    eq(MeshVersion.fromProtocol(0), MeshVersion.UNKNOWN, "protocol 0");

    // A: any local Bedrock API class present/broken => ALWAYS UNKNOWN, whatever Via or the handshake say.
    eq(TranslatorHooks.choose(P, A, false, MeshVersion.UNKNOWN, MeshVersion.V_26_3), MeshVersion.UNKNOWN, "A: bedrock present vetoes clean handshake");
    eq(TranslatorHooks.choose(P, P, true, MeshVersion.V_1_8, MeshVersion.V_26_3), MeshVersion.UNKNOWN, "A: bedrock present vetoes positive Via 47 (indeterminate-positive-Via case)");
    eq(TranslatorHooks.choose(P, P, true, MeshVersion.V_26_3, null), MeshVersion.UNKNOWN, "A: bedrock present vetoes positive Via server-protocol");
    eq(TranslatorHooks.choose(B, A, true, MeshVersion.V_1_8, MeshVersion.V_26_3), MeshVersion.UNKNOWN, "A: broken bedrock class vetoes too");
    eq(TranslatorHooks.choose(B, P, true, MeshVersion.V_1_8, null), MeshVersion.UNKNOWN, "A: broken bedrock + via present");
    // B: Via-class-only => loaded positive known protocol or UNKNOWN; NEVER the handshake.
    eq(TranslatorHooks.choose(A, P, true, MeshVersion.V_1_8, MeshVersion.V_26_3), MeshVersion.V_1_8, "B: via-only loaded positive 47");
    eq(TranslatorHooks.choose(A, P, true, MeshVersion.V_26_3, MeshVersion.V_26_3), MeshVersion.V_26_3, "B: via-only positive server protocol");
    eq(TranslatorHooks.choose(A, P, true, MeshVersion.UNKNOWN, MeshVersion.V_26_3), MeshVersion.UNKNOWN, "B: via-only unmapped/-1 stays UNKNOWN");
    eq(TranslatorHooks.choose(A, P, true, null, MeshVersion.V_26_3), MeshVersion.UNKNOWN, "B: via-only lookup error stays UNKNOWN");
    eq(TranslatorHooks.choose(A, P, false, MeshVersion.UNKNOWN, MeshVersion.V_26_3), MeshVersion.UNKNOWN, "B: via-only NOT loaded: no handshake fallback");
    eq(TranslatorHooks.choose(A, B, false, MeshVersion.UNKNOWN, MeshVersion.V_26_3), MeshVersion.UNKNOWN, "B: via class broken: no handshake");
    // C: translator-free => handshake protocol observation.
    eq(TranslatorHooks.choose(A, A, false, MeshVersion.UNKNOWN, MeshVersion.V_26_3), MeshVersion.V_26_3, "C: translator-free native handshake");
    eq(TranslatorHooks.choose(A, A, false, MeshVersion.UNKNOWN, MeshVersion.UNKNOWN), MeshVersion.UNKNOWN, "C: unknown handshake");
    eq(TranslatorHooks.choose(A, A, false, MeshVersion.UNKNOWN, null), MeshVersion.UNKNOWN, "C: null handshake");

    truth(TranslatorHooks.present("dev.meshac.data.BrokenDep") == B, "present-but-broken class is BROKEN");
    truth(TranslatorHooks.present("dev.meshac.data.NoSuchClass") == A, "missing class is ABSENT");
    truth(TranslatorHooks.present("com.viaversion.viaversion.api.Via") == P, "Via class is PRESENT on this classpath");

    // Composition on the all-vendor classpath: case A - always UNKNOWN, handshake NOT even consumed.
    Connection cp = new Connection();
    ClientVersionResolver.capture(cp, 777);
    eq(TranslatorHooks.resolve(UUID.randomUUID(), cp), MeshVersion.UNKNOWN, "A composition: all-vendor classpath always UNKNOWN");
    eq(ClientVersionResolver.resolve(cp), MeshVersion.V_26_3, "A composition: handshake never consumed under A");
    eq(TranslatorHooks.resolve(null, null), MeshVersion.UNKNOWN, "null player/connection stays UNKNOWN");
    Connection cn = new Connection();
    ClientVersionResolver.capture(cn, 777);
    eq(TranslatorHooks.resolve(null, cn), MeshVersion.UNKNOWN, "null playerId never resolves, even with a clean native handshake (guard)");
    eq(ClientVersionResolver.resolve(cn), MeshVersion.V_26_3, "null-guard path does not consume the handshake");
    System.out.println("TranslatorHookContractTest PASS (56 fromProtocol, 14 A/B/C table, 3 presence, 6 composition asserts)");
  }
}
'''
with tempfile.TemporaryDirectory(prefix='meshac-hook-contract-') as d:
 t=Path(d);files=[]
 for n,b in s.items():
  p=t/(n+'.java');p.parent.mkdir(parents=True,exist_ok=True);p.write_text('package '+n.rsplit('/',1)[0].replace('/','.')+'; '+b);files.append(str(p))
 p=t/'dev/meshac/data/TranslatorHookContractTest.java';p.write_text(test);files.append(str(p))
 for f in ['src/main/java/dev/meshac/config/MeshVersion.java','src/main/java/dev/meshac/data/ClientVersionResolver.java','src/main/java/dev/meshac/data/TranslatorHooks.java']:
  files.append(str(r/f))
 # BrokenDep must fail to LOAD at runtime: compile against BrokenDepBase, then delete BrokenDepBase.class.
 subprocess.run([str(jdk/'javac'),'-cp',jars,'-d',str(t/'classes'),*files],check=True)
 (t/'classes/dev/meshac/data/BrokenDepBase.class').unlink()
 subprocess.run([str(jdk/'java'),'-cp',str(t/'classes')+':'+jars,'dev.meshac.data.TranslatorHookContractTest'],check=True)
