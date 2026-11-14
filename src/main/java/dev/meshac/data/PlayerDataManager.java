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
		// Provisional: whenever any translator CLASS is present (loaded or not) this returns UNKNOWN and preserves the
		// captured handshake, because Bedrock/Via registration for this player can lag login.
		data.setClientVersion(TranslatorHooks.resolve(player.getUUID(), connection(player), true));
		data.setChecks(CheckManager.createChecks(data));
		MeshServices.players().put(player.getUUID(), data);
		dev.meshac.veil.Veil.grace(player, player.level().getServer().getTickCount());
	}

	/** Fabric JOIN client-version stage; translator registration readiness here is assumed, not runtime-verified. */
	public static void onFabricJoin(ServerPlayer player) {
		PlayerData data = get(player);
		if (data == null || data.getPlayer() != player) {
			// No login-send HEAD ran for this session; initialize (provisional), then fall
			// through to the definitive resolution below: this stage must never leave a
			// deferred UNKNOWN unresolved when it is the only stage that ran.
			onJoin(player);
			data = get(player);
			if (data == null || data.getPlayer() != player) return;
		}
		// Re-resolve only an UNKNOWN: a provisional KNOWN was only possible with no translator
		// present, so it is final. The handshake is still cached (the provisional stage never
		// consumed it). Still-unknown stays UNKNOWN (fail-closed).
		if (data.getClientVersion() == dev.meshac.config.MeshVersion.UNKNOWN) {
			data.setClientVersion(TranslatorHooks.resolve(player.getUUID(), connection(player), false));
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
