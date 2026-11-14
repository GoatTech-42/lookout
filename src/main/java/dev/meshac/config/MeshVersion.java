package dev.meshac.config;

/**
 * Comparable Minecraft release identity, mirroring the packetevents ServerVersion predicates
 * Vulcan's checks branch on. Each jar targets one server version, parsed once from the running
 * server; client versions come from dev.meshac.data.PlayerData (Via hook or server version).
 */
public enum MeshVersion {
	V_1_7_10, V_1_8, V_1_9, V_1_11, V_1_12, V_1_12_2, V_1_13, V_1_14, V_1_15, V_1_16, V_1_17, V_1_18, V_1_19, V_1_20, V_1_20_2, V_1_21, V_1_21_2, V_1_21_11, V_26_1, V_26_2, V_26_3, UNKNOWN;

	// PacketEvents ClientVersion.UNKNOWN has protocol -1, before every supported client.
	private int comparisonOrder() { return this == UNKNOWN ? -1 : ordinal(); }

	public boolean isNewerThan(MeshVersion other) {
		return this.comparisonOrder() > other.comparisonOrder();
	}

	public boolean isNewerThanOrEquals(MeshVersion other) {
		return this.comparisonOrder() >= other.comparisonOrder();
	}

	public boolean isOlderThanOrEquals(MeshVersion other) { return this.comparisonOrder() <= other.comparisonOrder(); }

	public boolean isOlderThan(MeshVersion other) {
		return this.comparisonOrder() < other.comparisonOrder();
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
			case "1.15", "1.15.1", "1.15.2" -> V_1_15;
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

	/**
	 * Maps a Java Edition protocol number (as reported by ViaVersion) to a client version.
	 * Table pinned from the ViaVersion API 5.12.2 ProtocolVersion release registry
	 * (com.viaversion:viaversion-api:5.12.2, jar SHA256
	 * a94413ddac541d356da02d603d62d6c6eab043e22756a1607fc7d748220134ee). Protocols with no
	 * matching enum granularity (4 = 1.7.2-1.7.5, 210 = 1.10.x), snapshots and anything unknown
	 * map to UNKNOWN, so version-dependent checks stay closed rather than guess. Floor granularity:
	 * 768..773 all map to V_1_21_2 (the 1.21.2-1.21.10 family); no exact-patch identity is implied
	 * and no predicate may claim a newer exact 1.21.x than the family floor.
	 */
	public static MeshVersion fromProtocol(int protocol) {
		return switch (protocol) {
			case 5 -> V_1_7_10;
			case 47 -> V_1_8;
			case 107, 108, 109, 110 -> V_1_9;
			case 315, 316 -> V_1_11;
			case 335, 338 -> V_1_12;
			case 340 -> V_1_12_2;
			case 393, 401, 404 -> V_1_13;
			case 477, 480, 485, 490, 498 -> V_1_14;
			case 573, 575, 578 -> V_1_15;
			case 735, 736, 751, 753, 754 -> V_1_16;
			case 755, 756 -> V_1_17;
			case 757, 758 -> V_1_18;
			case 759, 760, 761, 762 -> V_1_19;
			case 763 -> V_1_20;
			case 764, 765, 766 -> V_1_20_2;
			case 767 -> V_1_21;
			case 768, 769, 770, 771, 772, 773 -> V_1_21_2;
			case 774 -> V_1_21_11;
			case 775 -> V_26_1;
			case 776 -> V_26_2;
			case 777 -> V_26_3;
			default -> UNKNOWN;
		};
	}
}
