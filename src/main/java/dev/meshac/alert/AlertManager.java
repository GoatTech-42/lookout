package dev.meshac.alert;

import dev.meshac.MeshServices;
import dev.meshac.Perms;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.CheckUtils;
import dev.meshac.check.ExemptionManager;
import dev.meshac.config.MeshSettings;
import dev.meshac.data.PlayerData;
import dev.meshac.util.MathUtils;
import dev.meshac.util.TextUtil;
import net.minecraft.server.level.ServerPlayer;

/**
 * Violation processing. Ported from Vulcan's AlertManager: gate order, VL increments, per-category
 * counters, alert-interval/severity logic verbatim. Bungee/discord/api-event dispatch dropped
 * (UNSUPPORTED in PARITY.md); bypass node renamed vulcan.bypass -> meshac.bypass (rebrand).
 */
public class AlertManager {

	public void processViolation(AbstractCheck check, PlayerData data, String info) {
		if (!((Boolean) MeshSettings.checkEnabled.get(check.getCheckKey())).booleanValue()) return;
		if (System.currentTimeMillis() - data.getLastPunishmentAtMillis() < (long) MeshSettings.settingsPunishmentDelay) return;
		if (data.getPlayer() != null && data.getPlayer().hasDisconnected()) return;
		if (ExemptionManager.getTps() < (Double) MeshSettings.minimumTps.get(check.getCheckKey())) return;
		ServerPlayer pingPlayer = data.getPlayer();
		if (CheckUtils.getPing(pingPlayer) > (Integer) MeshSettings.maximumPing.get(check.getCheckKey())) return;
		if (MeshSettings.settingsIgnoreVivecraft && data.getClientBrand().toLowerCase().contains("vivecraft")) return;
		if (Perms.has(data.getPlayer(), "meshac.bypass." + check.getCheckKey().toLowerCase())) return;
		if (MeshSettings.settingsIgnoreFlagsGeyserClientBrand && data.getClientBrand().toLowerCase().contains("geyser")) return;
		if (MeshSettings.settingsIgnoreGeyserUuids && data.getPlayer().getUUID().toString().startsWith("00000")) return;
		if (MeshSettings.settingsIgnoreGeyserPrefixes && data.getPlayerName().startsWith(MeshSettings.settingsIgnoreGeyserPrefix)) return;
		String cancelHitsCheck = (check.getName() + check.getType()).toLowerCase().replace(" ", "");
		if (MeshSettings.cancelHitsEnabledForAllChecks || MeshSettings.cancelHitsChecks.contains(cancelHitsCheck)) {
			data.clearCancelHitsCooldown();
		}
		check.setVl(check.getVl() + 1);
		int violations = check.getVl();
		if (!check.isExperimental() && this.isCountedInvalidCheck(check)) {
			if (!check.getName().equalsIgnoreCase("timer")) data.setTotalViolations(data.getTotalViolations() + 1);
			if (check.getName().equalsIgnoreCase("timer")) data.setTimerViolations(data.getTimerViolations() + 1);
			if (check.getName().equalsIgnoreCase("autoclicker")) data.setAutoClickerViolations(data.getAutoClickerViolations() + 1);
			if (check.getName().equalsIgnoreCase("scaffold")) data.setScaffoldViolations(data.getScaffoldViolations() + 1);
			switch (check.getCategory()) {
				case "combat" -> {
					if (!check.getName().equalsIgnoreCase("autoclicker"))
						data.setCombatViolations(data.getCombatViolations() + 1);
				}
				case "movement" -> data.setMovementViolations(data.getMovementViolations() + 1);
				case "player" -> {
					if (!check.getName().equalsIgnoreCase("timer"))
						data.setPlayerViolations(data.getPlayerViolations() + 1);
				}
			}
		}
		int alertInterval = (Integer) MeshSettings.alertIntervals.get(check.getCheckKey());
		int minimumAlertViolations = (Integer) MeshSettings.minimumAlertViolations.get(check.getCheckKey());
		int maxViolations = (Integer) MeshSettings.maximumViolations.get(check.getCheckKey());
		if (violations % alertInterval == 0 && violations >= minimumAlertViolations) {
			if (MeshSettings.logFileEnabled) {
				MeshServices.alertExecutor().execute(() -> writeViolationLog(check, data, info));
			}
			if (MeshSettings.alertsPrintToConsole
					&& (!MeshSettings.settingsIgnoreAlertsOverMaxViolations
							|| violations <= maxViolations + MeshSettings.settingsIgnoreAlertsOverMaxViolationsAmount)) {
				String text = TextUtil.colorize(MeshSettings.alertsConsoleFormat)
						.replace("%player%", data.getPlayerName())
						.replace("%max-vl%", Integer.toString(maxViolations))
						.replace("%check%", check.getCheck().name())
						.replace("%info%", info)
						.replace("%vl%", Integer.toString(violations))
						.replace("%type%", Character.toString(check.getDisplayType()))
						.replace("%ping%", Integer.toString(CheckUtils.getPing(data.getPlayer())))
						.replace("%tps%", MathUtils.formatNumber(ExemptionManager.getTps()));
				MeshServices.log(text);
			}
			String chat = TextUtil.colorize(MeshSettings.alertsFormat)
					.replace("%prefix%", TextUtil.colorize(MeshSettings.prefix))
					.replace("%player%", data.getPlayerName())
					.replace("%check%", check.getCheck().name())
					.replace("%dev%", check.getCheck().experimental() ? MeshSettings.alertsExperimentalSymbol : "")
					.replace("%type%", Character.toString(check.getDisplayType()))
					.replace("%vl%", Integer.toString(violations))
					.replace("%max-vl%", Integer.toString(maxViolations));
			broadcastAlert(chat);
		}
		// Punish trigger, verbatim from Vulcan AlertManager.processViolation:
		//   checkPunishable gate -> experimental gate -> violations >= maxViolations && online.
		// JudgementDay gate dropped (feature not ported). VL reset lives in PunishmentHandler
		// (upstream resets ALL checks' VL there, not here).
		if (!((Boolean) MeshSettings.checkPunishable.get(check.getCheckKey())).booleanValue()) return;
		if (check.isExperimental()) return;
		if (violations >= maxViolations && data.getPlayer() != null && !data.getPlayer().hasDisconnected()) {
			MeshServices.punishments().punish(check, data);
		}
	}

	private boolean isCountedInvalidCheck(AbstractCheck check) {
		return true;
	}

	private void writeViolationLog(AbstractCheck check, PlayerData data, String info) {
		MeshServices.logViolation(check, data, info);
	}

	public void broadcastAlert(String message) {
		MeshServices.broadcastToOps(TextUtil.parse(message));
	}
}
