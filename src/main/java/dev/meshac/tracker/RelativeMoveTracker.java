package dev.meshac.tracker;

import dev.meshac.MeshServices;
import dev.meshac.collection.EvictingList;
import dev.meshac.data.PlayerData;
import dev.meshac.util.MathUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.phys.Vec3;

/**
 * Combat-target tracking: ticks since attack, attack distance and recent positions of the
 * player's current target, used to grade hits against where the target actually was.
 * Ported from Vulcan's RelativeMoveTracker; Bukkit Location/Vector became a small record.
 */
public class RelativeMoveTracker {

	/** One past target position with the server tick it was seen at (Vulcan stored Pair<Location, tick>). */
	public record TargetPos(double x, double y, double z, int tick) {
	}

	private final PlayerData playerData;
	private int ticksSinceAttack = 100;
	private int lastAttackGivenTick;
	private int lastAttackTakenTick;
	private int lastItemFrameInteractTick;
	private Entity lastAttackedEntity;
	private ServerPlayer player;
	private ServerPlayer previousTarget;
	private double attackDistance;
	private double targetDistance;
	private final EvictingList<TargetPos> targetPositions = new EvictingList<>(20);

	public RelativeMoveTracker(PlayerData data) {
		this.playerData = data;
	}

	public void handleInteract(int entityId, boolean attack) {
		ServerPlayer self = this.playerData.getPlayer();
		if (self == null) return;
		ServerLevel level = self.level() instanceof ServerLevel l ? l : null;
		if (level == null) return;
		Entity target = level.getEntity(entityId);
		if (target instanceof ItemFrame) {
			this.lastItemFrameInteractTick = MeshServices.ticks();
		}
		if (attack) {
			this.lastAttackedEntity = target;
			if (this.lastAttackedEntity != null) {
				Vec3 a = self.position(), b = this.lastAttackedEntity.position();
				this.attackDistance = Math.sqrt(sq(b.x - a.x) + sq(b.z - a.z)) - 0.42;
			}
			if (target instanceof ServerPlayer targetPlayer) {
				this.previousTarget = this.player == null ? targetPlayer : this.player;
				this.player = targetPlayer;
				if (this.player != this.previousTarget) this.targetPositions.clear();
			}
			this.ticksSinceAttack = 0;
		}
	}

	private static double sq(double v) {
		return v * v;
	}

	/** Clientbound entity relative move for the tracked target: record where it was. */
	public void handleTargetMove(int entityId, double deltaX, double deltaY, double deltaZ) {
		if (this.player == null || this.player.getId() != entityId) return;
		PlayerData targetData = MeshServices.players().get(this.player.getUUID());
		if (targetData == null || targetData.getPlayer() == null) return;
		int currentTick = MeshServices.ticks();
		dev.meshac.player.CollisionState collision = targetData.getCollisionState();
		this.targetPositions.add(new TargetPos(collision.getX() - deltaX, collision.getY() - deltaY, collision.getZ() - deltaZ, currentTick));
	}

	public void clearTargetPositions() {
		this.targetPositions.clear();
	}

	public void recordAttackGiven() {
		this.lastAttackGivenTick = MeshServices.ticks();
	}

	public void recordAttackTaken() {
		this.lastAttackTakenTick = MeshServices.ticks();
	}

	public int getTicksSinceAttackGiven() {
		return MeshServices.ticks() - this.lastAttackGivenTick;
	}

	public int getTicksSinceAttackTaken() {
		return MeshServices.ticks() - this.lastAttackTakenTick;
	}

	public void processMovement() {
		++this.ticksSinceAttack;
		if (!this.targetPositions.isEmpty() && this.player != null) {
			int currentTick = MeshServices.ticks();
			int latencyTicks = MathUtils.pingCompensationTicks(this.playerData);
			double x = this.playerData.getCollisionState().getX();
			double z = this.playerData.getCollisionState().getZ();
			this.targetDistance = this.targetPositions.stream()
					.filter(entry -> Math.abs(currentTick - entry.tick() - latencyTicks) < 3)
					.mapToDouble(entry -> Math.sqrt(sq(entry.x() - x) + sq(entry.z() - z)) - 0.52)
					.min().orElse(-1.0);
		}
	}

	public int getTicksSinceAttack() { return ticksSinceAttack; }
	public int getLastItemFrameInteractTick() { return lastItemFrameInteractTick; }
	public Entity getLastAttackedEntity() { return lastAttackedEntity; }
	public ServerPlayer getPlayer() { return player; }
	public double getAttackDistance() { return attackDistance; }
	public double getTargetDistance() { return targetDistance; }
	public EvictingList<TargetPos> getTargetPositions() { return targetPositions; }
}
