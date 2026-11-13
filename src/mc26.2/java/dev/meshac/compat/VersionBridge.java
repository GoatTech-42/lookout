package dev.meshac.compat;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.EntityType;
import dev.meshac.tracker.TrackedEntity;
import dev.meshac.mixin.ClientboundMoveEntityPacketAccessor;
/** Compile-time adapter for Minecraft 26.2. No check thresholds or eligibility changes. */
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
  return state != null && state.getBlock() instanceof net.minecraft.world.level.block.PotentSulfurBlock
   && state.getValue(net.minecraft.world.level.block.PotentSulfurBlock.STATE) != net.minecraft.world.level.block.state.properties.PotentSulfurState.DRY;
 }
 public static long chunkKey(net.minecraft.world.level.ChunkPos pos) { return pos.pack(); }
 public static int motionEntity(ClientboundSetEntityMotionPacket p) { return p.id(); }
 public static Vec3 motion(ClientboundSetEntityMotionPacket p) { return p.movement(); }
 public static dev.meshac.packet.InteractEntity interact(ServerboundInteractPacket p) {
  var action = p.location() == null ? dev.meshac.packet.InteractEntity.Action.INTERACT : dev.meshac.packet.InteractEntity.Action.INTERACT_AT;
  return new dev.meshac.packet.InteractEntity(p.entityId(), action, p.hand(), p.location(), p.usingSecondaryAction());
 }
 public static final EntityType<?> ENDER_DRAGON = net.minecraft.world.entity.EntityTypes.ENDER_DRAGON;
 public static final EntityType<?> FIREBALL = net.minecraft.world.entity.EntityTypes.FIREBALL;
 public static final EntityType<?> SMALL_FIREBALL = net.minecraft.world.entity.EntityTypes.SMALL_FIREBALL;
 public static final EntityType<?> DRAGON_FIREBALL = net.minecraft.world.entity.EntityTypes.DRAGON_FIREBALL;
 public static final EntityType<?> END_CRYSTAL = net.minecraft.world.entity.EntityTypes.END_CRYSTAL;
 public static final EntityType<?> RAVAGER = net.minecraft.world.entity.EntityTypes.RAVAGER;
 public static final EntityType<?> SHULKER = net.minecraft.world.entity.EntityTypes.SHULKER;
 public static final EntityType<?> HAPPY_GHAST = net.minecraft.world.entity.EntityTypes.HAPPY_GHAST;
 public static final EntityType<?> PLAYER = net.minecraft.world.entity.EntityTypes.PLAYER;
 public static boolean isBOUNCINESS(Object attribute) { return attribute == net.minecraft.world.entity.ai.attributes.Attributes.BOUNCINESS; }
 public static boolean isAIR_DRAG_MODIFIER(Object attribute) { return attribute == net.minecraft.world.entity.ai.attributes.Attributes.AIR_DRAG_MODIFIER; }
 public static boolean isFRICTION_MODIFIER(Object attribute) { return attribute == net.minecraft.world.entity.ai.attributes.Attributes.FRICTION_MODIFIER; }
}
