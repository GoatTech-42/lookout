package dev.meshac.check;

import dev.meshac.MeshServices;
import dev.meshac.Perms;
import dev.meshac.alert.AlertManager;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.config.MeshSettings;
import dev.meshac.data.PlayerData;
import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.packet.MeshPacketType;
import dev.meshac.punish.PunishmentHandler;
import java.util.Objects;

/**
 * Base check. Ported from Vulcan's AbstractCheck: fail/setback/buffer/VL semantics verbatim,
 * Bukkit API event dispatch removed, packet-type predicates operate on MeshPacketEvent.
 */
public abstract class AbstractCheck {

	protected final PlayerData playerData;
	private int violations;
	private double buffer;
	public final String checkName;
	public final double maximumBuffer;
	public final double bufferDecay;
	public final double bufferMultiplier;

	public AbstractCheck(PlayerData playerData) {
		this.checkName = this.getClass().getSimpleName();
		this.maximumBuffer = ((Integer) MeshSettings.maximumBuffers.get(this.checkName)).intValue();
		this.bufferDecay = (Double) MeshSettings.bufferDecays.get(this.checkName);
		this.bufferMultiplier = (Double) MeshSettings.bufferMultipliers.get(this.checkName);
		this.playerData = playerData;
	}

	public void fail(Object value) {
		if (this.playerData.getPlayer() == null) return;
		applySetback();
		this.applyConfiguredMitigation();
		this.applyConfiguredCommands();
		this.multiplyBuffer(this.bufferMultiplier);
		MeshServices.violationExecutor().execute(() -> this.reportViolationWithDetails(value));
	}

	public void fail() {
		if (this.playerData.getPlayer() == null) return;
		applySetback();
		this.applyConfiguredMitigation();
		this.applyConfiguredCommands();
		this.multiplyBuffer(this.bufferMultiplier);
		MeshServices.violationExecutor().execute(() -> this.reportViolation());
	}

	private void applySetback() {
		if (((Boolean) MeshSettings.setbackEnabled.get(this.getCheckKey())).booleanValue()
				&& this.getVl() > (Integer) MeshSettings.minimumSetbackViolations.get(this.getCheckKey())) {
			if (!Perms.has(this.playerData.getPlayer(), "meshac.bypass.setback." + this.getCheckKey().toLowerCase())) {
				this.playerData.getCollisionState().requestSetback();
			}
		}
	}

	public void update() {
		MeshServices.violationExecutor().execute(() -> this.executePunishment());
	}

	protected boolean isExempt(ExemptType[] exemptions) {
		return playerData.getPlayerToggles().isAnyExempt(exemptions);
	}

	public int getCurrentTick() {
		return MeshServices.ticks();
	}

	public double increaseBuffer() {
		return this.buffer = Math.min(10000.0, this.buffer + 1.0);
	}

	public double decreaseBuffer(double amount) {
		return this.buffer = Math.max(0.0, this.buffer - amount);
	}

	public double increaseBuffer(double amount) {
		return this.buffer = Math.min(10000.0, this.buffer + amount);
	}

	public void setBuffer() {
		this.buffer = 0.0;
	}

	public void multiplyBuffer(double amount) {
		this.buffer *= amount;
	}

	public void decayBuffer() {
		this.decreaseBuffer(this.bufferDecay);
	}

	public int getAttackTicks() {
		return this.playerData.getRelativeMoveTracker().getTicksSinceAttack();
	}

	public boolean hasRecentAttack(int ticks) {
		return this.playerData.getRelativeMoveTracker().getTicksSinceAttack() < ticks
				&& this.playerData.getRelativeMoveTracker().getTicksSinceAttackGiven() < ticks * 6;
	}

	public CheckInfo getCheck() {
		CheckInfo info = getClass().getAnnotation(CheckInfo.class);
		if (info == null) throw new IllegalStateException("Missing CheckInfo on " + checkName);
		return info;
	}

	public void debug(Object value) {
		ExemptionManager.debug(value);
	}

	public String getCategory() {
		String packageName = getClass().getPackageName();
		if (packageName.contains(".combat.")) return "combat";
		if (packageName.contains(".movement.")) return "movement";
		if (packageName.contains(".player.")) return "player";
		throw new IllegalStateException("Unknown check category: " + packageName);
	}

	public boolean isTeleporting() {
		return this.playerData.getMotionState().isTeleporting();
	}

	public boolean isSetbackPending() {
		return this.playerData.getCollisionState().isSetbackPending();
	}

	public String getName() {
		return this.getCheck().name().toLowerCase().replaceAll(" ", "");
	}

	public char getType() {
		return Character.toLowerCase(this.getCheck().type());
	}

	public int getVl() {
		return this.violations;
	}

	public int getMaxVl() {
		return (Integer) MeshSettings.maximumViolations.get(this.checkName);
	}

	public int getMinimumVlToNotify() {
		return (Integer) MeshSettings.minimumAlertViolations.get(this.checkName);
	}

	public double getMaxBuffer() {
		return this.maximumBuffer;
	}

	public String getDescription() {
		return this.getCheck().description();
	}

	public String getComplexType() {
		return this.getCheck().complexType();
	}

	public double getBufferDecay() {
		return this.bufferDecay;
	}

	public int getAlertInterval() {
		return (Integer) MeshSettings.alertIntervals.get(this.checkName);
	}

	public double getBufferMultiple() {
		return this.bufferMultiplier;
	}

	public boolean isExperimental() {
		return this.getCheck().experimental();
	}

	public boolean isPunishable() {
		return (Boolean) MeshSettings.checkPunishable.get(this.checkName);
	}

	public String getDisplayName() {
		return this.getCheck().name();
	}

	public char getDisplayType() {
		return this.getCheck().type();
	}

	public void applyConfiguredMitigation() {
		if (this.playerData != null && this.playerData.getPlayer() != null) {
			boolean condition = (Boolean) MeshSettings.hotbarShuffleEnabled.get(this.checkName);
			int index = (Integer) MeshSettings.minimumShuffleViolations.get(this.checkName);
			int index2 = (Integer) MeshSettings.shuffleIntervals.get(this.checkName);
			if (condition && this.violations >= index && this.violations % index2 == 0) {
				CheckUtils.sendHotbarShuffle(this.playerData.getPlayer());
			}
		}
	}

	public void applyConfiguredCommands() {
		if (this.playerData.getPlayer() != null) {
			boolean condition = (Boolean) MeshSettings.randomRotationEnabled.get(this.checkName);
			int index = (Integer) MeshSettings.minimumRotationViolations.get(this.checkName);
			int index2 = (Integer) MeshSettings.rotationIntervals.get(this.checkName);
			if (condition && this.violations >= index && this.violations % index2 == 0) {
				CheckUtils.rotatePlayerRandomly(this.playerData);
			}
		}
	}

	public boolean isRotationUpdatePacket(MeshPacketEvent event) {
		return event.isClient(MeshPacketType.PLAYER_ROTATION) || event.isClient(MeshPacketType.PLAYER_POSITION_AND_ROTATION);
	}

	public boolean isAttackPacket(MeshPacketEvent event) {
		return event.isClient(MeshPacketType.INTERACT_ENTITY) || event.isClient(MeshPacketType.ATTACK);
	}

	public boolean isMovementPacket(MeshPacketEvent event) {
		return event.isClient(MeshPacketType.PLAYER_FLYING) || event.isClient(MeshPacketType.PLAYER_POSITION)
				|| event.isClient(MeshPacketType.PLAYER_ROTATION) || event.isClient(MeshPacketType.PLAYER_POSITION_AND_ROTATION);
	}

	public boolean isBlockPlacementPacket(MeshPacketEvent event) {
		return event.isClient(MeshPacketType.PLAYER_BLOCK_PLACEMENT);
	}

	public boolean isUseItemPacket(MeshPacketEvent event) {
		return event.isClient(MeshPacketType.USE_ITEM);
	}

	public boolean isPositionUpdatePacket(MeshPacketEvent event) {
		return event.isClient(MeshPacketType.PLAYER_POSITION) || event.isClient(MeshPacketType.PLAYER_POSITION_AND_ROTATION);
	}

	public boolean isArmAnimationPacket(MeshPacketEvent event) {
		return event.isClient(MeshPacketType.ANIMATION) || event.isClient(MeshPacketType.ANIMATION);
	}

	public boolean isVehicleMovePacket(MeshPacketEvent event) {
		return event.isClient(MeshPacketType.VEHICLE_MOVE);
	}

	public boolean isEntityActionPacket(MeshPacketEvent event) {
		return event.isClient(MeshPacketType.ENTITY_ACTION);
	}

	public boolean isDiggingPacket(MeshPacketEvent event) {
		return event.isClient(MeshPacketType.PLAYER_DIGGING);
	}

	public boolean isAbilitiesPacket(MeshPacketEvent event) {
		return event.isClient(MeshPacketType.PLAYER_ABILITIES);
	}

	public boolean isSteerVehiclePacket(MeshPacketEvent event) {
		return event.isClient(MeshPacketType.STEER_VEHICLE);
	}

	public boolean isClientStatusPacket(MeshPacketEvent event) {
		return event.isClient(MeshPacketType.CLIENT_STATUS);
	}

	public boolean isHeldItemChangePacket(MeshPacketEvent event) {
		return event.isClient(MeshPacketType.HELD_ITEM_CHANGE);
	}

	public boolean isKeepAlivePacket(MeshPacketEvent event) {
		return event.isClient(MeshPacketType.KEEP_ALIVE);
	}

	public boolean isClickWindowPacket(MeshPacketEvent event) {
		return event.isClient(MeshPacketType.CLICK_WINDOW);
	}

	public boolean isSpectatePacket(MeshPacketEvent event) {
		return event.isClient(MeshPacketType.SPECTATE);
	}

	public boolean isServerPositionPacket(MeshPacketEvent event) {
		return event.isServer(MeshPacketType.PLAYER_POSITION_AND_LOOK);
	}

	public boolean isClientTickEndPacket(MeshPacketEvent event) {
		return event.isClient(MeshPacketType.CLIENT_TICK_END);
	}

	public PlayerData getPlayerData() {
		return this.playerData;
	}

	public double getBuffer() {
		return this.buffer;
	}

	public String getCheckKey() {
		return this.checkName;
	}

	public double getBufferThreshold() {
		return this.maximumBuffer;
	}

	public double getConfiguredBufferDecay() {
		return this.bufferDecay;
	}

	public double getConfiguredBufferMultiplier() {
		return this.bufferMultiplier;
	}

	public void setVl(int violations) {
		this.violations = violations;
	}

	public void setBuffer(double buffer) {
		this.buffer = buffer;
	}

	private void executePunishment() {
		PunishmentHandler punishmentHandler = MeshServices.punishments();
		punishmentHandler.punish(this, this.playerData);
	}

	private void reportViolation() {
		AlertManager alertManager = MeshServices.alerts();
		alertManager.processViolation(this, this.playerData, "");
	}

	private void reportViolationWithDetails(Object value) {
		AlertManager alertManager = MeshServices.alerts();
		alertManager.processViolation(this, this.playerData, Objects.toString(value));
	}

	/** Inbound/outbound packet dispatch entry; checks override. */
	public void handlePacket(MeshPacketEvent event) {
	}

	public void handleEvent(dev.meshac.event.MeshEvent event) {
	}
}
