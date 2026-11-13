package dev.meshac.world;

import dev.meshac.check.CheckUtils;
import dev.meshac.event.MeshBlock;
import dev.meshac.mcutil.MaterialSets;
import dev.meshac.util.EnchantUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of Vulcan's BukkitRegistry break-time calculation. Bukkit enchant/effect name lookups
 * became direct registry holders (efficiency, haste, mining fatigue - the legacy DIG_SPEED /
 * FAST_DIGGING / SLOW_DIGGING aliases map to the same modern entries).
 */
public final class BreakTimeUtil {

	private BreakTimeUtil() {
	}

	public static long getBlockBreakTime(MeshBlock meshBlock, ServerPlayer player) {
		BlockState state = meshBlock.getType();
		Block block = state.getBlock();
		if (!MaterialSets.blockHardness.containsKey(block)) {
			return 0L;
		}
		ItemStack itemInHand = player.getMainHandItem();
		String itemName = itemInHand.getItem().toString();
		if (itemName.equalsIgnoreCase("shears") || itemName.contains("copper")) {
			return 0L;
		}
		ServerLevel level = meshBlock.getLevel();
		boolean canDrop = !Block.getDrops(state, level, meshBlock.getPos(), level.getBlockEntity(meshBlock.getPos()), player, itemInHand).isEmpty();
		double amount = ((Float) MaterialSets.blockHardness.get(block)).floatValue() * (canDrop ? 1.5 : 5.0);
		double toolHarvestLevel = getToolHarvestLevel(itemName);
		int efficiency = EnchantUtil.level(itemInHand, Enchantments.EFFICIENCY);
		if (efficiency > 0) {
			toolHarvestLevel += efficiency * efficiency + 1;
		}
		if (player.hasEffect(MobEffects.HASTE)) {
			toolHarvestLevel *= 1.0 + 0.2 * CheckUtils.getPotionAmplifier(player, MobEffects.HASTE);
		}
		if (player.hasEffect(MobEffects.MINING_FATIGUE)) {
			toolHarvestLevel /= Math.pow(3.0, CheckUtils.getPotionAmplifier(player, MobEffects.MINING_FATIGUE));
		}
		amount /= toolHarvestLevel;
		return (long) (amount * 1000.0);
	}

	private static int getToolHarvestLevel(String name) {
		return switch (name) {
			case "iron_pickaxe", "iron_axe", "iron_shovel" -> 3;
			case "stone_pickaxe", "stone_shovel", "stone_axe" -> 2;
			case "wooden_pickaxe", "wooden_axe", "wooden_shovel" -> 1;
			case "netherite_axe", "netherite_shovel", "netherite_pickaxe" -> 6;
			case "diamond_pickaxe", "diamond_axe", "diamond_shovel" -> 5;
			case "golden_axe", "golden_shovel", "golden_pickaxe" -> 4;
			default -> 0;
		};
	}
}
