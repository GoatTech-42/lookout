package dev.meshac.event;

import dev.meshac.MeshServices;
import dev.meshac.check.AbstractCheck;
import dev.meshac.config.MeshSettings;
import dev.meshac.data.PlayerData;
import dev.meshac.data.PlayerDataManager;
import dev.meshac.player.CollisionState;
import dev.meshac.util.Location;
import dev.meshac.world.BlockUtils;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

/**
 * Move-event feed. Port of Vulcan's BlockInteractionListener.handlePlayer(PlayerMoveEvent):
 * runs on the main thread AFTER vanilla applied the move, reads the AUTHORITATIVE ServerLevel,
 * feeds CollisionState, then dispatches the event to every check.
 * PARITY STATUS: NON-PARITY, unresolved. Upstream Bukkit feeds this from the server-authoritative
 * world; on Fabric the authoritative ServerLevel is NOT the same input domain as the
 * client-acknowledged world cache some Vulcan logic was written against. Whether authoritative
 * feeds change any check outcome is an open parity question - do not treat this feed as proven
 * faithful until the authoritative-vs-client-cache distinction is resolved per check family.
 * Dropped: Folia scan branch (not a Folia server), 1.13 swim-fix branch gated on client version
 * UNKNOWN keeps upstream gating (condition false until version resolution lands - PARITY.md).
 */
public final class MoveEventDispatcher {

	private MoveEventDispatcher() {
	}

	public static void fireMove(ServerPlayer player, double fromX, double fromY, double fromZ) {
		ServerLevel level = player.level();
		Location from = new Location(level, fromX, fromY, fromZ, player.getYRot(), player.getXRot());
		Location to = new Location(level, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
		if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) return;
		PlayerData data = PlayerDataManager.get(player);
		if (data == null) return;
		MeshMoveEvent event = new MeshMoveEvent(player, from, to);
		CollisionState collisionState = data.getCollisionState();
		if (collisionState.isFrozen()) {
			player.sendSystemMessage(dev.meshac.util.TextUtil.parse(MeshSettings.messagesFrozen));
			event.setTo(new Location(level, collisionState.getSetbackX(), collisionState.getSetbackY(), collisionState.getSetbackZ(),
					data.getRotationTracker().getYaw(), data.getRotationTracker().getPitch()));
		}
		int x = (int) Math.floor(event.getTo().getX());
		int y = (int) Math.floor(event.getTo().getY());
		int z = (int) Math.floor(event.getTo().getZ());
		List<BlockState> surrounding = BlockUtils.getSurroundingMaterials(level, x, y, z, 1);
		collisionState.setSurroundingMaterials(surrounding);
		List<BlockState> deepFloor = BlockUtils.getDeepFloorMaterials(level, x, y, z);
		collisionState.setDeepFloorMaterials(deepFloor);
		BlockPos feet = new BlockPos(x, y, z);
		BlockState feetState = level.getBlockState(feet);
		BlockState below = level.getBlockState(feet.below(1));
		BlockState below2 = level.getBlockState(feet.below(2));
		BlockState below3 = level.getBlockState(feet.below(3));
		BlockState above = level.getBlockState(feet.above(1));
		BlockState above2 = level.getBlockState(feet.above(2));
		if (isWaterlogged(below) || isWaterlogged(feetState)) collisionState.recordFeetWaterlogged();
		if (isWaterlogged(above) || isWaterlogged(above2)) collisionState.recordHeadWaterlogged();
		collisionState.setGroundMaterial(below);
		collisionState.setSubGroundMaterial(below2);
		collisionState.setDeepGroundMaterial(below3);
		if (player.getVehicle() != null) {
			collisionState.setAreaScanMaterials(surrounding);
		}
		List nearby = level.getEntities(player, AABB.ofSize(player.position(), 3.0, 4.0, 3.0));
		collisionState.setNearbyEntities(nearby);
		for (AbstractCheck check : data.getChecks()) { if (dev.meshac.check.ContextGate.isGated(check.getClass().getSimpleName(), data)) continue; check.handleEvent(event); }
	}

	private static boolean isWaterlogged(BlockState state) {
		return state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED);
	}
}
