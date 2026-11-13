package dev.meshac.check;

import java.util.List;

/** Per-check runtime settings snapshot (ported from Vulcan's CheckData). */
public class CheckData {

	private final String name;
	private boolean enabled;
	private boolean punishable;
	private boolean broadcastPunishment;
	private boolean hotbarShuffle;
	private boolean randomRotation;
	private int maxViolations;
	private int alertInterval;
	private int minimumVlToNotify;
	private int maxPing;
	private int randomRotationInterval;
	private int minimumVlToRandomlyRotate;
	private int minimumVlToShuffleHotbar;
	private int hotbarShuffleInterval;
	private double minimumTps;
	private double maxBuffer;
	private double bufferDecay;
	private double bufferMultiple;
	private List<String> punishmentCommands;

	public CheckData(String name) {
		this.name = name;
	}

	public String getName() { return name; }
	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
	public boolean isPunishable() { return punishable; }
	public void setPunishable(boolean punishable) { this.punishable = punishable; }
	public boolean isBroadcastPunishment() { return broadcastPunishment; }
	public void setBroadcastPunishment(boolean broadcastPunishment) { this.broadcastPunishment = broadcastPunishment; }
	public boolean isHotbarShuffle() { return hotbarShuffle; }
	public void setHotbarShuffle(boolean hotbarShuffle) { this.hotbarShuffle = hotbarShuffle; }
	public boolean isRandomRotation() { return randomRotation; }
	public void setRandomRotation(boolean randomRotation) { this.randomRotation = randomRotation; }
	public int getMaxViolations() { return maxViolations; }
	public void setMaxViolations(int maxViolations) { this.maxViolations = maxViolations; }
	public int getAlertInterval() { return alertInterval; }
	public void setAlertInterval(int alertInterval) { this.alertInterval = alertInterval; }
	public int getMinimumVlToNotify() { return minimumVlToNotify; }
	public void setMinimumVlToNotify(int minimumVlToNotify) { this.minimumVlToNotify = minimumVlToNotify; }
	public int getMaxPing() { return maxPing; }
	public void setMaxPing(int maxPing) { this.maxPing = maxPing; }
	public int getRandomRotationInterval() { return randomRotationInterval; }
	public void setRandomRotationInterval(int randomRotationInterval) { this.randomRotationInterval = randomRotationInterval; }
	public int getMinimumVlToRandomlyRotate() { return minimumVlToRandomlyRotate; }
	public void setMinimumVlToRandomlyRotate(int minimumVlToRandomlyRotate) { this.minimumVlToRandomlyRotate = minimumVlToRandomlyRotate; }
	public int getMinimumVlToShuffleHotbar() { return minimumVlToShuffleHotbar; }
	public void setMinimumVlToShuffleHotbar(int minimumVlToShuffleHotbar) { this.minimumVlToShuffleHotbar = minimumVlToShuffleHotbar; }
	public int getHotbarShuffleInterval() { return hotbarShuffleInterval; }
	public void setHotbarShuffleInterval(int hotbarShuffleInterval) { this.hotbarShuffleInterval = hotbarShuffleInterval; }
	public double getMinimumTps() { return minimumTps; }
	public void setMinimumTps(double minimumTps) { this.minimumTps = minimumTps; }
	public double getMaxBuffer() { return maxBuffer; }
	public void setMaxBuffer(double maxBuffer) { this.maxBuffer = maxBuffer; }
	public double getBufferDecay() { return bufferDecay; }
	public void setBufferDecay(double bufferDecay) { this.bufferDecay = bufferDecay; }
	public double getBufferMultiple() { return bufferMultiple; }
	public void setBufferMultiple(double bufferMultiple) { this.bufferMultiple = bufferMultiple; }
	public List<String> getPunishmentCommands() { return punishmentCommands; }
	public void setPunishmentCommands(List<String> punishmentCommands) { this.punishmentCommands = punishmentCommands; }
}
