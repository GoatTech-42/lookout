package dev.meshac.punish;

import dev.meshac.MeshServices;
import dev.meshac.Perms;
import dev.meshac.check.AbstractCheck;
import dev.meshac.config.MeshSettings;
import dev.meshac.data.PlayerData;

/**
 * Punishment entry. Vulcan's PunishmentHandler punish() gate order is preserved (punishable,
 * punishment-delay cooldown, bypass permission); the action itself routes into meshac's heat
 * ladder (Luke's design) instead of Vulcan's configured command list.
 */
public class PunishmentHandler {

	private final HeatLadder ladder = new HeatLadder();

	public void punish(AbstractCheck check, PlayerData playerData) {
		if (!((Boolean) MeshSettings.checkPunishable.get(check.getCheckKey())).booleanValue()) return;
		if (playerData.getPlayer() == null) return;
		if (System.currentTimeMillis() - playerData.getLastPunishmentAtMillis() < MeshSettings.settingsPunishmentDelay) return;
		if (Perms.has(playerData.getPlayer(), "meshac.bypass." + check.getCheckKey().toLowerCase())) return;
		// Stamp only an accepted, non-bypassed ladder invocation: unpunishable checks, null players,
		// in-cooldown calls and bypassed staff leave lastPunishmentAtMillis untouched. Stamping here
		// (not clearPunishmentCooldown's reset-to-0) is what makes the delay gate above engage.
		playerData.setLastPunishmentAtMillis(System.currentTimeMillis());
		ladder.apply(check, playerData);
		// Upstream resets VL on ALL of the player's checks after a punishment
		// (PunishmentHandler.resetCheckViolations), not only the punished check.
		playerData.getChecks().forEach(c -> c.setVl(0));
	}
}
