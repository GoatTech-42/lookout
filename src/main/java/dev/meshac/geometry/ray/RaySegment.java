package dev.meshac.geometry.ray;

import dev.meshac.event.MeshBlock;
import dev.meshac.util.Location;
import dev.meshac.util.Vector;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;

/** Port of Vulcan's RaySegment. Bukkit Vector/World/Block became Vector/ServerLevel/MeshBlock. */
public final class RaySegment {

	public Vector origin;

	public Vector direction;

	public RaySegment(Vector origin, Vector direction) {
		this.origin = origin;
		this.direction = direction;
	}

	public static boolean contains(Vector point, Vector minimum, Vector maximum) {
		return point.getX() >= minimum.getX() && point.getX() <= maximum.getX()
				&& point.getY() >= minimum.getY() && point.getY() <= maximum.getY()
				&& point.getZ() >= minimum.getZ() && point.getZ() <= maximum.getZ();
	}

	public Vector pointAt(double distance) {
		return origin.clone().add(direction.clone().multiply(distance));
	}

	public boolean containsPointOnRay(Vector point) {
		Vector offset = point.clone().subtract(origin);
		double lengthSquared = direction.lengthSquared();
		if (lengthSquared == 0)
			return offset.lengthSquared() < 1.0E-14;
		double distance = offset.dot(direction) / lengthSquared;
		return distance >= 0 && pointAt(distance).distanceSquared(point) < 1.0E-14;
	}

	public List<Vector> samplePoints(double end, double step) {
		return samplePoints(0, end, step);
	}

	public List<Vector> samplePoints(double start, double end, double step) {
		if (!Double.isFinite(start) || !Double.isFinite(end) || !Double.isFinite(step) || step <= 0)
			throw new IllegalArgumentException("Ray sample bounds must be finite and step must be positive");
		List<Vector> points = new ArrayList<>();
		for (double distance = start; distance <= end; ) {
			points.add(pointAt(distance));
			if (distance == end)
				break;
			double next = distance + step;
			if (next <= distance)
				throw new IllegalArgumentException("Ray sample step is too small for these bounds");
			distance = next;
		}
		return points;
	}

	public List<MeshBlock> solidBlocksAlongRay(ServerLevel world, double end, double step) {
		List<MeshBlock> blocks = new ArrayList<>();
		for (Vector point : samplePoints(end, step)) {
			MeshBlock block = new MeshBlock(world, point.toLocation(world).getBlock());
			if (block.getType().isSolid())
				blocks.add(block);
		}
		return blocks;
	}

	public Vector firstPointInBox(Vector minimum, Vector maximum, double end, double step) {
		for (Vector point : samplePoints(end, step)) if (contains(point, minimum, maximum))
			return point;
		return null;
	}

	public boolean intersectsBox(Vector minimum, Vector maximum, double end, double step) {
		return firstPointInBox(minimum, maximum, end, step) != null;
	}
}
