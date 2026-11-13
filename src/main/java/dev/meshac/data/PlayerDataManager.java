package dev.meshac.data;

import dev.meshac.MeshServices;
import dev.meshac.check.manager.CheckManager;
import net.minecraft.server.level.ServerPlayer;

/** Per-player data lifecycle (join/quit), replacing Vulcan's PlayerListener. */
public final class PlayerDataManager {

	private PlayerDataManager() {
	}

	public static void onJoin(ServerPlayer player) {
		PlayerData data = new PlayerData(player);
		// Fabric JOIN occurs after the login packet, before later entity-scoped updates.
		data.setEntityId(player.getId());
		data.setUniqueId(player.getUUID());
		data.setPlayerName(player.getName().getString());
		data.setClientVersion(ClientVersionResolver.resolve(player.connection == null ? null : ((dev.meshac.mixin.ServerCommonPacketListenerImplAccessor) player.connection).meshac$getConnection()));
		data.setChecks(CheckManager.createChecks(data));
		MeshServices.players().put(player.getUUID(), data);
		dev.meshac.veil.Veil.grace(player, player.level().getServer().getTickCount());
	}

	/** Fabric COPY_FROM follows restoreFrom and precedes respawn's outbound state packets. */
	public static void onPlayerCopy(ServerPlayer previous, ServerPlayer replacement) {
		PlayerData data = get(previous);
		if (data != null && data.getPlayer() == previous) data.setPlayer(replacement);
	}

	public static void onQuit(ServerPlayer player) {
		MeshServices.players().remove(player.getUUID());
		dev.meshac.veil.Veil.forgetPlayer(player);
	}

	public static PlayerData get(net.minecraft.world.entity.player.Player player) {
		return player == null ? null : MeshServices.players().get(player.getUUID());
	}
}
