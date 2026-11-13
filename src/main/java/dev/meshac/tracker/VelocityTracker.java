package dev.meshac.tracker;

import dev.meshac.data.PlayerData;
import dev.meshac.packet.Flying;
import dev.meshac.player.CollisionState;
import dev.meshac.util.Vec3d;
import java.util.LinkedList;

/**
 * Tracks knockback/explosion velocity pending client acknowledgement. Ported from Vulcan's
 * VelocityTracker; packetevents send events became direct calls from the outbound interceptor.
 */
public class VelocityTracker {

	private final PlayerData playerData;
	private final LinkedList<Vec3d> pendingVelocities = new LinkedList<>();
	private final LinkedList<Vec3d> pendingExplosions = new LinkedList<>();
	private boolean pendingVelocity = false;
	private boolean pendingExplosion = false;
	private double maximumHorizontalVelocity = 0.0;
	private double maximumVerticalVelocity = 0.0;
	private double velocityX = 0.0;
	private double velocityY = 0.0;
	private double velocityZ = 0.0;
	private double horizontalVelocity = 0.0;
	private double explosionX = 0.0;
	private double explosionY = 0.0;
	private double explosionZ = 0.0;
	private double horizontalExplosion = 0.0;
	private double combinedExplosionY = 0.0;
	private double combinedHorizontalExplosion = 0.0;
	private double lastExplosionY = 0.0;
	private double lastHorizontalExplosion = 0.0;
	private double lastVelocityY = 0.0;
	private int ticksSinceVelocity = 0;
	private int ticksSinceExplosion = 0;
	private int ticksSinceVelocityPacket = 0;
	private int ticksSinceLargeVelocity = 1000;
	private int ticksSinceUnloadedPosition = 1000;

	public VelocityTracker(PlayerData data) {
		this.playerData = data;
	}

	/** Called by the outbound interceptor when an entity-velocity packet for this player is sent. */
	public void handleVelocity(Vec3d velocity) {
		ConfirmationTracker confirmations = this.playerData.getConfirmationTracker();
		confirmations.runAfterConfirmation(() -> queueEntityVelocity(velocity));
		confirmations.runAfterNextConfirmation(this::discardAcknowledgedVelocity);
		confirmVelocitySent();
	}

	/** Called by the outbound interceptor when an explosion packet carrying knockback is sent. */
	public void handleExplosion(Vec3d knockback) {
		if (knockback == null || (knockback.getX() == 0.0 && knockback.getY() == 0.0 && knockback.getZ() == 0.0)) return;
		ConfirmationTracker confirmations = this.playerData.getConfirmationTracker();
		confirmations.runAfterConfirmation(() -> queueExplosionKnockback(knockback));
		confirmations.runAfterNextConfirmation(this::discardAcknowledgedExplosion);
		confirmExplosionSent();
	}

	public void processMovement(Flying packet) {
		if (!packet.hasPositionChanged()) return;
		this.pendingVelocity = !this.pendingVelocities.isEmpty();
		this.pendingExplosion = !this.pendingExplosions.isEmpty();
		for (Vec3d vector : this.pendingVelocities) {
			this.velocityX = vector.getX();
			this.velocityY = vector.getY();
			this.velocityZ = vector.getZ();
			this.horizontalVelocity = Math.hypot(this.velocityX, this.velocityZ);
			this.maximumVerticalVelocity = Math.max(this.maximumVerticalVelocity, vector.getY());
			this.maximumHorizontalVelocity = Math.max(this.maximumHorizontalVelocity, Math.hypot(vector.getX(), vector.getZ()));
			if (this.maximumHorizontalVelocity > 0.75) this.ticksSinceLargeVelocity = 0;
		}
		this.combinedExplosionY = 0.0;
		this.combinedHorizontalExplosion = 0.0;
		for (Vec3d vector : this.pendingExplosions) {
			this.explosionX = vector.getX();
			this.explosionY = vector.getY();
			this.explosionZ = vector.getZ();
			this.horizontalExplosion = Math.hypot(this.explosionX, this.explosionZ);
			this.combinedExplosionY += this.explosionY;
			this.combinedHorizontalExplosion += this.horizontalExplosion;
		}
		if (this.combinedExplosionY > 0.0) this.lastExplosionY = this.combinedExplosionY;
		if (this.combinedHorizontalExplosion > 0.0) this.lastHorizontalExplosion = this.combinedHorizontalExplosion;
		if (packet.isOnGround()) this.lastExplosionY = 0.0;
		this.ticksSinceVelocity = this.pendingVelocity ? 0 : ++this.ticksSinceVelocity;
		this.ticksSinceExplosion = this.pendingExplosion ? 0 : ++this.ticksSinceExplosion;
		++this.ticksSinceVelocityPacket;
		++this.ticksSinceLargeVelocity;
		++this.ticksSinceUnloadedPosition;
		double x = this.playerData.getCollisionState().getX();
		double y = this.playerData.getCollisionState().getY();
		double z = this.playerData.getCollisionState().getZ();
		if (this.playerData.getWorldBlockTracker().hasTrackedBlockNearby(x, y, z)) this.ticksSinceUnloadedPosition = 0;
	}

	public void update() {
	}

	public LinkedList<Vec3d> getPendingVelocities() { return this.pendingVelocities; }
	public boolean isPendingVelocity() { return this.pendingVelocity; }
	public boolean isPendingExplosion() { return this.pendingExplosion; }
	public double getMaximumHorizontalVelocity() { return this.maximumHorizontalVelocity; }
	public double getMaximumVerticalVelocity() { return this.maximumVerticalVelocity; }
	public double getVelocityX() { return this.velocityX; }
	public double getVelocityY() { return this.velocityY; }
	public double getVelocityZ() { return this.velocityZ; }
	public double getCombinedExplosionY() { return this.combinedExplosionY; }
	public double getCombinedHorizontalExplosion() { return this.combinedHorizontalExplosion; }
	public double getLastExplosionY() { return this.lastExplosionY; }
	public double getLastHorizontalExplosion() { return this.lastHorizontalExplosion; }
	public double getLastVelocityY() { return this.lastVelocityY; }
	public int getTicksSinceVelocity() { return this.ticksSinceVelocity; }
	public int getTicksSinceExplosion() { return this.ticksSinceExplosion; }
	public int getTicksSinceVelocityPacket() { return this.ticksSinceVelocityPacket; }
	public int getTicksSinceLargeVelocity() { return this.ticksSinceLargeVelocity; }
	public int getTicksSinceUnloadedPosition() { return this.ticksSinceUnloadedPosition; }

	private void confirmExplosionSent() {
		this.playerData.getConfirmationTracker().sendConfirmation(false);
	}

	private void discardAcknowledgedExplosion() {
		CollisionState collisionState = this.playerData.getCollisionState();
		collisionState.queueAcknowledgedTask(this.pendingExplosions::poll);
	}

	private void queueExplosionKnockback(Vec3d knockback) {
		this.pendingExplosions.add(knockback);
	}

	private void confirmVelocitySent() {
		this.playerData.getConfirmationTracker().sendConfirmation(false);
	}

	private void discardAcknowledgedVelocity() {
		CollisionState collisionState = this.playerData.getCollisionState();
		collisionState.queueAcknowledgedTask(this.pendingVelocities::poll);
	}

	private void queueEntityVelocity(Vec3d velocity) {
		this.lastVelocityY = velocity.getY();
		this.ticksSinceVelocityPacket = 0;
		this.pendingVelocities.add(velocity);
	}
}
