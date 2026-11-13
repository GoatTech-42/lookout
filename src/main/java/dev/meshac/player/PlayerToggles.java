package dev.meshac.player;

import dev.meshac.check.ExemptType;
import dev.meshac.data.PlayerData;

/** Exemption predicates for one player. Ported from Vulcan's PlayerToggles. */
public class PlayerToggles {

	private final PlayerData playerData;

	public PlayerToggles(PlayerData playerData) {
		this.playerData = playerData;
	}

	public boolean isExempt(ExemptType exemption) {
		return exemption.getFunction().apply(playerData);
	}

	public boolean isAnyExempt(ExemptType... exemptions) {
		for (ExemptType exemption : exemptions) if (isExempt(exemption)) return true;
		return false;
	}

	public boolean matches(java.util.function.Function<PlayerData, Boolean> function) {
		return function.apply(playerData);
	}
}
