import com.mojang.brigadier.CommandDispatcher;
import dev.meshac.product.*;
import dev.meshac.safety.Containment;
import net.minecraft.commands.CommandSourceStack;
import java.nio.file.*;
import java.util.*;

/** Real registered commands and case persistence; Minecraft connection/config-dir are boundary doubles. */
public class ProductSafetyTest {
 static int failures;
 static void check(boolean ok,String msg){if(!ok){failures++;System.err.println("FAIL: "+msg);}}
 public static void main(String[] args)throws Exception {
  check(Containment.mode()==Containment.Mode.LOG_ONLY,"fresh default must contain effects");
  var d=new CommandDispatcher<CommandSourceStack>(); MeshCommands.register(d);
  var s=new CommandSourceStack(); var id=s.player.getUUID();
  var cf=Config.get(); cf.monitorOnly=false; cf.egregiousChecks.add("fly");
  // Seed only in isolated ENFORCE fixture, then prove LOG_ONLY cannot change inherited bans.
  Containment.setMode(Containment.Mode.ENFORCE);
  var seed=Cases.add("fixture",id,"ban","seed","meshac",0,List.of("fixture"));
  Path store=Path.of(System.getProperty("fixture.config"),"meshac-cases.json");
  String before=Files.readString(store); double offences=Cases.offences(id);
  Containment.setMode(Containment.Mode.LOG_ONLY);
  for(String command:List.of("mesh kick fixture test","mesh ban fixture 1m test","mesh pardon "+seed.id,"mesh unban fixture")) {
   s.messages.clear(); d.execute(command,s);
   check(Files.readString(store).equals(before),command+" changed persistent store");
   check(Cases.activeBan(id)==seed && !seed.pardoned && !seed.lifted,command+" changed active ban");
   check(Cases.offences(id)<=offences,command+" changed escalation");
   check(s.player.connection.disconnects==0,command+" disconnected");
   check(s.messages.stream().anyMatch(m->m.contains("[log-only]")),command+" did not identify simulation");
  }
  Cases.add("fixture",id,"ban","direct-call","meshac",0,List.of("test"));
  Cases.pardon(seed.id); Cases.unban("fixture");
  check(Files.readString(store).equals(before) && !seed.pardoned && !seed.lifted,"direct case mutation bypassed log-only");
  // Nonblank inherited webhook must be blocked before URI/request construction, even for direct calls.
  cf.discordWebhook="invalid-fixture:must-not-build-an-http-request";
  try {Discord.post(seed);Discord.hold("fixture","fly","test");}catch(Exception e){check(false,"LOG_ONLY webhook escaped: "+e.getClass().getSimpleName());}
  cf.veilXray=true; cf.veilEsp=true; Config.applyVeil();
  check(!dev.meshac.veil.Veil.xrayOn && !dev.meshac.veil.Veil.espOn,"inherited config enabled unqualified veil");
  cf.holdAt=0.5; cf.removeAt=1.0; cf.requireClusters=1; cf.requireClustersTierA=1;
  String verdictBefore=Files.readString(store);
  var step=Verdict.signal(s.player,"fly","forced isolated REMOVE",3);
  check(step==Verdict.Step.REMOVE,"fixture did not reach real REMOVE branch");
  check(Files.readString(store).equals(verdictBefore),"Verdict REMOVE persisted simulated ban");
  check(s.player.connection.disconnects==0,"Verdict REMOVE escaped disconnect containment");
  // A rejoin's persistent evidence remains the seeded ban only; no simulated offence exists.
  Cases.load(); check(Cases.recent(20).size()==1,"reload exposed simulated punishment");
  cf.discordWebhook="";
  Containment.setMode(Containment.Mode.ENFORCE);
  d.execute("mesh kick fixture enforce-fixture",s);
  check(s.player.connection.disconnects==1,"isolated ENFORCE kick did not execute");
  check(Cases.recent(20).size()==2,"ENFORCE kick did not persist exactly once");
  d.execute("mesh pardon "+seed.id,s); check(Cases.find(seed.id).pardoned,"ENFORCE pardon unreachable");
  Containment.setMode(Containment.Mode.LOG_ONLY);
  if(failures>0)throw new AssertionError(failures+" product safety failures");
  System.out.println("ProductSafetyTest PASS (registered kick/ban/pardon/unban; persistence, escalation, disconnect, egress, inherited veil)");
 }
}
