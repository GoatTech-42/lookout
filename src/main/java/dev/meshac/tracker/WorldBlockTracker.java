package dev.meshac.tracker;

import dev.meshac.data.PlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client-acknowledged world view. PARITY BLOCKER: Vulcan mirrored the client's acknowledged chunk
 * cache from outbound packets and answered block queries from that cache (air when unknown). This
 * port does NOT substitute the authoritative ServerLevel: until the acknowledged-cache is
 * implemented, lookups fail safe exactly like Vulcan's empty cache (air / no tracked blocks).
 */
public class WorldBlockTracker {

	private final PlayerData playerData;

	public WorldBlockTracker(PlayerData playerData) {
		this.playerData = playerData;
	}

	public void reset() {
	}

	/** No acknowledged cache yet: Vulcan's empty-cache behavior is "no tracked block nearby". */
	public boolean hasTrackedBlockNearby(double x, double y, double z) {
		return false;
	}

	/** No acknowledged cache yet: Vulcan's empty-cache behavior is air. */
	public BlockState getBlockState(int x, int y, int z) {
		return Blocks.AIR.defaultBlockState();
	}

	public BlockState getBlockState(BlockPos position) {
		return getBlockState(position.getX(), position.getY(), position.getZ());
	}

	public BlockPos getPosition(BlockPos position, Direction face) {
		return position.relative(face);
	}
}
