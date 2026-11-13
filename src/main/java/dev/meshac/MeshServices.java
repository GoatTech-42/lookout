package dev.meshac;

import dev.meshac.alert.AlertManager;
import dev.meshac.check.AbstractCheck;
import dev.meshac.data.PlayerData;
import dev.meshac.punish.PunishmentHandler;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Static service hub replacing Vulcan's PluginServices. */
public final class MeshServices {

	private static final Logger LOGGER = LoggerFactory.getLogger("meshac");
	private static final Map<UUID, PlayerData> PLAYERS = new ConcurrentHashMap<>();
	private static final AlertManager ALERTS = new AlertManager();
	private static final PunishmentHandler PUNISHMENTS = new PunishmentHandler();
	private static final ExecutorService VIOLATION_EXECUTOR = Executors.newSingleThreadExecutor(r -> daemon(r, "meshac-violations"));
	private static final ExecutorService ALERT_EXECUTOR = Executors.newSingleThreadExecutor(r -> daemon(r, "meshac-alerts"));
	private static MinecraftServer server;
	private static int totalTicks;

	private MeshServices() {
	}

	private static Thread daemon(Runnable r, String name) {
		Thread t = new Thread(r, name);
		t.setDaemon(true);
		return t;
	}

	public static void init(MinecraftServer minecraftServer) {
		server = minecraftServer;
	}

	private static int lagSpikes;
	private static long lastTickAtMillis = Long.MAX_VALUE;
	private static long tickDurationMillis = Long.MAX_VALUE;

	/**
	 * Server tick. Port of Vulcan's TickProcessor.run core: per-player confirmation sends and
	 * lag-spike tracking. Dropped vs upstream: punishment statistics broadcast, JudgementDay,
	 * discord webhook cooldown (unported features - PARITY.md).
	 */
	public static void tick() {
		totalTicks++;
		for (PlayerData playerData : PLAYERS.values()) {
			playerData.getConfirmationTracker().sendConfirmation(true);
		}
		if (totalTicks % 200 == 0) lagSpikes = 0;
		long now = System.currentTimeMillis();
		tickDurationMillis = now - lastTickAtMillis;
		lastTickAtMillis = now;
		// Upstream: spike when tickDuration > 90, or <= 90 but < 10 (impossibly fast catch-up tick).
		if (totalTicks > 500 && (tickDurationMillis > 90L || tickDurationMillis < 10L)) {
			lagSpikes++;
		}
	}

	public static int lagSpikes() { return lagSpikes; }
	public static long tickDurationMillis() { return tickDurationMillis; }

	/** Boat entity id -> tick when hooked by a fishing rod. PARITY GAP: populated upstream by
	 *  PlayerFishEvent; the fishing-hook capture mixin is not yet ported, so this stays empty
	 *  (dependent BoatFly exemption inert) until it lands. */
	public static Map<Integer, Integer> hookedBoatTicks() { return HOOKED_BOAT_TICKS; }
	private static final Map<Integer, Integer> HOOKED_BOAT_TICKS = new ConcurrentHashMap<>();

	public static MinecraftServer server() { return server; }
	private static final long STARTED_AT_MILLIS = System.currentTimeMillis();
	public static long getStartedAtMillis() { return STARTED_AT_MILLIS; }
	public static int ticks() { return totalTicks; }
	public static Map<UUID, PlayerData> players() { return PLAYERS; }
	public static AlertManager alerts() { return ALERTS; }
	public static PunishmentHandler punishments() { return PUNISHMENTS; }
	public static ExecutorService violationExecutor() { return VIOLATION_EXECUTOR; }
	public static ExecutorService alertExecutor() { return ALERT_EXECUTOR; }

	public static double tps() {
		if (server == null) return 20.0;
		float mspt = server.getCurrentSmoothedTickTime();
		return mspt <= 0 ? 20.0 : Math.min(20.0, 1000.0 / mspt);
	}

	public static void log(String message) {
		LOGGER.info(message);
	}

	public static void logViolation(AbstractCheck check, PlayerData data, String info) {
		LOGGER.info("{} failed {} ({}) vl={} {}", data.getPlayerName(), check.getCheckKey(), check.getDisplayType(), check.getVl(), info);
	}

	public static void broadcastToOps(Component message) {
		if (server == null) return;
		server.execute(() -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (Perms.has(player, "meshac.alerts")) player.sendSystemMessage(message);
			}
		});
	}

	public static void mainThread(Runnable runnable) {
		if (server != null) server.execute(runnable);
	}
}
