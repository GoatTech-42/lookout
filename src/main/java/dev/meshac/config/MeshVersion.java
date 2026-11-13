package dev.meshac.config;

/**
 * Comparable Minecraft release identity, mirroring the packetevents ServerVersion predicates
 * Vulcan's checks branch on. Each jar targets one server version, parsed once from the running
 * server; client versions come from dev.meshac.data.PlayerData (Via hook or server version).
 */
public enum MeshVersion {
	V_1_7_10, V_1_8, V_1_9, V_1_11, V_1_12, V_1_12_2, V_1_13, V_1_14, V_1_16, V_1_17, V_1_18, V_1_19, V_1_20, V_1_20_2, V_1_21, V_1_21_2, V_1_21_11, V_26_1, V_26_2, V_26_3, UNKNOWN;

	public boolean isNewerThan(MeshVersion other) {
		return this.ordinal() > other.ordinal();
	}

	public boolean isNewerThanOrEquals(MeshVersion other) {
		return this.ordinal() >= other.ordinal();
	}

	public boolean isOlderThan(MeshVersion other) {
		return this.ordinal() < other.ordinal();
	}

	/** Parses names like "1.20.4", "1.21.11", "26.3" from DetectedVersion. */
	public static MeshVersion fromName(String name) {
		if (name == null) return UNKNOWN;
		String n = name.trim();
		return switch (n) {
			case "1.7.10" -> V_1_7_10;
			case "1.8", "1.8.8", "1.8.9" -> V_1_8;
			case "1.9", "1.9.4" -> V_1_9;
			case "1.11", "1.11.2" -> V_1_11;
			case "1.12", "1.12.1" -> V_1_12;
			case "1.12.2" -> V_1_12_2;
			case "1.13", "1.13.2" -> V_1_13;
			case "1.14", "1.14.4" -> V_1_14;
			case "1.16", "1.16.5" -> V_1_16;
			case "1.17", "1.17.1" -> V_1_17;
			case "1.18", "1.18.2" -> V_1_18;
			case "1.19", "1.19.4" -> V_1_19;
			case "1.20", "1.20.1" -> V_1_20;
			case "1.20.2", "1.20.4", "1.20.6" -> V_1_20_2;
			case "1.21", "1.21.1" -> V_1_21;
			case "1.21.2", "1.21.4" -> V_1_21_2;
			case "1.21.11" -> V_1_21_11;
			case "26.1", "26.1.2" -> V_26_1;
			case "26.2" -> V_26_2;
			case "26.3" -> V_26_3;
			default -> UNKNOWN;
		};
	}
}
