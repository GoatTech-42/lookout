package dev.meshac.compat;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.EntityType;
import dev.meshac.tracker.TrackedEntity;
import dev.meshac.mixin.ClientboundMoveEntityPacketAccessor;
/** Compile-time adapter for Minecraft 26.1.2. No check thresholds or eligibility changes. */
public final class VersionBridge {
 private VersionBridge() {}
 public static BlockHitResult hit(ServerboundUseItemOnPacket p) { return p.getHitResult(); }
 public static InteractionHand hand(ServerboundUseItemOnPacket p) { return p.getHand(); }
 public static int sequence(ServerboundUseItemOnPacket p) { return p.getSequence(); }
 public static Vec3 vehiclePosition(ServerboundMoveVehiclePacket p) { return p.position(); }
 public static int chunkX(ClientboundLevelChunkWithLightPacket p) { return p.getX(); }
 public static int chunkZ(ClientboundLevelChunkWithLightPacket p) { return p.getZ(); }
 public static int[] removedIds(ClientboundRemoveEntitiesPacket p) { return p.getEntityIds().toIntArray(); }
 public static Vec3 relativeDelta(ClientboundMoveEntityPacketAccessor acc, TrackedEntity tracked) {
  return new Vec3(acc.meshac$getXa()/4096.0, acc.meshac$getYa()/4096.0, acc.meshac$getZa()/4096.0);
 }
 public static boolean wetSulfur(BlockState state) {
  return false; // This server version has no potent sulfur block (verified target jar).
 }
 public static ClientboundLevelChunkPacketData chunkData(ClientboundLevelChunkWithLightPacket packet) { return packet.getChunkData(); }
 public static void blockEntityPositions(ClientboundLevelChunkPacketData payload, int chunkX, int chunkZ, java.util.function.Consumer<net.minecraft.core.BlockPos> out) {
  payload.getBlockEntitiesTagsConsumer(chunkX, chunkZ).accept((pos, type, tag) -> out.accept(pos.immutable()));
 }
 public static long chunkKey(net.minecraft.world.level.ChunkPos pos) { return pos.pack(); }
 public static int motionEntity(ClientboundSetEntityMotionPacket p) { return p.id(); }
 public static Vec3 motion(ClientboundSetEntityMotionPacket p) { return p.movement(); }
 public static dev.meshac.packet.InteractEntity interact(ServerboundInteractPacket p) {
  var action = p.location() == null ? dev.meshac.packet.InteractEntity.Action.INTERACT : dev.meshac.packet.InteractEntity.Action.INTERACT_AT;
  return new dev.meshac.packet.InteractEntity(p.entityId(), action, p.hand(), p.location(), p.usingSecondaryAction());
 }
 public static final EntityType<?> ENDER_DRAGON = net.minecraft.world.entity.EntityType.ENDER_DRAGON;
 public static final EntityType<?> FIREBALL = net.minecraft.world.entity.EntityType.FIREBALL;
 public static final EntityType<?> SMALL_FIREBALL = net.minecraft.world.entity.EntityType.SMALL_FIREBALL;
 public static final EntityType<?> DRAGON_FIREBALL = net.minecraft.world.entity.EntityType.DRAGON_FIREBALL;
 public static final EntityType<?> END_CRYSTAL = net.minecraft.world.entity.EntityType.END_CRYSTAL;
 public static final EntityType<?> RAVAGER = net.minecraft.world.entity.EntityType.RAVAGER;
 public static final EntityType<?> SHULKER = net.minecraft.world.entity.EntityType.SHULKER;
 public static final EntityType<?> HAPPY_GHAST = net.minecraft.world.entity.EntityType.HAPPY_GHAST;
 public static final EntityType<?> PLAYER = net.minecraft.world.entity.EntityType.PLAYER;
 public static boolean isBOUNCINESS(Object attribute) { return false; /* Native attribute absent in this target. */ }
 public static boolean isAIR_DRAG_MODIFIER(Object attribute) { return false; /* Native attribute absent in this target. */ }
 public static boolean isFRICTION_MODIFIER(Object attribute) { return false; /* Native attribute absent in this target. */ }
}
