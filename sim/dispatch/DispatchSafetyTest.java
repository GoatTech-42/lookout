import dev.meshac.check.*;
import dev.meshac.data.*;
import dev.meshac.packet.*;
import dev.meshac.event.*;
import net.minecraft.server.level.ServerPlayer;
public class DispatchSafetyTest {
 public static class SpeedA extends AbstractCheck {}
 public static class UnknownFixture extends AbstractCheck {}
 public static class JumpD extends AbstractCheck {}
 public static void main(String[] args) {
  for(String route:new String[]{"inbound-packet","event-dispatch","placement-packet","digging-and-event","outbound-position","movement-event"}) {
   var data=new PlayerData(); PlayerDataManager.current=data;
   var world=new SpeedA();var unknown=new UnknownFixture();var safe=new JumpD();
   data.checks.add(world);data.checks.add(unknown);data.checks.add(safe);
   var player=new ServerPlayer();
   switch(route) {
    case "inbound-packet" -> PacketDispatcher.dispatchChecks(data,new MeshPacketEvent(MeshPacketType.ANIMATION,player,null,true));
    case "event-dispatch" -> PacketDispatcher.dispatchEvents(data,new MeshInteractEvent());
    case "placement-packet" -> PacketDispatcher.onPlacement(player,new net.minecraft.network.protocol.game.ServerboundUseItemOnPacket());
    case "digging-and-event" -> PacketDispatcher.onDigging(player,new net.minecraft.network.protocol.game.ServerboundPlayerActionPacket());
    case "outbound-position" -> OutboundDispatcher.onSend(player,new net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket());
    case "movement-event" -> MoveEventDispatcher.fireMove(player,0,0,0);
   }
   if(world.handlers!=0||unknown.handlers!=0||world.vl!=0||world.alerts!=0||world.heat!=0||world.effects!=0)throw new AssertionError("FAIL gated check evaluated: "+route);
   int expected=route.equals("digging-and-event")?2:1;
   if(safe.handlers!=expected)throw new AssertionError("FAIL safe control did not execute expected times: "+route+" got "+safe.handlers);
   System.out.println("PASS: "+route+" world/unknown suppressed, eligible control executed");
  }
 }
}
