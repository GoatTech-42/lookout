package dev.meshac.tracker;

import dev.meshac.data.PlayerData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PotentSulfurBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PotentSulfurState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Client-acknowledged world view, faithful to Vulcan's WorldBlockTracker semantics:
 * fed by outbound chunk columns, section multi-block updates, single block updates,
 * chunk forgets and respawn/login resets, all applied after client confirmation
 * (upstream defers every apply through the confirmation tracker).
 * hasTrackedBlockNearby is the upstream wet-potent-sulfur proximity query, NOT
 * generic chunk presence. Full block-state mirroring is not implemented:
 * getBlockState keeps the empty-cache air fallback and any future consumer must
 * re-open the parity review before use. ServerLevel is never substituted here.
 */
public class WorldBlockTracker {

	private static final int MIN_Y = -64;
	private static final int MAX_Y = 319;

	private final PlayerData playerData;
	/** chunkKey -> sectionIndex -> packed position keys of wet potent sulfur. */
	private final Map<Long, Map<Integer, Set<Long>>> trackedSulfur = new HashMap<>();

	public WorldBlockTracker(PlayerData playerData) {
		this.playerData = playerData;
	}

	public void reset() {
		trackedSulfur.clear();
	}

	public void handleChunkColumn(LevelChunk chunk) {
		if (chunk == null) return;
		long packedPos = chunk.getPos().pack();
		int chunkX = ChunkPos.getX(packedPos), chunkZ = ChunkPos.getZ(packedPos);
		playerData.getConfirmationTracker().runAfterConfirmation(() -> {
			clearChunk(chunkX, chunkZ);
			LevelChunkSection[] sections = chunk.getSections();
			for (int i = 0; i < sections.length; i++) {
				LevelChunkSection section = sections[i];
				if (section == null || section.hasOnlyAir()) continue;
				dev.meshac.veil.Veil.Mod[] mods = dev.meshac.veil.Veil.mods(chunk, i, section);
				Set<Integer> hidden = new HashSet<>();
				if (mods != null) for (dev.meshac.veil.Veil.Mod m : mods) hidden.add(m.idx());
				int sectionMinY = chunk.getSectionYFromSectionIndex(i) << 4;
				scanSection(chunkX, chunkZ, i, sectionMinY, section, hidden);
			}
		});
	}

	private void scanSection(int chunkX, int chunkZ, int sectionIndex, int sectionMinY, LevelChunkSection section, Set<Integer> hidden) {
		for (int ly = 0; ly < 16; ly++) {
			for (int lz = 0; lz < 16; lz++) {
				for (int lx = 0; lx < 16; lx++) {
					int idx = (ly << 8) | (lz << 4) | lx;
					if (hidden.contains(idx)) continue;
					BlockState state = section.getBlockState(lx, ly, lz);
					if (isWetPotentSulfur(state)) {
						track(chunkX, chunkZ, sectionIndex, (chunkX << 4) + lx, sectionMinY + ly, (chunkZ << 4) + lz);
					}
				}
			}
		}
	}

	public void handleBlockUpdate(BlockPos pos, BlockState state) {
		playerData.getConfirmationTracker().runAfterConfirmation(() -> applyBlockChange(pos, state));
	}

	public void handleSectionUpdates(ClientboundSectionUpdatesConsumer consumer) {
		playerData.getConfirmationTracker().runAfterConfirmation(() ->
				consumer.consume(this::applyBlockChange));
	}

	/** Functional bridge so this class need not import the packet type in tests. */
	public interface ClientboundSectionUpdatesConsumer {
		void consume(java.util.function.BiConsumer<BlockPos, BlockState> out);
	}

	public void handleChunkForget(ChunkPos pos) {
		playerData.getConfirmationTracker().runAfterConfirmation(() -> clearChunk(ChunkPos.getX(pos.pack()), ChunkPos.getZ(pos.pack())));
	}

	private void applyBlockChange(BlockPos pos, BlockState state) {
		int chunkX = pos.getX() >> 4, chunkZ = pos.getZ() >> 4;
		int sectionIndex = (pos.getY() - MIN_Y) >> 4;
		if (sectionIndex < 0 || sectionIndex >= (MAX_Y - MIN_Y + 1) / 16) return;
		if (isWetPotentSulfur(state)) track(chunkX, chunkZ, sectionIndex, pos.getX(), pos.getY(), pos.getZ());
		else untrack(chunkX, chunkZ, sectionIndex, pos.getX(), pos.getY(), pos.getZ());
	}

	private void track(int chunkX, int chunkZ, int sectionIndex, int x, int y, int z) {
		trackedSulfur.computeIfAbsent(chunkKey(chunkX, chunkZ), k -> new HashMap<>())
				.computeIfAbsent(sectionIndex, k -> new HashSet<>())
				.add(positionKey(x, y, z));
	}

	private void untrack(int chunkX, int chunkZ, int sectionIndex, int x, int y, int z) {
		Map<Integer, Set<Long>> sections = trackedSulfur.get(chunkKey(chunkX, chunkZ));
		if (sections == null) return;
		Set<Long> positions = sections.get(sectionIndex);
		if (positions != null) {
			positions.remove(positionKey(x, y, z));
			if (positions.isEmpty()) sections.remove(sectionIndex);
		}
		if (sections.isEmpty()) trackedSulfur.remove(chunkKey(chunkX, chunkZ));
	}

	private void clearChunk(int chunkX, int chunkZ) {
		trackedSulfur.remove(chunkKey(chunkX, chunkZ));
	}

	/** Upstream query: wet potent sulfur within 3.5 horizontally and [0, 20) above. */
	public boolean hasTrackedBlockNearby(double x, double y, double z) {
		int minChunkX = floorInt(x - 3.5) >> 4;
		int maxChunkX = floorInt(x + 3.5) >> 4;
		int minChunkZ = floorInt(z - 3.5) >> 4;
		int maxChunkZ = floorInt(z + 3.5) >> 4;
		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				Map<Integer, Set<Long>> sections = trackedSulfur.get(chunkKey(chunkX, chunkZ));
				if (sections == null) continue;
				for (Set<Long> positions : sections.values()) {
					for (long key : positions) {
						int px = unpackX(key), py = unpackY(key), pz = unpackZ(key);
						double dx = x - (px + 0.5), dz = z - (pz + 0.5), dy = y - (py + 0.5);
						if (dx * dx + dz * dz < 12.25 && dy >= 0.0 && dy < 20.0) return true;
					}
				}
			}
		}
		return false;
	}

	/** Empty-cache fallback (air). Full block mirroring is NOT implemented; see class doc. */
	public BlockState getBlockState(int x, int y, int z) {
		return Blocks.AIR.defaultBlockState();
	}

	public BlockState getBlockState(BlockPos position) {
		return getBlockState(position.getX(), position.getY(), position.getZ());
	}

	public BlockPos getPosition(BlockPos position, Direction face) {
		return position.relative(face);
	}

	private static boolean isWetPotentSulfur(BlockState state) {
		return state != null && state.getBlock() instanceof PotentSulfurBlock
				&& state.getValue(PotentSulfurBlock.STATE) != PotentSulfurState.DRY;
	}

	private static long chunkKey(int chunkX, int chunkZ) {
		return ((long) chunkX & 0xFFFFFFFFL) << 32 | ((long) chunkZ & 0xFFFFFFFFL);
	}

	private static long positionKey(int x, int y, int z) {
		return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | (long) (y & 0xFFF);
	}

	private static int unpackX(long key) { return (int) (key >> 38); }
	private static int unpackZ(long key) { return (int) ((key >> 12) & 0x3FFFFFFL); }
	private static int unpackY(long key) { return (int) (key & 0xFFFL); }

	private static int floorInt(double value) {
		int truncated = (int) value;
		return value < truncated ? truncated - 1 : truncated;
	}
}
