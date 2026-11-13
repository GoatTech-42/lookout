package dev.meshac.tracker;

import dev.meshac.data.PlayerData;
import dev.meshac.util.Vec3d;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.world.entity.EntityType;

/**
 * Server-side view of entities around the player, updated from outbound entity packets after the
 * client confirms them. Ported from Vulcan's EntityTracker.
 */
public class EntityTracker {

	private final PlayerData playerData;
	private final Map<Integer, TrackedEntity> trackedEntities = new HashMap<>();

	public EntityTracker(PlayerData data) {
		this.playerData = data;
	}

	/** Outbound spawn (any entity). */
	public void handleSpawn(int entityId, UUID uniqueId, EntityType<?> entityType, double x, double y, double z) {
		this.playerData.getConfirmationTracker().runAfterConfirmation(() -> trackEntity(entityId, uniqueId, entityType, new Vec3d(x, y, z)));
	}

	/** Outbound entity removal. */
	public void handleDestroy(int[] entityIds) {
		this.playerData.getConfirmationTracker().runAfterConfirmation(() -> {
			for (int id : entityIds) this.trackedEntities.remove(id);
		});
	}

	/** Outbound relative move. */
	public void handleRelativeMove(int entityId, double dx, double dy, double dz) {
		this.playerData.getConfirmationTracker().runAfterConfirmation(() -> updateEntity(entityId, dx, dy, dz, true, null, null));
	}

	/** Outbound relative move + rotation. */
	public void handleRelativeMoveAndRotation(int entityId, double dx, double dy, double dz, float yaw, float pitch) {
		this.playerData.getConfirmationTracker().runAfterConfirmation(() -> updateEntity(entityId, dx, dy, dz, true, yaw * 0.7111111F, pitch * 0.7111111F));
	}

	/** Outbound rotation-only. */
	public void handleRotation(int entityId, float yaw, float pitch) {
		this.playerData.getConfirmationTracker().runAfterConfirmation(() -> {
			TrackedEntity entity = trackedEntities.get(entityId);
			if (entity != null) {
				entity.setYaw(yaw * 0.7111111F);
				entity.setPitch(pitch * 0.7111111F);
			}
		});
	}

	/** Outbound teleport / position sync. */
	public void handleTeleport(int entityId, double x, double y, double z, float yaw, float pitch) {
		this.playerData.getConfirmationTracker().runAfterConfirmation(() -> updateEntity(entityId, x, y, z, false, yaw, pitch));
	}

	/** Outbound equipment. */
	public void handleEquipment(int entityId, net.minecraft.world.entity.EquipmentSlot slot, net.minecraft.world.item.ItemStack item) {
		TrackedEntity trackedEntity = this.trackedEntities.get(entityId);
		if (trackedEntity == null) return;
		trackedEntity.setEquipment(slot, item);
	}

	private void trackEntity(int entityId, UUID uniqueId, EntityType<?> entityType, Vec3d position) {
		TrackedEntity entity = new TrackedEntity(entityId);
		entity.setUniqueId(uniqueId);
		entity.setEntityType(entityType);
		entity.setX(position.getX());
		entity.setY(position.getY());
		entity.setZ(position.getZ());
		this.trackedEntities.put(entityId, entity);
	}

	private void updateEntity(int id, double x, double y, double z, boolean relative, Float yaw, Float pitch) {
		TrackedEntity entity = trackedEntities.get(id);
		if (entity == null) return;
		entity.setX(relative ? entity.getX() + x : x);
		entity.setY(relative ? entity.getY() + y : y);
		entity.setZ(relative ? entity.getZ() + z : z);
		if (yaw != null && pitch != null) {
			entity.setYaw(yaw);
			entity.setPitch(pitch);
		}
	}

	public TrackedEntity getTrackedEntity(int entityId) {
		return this.trackedEntities.get(entityId);
	}

	public List<TrackedEntity> getNearbyEntities(double x, double y, double z, double radius) {
		double radiusSquared = radius * radius;
		return this.trackedEntities.values().stream().filter(entity -> isWithinRadius(x, y, z, radiusSquared, entity)).collect(Collectors.toList());
	}

	public List<TrackedEntity> getNearbyEntities(double radius) {
		double x = this.playerData.getCollisionState().getX();
		double y = this.playerData.getCollisionState().getY();
		double z = this.playerData.getCollisionState().getZ();
		double minX = x - radius, maxX = x + radius;
		double minY = y - radius, maxY = y + 1.8 + radius;
		double minZ = z - radius, maxZ = z + radius;
		return this.trackedEntities.values().stream().filter(entity -> isWithinBounds(minX, maxX, minY, maxY, minZ, maxZ, entity)).collect(Collectors.toList());
	}

	public PlayerData getPlayerData() { return this.playerData; }
	public Map<Integer, TrackedEntity> getTrackedEntities() { return this.trackedEntities; }

	private static boolean isWithinBounds(double minX, double maxX, double minY, double maxY, double minZ, double maxZ, TrackedEntity entity) {
		return entity.getX() >= minX && entity.getX() <= maxX && entity.getY() >= minY && entity.getY() <= maxY && entity.getZ() >= minZ && entity.getZ() <= maxZ;
	}

	private static boolean isWithinRadius(double x, double y, double z, double radiusSquared, TrackedEntity entity) {
		double dx = entity.getX() - x;
		double dy = entity.getY() - y;
		double dz = entity.getZ() - z;
		return dx * dx + dy * dy + dz * dz <= radiusSquared;
	}
}
