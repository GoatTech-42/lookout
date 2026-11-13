package dev.meshac.player;

import dev.meshac.check.ExemptType;
import dev.meshac.collection.EvictingList;
import dev.meshac.data.PlayerData;
import dev.meshac.util.MathUtils;

/** Swing/dig/place timing for click-pattern checks. Ported from Vulcan's ClickTimingData. */
public class ClickTimingData {

	private final PlayerData playerData;
	private long lastClickAt = -1L;
	private long clickInterval;
	private long lastDigAt = System.currentTimeMillis();
	private long lastSwingAt = System.currentTimeMillis();
	private long lastPlaceAt = System.currentTimeMillis();
	private final EvictingList<Long> clickIntervals = new EvictingList<>(20);
	private double clicksPerSecond;
	private double kurtosis;

	public ClickTimingData(PlayerData playerData) {
		this.playerData = playerData;
	}

	public void recordClick() {
		if (playerData.getPlayerToggles().isExempt(ExemptType.AUTOCLICKER_NON_DIG)) return;
		long now = System.currentTimeMillis();
		if (lastClickAt > 0) {
			clickInterval = now - lastClickAt;
			clickIntervals.add(clickInterval);
			if (clickIntervals.isFull()) clicksPerSecond = MathUtils.clicksPerSecond(clickIntervals);
		}
		lastClickAt = now;
	}

	public void recordDig() {
		lastDigAt = System.currentTimeMillis();
	}

	public void recordPlacement() {
		lastPlaceAt = System.currentTimeMillis();
	}

	public void recordSwing() {
		lastSwingAt = System.currentTimeMillis();
		recordClick();
	}

	public long getClickInterval() { return clickInterval; }
	public long getLastDigAt() { return lastDigAt; }
	public long getLastSwingAt() { return lastSwingAt; }
	public long getLastPlaceAt() { return lastPlaceAt; }
	public double getClicksPerSecond() { return clicksPerSecond; }
	public double getKurtosis() { return kurtosis; }
	public void setKurtosis(double kurtosis) { this.kurtosis = kurtosis; }
	public void resetClickTime() { lastClickAt = 0L; }
	public void resetClickInterval() { clickInterval = 0L; }
	public void resetDigTime() { lastDigAt = 0L; }
	public void resetSwingTime() { lastSwingAt = 0L; }
	public void resetPlacementTime() { lastPlaceAt = 0L; }
	public void setClicksPerSecond(double clicksPerSecond) { this.clicksPerSecond = clicksPerSecond; }
}
