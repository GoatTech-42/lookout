package dev.meshac.data;

import dev.meshac.MeshServices;
import dev.meshac.check.manager.CheckManager;
import net.minecraft.server.level.ServerPlayer;

/** Per-player data lifecycle (join/quit), replacing Vulcan's PlayerListener. */
public final class PlayerDataManager {

	private PlayerDataManager() {
	}

	/** Login-send HEAD stage (mixin): initialize once, PROVISIONAL client version. */
	public static void onJoin(ServerPlayer player) {
		PlayerData current = get(player);
		if (current != null && current.getPlayer() == player) return;
		PlayerData data = new PlayerData(player);
		// Login-send HEAD initializes once; the later Fabric JOIN reuses this session.
		data.setEntityId(player.getId());
		data.setUniqueId(player.getUUID());
		data.setPlayerName(player.getName().getString());
		// Stage-independent resolution (TranslatorHooks contract A/B/C); a Via loading after this
		// point is picked up by the JOIN stage re-resolution below.
		data.setClientVersion(TranslatorHooks.resolve(player.getUUID(), connection(player)));
		data.setChecks(CheckManager.createChecks(data));
		MeshServices.players().put(player.getUUID(), data);
		dev.meshac.veil.Veil.grace(player, player.level().getServer().getTickCount());
	}

	/** Fabric JOIN client-version stage: re-resolves an UNKNOWN so a Via that loaded after the login-send HEAD is picked up; a Via loading only after JOIN stays UNKNOWN (fail-closed, no retry hook). */
	public static void onFabricJoin(ServerPlayer player) {
		PlayerData data = get(player);
		if (data == null || data.getPlayer() != player) {
			// No login-send HEAD ran for this session; initialize now (resolution is
			// stage-independent, so initialization already resolves with current state).
			onJoin(player);
			return;
		}
		// Re-resolve only an UNKNOWN: a KNOWN from the HEAD stage is final under the
		// stage-independent A/B/C contract; an UNKNOWN can still upgrade here when Via loaded
		// between the stages. Still-unknown stays UNKNOWN (fail-closed).
		if (data.getClientVersion() == dev.meshac.config.MeshVersion.UNKNOWN) {
			data.setClientVersion(TranslatorHooks.resolve(player.getUUID(), connection(player)));
		}
	}

	private static net.minecraft.network.Connection connection(ServerPlayer player) {
		return player.connection == null ? null : ((dev.meshac.mixin.ServerCommonPacketListenerImplAccessor) player.connection).meshac$getConnection();
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
