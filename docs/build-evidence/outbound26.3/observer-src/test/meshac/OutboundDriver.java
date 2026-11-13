package test.meshac;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.phys.Vec3;
public class OutboundDriver implements DedicatedServerModInitializer {
 int ready;boolean done;
 public void onInitializeServer(){ServerTickEvents.END_SERVER_TICK.register(server->{if(done||server.getPlayerList().getPlayers().isEmpty())return;var p=server.getPlayerList().getPlayers().getFirst();var data=dev.meshac.data.PlayerDataManager.get(p);if(data==null||!data.isHandshakeComplete())return;if(++ready<80)return;done=true;
  System.out.println("[OUTBOUND-DRIVER] deliberate disposable packet scenario begins");
  p.connection.send(new ClientboundSetEntityMotionPacket(p.getId(),new Vec3(0.01,0.02,0.03)));
  p.connection.send(new ClientboundSetEntityMotionPacket(p.getId(),new Vec3(0.02,0.03,0.04)),future->System.out.println("[OUTBOUND-DRIVER] native caller completion success="+future.isSuccess()));
  p.connection.send(new ClientboundBundlePacket(java.util.List.of(new ClientboundSetEntityMotionPacket(p.getId(),new Vec3(0.03,0.04,0.05)))));
  p.connection.send(new ClientboundExplodePacket(p.position(),0,0,java.util.Optional.of(new Vec3(0.01,0.01,0.01)),net.minecraft.core.particles.ParticleTypes.EXPLOSION,net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,net.minecraft.util.random.WeightedList.of(),false));
  System.out.println("[OUTBOUND-DRIVER] deliberate scenario queued");
 });}
}