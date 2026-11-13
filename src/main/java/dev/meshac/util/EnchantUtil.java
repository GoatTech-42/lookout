package dev.meshac.util;

import dev.meshac.MeshServices;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Enchantment lookups through the server registry (1.21+ holder model). */
public final class EnchantUtil {

	private EnchantUtil() {
	}

	public static boolean has(ItemStack stack, ResourceKey<Enchantment> enchantment) {
		return level(stack, enchantment) > 0;
	}

	public static int level(ItemStack stack, ResourceKey<Enchantment> enchantment) {
		if (stack == null || stack.isEmpty() || MeshServices.server() == null) return 0;
		var holder = MeshServices.server().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(enchantment);
		return holder.map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, stack)).orElse(0);
	}
}
