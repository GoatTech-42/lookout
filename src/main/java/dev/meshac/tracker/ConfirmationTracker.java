package dev.meshac.tracker;

import dev.meshac.MeshServices;
import dev.meshac.check.ExemptionManager;
import dev.meshac.config.MeshSettings;
import dev.meshac.data.PlayerData;
import dev.meshac.player.CollisionState;
import dev.meshac.util.MathUtils;
import dev.meshac.util.TextUtil;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.server.level.ServerPlayer;

/**
 * Transaction/ping confirmation tracking. Ported from Vulcan's ConfirmationTracker; kicks became
 * connection.disconnect, packetevents send went through ServerGamePacketListenerImpl.send.
 */
public class ConfirmationTracker {

	private final PlayerData playerData;
	private final Deque<PendingConfirmation> pendingConfirmations = new ConcurrentLinkedDeque<>();
	private final Deque<Runnable> tasksForNextConfirmation = new ConcurrentLinkedDeque<>();
	private short nextConfirmationId = -32348;
	private short lastSentConfirmationId = -69;
	private short joinConfirmationId = -69;
	private long transactionPingMillis = 0L;
	private long lastMovementAtMillis = -1L;
	private long movementDelayMillis = -1L;
	private long lastConfirmationAtMillis = System.currentTimeMillis();
	private long lastClientLagSpikeAtMillis = Long.MAX_VALUE;
	private int lastRepliedConfirmationId = -42069;

	public ConfirmationTracker(PlayerData playerData) {
		this.playerData = playerData;
	}

	public void runAfterConfirmation(Runnable callback) {
		PendingConfirmation confirmation = pendingConfirmations.peekLast();
		if (confirmation == null) callback.run();
		else confirmation.addCallback(callback);
	}

	public void runAfterNextConfirmation(Runnable callback) {
		tasksForNextConfirmation.add(callback);
	}

	public void recordSentConfirmation(int id) {
		if (id == lastSentConfirmationId && playerData.isHandshakeComplete() && !MeshSettings.connectionOtherAnticheatSupport)
			queueConfirmation(id);
	}

	private void queueConfirmation(int id) {
		PendingConfirmation confirmation = new PendingConfirmation(id, System.currentTimeMillis());
		Runnable callback;
		while ((callback = tasksForNextConfirmation.poll()) != null) confirmation.addCallback(callback);
		pendingConfirmations.add(confirmation);
	}

	public void handleConfirmationReply(int id) {
		if (playerData.isConnectionKickQueued() || !playerData.isHandshakeComplete()) return;
		if (MeshSettings.connectionOtherAnticheatSupport && pendingConfirmations.stream().noneMatch(c -> c.getId() == id)) return;
		PendingConfirmation expected = pendingConfirmations.peek();
		if (expected == null) {
			String staff = MeshSettings.connectionInvalidStaffAlertMessage.replace("%id%", Integer.toString(id)).replace("%player%", playerData.getPlayerName());
			String console = MeshSettings.connectionInvalidConsoleAlertMessage.replace("%id%", Integer.toString(id)).replace("%player%", playerData.getPlayerName());
			rejectConfirmation(staff, console, MeshSettings.connectionInvalidKickMessage);
			return;
		}
		if (id != expected.getId()) {
			String staff = MeshSettings.connectionOutOfOrderStaffAlertMessage.replace("%id%", Integer.toString(id)).replace("%last-id%", Integer.toString(expected.getId())).replace("%player%", playerData.getPlayerName());
			String console = MeshSettings.connectionOutOfOrderConsoleAlertMessage.replace("%id%", Integer.toString(id)).replace("%last-id%", Integer.toString(expected.getId())).replace("%player%", playerData.getPlayerName());
			rejectConfirmation(staff, console, MeshSettings.connectionOutOfOrderKickMessage);
			return;
		}
		pendingConfirmations.poll();
		long now = System.currentTimeMillis();
		lastRepliedConfirmationId = id;
		transactionPingMillis = now - expected.getSentAtMillis();
		lastConfirmationAtMillis = now;
		expected.complete();
	}

	private void rejectConfirmation(String staffMessage, String consoleMessage, String kickMessage) {
		MeshServices.alerts().broadcastAlert(staffMessage);
		ExemptionManager.logColored(consoleMessage);
		if (MeshSettings.connectionDebugMessages) logDebugState();
		playerData.setConnectionKickQueued(true);
		MeshServices.mainThread(() -> {
			ServerPlayer player = playerData.getPlayer();
			if (player != null && !player.hasDisconnected())
				player.connection.disconnect(TextUtil.parse(kickMessage));
		});
	}

	public void checkResponseTimeout() {
		long now = System.currentTimeMillis();
		long unansweredMillis = now - lastConfirmationAtMillis;
		boolean joined = now - playerData.getJoinTime() > 30000L;
		if (!playerData.isConnectionKickQueued() && joined && !pendingConfirmations.isEmpty()) {
			boolean timeout = unansweredMillis > TimeUnit.SECONDS.toMillis(MeshSettings.connectionNoResponseDelayInSeconds);
			if (timeout && MeshSettings.connectionNoResponseCodeOfConductFix) {
				CollisionState collision = playerData.getCollisionState();
				timeout = MathUtils.length3(collision.getX() - collision.getJoinX(), collision.getY() - collision.getJoinY(), collision.getZ() - collision.getJoinZ()) >= 5.0;
			}
			if (timeout) {
				rejectConfirmation(
						MeshSettings.connectionNoResponseStaffAlertMessage.replace("%delay%", Long.toString(unansweredMillis)).replace("%player%", playerData.getPlayerName()),
						MeshSettings.connectionNoResponseConsoleAlertMessage.replace("%delay%", Long.toString(unansweredMillis)).replace("%player%", playerData.getPlayerName()),
						MeshSettings.connectionNoResponseKickMessage);
				return;
			}
			if (pendingConfirmations.size() > MeshSettings.connectionMaxSizeAmount) {
				String size = Integer.toString(pendingConfirmations.size());
				rejectConfirmation(
						MeshSettings.connectionMaxSizeStaffAlertMessage.replace("%size%", size).replace("%player%", playerData.getPlayerName()),
						MeshSettings.connectionMaxSizeConsoleAlertMessage.replace("%size%", size).replace("%player%", playerData.getPlayerName()),
						MeshSettings.connectionMaxSizeKickMessage);
			}
		}
		if (lastMovementAtMillis > 0) {
			movementDelayMillis = now - lastMovementAtMillis;
			if (movementDelayMillis < 5) lastClientLagSpikeAtMillis = now;
		}
		lastMovementAtMillis = now;
	}

	private void logDebugState() {
		ExemptionManager.logColored("Pending Confirmations (" + pendingConfirmations.size() + "): "
				+ pendingConfirmations.stream().map(c -> Integer.toString(c.getId())).collect(Collectors.joining(", ")));
		ExemptionManager.logColored("Time since last replied Confirmation: " + (System.currentTimeMillis() - lastConfirmationAtMillis));
		ExemptionManager.logColored("Time since joining: " + (System.currentTimeMillis() - playerData.getJoinTime()));
		ExemptionManager.logColored("Last Replied ID: " + lastRepliedConfirmationId);
		ExemptionManager.logColored("Last Replied IDs: " + playerData.getRepliedConfirmationIds());
		ExemptionManager.logColored("Last Raw IDs: " + playerData.getRawConfirmationIds());
	}

	public void sendConfirmation(boolean inEventLoop) {
		if (!playerData.isHandshakeComplete()) return;
		sendConfirmationPacket();
	}

	public void sendPacket(int id) {
		ServerPlayer player = playerData.getPlayer();
		if (player != null) player.connection.send(new ClientboundPingPacket(id));
	}

	public short generateConfirmationId() {
		if (++nextConfirmationId > -420) nextConfirmationId = -32348;
		return nextConfirmationId;
	}

	public Deque<PendingConfirmation> getPendingConfirmations() { return this.pendingConfirmations; }
	public short getJoinConfirmationId() { return this.joinConfirmationId; }
	public long getTransactionPingMillis() { return this.transactionPingMillis; }
	public long getLastConfirmationAtMillis() { return this.lastConfirmationAtMillis; }
	public long getLastClientLagSpikeAtMillis() { return this.lastClientLagSpikeAtMillis; }
	public void setNextConfirmationId(int id) { this.nextConfirmationId = (short) id; }
	public void setLastSentConfirmationId(int id) { this.lastSentConfirmationId = (short) id; }
	public void setJoinConfirmationId(int id) { this.joinConfirmationId = (short) id; }

	private void sendConfirmationPacket() {
		lastSentConfirmationId = generateConfirmationId();
		if (MeshSettings.connectionOtherAnticheatSupport) queueConfirmation(lastSentConfirmationId);
		sendPacket(lastSentConfirmationId);
	}
}
