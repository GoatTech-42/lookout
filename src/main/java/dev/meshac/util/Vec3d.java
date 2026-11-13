package dev.meshac.util;

/** Minimal immutable vector keeping packetevents' Vector3d accessor names. */
public class Vec3d {

	private final double x;
	private final double y;
	private final double z;

	public Vec3d(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public double getX() { return x; }
	public double getY() { return y; }
	public double getZ() { return z; }

	public double length() {
		return Math.sqrt(x * x + y * y + z * z);
	}

	@Override
	public String toString() {
		return "Vec3d[" + x + ", " + y + ", " + z + "]";
	}
}
