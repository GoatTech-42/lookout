package dev.meshac.product;

import net.minecraft.server.level.ServerPlayer;

/** A per-player action timeline for test servers. Switched on with MESHAC_TRACE; off means one boolean check and nothing else. */
public final class Trace {
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("meshac");
	public static final boolean ON = System.getenv("MESHAC_TRACE") != null;

	public static void t(ServerPlayer pl, String what) {
		if (ON) LOG.info("[tl] {} {} {} pos={},{},{} hp={}", System.currentTimeMillis(), pl.getGameProfile().name(), what, Math.round(pl.getX() * 100) / 100.0, Math.round(pl.getY() * 100) / 100.0, Math.round(pl.getZ() * 100) / 100.0, pl.getHealth());
	}
}

