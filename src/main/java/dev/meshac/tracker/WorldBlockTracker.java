package dev.meshac.tracker;

import dev.meshac.data.PlayerData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Partial outbound sulfur-proximity view based on Vulcan's WorldBlockTracker:
 * fed by outbound chunk columns, section multi-block updates, single block updates,
 * chunk forgets and respawn/login resets. Updates use the confirmation tracker;
 * acknowledgement ordering and JOIN anchoring remain unqualified.
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
		generation++;
		trackedSulfur.clear();
	}

	/** Read the packet-owned encoded bytes now; defer only applying immutable results. */
	public void handleChunkPacket(net.minecraft.server.level.ServerLevel level,
			net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket packet) {
		int chunkX = dev.meshac.compat.VersionBridge.chunkX(packet);
		int chunkZ = dev.meshac.compat.VersionBridge.chunkZ(packet);
		var payload = dev.meshac.compat.VersionBridge.chunkData(packet);
		Map<Integer, Set<Long>> snapshot = new HashMap<>();
		var buffer = payload.getReadBuffer();
		try {
			LevelChunkSection[] sections = new LevelChunkSection[level.getSectionsCount()];
			for (int i = 0; i < sections.length; i++) {
				sections[i] = new LevelChunkSection(level.palettedContainerFactory());
				sections[i].read(buffer);
			}
			if (buffer.isReadable()) throw new IllegalStateException("Trailing chunk section bytes: " + buffer.readableBytes());
			int firstSectionY = level.getMinSectionY();
			// Upstream indexes emitted tile entities, then checks the corresponding emitted section state.
			dev.meshac.compat.VersionBridge.blockEntityPositions(payload, chunkX, chunkZ, pos -> {
				int section = (pos.getY() >> 4) - firstSectionY;
				if (section < 0 || section >= sections.length || pos.getY() < MIN_Y || pos.getY() > MAX_Y) return;
				if (isWetPotentSulfur(sections[section].getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15))) {
					int index = (pos.getY() - MIN_Y) >> 4;
					snapshot.computeIfAbsent(index, key -> new HashSet<>()).add(positionKey(pos.getX(), pos.getY(), pos.getZ()));
				}
			});
		} catch (RuntimeException failure) {
			reset();
			dev.meshac.MeshServices.log("[parity] chunk payload decode failed; world tracking reset and eligibility remains closed: " + failure);
			return;
		} finally {
			buffer.release();
		}
		Map<Integer, Set<Long>> frozen = new HashMap<>();
		snapshot.forEach((section, positions) -> frozen.put(section, Set.copyOf(positions)));
		afterConfirmation(() -> {
			clearChunk(chunkX, chunkZ);
			if (!frozen.isEmpty()) {
				Map<Integer, Set<Long>> mutable = new HashMap<>();
				frozen.forEach((section, positions) -> mutable.put(section, new HashSet<>(positions)));
				trackedSulfur.put(chunkKey(chunkX, chunkZ), mutable);
			}
		});
	}

	private long generation;
	private void afterConfirmation(Runnable action) {
		long capturedGeneration = generation;
		playerData.getConfirmationTracker().runAfterConfirmation(() -> {
			if (generation == capturedGeneration) action.run();
		});
	}

	public void handleBlockUpdate(BlockPos pos, BlockState state) {
		BlockPos frozenPosition = pos.immutable();
		afterConfirmation(() -> applyBlockChange(frozenPosition, state));
	}

	public void handleSectionUpdates(ClientboundSectionUpdatesConsumer consumer) {
		java.util.List<java.util.Map.Entry<BlockPos, BlockState>> frozen = new java.util.ArrayList<>();
		consumer.consume((pos, state) -> frozen.add(java.util.Map.entry(pos.immutable(), state)));
		afterConfirmation(() -> frozen.forEach(entry -> applyBlockChange(entry.getKey(), entry.getValue())));
	}

	/** Functional bridge so this class need not import the packet type in tests. */
	public interface ClientboundSectionUpdatesConsumer {
		void consume(java.util.function.BiConsumer<BlockPos, BlockState> out);
	}

	public void handleChunkForget(ChunkPos pos) {
		afterConfirmation(() -> clearChunk(ChunkPos.getX(dev.meshac.compat.VersionBridge.chunkKey(pos)), ChunkPos.getZ(dev.meshac.compat.VersionBridge.chunkKey(pos))));
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
		return dev.meshac.compat.VersionBridge.wetSulfur(state);
	}

	private static long chunkKey(int chunkX, int chunkZ) {
		return ((long) chunkX & 0xFFFFFFFFL) << 32 | ((long) chunkZ & 0xFFFFFFFFL);
	}

	private static long positionKey(int x, int y, int z) {
		return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | (long) (y & 0xFFF);
	}

	private static int unpackX(long key) { return (int) (key >> 38); } // arithmetic: sign-extends the 26-bit field
	private static int unpackZ(long key) { return (int) ((key << 26) >> 38); } // move 26-bit field to top, arithmetic back: signed
	private static int unpackY(long key) { return (int) ((key << 52) >> 52); } // sign-extended 12-bit Y (negative Y decodes negative)

	private static int floorInt(double value) {
		int truncated = (int) value;
		return value < truncated ? truncated - 1 : truncated;
	}
}
