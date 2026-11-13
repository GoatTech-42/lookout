package dev.meshac.tracker;

import dev.meshac.data.PlayerData;
import net.minecraft.world.entity.EntityType;

/** Vehicle/passenger state. Ported from Vulcan's VehicleTracker. */
public class VehicleTracker {

	private final PlayerData playerData;
	private boolean riding = false;
	private EntityType<?> entityType = null;
	private int vehicleEntityId = -1;

	public VehicleTracker(PlayerData data) {
		this.playerData = data;
	}

	/** Called by the outbound interceptor when a set-passengers packet is sent. */
	public void handlePassengers(int entityId, int[] passengers) {
		this.playerData.getConfirmationTracker().runAfterConfirmation(() -> applyPassengers(entityId, passengers));
	}

	public EntityType<?> getEntityType() { return this.entityType; }
	public int getVehicleEntityId() { return this.vehicleEntityId; }

	private void applyPassengers(int entityId, int[] passengers) {
		riding = false;
		for (int passenger : passengers) {
			if (passenger == playerData.getEntityId()) {
				riding = true;
				break;
			}
		}
		vehicleEntityId = riding ? entityId : -1;
		TrackedEntity vehicle = riding ? playerData.getEntityTracker().getTrackedEntity(vehicleEntityId) : null;
		entityType = vehicle == null ? null : vehicle.getEntityType();
	}
}
