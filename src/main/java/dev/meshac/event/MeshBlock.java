package dev.meshac.event;

import dev.meshac.util.Location;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block view handed to checks, replacing org.bukkit.block.Block. getType() returns the live
 * BlockState so upstream chains like getBlock().getType().isSolid() keep working on Mojmap.
 */
public final class MeshBlock {

	private final ServerLevel level;
	private final BlockPos pos;

	public MeshBlock(ServerLevel level, BlockPos pos) {
		this.level = level;
		this.pos = pos;
	}

	public BlockState getType() {
		return level.getBlockState(pos);
	}

	public BlockState getState() {
		return level.getBlockState(pos);
	}

	public Location getLocation() {
		return new Location(level, pos.getX(), pos.getY(), pos.getZ());
	}

	public BlockPos getPos() { return pos; }
	public ServerLevel getLevel() { return level; }
	public int getX() { return pos.getX(); }
	public int getY() { return pos.getY(); }
	public int getZ() { return pos.getZ(); }

	public Direction getFace(MeshBlock other) {
		BlockPos d = pos.subtract(other.pos);
		for (Direction face : Direction.values()) {
			if (d.getX() == face.getStepX() && d.getY() == face.getStepY() && d.getZ() == face.getStepZ()) return face;
		}
		return null;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof MeshBlock b && b.pos.equals(pos) && b.level == level;
	}

	@Override
	public int hashCode() {
		return pos.hashCode();
	}
}
