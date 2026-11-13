import dev.meshac.tracker.WorldBlockTracker;
import dev.meshac.data.PlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import java.util.*;
public class WorldSnapshotTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError("FAIL: "+s);}
 static void packet(WorldBlockTracker t,ClientboundLevelChunkWithLightPacket p)throws Exception {
  java.lang.reflect.Method m;
  try{m=WorldBlockTracker.class.getMethod("handleChunkPacket",ServerLevel.class,ClientboundLevelChunkWithLightPacket.class);}
  catch(NoSuchMethodException e){throw new AssertionError("FAIL: missing exact packet-payload snapshot entrypoint");}
  m.invoke(t,new ServerLevel(),p);
 }
 public static void main(String[] args)throws Exception {
  var data=new PlayerData();var tracker=new WorldBlockTracker(data);
  var position=new BlockPos(-1,-64,-1);
  var wet=new ArrayList<BlockPos>();wet.add(position);
  var tiles=new ArrayList<BlockPos>();tiles.add(position);
  var p=new ClientboundLevelChunkWithLightPacket(-1,-1,wet,tiles);
  packet(tracker,p);
  var secondData=new PlayerData();var secondTracker=new WorldBlockTracker(secondData);
  packet(secondTracker,p); // The same immutable outbound packet may be sent to more than one recipient.
  check(!tracker.hasTrackedBlockNearby(-0.5,-63,-0.5),"state applied before acknowledgement");
  wet.clear();tiles.clear(); // change the source after capture; no global metadata or live world may be consulted later.
  secondData.confirmation.ack(); data.confirmation.ack();
  check(secondTracker.hasTrackedBlockNearby(-0.5,-63,-0.5),"second recipient lost shared packet state");
  check(tracker.hasTrackedBlockNearby(-0.5,-63,-0.5),"packet A lost after packet/live-source changed");
  check(!tracker.hasTrackedBlockNearby(3.0,-63,-0.5),"horizontal radius includes 3.5 boundary");
  check(tracker.hasTrackedBlockNearby(2.999,-63,-0.5),"horizontal interior incorrectly excluded");
  check(tracker.hasTrackedBlockNearby(-0.5,-63.5,-0.5),"vertical lower boundary must include zero");
  check(!tracker.hasTrackedBlockNearby(-0.5,-63.6,-0.5),"position below block center included");
  check(!tracker.hasTrackedBlockNearby(-0.5,-43.5,-0.5),"vertical 20 boundary included");
  // Updates must be possible after a frozen chunk, not fail on an unmodifiable Set.
  tracker.handleBlockUpdate(new BlockPos(-1,-64,-1),new net.minecraft.world.level.block.state.BlockState(false));data.confirmation.ack();
  check(!tracker.hasTrackedBlockNearby(-0.5,-63,-0.5),"block removal did not clear frozen chunk state");
  // Upstream's initial chunk scan only considers emitted tile entities.
  packet(tracker,new ClientboundLevelChunkWithLightPacket(0,0,List.of(new BlockPos(0,-64,0)),List.of()));data.confirmation.ack();
  check(!tracker.hasTrackedBlockNearby(0.5,-63,0.5),"non-tile section state was treated as an emitted tile entity");
  // Multi-update consumers may reuse one mutable position. Copy each before deferring.
  var pos=new BlockPos(-17,-64,-17);
  tracker.handleSectionUpdates(out->{out.accept(pos,new net.minecraft.world.level.block.state.BlockState(true));pos.x=123;pos.z=123;});
  data.confirmation.ack();check(tracker.hasTrackedBlockNearby(-16.5,-63,-16.5),"mutable update position not frozen");
  // A reset must prevent pending old-dimension callbacks from reviving old state.
  tracker.handleBlockUpdate(new BlockPos(0,-64,0),new net.minecraft.world.level.block.state.BlockState(true));tracker.reset();data.confirmation.ack();
  check(!tracker.hasTrackedBlockNearby(0.5,-63,0.5),"old generation revived after reset");
  System.out.println("WorldSnapshotTest PASS (payload freeze, negative coordinates, updates, reset generation)");
 }
}
