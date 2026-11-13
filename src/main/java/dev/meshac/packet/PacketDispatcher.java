package dev.meshac.packet;

import dev.meshac.check.AbstractCheck;
import dev.meshac.data.PlayerData;
import dev.meshac.data.PlayerDataManager;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.level.ServerPlayer;

/**
 * Inbound packet dispatch. Mirrors the processing order of Vulcan's PacketListener.handlePacket:
 * confirmations first, then tracker updates in upstream order, then every check's handlePacket,
 * then acknowledged-task flush while mitigation is pending.
 * Dropped vs upstream: WorldBlockTracker.handleBlockPlace (client-cache tracker is a fail-safe
 * stub; dependent checks are gated by ContextGate - see PARITY.md).
 */
public final class PacketDispatcher {

	private PacketDispatcher() {
	}

	private static PlayerData data(ServerPlayer player) {
		return PlayerDataManager.get(player);
	}

	public static void onMove(ServerPlayer player, ServerboundMovePlayerPacket p) {
		PlayerData data = data(player);
		if (data == null) return;
		Flying flying = Packets.toFlying(p, player);
		MeshPacketEvent event = new MeshPacketEvent(Packets.flyingType(p), player, p, true);
		data.setLastFlyingPacket(flying);
		if (flying.hasRotationChanged())
			data.getRotationTracker().handleRotation(flying.getYaw(), flying.getPitch());
		data.getCollisionState().processMovement(flying);
		data.getMotionState().processMovement(flying);
		data.getVelocityTracker().processMovement(flying);
		data.getConnectionTracker().processMovement();
		data.getRelativeMoveTracker().processMovement();
		if (data.isHandshakeComplete())
			data.getConfirmationTracker().checkResponseTimeout();
		dispatchChecks(data, event);
		if (data.isMitigationPending()) {
			data.getCollisionState().runAcknowledgedTasks();
			data.setMitigationPending(false);
		}
	}

	public static void onInteractEntity(ServerPlayer player, ServerboundInteractPacket p) {
		dispatchInteraction(player, p, Packets.toInteractEntity(p));
	}

	public static void dispatchInteraction(ServerPlayer player, Object raw, InteractEntity interaction) {
		PlayerData data = data(player);
		if (data == null) return;
		data.setLastInteractionPacket(interaction);
		data.getMotionState().handleEntityInteraction(interaction);
		boolean attack = interaction.getAction() == InteractEntity.Action.ATTACK;
		data.getRelativeMoveTracker().handleInteract(interaction.getEntityId(), attack);
		if (attack) {
			net.minecraft.world.entity.Entity target = player.level().getEntity(interaction.getEntityId());
			if (target != null) dev.meshac.veil.Veil.attacked(player, target, player.level().getServer().getTickCount());
		}
		dispatchChecks(data, new MeshPacketEvent(MeshPacketType.INTERACT_ENTITY, player, raw, true));
	}

	public static void onEntityAction(ServerPlayer player, ServerboundPlayerCommandPacket p) {
		PlayerData data = data(player);
		if (data == null) return;
		EntityAction action = Packets.toEntityAction(p);
		if (action == null) return;
		data.setLastEntityActionPacket(action);
		data.getMotionState().handleEntityAction(action);
		dispatchChecks(data, new MeshPacketEvent(MeshPacketType.ENTITY_ACTION, player, p, true));
	}

	public static void onDigging(ServerPlayer player, ServerboundPlayerActionPacket p) {
		PlayerData data = data(player);
		if (data == null) return;
		Digging digging = Packets.toDigging(p);
		data.setLastDiggingPacket(digging);
		data.getMotionState().handleDigging(digging);
		data.getClickTimingData().recordDig();
		dispatchChecks(data, new MeshPacketEvent(MeshPacketType.PLAYER_DIGGING, player, p, true));
		// Upstream Bukkit fires PlayerInteractEvent(LEFT_CLICK_BLOCK) from the same input;
		// FastBreakA and friends consume it as an event, not a packet.
		if (digging.getAction() == Digging.Action.START_DIGGING) {
			dev.meshac.event.MeshBlock clicked = new dev.meshac.event.MeshBlock(player.level(), digging.getBlockPosition());
			dispatchEvents(data, new dev.meshac.event.MeshInteractEvent(player,
					dev.meshac.event.MeshInteractEvent.Action.LEFT_CLICK_BLOCK, clicked, digging.getBlockFace(),
					net.minecraft.world.InteractionHand.MAIN_HAND, player.getMainHandItem()));
		}
	}

	public static void onAbilities(ServerPlayer player, Object p) {
		PlayerData data = data(player);
		if (data == null) return;
		dispatchChecks(data, new MeshPacketEvent(MeshPacketType.PLAYER_ABILITIES, player, p, true));
	}

	public static void dispatchEvents(PlayerData data, dev.meshac.event.MeshEvent event) {
		for (dev.meshac.check.AbstractCheck check : data.getChecks()) { if (dev.meshac.check.ContextGate.isGated(check.getClass().getSimpleName(), data)) continue; check.handleEvent(event); }
	}

	public static void onPlacement(ServerPlayer player, ServerboundUseItemOnPacket p) {
		PlayerData data = data(player);
		if (data == null) return;
		data.setLastPlacementPacket(Packets.toPlacement(p));
		data.getClickTimingData().recordPlacement();
		// Upstream WorldBlockTracker.handleBlockPlace: cache the placed block's default state.
		var placedItem = player.getItemInHand(dev.meshac.compat.VersionBridge.hand(p));
		if (placedItem.getItem() instanceof net.minecraft.world.item.BlockItem blockItem) {
			net.minecraft.core.BlockPos placedPos = dev.meshac.compat.VersionBridge.hit(p).getBlockPos().relative(dev.meshac.compat.VersionBridge.hit(p).getDirection());
			data.getWorldBlockTracker().handleBlockUpdate(placedPos, blockItem.getBlock().defaultBlockState());
		}
		dispatchChecks(data, new MeshPacketEvent(MeshPacketType.PLAYER_BLOCK_PLACEMENT, player, p, true));
	}

	public static void onHeldItemChange(ServerPlayer player, ServerboundSetCarriedItemPacket p) {
		PlayerData data = data(player);
		if (data == null) return;
		data.setLastHeldItemPacket(Packets.toHeldItemChange(p));
		dispatchChecks(data, new MeshPacketEvent(MeshPacketType.HELD_ITEM_CHANGE, player, p, true));
	}

	public static void onSwing(ServerPlayer player, Object p) {
		PlayerData data = data(player);
		if (data == null) return;
		data.getClickTimingData().recordSwing();
		dispatchChecks(data, new MeshPacketEvent(MeshPacketType.ANIMATION, player, p, true));
	}

	public static void onKeepAlive(ServerPlayer player, Object p) {
		PlayerData data = data(player);
		if (data == null) return;
		data.getConnectionTracker().recordKeepAlive();
		dispatchChecks(data, new MeshPacketEvent(MeshPacketType.KEEP_ALIVE, player, p, true));
	}

	public static void onClientStatusRespawn(ServerPlayer player, Object p) {
		PlayerData data = data(player);
		if (data == null) return;
		data.getMotionState().setLastRespawnTick();
		dispatchChecks(data, new MeshPacketEvent(MeshPacketType.CLIENT_STATUS, player, p, true));
	}

	/** Confirmation ids from ServerboundPongPacket or legacy container ack. Verbatim upstream logic. */
	public static void onConfirmation(ServerPlayer player, int id, MeshPacketType type, Object raw) {
		PlayerData data = data(player);
		if (data == null) return;
		data.getRawConfirmationIds().add(id);
		if (!data.isHandshakeComplete() && id == data.getConfirmationTracker().getJoinConfirmationId()) {
			data.setHandshakeComplete(true);
		} else if (id < 0 && data.isHandshakeComplete()) {
			data.getConfirmationTracker().handleConfirmationReply(id);
			if (!data.isConnectionKickQueued())
				data.getRepliedConfirmationIds().add(id);
		}
		dispatchChecks(data, new MeshPacketEvent(type, player, raw, true));
	}

	public static void dispatchChecks(PlayerData data, MeshPacketEvent event) {
		for (AbstractCheck check : data.getChecks()) { if (dev.meshac.check.ContextGate.isGated(check.getClass().getSimpleName(), data)) continue; check.handlePacket(event); }
	}
}
