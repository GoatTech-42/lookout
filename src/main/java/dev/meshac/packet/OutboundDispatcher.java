package dev.meshac.packet;

import dev.meshac.check.AbstractCheck;
import dev.meshac.data.PlayerData;
import dev.meshac.data.PlayerDataManager;
import dev.meshac.player.CollisionState;
import dev.meshac.util.Vec3d;
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
import net.minecraft.server.network.ServerGamePacketListenerImpl;
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
			data.setEntityId(login.playerId());
			data.getWorldBlockTracker().reset();
			return;
		}
		if (packet instanceof net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket chunk) {
			data.getWorldBlockTracker().handleChunkPacket(player.level(), chunk);
			return;
		}
		if (packet instanceof net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket forget) {
			data.getWorldBlockTracker().handleChunkForget(forget.pos());
			return;
		}
		if (packet instanceof net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket blockUpdate) {
			data.getWorldBlockTracker().handleBlockUpdate(blockUpdate.getPos(), blockUpdate.getBlockState());
			return;
		}
		if (packet instanceof net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket sectionUpdates) {
			data.getWorldBlockTracker().handleSectionUpdates(sectionUpdates::runUpdates);
			return;
		}
		if (packet instanceof net.minecraft.network.protocol.game.ClientboundRespawnPacket) {
			data.getWorldBlockTracker().reset();
			return;
		}
		if (packet instanceof ClientboundPingPacket ping) {
			if (ping.getId() < 0) data.getConfirmationTracker().recordSentConfirmation(ping.getId());
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
			if (removed.entityId() == data.getEntityId())
				data.getMotionState().handleRemovedPotionEffect(removed.effect());
			return;
		}
		if (packet instanceof ClientboundPlayerPositionPacket position) {
			Vec3 pos = position.change().position();
			data.getMotionState().handleServerPosition(pos.x, pos.y, pos.z);
			data.getRelativeMoveTracker().clearTargetPositions();
			MeshPacketEvent event = new MeshPacketEvent(MeshPacketType.SERVER_POSITION, player, packet, false);
			for (AbstractCheck check : data.getChecks()) { if (dev.meshac.check.ContextGate.isGated(check.getClass().getSimpleName(), data)) continue; check.handlePacket(event); }
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
			if (gameEvent.getEvent() == ClientboundGameEventPacket.CHANGE_GAME_MODE) data.getMotionState().handleGameState((byte) 3, gameEvent.getParam());
			else if (gameEvent.getEvent() == ClientboundGameEventPacket.START_RAINING) data.getMotionState().handleGameState((byte) 1, gameEvent.getParam());
			else if (gameEvent.getEvent() == ClientboundGameEventPacket.STOP_RAINING) data.getMotionState().handleGameState((byte) 2, gameEvent.getParam());
			return;
		}
		if (packet instanceof ClientboundMoveEntityPacket move) {
			dev.meshac.mixin.ClientboundMoveEntityPacketAccessor acc = (dev.meshac.mixin.ClientboundMoveEntityPacketAccessor) move;
			int moveId = acc.meshac$getEntityId();
			dev.meshac.tracker.TrackedEntity tracked = data.getEntityTracker().getTrackedEntity(moveId);
			if (tracked == null) return;
			var delta = dev.meshac.compat.VersionBridge.relativeDelta(acc, tracked);
			double dx = delta.x, dy = delta.y, dz = delta.z;
			if (acc.meshac$hasRot()) {
				float yaw = acc.meshac$getYRot() * 360.0f / 256.0f;
				float pitch = acc.meshac$getXRot() * 360.0f / 256.0f;
				data.getEntityTracker().handleRelativeMoveAndRotation(moveId, dx, dy, dz, yaw, pitch);
				data.getRelativeMoveTracker().handleTargetMove(moveId, dx, dy, dz);
			} else if (acc.meshac$hasPos()) {
				data.getEntityTracker().handleRelativeMove(moveId, dx, dy, dz);
				data.getRelativeMoveTracker().handleTargetMove(moveId, dx, dy, dz);
			} else if (acc.meshac$hasRot() == false && acc.meshac$hasPos() == false) {
				// rotation-only variant
			}
			return;
		}
		if (packet instanceof ClientboundTeleportEntityPacket teleport) {
			data.getEntityTracker().handleTeleport(teleport.id(), teleport.change().position().x, teleport.change().position().y, teleport.change().position().z, teleport.change().yRot(), teleport.change().xRot());
			return;
		}
		if (packet instanceof ClientboundAddEntityPacket spawn) {
			data.getEntityTracker().handleSpawn(spawn.getId(), spawn.getUUID(), spawn.getType(), spawn.getX(), spawn.getY(), spawn.getZ());
			return;
		}
		if (packet instanceof ClientboundRemoveEntitiesPacket removed) {
			data.getEntityTracker().handleDestroy(dev.meshac.compat.VersionBridge.removedIds(removed));
			return;
		}
		if (packet instanceof ClientboundSetPassengersPacket passengers) {
			data.getVehicleTracker().handlePassengers(passengers.getVehicle(), passengers.getPassengers());
		}
		// All other server packet types do not change tracked state here.
	}


	/** Prepare upstream acknowledgement state, returning only this write's after-send task. */
	public static Runnable beforeVelocityWrite(PlayerData data, Packet<?> packet) {
		if (packet instanceof ClientboundExplodePacket explosion) {
			return explosion.playerKnockback().map(v -> data.getVelocityTracker().handleExplosion(new Vec3d(v.x, v.y, v.z))).orElse(null);
		}
		if (packet instanceof ClientboundSetEntityMotionPacket motion
				&& dev.meshac.compat.VersionBridge.motionEntity(motion) == data.getEntityId()) {
			Vec3 velocity = dev.meshac.compat.VersionBridge.motion(motion);
			return data.getVelocityTracker().handleVelocity(new Vec3d(velocity.x, velocity.y, velocity.z));
		}
		return null;
	}

    public static Runnable beforeVelocityWrite(net.minecraft.network.Connection connection, Object message) {
        if (!(message instanceof net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
                || message instanceof net.minecraft.network.protocol.game.ClientboundExplodePacket)
                || !(connection.getPacketListener() instanceof ServerGamePacketListenerImpl game)
                || game.player.connection != game) return null;
        PlayerData data = PlayerDataManager.get(game.player);
        if (data == null || !dev.meshac.tracker.ConfirmationTransport.isPlay(data, connection)) return null;
        Runnable task = dev.meshac.packet.OutboundDispatcher.beforeVelocityWrite(data, (Packet<?>) message);
        // Defensive session/phase policy; upstream tasks themselves run even on failed promises.
        return task == null ? null : () -> {
            if (dev.meshac.tracker.ConfirmationTransport.isPlay(data, connection)
                    && data.getPlayer().connection == connection.getPacketListener()) task.run();
        };
    }

}
