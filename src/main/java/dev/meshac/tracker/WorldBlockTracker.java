package dev.meshac.tracker;

import dev.meshac.data.PlayerData;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client-acknowledged world view, fed from outbound chunk-level packets
 * (level chunk load, forget chunk, single block update). Chunk payload blocks
 * are not mirrored yet; outside tracked updates, lookups keep Vulcan's
 * empty-cache behavior (air), so nothing downstream gets a guessed block.
 * ServerLevel state is never substituted here.
 */
public class WorldBlockTracker {

	private static final int MAX_TRACKED_UPDATES = 4096;

	private final PlayerData playerData;
	private final Set<Long> loadedChunks = new HashSet<>();
	private final Map<BlockPos, BlockState> trackedUpdates = new LinkedHashMap<>() {
		@Override protected boolean removeEldestEntry(Map.Entry<BlockPos, BlockState> eldest) {
			return size() > MAX_TRACKED_UPDATES;
		}
	};

	public WorldBlockTracker(PlayerData playerData) {
		this.playerData = playerData;
	}

	public void reset() {
		loadedChunks.clear();
		trackedUpdates.clear();
	}

	public void handleChunkLoad(int x, int z) {
		loadedChunks.add(new ChunkPos(x, z).pack());
	}

	public void handleChunkForget(ChunkPos pos) {
		loadedChunks.remove(pos.pack());
		trackedUpdates.keySet().removeIf(p -> ChunkPos.containing(p).pack() == pos.pack());
	}

	public void handleBlockUpdate(BlockPos pos, BlockState state) {
		if (loadedChunks.contains(ChunkPos.containing(pos).pack())) {
			trackedUpdates.put(pos.immutable(), state);
		}
	}

	/** True when the client has been sent chunk data covering this position. */
	public boolean hasTrackedBlockNearby(double x, double y, double z) {
		return loadedChunks.contains(new ChunkPos(((int) Math.floor(x)) >> 4, ((int) Math.floor(z)) >> 4).pack());
	}

	/** Tracked update or Vulcan's empty-cache fallback (air); never a ServerLevel read. */
	public BlockState getBlockState(int x, int y, int z) {
		BlockState updated = trackedUpdates.get(new BlockPos(x, y, z));
		return updated != null ? updated : Blocks.AIR.defaultBlockState();
	}

	public BlockState getBlockState(BlockPos position) {
		return getBlockState(position.getX(), position.getY(), position.getZ());
	}

	public BlockPos getPosition(BlockPos position, Direction face) {
		return position.relative(face);
	}
}
