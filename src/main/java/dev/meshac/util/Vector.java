package dev.meshac.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Mutable 3-vector with the Bukkit Vector surface the ported checks use
 * (clone/subtract/add/multiply/dot/angle/distance family). Written fresh for this port.
 */
public class Vector implements Cloneable {

	private double x;
	private double y;
	private double z;

	public Vector() {
		this(0, 0, 0);
	}

	public Vector(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public double getX() { return x; }
	public double getY() { return y; }
	public double getZ() { return z; }
	public Vector setX(double x) { this.x = x; return this; }
	public Vector setY(double y) { this.y = y; return this; }
	public Vector setZ(double z) { this.z = z; return this; }

	public Vector add(Vector v) {
		this.x += v.x; this.y += v.y; this.z += v.z;
		return this;
	}

	public Vector subtract(Vector v) {
		this.x -= v.x; this.y -= v.y; this.z -= v.z;
		return this;
	}

	public Vector multiply(double m) {
		this.x *= m; this.y *= m; this.z *= m;
		return this;
	}

	public double dot(Vector v) {
		return x * v.x + y * v.y + z * v.z;
	}

	public double length() {
		return Math.sqrt(lengthSquared());
	}

	public double lengthSquared() {
		return x * x + y * y + z * z;
	}

	public double distance(Vector v) {
		return Math.sqrt(distanceSquared(v));
	}

	public double distanceSquared(Vector v) {
		double dx = x - v.x, dy = y - v.y, dz = z - v.z;
		return dx * dx + dy * dy + dz * dz;
	}

	public float angle(Vector other) {
		double dot = dot(other) / (length() * other.length());
		if (dot > 1) dot = 1;
		if (dot < -1) dot = -1;
		return (float) Math.acos(dot);
	}

	public Location toLocation(ServerLevel world) {
		return new Location(world, x, y, z);
	}

	@Override
	public Vector clone() {
		return new Vector(x, y, z);
	}

	@Override
	public String toString() {
		return x + "," + y + "," + z;
	}
}
