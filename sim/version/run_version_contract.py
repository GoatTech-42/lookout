#!/usr/bin/env python3
"""Actual MeshVersion + ExemptionManager against explicit native version/service boundaries."""
from pathlib import Path
import tempfile,subprocess
r=Path(__file__).resolve().parents[2];jdk=Path.home()/'jdk25/bin'
s={
'net/minecraft/SharedConstants':'public class SharedConstants {public static SharedConstants getCurrentVersion(){return new SharedConstants();}public String name(){return "26.1.2";}}',
'dev/meshac/MeshServices':'public class MeshServices {public static double tps(){return 20;}public static void log(String s){}public static void broadcastToOps(Object o){}}',
'dev/meshac/config/MeshSettings':'public class MeshSettings {public static boolean settingsDebug;}',
'dev/meshac/util/TextUtil':'public class TextUtil {public static Object parse(String s){return s;}}'
}
test='''import dev.meshac.config.MeshVersion;import dev.meshac.check.ExemptionManager;
public class VersionContractTest {public static void main(String[] args){
if(ExemptionManager.getServerVersion()!=MeshVersion.V_26_1)throw new AssertionError("actual native server version never initialized");
if(!ExemptionManager.isAtLeast26_1()||!ExemptionManager.isAtLeast1_21_2())throw new AssertionError("known native server predicates wrong");
if(MeshVersion.UNKNOWN.isNewerThan(MeshVersion.V_1_7_10)||MeshVersion.UNKNOWN.isNewerThanOrEquals(MeshVersion.V_26_3))throw new AssertionError("UNKNOWN client compared as newest");
if(!MeshVersion.UNKNOWN.isOlderThan(MeshVersion.V_1_7_10)||!MeshVersion.UNKNOWN.isOlderThanOrEquals(MeshVersion.V_26_3))throw new AssertionError("UNKNOWN client must preserve protocol -1 ordering");
if(!MeshVersion.V_26_2.isNewerThan(MeshVersion.V_26_1)||!MeshVersion.V_1_21_11.isOlderThan(MeshVersion.V_26_1))throw new AssertionError("known chronology changed");
System.out.println("VersionContractTest PASS (native initialization, UNKNOWN -1, known chronology)");}}'''
with tempfile.TemporaryDirectory(prefix='meshac-version-') as d:
 t=Path(d);files=[]
 for n,b in s.items():
  p=t/(n+'.java');p.parent.mkdir(parents=True,exist_ok=True);p.write_text('package '+n.rsplit('/',1)[0].replace('/','.')+'; '+b);files.append(str(p))
 p=t/'VersionContractTest.java';p.write_text(test);files.extend([str(p),str(r/'src/main/java/dev/meshac/config/MeshVersion.java'),str(r/'src/main/java/dev/meshac/check/ExemptionManager.java')]);subprocess.run([str(jdk/'javac'),'-d',str(t/'classes'),*files],check=True);subprocess.run([str(jdk/'java'),'-cp',str(t/'classes'),'VersionContractTest'],check=True)
