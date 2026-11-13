package dev.meshac.util;

import net.minecraft.server.level.ServerLevel;

/** Minimal Bukkit-Location analog for setback/teleport bookkeeping. */
public class Location {

	private final ServerLevel world;
	private double x;
	private double y;
	private double z;
	private float yaw;
	private float pitch;

	public Location(ServerLevel world, double x, double y, double z) {
		this(world, x, y, z, 0.0F, 0.0F);
	}

	public Location(ServerLevel world, double x, double y, double z, float yaw, float pitch) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
	}

	public ServerLevel getWorld() { return world; }
	public double getX() { return x; }
	public double getY() { return y; }
	public double getZ() { return z; }
	public float getYaw() { return yaw; }
	public float getPitch() { return pitch; }
	public void setX(double x) { this.x = x; }
	public void setY(double y) { this.y = y; }
	public void setZ(double z) { this.z = z; }
	public void setYaw(float yaw) { this.yaw = yaw; }
	public void setPitch(float pitch) { this.pitch = pitch; }

	public Location clone() {
		return new Location(world, x, y, z, yaw, pitch);
	}

	public Location subtract(double dx, double dy, double dz) {
		this.x -= dx; this.y -= dy; this.z -= dz;
		return this;
	}

	public Location add(double dx, double dy, double dz) {
		this.x += dx; this.y += dy; this.z += dz;
		return this;
	}

	public Vector toVector() {
		return new Vector(x, y, z);
	}

	public net.minecraft.core.BlockPos getBlock() {
		return new net.minecraft.core.BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
	}
}
