package dev.meshac.packet;

/**
 * Normalized view of a ServerboundMovePlayerPacket, mirroring WrapperPlayClientPlayerFlying:
 * same accessors the checks call (hasPositionChanged/hasRotationChanged/onGround + coordinates).
 */
public final class Flying {

	private final boolean hasPosition;
	private final boolean hasRotation;
	private final double x, y, z;
	private final float yaw, pitch;
	private final boolean onGround;
	private final boolean horizontalCollision;

	public Flying(boolean hasPosition, boolean hasRotation, double x, double y, double z, float yaw, float pitch, boolean onGround, boolean horizontalCollision) {
		this.hasPosition = hasPosition;
		this.hasRotation = hasRotation;
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
		this.onGround = onGround;
		this.horizontalCollision = horizontalCollision;
	}

	public boolean hasPositionChanged() {
		return hasPosition;
	}

	public boolean hasRotationChanged() {
		return hasRotation;
	}

	public double getX() { return x; }
	public double getY() { return y; }
	public double getZ() { return z; }
	public float getYaw() { return yaw; }
	public float getPitch() { return pitch; }
	public boolean isOnGround() { return onGround; }
	public boolean isHorizontalCollision() { return horizontalCollision; }
}
