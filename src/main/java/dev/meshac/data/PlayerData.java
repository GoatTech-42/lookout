package dev.meshac.data;

import dev.meshac.MeshServices;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.CheckUtils;
import dev.meshac.check.ExemptType;
import dev.meshac.check.ExemptionManager;
import dev.meshac.collection.EvictingList;
import dev.meshac.config.MeshVersion;
import dev.meshac.packet.Flying;
import dev.meshac.packet.Placement;
import dev.meshac.packet.InteractEntity;
import dev.meshac.packet.Digging;
import dev.meshac.packet.EntityAction;
import dev.meshac.packet.HeldItemChange;
import dev.meshac.player.ClickTimingData;
import dev.meshac.player.CollisionState;
import dev.meshac.player.MotionState;
import dev.meshac.player.PlayerToggles;
import dev.meshac.tracker.ConfirmationTracker;
import dev.meshac.tracker.ConnectionTracker;
import dev.meshac.tracker.EntityTracker;
import dev.meshac.tracker.RelativeMoveTracker;
import dev.meshac.tracker.RotationTracker;
import dev.meshac.tracker.VehicleTracker;
import dev.meshac.tracker.VelocityTracker;
import dev.meshac.tracker.WorldBlockTracker;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * Per-player session data. Ported from Vulcan's PlayerData: packetevents User/ClientVersion became
 * ServerPlayer/MeshVersion; last-packet slots hold our own packet records.
 * PARITY BLOCKER: clientVersion stays UNKNOWN (PacketEvents protocol -1 ordering) until a translator-aware resolution hook (ViaVersion/Geyser) is implemented. Do not
 * assume the server version implies the client version.
 */
public class PlayerData {

	private ServerPlayer player;
	private String clientBrand = "vanilla";
	private int entityId = -42069;
	private UUID uniqueId = null;
	private MeshVersion clientVersion = MeshVersion.UNKNOWN;
	private String playerName = "Unresolved";
	private boolean clientBrandResolved = false;
	private boolean mitigationPending = false;
	private boolean handshakeComplete = true;
	private boolean connectionKickQueued = false;
	private int totalViolations;
	private int combatViolations;
	private int movementViolations;
	private int playerViolations;
	private int autoClickerViolations;
	private int scaffoldViolations;
	private int timerViolations;
	private final long connectedAt;
	private final int joinedAtTick;
	private long lastClientBrandAlert;
	private long lastPunishmentAtMillis;
	private Flying lastFlyingPacket;
	private InteractEntity lastInteractionPacket;
	private Digging lastDiggingPacket;
	private EntityAction lastEntityActionPacket;
	private HeldItemChange lastHeldItemPacket;
	private Placement lastPlacementPacket;
	private int queuedTransactionCount;
	private List<AbstractCheck> checks;
	private final VehicleTracker vehicleTracker;
	private final WorldBlockTracker worldBlockTracker;
	private final EntityTracker entityTracker;
	private final PlayerToggles playerToggles;
	private final RelativeMoveTracker relativeMoveTracker;
	private final MotionState motionState;
	private final ClickTimingData clickTimingData;
	private final CollisionState collisionState;
	private final RotationTracker rotationTracker;
	private final VelocityTracker velocityTracker;
	private final ConnectionTracker connectionTracker;
	private final ConfirmationTracker confirmationTracker;
	private final EvictingList repliedConfirmationIds;
	private final EvictingList rawConfirmationIds;
	private long lastCancelHitsAtMillis;

	public PlayerData(ServerPlayer player) {
		this.connectedAt = System.currentTimeMillis();
		this.joinedAtTick = MeshServices.ticks();
		this.queuedTransactionCount = 0;
		this.checks = new ArrayList<>();
		this.vehicleTracker = new VehicleTracker(this);
		this.worldBlockTracker = new WorldBlockTracker(this);
		this.entityTracker = new EntityTracker(this);
		this.playerToggles = new PlayerToggles(this);
		this.relativeMoveTracker = new RelativeMoveTracker(this);
		this.motionState = new MotionState(this);
		this.clickTimingData = new ClickTimingData(this);
		this.collisionState = new CollisionState(this);
		this.rotationTracker = new RotationTracker(this);
		this.velocityTracker = new VelocityTracker(this);
		this.connectionTracker = new ConnectionTracker(this);
		this.confirmationTracker = new ConfirmationTracker(this);
		this.repliedConfirmationIds = new EvictingList(100);
		this.rawConfirmationIds = new EvictingList(100);
		this.lastCancelHitsAtMillis = -1L;
		this.player = player;
	}

	public boolean isAtLeast1_17() {
		return CheckUtils.isAtLeast1_17(this);
	}

	public boolean isAtLeast1_13() {
		return CheckUtils.isAtLeast1_13(this);
	}

	public boolean isNewerThan1_9() {
		return CheckUtils.isNewerThan1_9(this);
	}

	public boolean isOlderThan1_9() {
		return this.clientVersion.isOlderThan(MeshVersion.V_1_9);
	}

	public boolean isAtLeast1_21_2() {
		return this.clientVersion.isNewerThanOrEquals(MeshVersion.V_1_21_2) && ExemptionManager.isAtLeast1_21_2();
	}

	public List<String> getExemptionNames() {
		List<String> names = new ArrayList<>();
		for (ExemptType exemptType : ExemptType.values()) {
			if (this.playerToggles.isExempt(exemptType)) names.add(exemptType.toString());
		}
		return names;
	}

	/** Brand tracking hook; the Fabric port has no blacklist/whitelist gate. */
	public void update() {
	}

	public ServerPlayer getPlayer() { return this.player; }
	public String getClientBrand() { return this.clientBrand; }
	public int getEntityId() { return this.entityId; }
	public UUID getUniqueId() { return this.uniqueId; }
	public MeshVersion getClientVersion() { return this.clientVersion; }
	public String getPlayerName() { return this.playerName; }
	public boolean isClientBrandResolved() { return this.clientBrandResolved; }
	public boolean isMitigationPending() { return this.mitigationPending; }
	public boolean isHandshakeComplete() { return this.handshakeComplete; }
	public boolean isConnectionKickQueued() { return this.connectionKickQueued; }
	public int getTotalViolations() { return this.totalViolations; }
	public int getCombatViolations() { return this.combatViolations; }
	public int getMovementViolations() { return this.movementViolations; }
	public int getPlayerViolations() { return this.playerViolations; }
	public int getAutoClickerViolations() { return this.autoClickerViolations; }
	public int getScaffoldViolations() { return this.scaffoldViolations; }
	public int getTimerViolations() { return this.timerViolations; }
	public long getJoinTime() { return this.connectedAt; }
	public int getJoinTicks() { return this.joinedAtTick; }
	public long getLastClientBrandAlert() { return this.lastClientBrandAlert; }
	public long getLastPunishmentAtMillis() { return this.lastPunishmentAtMillis; }
	public Flying getLastFlyingPacket() { return this.lastFlyingPacket; }
	public InteractEntity getLastInteractionPacket() { return this.lastInteractionPacket; }
	public Digging getLastDiggingPacket() { return this.lastDiggingPacket; }
	public EntityAction getLastEntityActionPacket() { return this.lastEntityActionPacket; }
	public HeldItemChange getLastHeldItemPacket() { return this.lastHeldItemPacket; }
	public Placement getLastPlacementPacket() { return this.lastPlacementPacket; }
	public int getQueuedTransactionCount() { return this.queuedTransactionCount; }
	public List<AbstractCheck> getChecks() { return this.checks; }
	public VehicleTracker getVehicleTracker() { return this.vehicleTracker; }
	public WorldBlockTracker getWorldBlockTracker() { return this.worldBlockTracker; }
	public EntityTracker getEntityTracker() { return this.entityTracker; }
	public PlayerToggles getPlayerToggles() { return this.playerToggles; }
	public RelativeMoveTracker getRelativeMoveTracker() { return this.relativeMoveTracker; }
	public MotionState getMotionState() { return this.motionState; }
	public ClickTimingData getClickTimingData() { return this.clickTimingData; }
	public CollisionState getCollisionState() { return this.collisionState; }
	public RotationTracker getRotationTracker() { return this.rotationTracker; }
	public VelocityTracker getVelocityTracker() { return this.velocityTracker; }
	public ConnectionTracker getConnectionTracker() { return this.connectionTracker; }
	public ConfirmationTracker getConfirmationTracker() { return this.confirmationTracker; }
	public EvictingList getRepliedConfirmationIds() { return this.repliedConfirmationIds; }
	public EvictingList getRawConfirmationIds() { return this.rawConfirmationIds; }
	public long getLastCancelHitsAtMillis() { return this.lastCancelHitsAtMillis; }

	public void setPlayer(ServerPlayer player) { this.player = player; }
	public void setClientBrand(String clientBrand) { this.clientBrand = clientBrand; }
	public void setEntityId(int entityId) { this.entityId = entityId; }
	public void setUniqueId(UUID uniqueId) { this.uniqueId = uniqueId; }
	public void setClientVersion(MeshVersion clientVersion) { this.clientVersion = clientVersion; }
	public void setPlayerName(String playerName) { this.playerName = playerName; }
	public void setClientBrandResolved(boolean clientBrandResolved) { this.clientBrandResolved = clientBrandResolved; }
	public void setMitigationPending(boolean mitigationPending) { this.mitigationPending = mitigationPending; }
	public void setHandshakeComplete(boolean handshakeComplete) { this.handshakeComplete = handshakeComplete; }
	public void setConnectionKickQueued(boolean connectionKickQueued) { this.connectionKickQueued = connectionKickQueued; }
	public void setTotalViolations(int totalViolations) { this.totalViolations = totalViolations; }
	public void setCombatViolations(int combatViolations) { this.combatViolations = combatViolations; }
	public void setMovementViolations(int movementViolations) { this.movementViolations = movementViolations; }
	public void setPlayerViolations(int playerViolations) { this.playerViolations = playerViolations; }
	public void setAutoClickerViolations(int autoClickerViolations) { this.autoClickerViolations = autoClickerViolations; }
	public void setScaffoldViolations(int scaffoldViolations) { this.scaffoldViolations = scaffoldViolations; }
	public void setTimerViolations(int timerViolations) { this.timerViolations = timerViolations; }
	public void setLastClientBrandAlert(long lastClientBrandAlert) { this.lastClientBrandAlert = lastClientBrandAlert; }
	public void clearPunishmentCooldown() { this.lastPunishmentAtMillis = 0L; }
	public void setLastFlyingPacket(Flying lastFlyingPacket) { this.lastFlyingPacket = lastFlyingPacket; }
	public void setLastInteractionPacket(InteractEntity lastInteractionPacket) { this.lastInteractionPacket = lastInteractionPacket; }
	public void setLastDiggingPacket(Digging lastDiggingPacket) { this.lastDiggingPacket = lastDiggingPacket; }
	public void setLastEntityActionPacket(EntityAction lastEntityActionPacket) { this.lastEntityActionPacket = lastEntityActionPacket; }
	public void setLastHeldItemPacket(HeldItemChange lastHeldItemPacket) { this.lastHeldItemPacket = lastHeldItemPacket; }
	public void setLastPlacementPacket(Placement lastPlacementPacket) { this.lastPlacementPacket = lastPlacementPacket; }
	public void setQueuedTransactionCount(int queuedTransactionCount) { this.queuedTransactionCount = queuedTransactionCount; }
	public void setChecks(List<AbstractCheck> checks) { this.checks = checks; }
	public void clearCancelHitsCooldown() { this.lastCancelHitsAtMillis = 0L; }
}
