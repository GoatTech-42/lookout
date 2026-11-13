package dev.meshac.check;

import dev.meshac.MeshServices;
import dev.meshac.data.PlayerData;
import dev.meshac.player.MotionState;
import dev.meshac.util.MathUtils;
import dev.meshac.world.BlockUtils;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

/**
 * Shared check helpers. Ported from Vulcan's CheckUtils; potion/enchant/inventory calls became
 * 26.3 equivalents, the Bukkit BlockPlaceEvent became PlacementContext.
 */
public final class CheckUtils {

	/** What the dispatcher knows about a block placement (replaces Bukkit's BlockPlaceEvent). */
	public record PlacementContext(ServerPlayer player, BlockState placed, BlockPos placedPos, BlockPos againstPos, Direction face) {
	}

	private CheckUtils() {
	}

	public static int getPing(ServerPlayer player) {
		return player.connection.latency();
	}

	public static boolean isAtLeast1_17(PlayerData data) {
		return data.getClientVersion().isNewerThanOrEquals(dev.meshac.config.MeshVersion.V_1_17);
	}

	public static boolean isNewerThan1_9(PlayerData playerData) {
		return playerData.getClientVersion().isNewerThan(dev.meshac.config.MeshVersion.V_1_9);
	}

	public static boolean isAtLeast1_13(PlayerData playerData) {
		return playerData.getClientVersion().isNewerThanOrEquals(dev.meshac.config.MeshVersion.V_1_13);
	}

	public static void sendPingPacket(PlayerData playerData, int index) {
		ServerPlayer player = playerData.getPlayer();
		if (player != null) player.connection.send(new net.minecraft.network.protocol.game.ClientboundPingPacket(index));
	}

	public static boolean isSwimming(PlayerData data) {
		return data.getPlayer() != null && ExemptionManager.isNewerThan1_13() && data.getPlayer().isSwimming();
	}

	public static boolean isRiptiding(PlayerData playerData) {
		return playerData.getPlayer() != null && ExemptionManager.isNewerThan1_13() && playerData.getPlayer().isAutoSpinAttack();
	}

	public static boolean isGliding(PlayerData playerData) {
		return playerData.getPlayer() != null && ExemptionManager.isAtLeast1_9() && playerData.getPlayer().isFallFlying();
	}

	public static boolean isEnclosedInSolidBlocks(PlayerData playerData) {
		List<BlockState> m = playerData.getCollisionState().getSurroundingMaterials();
		return m != null && !m.isEmpty() && m.get(16).isSolid() && m.get(19).isSolid();
	}

	public static boolean isEnclosedInSolidBlocksLegacy(PlayerData playerData) {
		List<BlockState> m = playerData.getCollisionState().getSurroundingMaterialsLegacy();
		return m != null && !m.isEmpty() && m.get(16).isSolid() && m.get(19).isSolid();
	}

	public static boolean touchesSolidBlocks(PlayerData playerData) {
		List<BlockState> m = playerData.getCollisionState().getSurroundingMaterialsLegacy();
		return m != null && !m.isEmpty() && (m.get(16).isSolid() || m.get(19).isSolid());
	}

	public static boolean hasDolphinsGrace(PlayerData playerData) {
		return playerData.getPlayer() != null && ExemptionManager.isNewerThan1_13()
				&& playerData.getPlayer().hasEffect(MobEffects.DOLPHINS_GRACE);
	}

	public static boolean touchesBubbleColumn(PlayerData playerData) {
		List<BlockState> m = playerData.getCollisionState().getSurroundingMaterials();
		return m != null && !m.isEmpty() && (BlockUtils.isBubbleColumn(m.get(16)) || BlockUtils.isBubbleColumn(m.get(19)));
	}

	public static double getYawDifference(ServerPlayer first, ServerPlayer second) {
		return Math.abs(180.0F - Math.abs(first.getYRot() - second.getYRot()));
	}

	public static List<BlockState> getFloorMaterials(PlayerData playerData) {
		LinkedList<BlockState> list = new LinkedList<>();
		List<BlockState> packed = playerData.getCollisionState().getSurroundingMaterials();
		if (packed == null || packed.isEmpty()) return list;
		list.add(packed.get(0)); list.add(packed.get(1)); list.add(packed.get(2));
		list.add(packed.get(12)); list.add(packed.get(13)); list.add(packed.get(14));
		list.add(packed.get(24)); list.add(packed.get(25)); list.add(packed.get(26));
		return list;
	}

	public static List<BlockState> getHeadMaterials(PlayerData playerData) {
		LinkedList<BlockState> list = new LinkedList<>();
		List<BlockState> packed = playerData.getCollisionState().getSurroundingMaterials();
		if (packed == null || packed.isEmpty()) return list;
		list.add(packed.get(6)); list.add(packed.get(7)); list.add(packed.get(8));
		list.add(packed.get(18)); list.add(packed.get(19)); list.add(packed.get(20));
		list.add(packed.get(30)); list.add(packed.get(31)); list.add(packed.get(32));
		return list;
	}

	public static List<BlockState> getHeadMaterialsLegacy(PlayerData playerData) {
		LinkedList<BlockState> list = new LinkedList<>();
		List<BlockState> packed = playerData.getCollisionState().getSurroundingMaterialsLegacy();
		if (packed == null || packed.isEmpty()) return list;
		list.add(packed.get(6)); list.add(packed.get(7)); list.add(packed.get(8));
		list.add(packed.get(18)); list.add(packed.get(19)); list.add(packed.get(20));
		list.add(packed.get(30)); list.add(packed.get(31)); list.add(packed.get(32));
		return list;
	}

	public static List<BlockState> getFloorMaterialsLegacy(PlayerData playerData) {
		LinkedList<BlockState> list = new LinkedList<>();
		List<BlockState> packed = playerData.getCollisionState().getSurroundingMaterialsLegacy();
		if (packed == null || packed.isEmpty()) return list;
		list.add(packed.get(0)); list.add(packed.get(1)); list.add(packed.get(2));
		list.add(packed.get(12)); list.add(packed.get(13)); list.add(packed.get(14));
		list.add(packed.get(24)); list.add(packed.get(25)); list.add(packed.get(26));
		return list;
	}

	public static List<BlockState> getBodyMaterials(PlayerData playerData) {
		LinkedList<BlockState> list = new LinkedList<>();
		List<BlockState> packed = playerData.getCollisionState().getSurroundingMaterials();
		if (packed == null || packed.isEmpty()) return list;
		list.add(packed.get(3)); list.add(packed.get(4)); list.add(packed.get(5));
		list.add(packed.get(6)); list.add(packed.get(7)); list.add(packed.get(8));
		list.add(packed.get(15)); list.add(packed.get(16)); list.add(packed.get(17));
		list.add(packed.get(18)); list.add(packed.get(19)); list.add(packed.get(20));
		list.add(packed.get(27)); list.add(packed.get(28)); list.add(packed.get(29));
		list.add(packed.get(30)); list.add(packed.get(31)); list.add(packed.get(32));
		return list;
	}

	public static List<BlockState> getBodyMaterialsLegacy(PlayerData playerData) {
		LinkedList<BlockState> list = new LinkedList<>();
		List<BlockState> packed = playerData.getCollisionState().getSurroundingMaterialsLegacy();
		if (packed == null || packed.isEmpty()) return list;
		list.add(packed.get(3)); list.add(packed.get(4)); list.add(packed.get(5));
		list.add(packed.get(6)); list.add(packed.get(7)); list.add(packed.get(8));
		list.add(packed.get(15)); list.add(packed.get(16)); list.add(packed.get(17));
		list.add(packed.get(18)); list.add(packed.get(19)); list.add(packed.get(20));
		list.add(packed.get(27)); list.add(packed.get(28)); list.add(packed.get(29));
		list.add(packed.get(30)); list.add(packed.get(31)); list.add(packed.get(32));
		return list;
	}

	public static List<BlockState> getCeilingMaterials(PlayerData playerData) {
		LinkedList<BlockState> list = new LinkedList<>();
		List<BlockState> packed = playerData.getCollisionState().getSurroundingMaterials();
		if (packed == null || packed.isEmpty()) return list;
		list.add(packed.get(9)); list.add(packed.get(10)); list.add(packed.get(11));
		list.add(packed.get(21)); list.add(packed.get(22)); list.add(packed.get(23));
		list.add(packed.get(33)); list.add(packed.get(34)); list.add(packed.get(35));
		return list;
	}

	public static List<BlockState> getCeilingMaterialsLegacy(PlayerData playerData) {
		LinkedList<BlockState> list = new LinkedList<>();
		List<BlockState> packed = playerData.getCollisionState().getSurroundingMaterialsLegacy();
		if (packed == null || packed.isEmpty()) return list;
		list.add(packed.get(9)); list.add(packed.get(10)); list.add(packed.get(11));
		list.add(packed.get(21)); list.add(packed.get(22)); list.add(packed.get(23));
		list.add(packed.get(33)); list.add(packed.get(34)); list.add(packed.get(35));
		return list;
	}

	public static int getPotionAmplifier(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
		MobEffectInstance instance = player.getEffect(effect);
		return instance == null ? 0 : instance.getAmplifier() + 1;
	}

	public static boolean touchesIceLegacy(PlayerData playerData) {
		List<BlockState> m = playerData.getCollisionState().getSurroundingMaterialsLegacy();
		return m != null && !m.isEmpty()
				&& (BlockUtils.isIce(m.get(13)) || BlockUtils.isIce(playerData.getCollisionState().getSubGroundMaterialLegacy())
						|| BlockUtils.isIce(playerData.getCollisionState().getDeepGroundMaterialLegacy()));
	}

	public static boolean touchesWeb(PlayerData data) {
		List<BlockState> m = data.getCollisionState().getSurroundingMaterials();
		return m != null && m.size() > 19 && (BlockUtils.isWeb(m.get(16)) || BlockUtils.isWeb(m.get(19)));
	}

	public static boolean touchesWebLegacy(PlayerData data) {
		List<BlockState> m = data.getCollisionState().getSurroundingMaterialsLegacy();
		return m != null && m.size() > 19 && (BlockUtils.isWeb(m.get(16)) || BlockUtils.isWeb(m.get(19)));
	}

	public static boolean touchesLiquid(PlayerData playerData) {
		List<BlockState> m = playerData.getCollisionState().getSurroundingMaterials();
		return m != null && !m.isEmpty() && (BlockUtils.isLiquid(m.get(16)) || BlockUtils.isLiquid(m.get(19)));
	}

	public static boolean touchesLiquidLegacy(PlayerData playerData) {
		List<BlockState> m = playerData.getCollisionState().getSurroundingMaterialsLegacy();
		return m != null && !m.isEmpty() && (BlockUtils.isLiquid(m.get(16)) || BlockUtils.isLiquid(m.get(19)));
	}

	public static boolean touchesClimbable(PlayerData playerData) {
		List<BlockState> m = playerData.getCollisionState().getSurroundingMaterials();
		return m != null && !m.isEmpty() && BlockUtils.isClimbable(m.get(16)) && BlockUtils.isClimbable(m.get(19));
	}

	public static boolean touchesClimbableLegacy(PlayerData playerData) {
		List<BlockState> m = playerData.getCollisionState().getSurroundingMaterialsLegacy();
		return m != null && !m.isEmpty() && BlockUtils.isClimbable(m.get(16)) && BlockUtils.isClimbable(m.get(19));
	}

	public static int getSpeedPotionLevel(LivingEntity entity) {
		for (MobEffectInstance effect : entity.getActiveEffects()) {
			if (effect.getEffect().is(MobEffects.MOVEMENT_SPEED.unwrapKey().orElseThrow())) return effect.getAmplifier() + 1;
		}
		return 0;
	}

	public static boolean isValidBlockPlacement(PlacementContext context) {
		if (!context.placed().isSolid()) return false;
		if (BlockUtils.isDirtPath(context.placed())) return false;
		if (BlockUtils.isFarmland(context.placed())) return false;
		if (BlockUtils.isSand(context.placed())) return false;
		if (BlockUtils.isConcretePowder(context.placed())) return false;
		if (BlockUtils.isGravel(context.placed())) return false;
		PlayerData playerData = MeshServices.players().getPlayerData(context.player());
		if (playerData == null) return false;
		BlockState material = ExemptionManager.isNewerThan1_13()
				? playerData.getCollisionState().getSubGroundMaterial()
				: playerData.getCollisionState().getSubGroundMaterialLegacy();
		Direction blockFace = context.face();
		double x = context.againstPos().getX();
		double y = context.againstPos().getY();
		double z = context.againstPos().getZ();
		double x2 = playerData.getCollisionState().getX();
		double y2 = playerData.getCollisionState().getY();
		double z2 = playerData.getCollisionState().getZ();
		double xDifference = Math.abs(x - x2);
		double zDifference = Math.abs(z - z2);
		double amount = MathUtils.hypot(xDifference, zDifference);
		List<BlockState> list = playerData.getCollisionState().getFloorMaterials();
		if (list == null) return false;
		long index = list.stream().filter(BlockUtils::isAir).count();
		if (material == null) return false;
		if (blockFace == null) return false;
		if (blockFace == Direction.UP) return false;
		if (!BlockUtils.isAir(material)) return false;
		if (!(amount < 2.25)) return false;
		if (!(y < y2)) return false;
		if (index <= 6) return false;
		return true;
	}

	public static boolean isFullySubmerged(PlayerData playerData) {
		List<BlockState> m = playerData.getCollisionState().getSurroundingMaterials();
		return m != null && !m.isEmpty() && BlockUtils.isLiquid(m.get(16)) && BlockUtils.isLiquid(m.get(19));
	}

	public static boolean isFullySubmergedLegacy(PlayerData playerData) {
		List<BlockState> m = playerData.getCollisionState().getSurroundingMaterialsLegacy();
		return m != null && !m.isEmpty() && BlockUtils.isLiquid(m.get(16)) && BlockUtils.isLiquid(m.get(19));
	}

	public static boolean holdsRiptideTrident(PlayerData playerData) {
		if (playerData.getPlayer() == null || !ExemptionManager.isNewerThan1_13()) return false;
		return isRiptideTrident(playerData.getPlayer().getMainHandItem()) || isRiptideTrident(playerData.getPlayer().getOffhandItem());
	}

	private static boolean isRiptideTrident(ItemStack stack) {
		if (!stack.is(Items.TRIDENT)) return false;
		var registry = MeshServices.server().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
		var riptide = registry.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.RIPTIDE);
		return EnchantmentHelper.getItemEnchantmentLevel(riptide, stack) > 0;
	}

	public static void sendHotbarShuffle(ServerPlayer player) {
		int index = ThreadLocalRandom.current().nextInt(8);
		player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(index));
	}

	public static float getMaxSpeed(PlayerData playerData, float amount) {
		MotionState motionState = playerData.getMotionState();
		float scaledSpeedAmplifier = motionState.getSpeedAmplifier() * 0.0675F;
		float adjustedWalkSpeedDifference = amount + (scaledSpeedAmplifier + (playerData.getCollisionState().getWalkSpeed() - 0.2F) * 3.5F);
		if (!(adjustedWalkSpeedDifference < amount)) return amount;
		return amount + motionState.getSpeedAmplifier() * 0.0675F;
	}

	public static void rotatePlayerRandomly(PlayerData playerData) {
		ServerPlayer player = playerData.getPlayer();
		if (player == null) return;
		float yaw = (float) ThreadLocalRandom.current().nextDouble(360.0) - 180.0f;
		float pitch = (float) ThreadLocalRandom.current().nextDouble(180.0) - 90.0f;
		MeshServices.mainThread(() -> player.connection.teleport(player.getX(), player.getY(), player.getZ(), yaw, pitch, java.util.Set.of(), false));
	}

	public static boolean isWearingNetheriteArmor(PlayerData playerData) {
		if (!ExemptionManager.isAtLeast1_16() || playerData.getPlayer() == null) return false;
		ServerPlayer player = playerData.getPlayer();
		if (!player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty())
			return player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(Items.NETHERITE_HELMET);
		if (!player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).isEmpty())
			return player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(Items.NETHERITE_CHESTPLATE);
		if (!player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).isEmpty())
			return player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).is(Items.NETHERITE_LEGGINGS);
		if (!player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).isEmpty())
			return player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(Items.NETHERITE_BOOTS);
		return false;
	}
}
