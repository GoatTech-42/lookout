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
		data.setChecks(CheckManager.createChecks(data));
		MeshServices.players().put(player.getUUID(), data);
		dev.meshac.veil.Veil.grace(player, player.level().getServer().getTickCount());
	}

	public static void onQuit(ServerPlayer player) {
		MeshServices.players().remove(player.getUUID());
		dev.meshac.veil.Veil.forgetPlayer(player);
	}

	public static PlayerData get(ServerPlayer player) {
		return MeshServices.players().get(player.getUUID());
	}
}
