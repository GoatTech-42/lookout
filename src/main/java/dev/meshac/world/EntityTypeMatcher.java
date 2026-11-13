package dev.meshac.world;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/** Port of Vulcan's EntityTypeMatcher: name-based matching against the registry id. */
public class EntityTypeMatcher {

	public static boolean matches(Entity entity, String name) {
		if (entity == null || name == null) return false;
		return EntityType.getKey(entity.getType()).getPath().equalsIgnoreCase(name);
	}

	public static boolean matchesAny(Entity entity, Iterable<String> names) {
		for (String name : names) {
			if (matches(entity, name)) return true;
		}
		return false;
	}

	public static boolean isShulker(Entity entity) { return entity instanceof net.minecraft.world.entity.monster.Shulker; }

	public static boolean isBoat(net.minecraft.world.entity.Entity entity) {
		return entity instanceof net.minecraft.world.entity.vehicle.boat.AbstractBoat;
	}

	public static boolean isHappyGhast(net.minecraft.world.entity.Entity entity) {
		return entity.getType() == net.minecraft.world.entity.EntityTypes.HAPPY_GHAST;
	}
}
