package test.meshac;
import dev.meshac.data.PlayerDataManager;
import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
public final class AnchorTrace {
 public static void record(Connection connection,String stage,int id) {
  if (!(connection.getPacketListener() instanceof ServerGamePacketListenerImpl game)) {
   System.out.println("[ANCHOR-OBSERVER] "+stage+" listenerGame=false id="+id);return;
  }
  var d=PlayerDataManager.get(game.player);
  if(d==null){System.out.println("[ANCHOR-OBSERVER] "+stage+" listenerGame=true NO_SESSION id="+id);return;}
  if(stage.startsWith("PING")&&id!=d.getConfirmationTracker().getJoinConfirmationId())return;
  boolean same=((dev.meshac.mixin.ServerCommonPacketListenerImplAccessor)game).meshac$getConnection()==connection;
  System.out.println("[ANCHOR-OBSERVER] "+stage+" id="+id+" listenerGame=true listenerConnectionSame="+same+" playerRefSame="+(d.getPlayer()==game.player)+" nativeId="+game.player.getId()+" storedId="+d.getEntityId()+" joinID="+d.getConfirmationTracker().getJoinConfirmationId()+" handshake="+d.isHandshakeComplete()+" pending="+d.getConfirmationTracker().getPendingConfirmations().size()+" thread="+Thread.currentThread().getName());
 }
}
