package dev.meshac.packet;

import dev.meshac.check.AbstractCheck;
import dev.meshac.data.PlayerData;
import dev.meshac.data.PlayerDataManager;
import dev.meshac.player.CollisionState;
import dev.meshac.util.Vec3d;
import java.util.Optional;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Outbound (clientbound) dispatch, port of Vulcan's EntityMetadataTracker.handlePacket routing.
 * VERIFY at compile: 26.3 accessor names (ClientboundPlayerPositionPacket.change().position(),
 * ClientboundSetEntityMotionPacket.getXa/getYa/getZa, ClientboundExplodePacket knockback,
 * ClientboundMoveEntityPacket entity resolution). Dropped vs upstream: legacy WINDOW_CONFIRMATION
 * branch (protocol gone), WorldBlockTracker.dispatchServerPacket (client-cache stub - PARITY.md).
 */
public final class OutboundDispatcher {

	private OutboundDispatcher() {
	}

	public static void onSend(ServerPlayer player, Packet<?> packet) {
		PlayerData data = PlayerDataManager.get(player);
		if (data == null) return;

		if (packet instanceof ClientboundLoginPacket login) {
			data.setEntityId(login.entityId());
			return;
		}
		if (packet instanceof ClientboundPingPacket ping) {
			if (ping.getId() < 0) data.getConfirmationTracker().recordSentConfirmation(ping.getId());
			return;
		}
		if (packet instanceof ClientboundExplodePacket explosion) {
			Optional<Vec3> knockback = explosion.playerKnockback();
			knockback.ifPresent(v -> data.getVelocityTracker().handleExplosion(new Vec3d(v.x, v.y, v.z)));
			return;
		}
		if (packet instanceof ClientboundSetEntityMotionPacket motion) {
			if (motion.getId() == data.getEntityId())
				data.getVelocityTracker().handleVelocity(new Vec3d(motion.getXa(), motion.getYa(), motion.getZa()));
			return;
		}
		if (packet instanceof ClientboundUpdateAttributesPacket attributes) {
			if (attributes.getEntityId() == data.getEntityId())
				data.getMotionState().handleAttributes(attributes);
			return;
		}
		if (packet instanceof ClientboundUpdateMobEffectPacket effect) {
			if (effect.getEntityId() == data.getEntityId())
				data.getMotionState().handlePotionEffect(effect.getEffect(), effect.getEffectAmplifier());
			return;
		}
		if (packet instanceof ClientboundRemoveMobEffectPacket removed) {
			if (removed.getEntityId() == data.getEntityId())
				data.getMotionState().handleRemovedPotionEffect(removed.getEffect());
			return;
		}
		if (packet instanceof ClientboundPlayerPositionPacket position) {
			Vec3 pos = position.change().position();
			data.getMotionState().handleServerPosition(pos.x, pos.y, pos.z);
			data.getRelativeMoveTracker().clearTargetPositions();
			MeshPacketEvent event = new MeshPacketEvent(MeshPacketType.SERVER_POSITION, player, packet, false);
			for (AbstractCheck check : data.getChecks()) check.handlePacket(event);
			return;
		}
		if (packet instanceof ClientboundSetEntityDataPacket metadata) {
			if (metadata.id() == data.getEntityId())
				data.getMotionState().handlePlayerMetadata(metadata);
			return;
		}
		if (packet instanceof ClientboundPlayerAbilitiesPacket abilities) {
			data.getCollisionState().handlePlayerAbilities(
					new CollisionState.ServerAbilities(abilities.canFly(), abilities.getFlyingSpeed()));
			return;
		}
		if (packet instanceof ClientboundGameEventPacket gameEvent) {
			data.getMotionState().handleGameState(gameEvent.getEvent(), gameEvent.getParam());
			return;
		}
		if (packet instanceof ClientboundMoveEntityPacket.Pos posMove) {
			routeEntityMove(player, data, posMove);
			return;
		}
		if (packet instanceof ClientboundMoveEntityPacket.PosRot posRot) {
			routeEntityMove(player, data, posRot);
			return;
		}
		if (packet instanceof ClientboundMoveEntityPacket.Rot rot) {
			int id = entityId(player, rot);
			if (id >= 0) data.getEntityTracker().handleRotation(id, rot.getyRot(), rot.getxRot());
			return;
		}
		if (packet instanceof ClientboundTeleportEntityPacket teleport) {
			data.getEntityTracker().handleTeleport(teleport.id(), teleport.x(), teleport.y(), teleport.z(), teleport.yRot(), teleport.xRot());
			return;
		}
		if (packet instanceof ClientboundAddEntityPacket spawn) {
			data.getEntityTracker().handleSpawn(spawn.id(), spawn.getUUID(), spawn.getType(), spawn.getX(), spawn.getY(), spawn.getZ());
			return;
		}
		if (packet instanceof ClientboundRemoveEntitiesPacket removed) {
			data.getEntityTracker().handleDestroy(removed.getEntityIds().toIntArray());
			return;
		}
		if (packet instanceof ClientboundSetPassengersPacket passengers) {
			data.getVehicleTracker().handlePassengers(passengers.getVehicle(), passengers.getPassengers());
		}
		// All other server packet types do not change tracked state here.
	}

	private static void routeEntityMove(ServerPlayer player, PlayerData data, ClientboundMoveEntityPacket packet) {
		int id = entityId(player, packet);
		if (id < 0) return;
		if (packet instanceof ClientboundMoveEntityPacket.PosRot posRot) {
			data.getEntityTracker().handleRelativeMoveAndRotation(id, posRot.getXa() / 4096.0, posRot.getYa() / 4096.0, posRot.getZa() / 4096.0, posRot.getyRot(), posRot.getxRot());
			data.getRelativeMoveTracker().handleTargetMove(id, posRot.getXa() / 4096.0, posRot.getYa() / 4096.0, posRot.getZa() / 4096.0);
		} else if (packet instanceof ClientboundMoveEntityPacket.Pos pos) {
			data.getEntityTracker().handleRelativeMove(id, pos.getXa() / 4096.0, pos.getYa() / 4096.0, pos.getZa() / 4096.0);
			data.getRelativeMoveTracker().handleTargetMove(id, pos.getXa() / 4096.0, pos.getYa() / 4096.0, pos.getZa() / 4096.0);
		}
	}

	private static int entityId(ServerPlayer player, ClientboundMoveEntityPacket packet) {
		try {
			net.minecraft.world.entity.Entity entity = packet.getEntity(player.level());
			return entity != null ? entity.getId() : -1;
		} catch (Throwable t) {
			return -1;
		}
	}
}
