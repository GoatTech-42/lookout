package dev.meshac.check;

import dev.meshac.MeshServices;
import dev.meshac.config.MeshVersion;

/**
 * Server-wide version predicates and TPS. Ported from Vulcan's ExemptionManager.
 * Version constants resolve once from the running server; TPS comes from the server's
 * own tick-time average (same "min(20, tps)" semantics as Vulcan).
 */
public final class ExemptionManager {

	private static volatile MeshVersion serverVersion = MeshVersion.fromName(
			net.minecraft.SharedConstants.getCurrentVersion().name());

	private ExemptionManager() {
	}

	public static void init(MeshVersion version) {
		serverVersion = version;
	}

	public static MeshVersion getServerVersion() {
		return serverVersion;
	}

	public static double getTps() {
		return MeshServices.tps();
	}

	public static void logInfo(String message) {
		MeshServices.log(message);
	}

	public static void logColored(String message) {
		MeshServices.log(message);
	}

	public static void debug(Object value) {
		if (dev.meshac.config.MeshSettings.settingsDebug) {
			broadcast("&c[!] &7" + value);
		}
	}

	public static void broadcast(String message) {
		MeshServices.broadcastToOps(dev.meshac.util.TextUtil.parse(message));
	}

	public static boolean isNewerThan1_7_10() { return serverVersion.isNewerThan(MeshVersion.V_1_7_10); }
	public static boolean isAtLeast1_8() { return serverVersion.isNewerThanOrEquals(MeshVersion.V_1_8); }
	public static boolean isAtLeast1_9() { return serverVersion.isNewerThanOrEquals(MeshVersion.V_1_9); }
	public static boolean isBefore1_13() { return serverVersion.isOlderThan(MeshVersion.V_1_13); }
	public static boolean isBefore1_8() { return serverVersion.isOlderThan(MeshVersion.V_1_8); }
	public static boolean isBefore1_9() { return serverVersion.isOlderThan(MeshVersion.V_1_9); }
	public static boolean isNewerThan1_13() { return serverVersion.isNewerThan(MeshVersion.V_1_13); }
	public static boolean isAtLeast1_16() { return serverVersion.isNewerThanOrEquals(MeshVersion.V_1_16); }
	public static boolean isAtLeast1_12() { return serverVersion.isNewerThanOrEquals(MeshVersion.V_1_12); }
	public static boolean isAtLeast1_11() { return serverVersion.isNewerThanOrEquals(MeshVersion.V_1_11); }
	public static boolean isAtLeast1_17() { return serverVersion.isNewerThanOrEquals(MeshVersion.V_1_17); }
	public static boolean isAtLeast1_14() { return serverVersion.isNewerThanOrEquals(MeshVersion.V_1_14); }
	public static boolean isAtLeast1_18() { return serverVersion.isNewerThanOrEquals(MeshVersion.V_1_18); }
	public static boolean isAtLeast1_19() { return serverVersion.isNewerThanOrEquals(MeshVersion.V_1_19); }
	public static boolean isAtLeast1_20() { return serverVersion.isNewerThanOrEquals(MeshVersion.V_1_20); }
	public static boolean isAtLeast1_20_2() { return serverVersion.isNewerThanOrEquals(MeshVersion.V_1_20_2); }
	public static boolean isBefore1_21() { return serverVersion.isOlderThan(MeshVersion.V_1_21); }
	public static boolean isAtLeast1_12_2() { return serverVersion.isNewerThanOrEquals(MeshVersion.V_1_12_2); }
	public static boolean isAtLeast1_21_2() { return serverVersion.isNewerThanOrEquals(MeshVersion.V_1_21_2); }
	public static boolean isAtLeast26_1() { return serverVersion.isNewerThanOrEquals(MeshVersion.V_26_1); }
	/** Never on Fabric: kept so ported branches resolve identically to Vulcan on a non-Folia server. */
	public static boolean isFolia() { return false; }
}
