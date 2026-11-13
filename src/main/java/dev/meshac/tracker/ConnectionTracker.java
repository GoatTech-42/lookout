package dev.meshac.tracker;

import dev.meshac.MeshServices;
import dev.meshac.config.MeshSettings;
import dev.meshac.data.PlayerData;
import net.minecraft.server.level.ServerPlayer;

/** Ping and keepalive state. Ported from Vulcan's ConnectionTracker; kicks route through the server's main thread. */
public class ConnectionTracker {

	private final PlayerData playerData;
	private int keepAlivePing;
	private long lastMovementAtMillis;
	private long movementDelayMillis;
	private long lastKeepAliveAtMillis = System.currentTimeMillis();
	private long lastFastMovementAtMillis = System.currentTimeMillis();
	private boolean fastMovement;
	private double keepAliveTimeoutBuffer;
	private boolean keepAliveKickQueued;
	private int highPingTicks;
	private boolean highPingKickQueued;

	public ConnectionTracker(PlayerData data) {
		this.playerData = data;
	}

	public void processMovement() {
		long now = System.currentTimeMillis();
		this.movementDelayMillis = now - this.lastMovementAtMillis;
		this.fastMovement = this.movementDelayMillis <= 5L;
		if (this.fastMovement) this.lastFastMovementAtMillis = System.currentTimeMillis();
		ServerPlayer player = this.playerData.getPlayer();
		this.keepAlivePing = player == null ? 0 : player.connection.latency();
		this.lastMovementAtMillis = now;
		this.checkKeepAliveTimeout();
		this.checkMaxPing();
	}

	public void recordKeepAlive() {
		this.lastKeepAliveAtMillis = System.currentTimeMillis();
	}

	private void checkKeepAliveTimeout() {
		if (this.playerData.getPlayer() == null) return;
		if (!MeshSettings.connectionKeepaliveKickEnabled || dev.meshac.Perms.has(this.playerData.getPlayer(), "meshac.bypass.connection.keepalive")) return;
		long delay = System.currentTimeMillis() - this.lastKeepAliveAtMillis;
		if (delay > MeshSettings.connectionKeepaliveMaxDelay && !this.keepAliveKickQueued) {
			this.keepAliveTimeoutBuffer += 1.0;
			if (this.keepAliveTimeoutBuffer > 5.0) {
				MeshServices.mainThread(() -> this.kickForKeepAliveTimeout());
				if (!MeshSettings.connectionKeepaliveStaffAlertMessage.isEmpty())
					MeshServices.alerts().broadcastAlert(MeshSettings.connectionKeepaliveStaffAlertMessage.replaceAll("%delay%", Long.toString(delay)).replaceAll("%player%", this.playerData.getPlayerName()));
				if (!MeshSettings.connectionKeepaliveConsoleMessage.isEmpty())
					MeshServices.log(MeshSettings.connectionKeepaliveConsoleMessage.replaceAll("%player%", this.playerData.getPlayerName()).replaceAll("%delay%", Long.toString(delay)));
				this.keepAliveKickQueued = true;
			} else if (this.keepAliveTimeoutBuffer > 0.0) {
				this.keepAliveTimeoutBuffer -= 0.1;
			}
		}
	}

	private void checkMaxPing() {
		if (this.playerData.getPlayer() == null) return;
		if (!MeshSettings.connectionMaxPingKickEnabled) return;
		if (dev.meshac.Perms.has(this.playerData.getPlayer(), "meshac.bypass.connection.max-ping")) return;
		if (this.keepAlivePing > MeshSettings.connectionMaxPingMaxPing && !this.highPingKickQueued) {
			if (++this.highPingTicks <= MeshSettings.connectionMaxPingMaxTicks) return;
			MeshServices.mainThread(() -> this.kickForHighPing());
			if (!MeshSettings.connectionMaxPingStaffAlertMessage.isEmpty())
				MeshServices.alerts().broadcastAlert(MeshSettings.connectionMaxPingStaffAlertMessage.replaceAll("%ping%", Long.toString(this.keepAlivePing)).replaceAll("%player%", this.playerData.getPlayerName()));
			if (!MeshSettings.connectionMaxPingConsoleMessage.isEmpty())
				MeshServices.log(MeshSettings.connectionMaxPingConsoleMessage.replaceAll("%player%", this.playerData.getPlayerName()).replaceAll("%ping%", Long.toString(this.keepAlivePing)));
			this.highPingKickQueued = true;
			return;
		}
		if (this.highPingTicks <= 0) return;
		--this.highPingTicks;
	}

	public int getKeepAlivePing() {
		return this.keepAlivePing;
	}

	public long getMovementDelayMillis() {
		return this.movementDelayMillis;
	}

	public long getLastFastMovementAtMillis() {
		return this.lastFastMovementAtMillis;
	}

	public boolean isFastMovement() {
		return this.fastMovement;
	}

	private void kickForHighPing() {
		ServerPlayer player = this.playerData.getPlayer();
		if (player != null) player.connection.disconnect(dev.meshac.util.TextUtil.parse(MeshSettings.connectionMaxPingKickMessage));
	}

	private void kickForKeepAliveTimeout() {
		ServerPlayer player = this.playerData.getPlayer();
		if (player != null) player.connection.disconnect(dev.meshac.util.TextUtil.parse(MeshSettings.connectionKeepaliveKickMessage));
	}
}
