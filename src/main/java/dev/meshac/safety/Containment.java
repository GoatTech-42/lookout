package dev.meshac.safety;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central gate for every gameplay-visible side effect (kicks, teleports, rotation
 * changes, held-slot changes, vehicle ejections). Default mode is LOG_ONLY: the
 * effect is recorded and logged but never executed, so an unqualified build cannot
 * move, kick or mutate a player. Mode flips to ENFORCE only after runtime
 * qualification. This class is deliberately free of Minecraft types so the
 * LOG_ONLY contract is testable headless.
 */
public final class Containment {

	public enum Mode { LOG_ONLY, ENFORCE }

	public record Effect(String kind, String target, String detail) { }

	private static volatile Mode mode = Mode.LOG_ONLY;
	private static volatile java.util.function.Consumer<String> logger = System.out::println;
	private static final List<Effect> RECORDED = new CopyOnWriteArrayList<>();

	private Containment() { }

	public static Mode mode() { return mode; }

	public static void setLogger(java.util.function.Consumer<String> newLogger) {
		if (newLogger != null) logger = newLogger;
	}

	public static void setMode(Mode newMode) {
		mode = newMode == null ? Mode.LOG_ONLY : newMode;
		logger.accept("[meshac] containment mode: " + mode);
	}

	/** Effects recorded since the last drain (log-only or enforced). */
	public static List<Effect> recorded() { return List.copyOf(RECORDED); }

	/** Test hook: clears the recorded effect list. */
	public static void drain() { RECORDED.clear(); }

	/**
	 * Runs action only in ENFORCE mode. Always records the effect. Returns true
	 * when the action actually ran.
	 */
	public static boolean dispatch(Effect effect, Runnable action) {
		RECORDED.add(effect);
		if (mode != Mode.ENFORCE) {
			logger.accept("[meshac] contained effect (log-only): " + effect.kind()
					+ " target=" + effect.target() + " detail=" + effect.detail());
			return false;
		}
		if (action != null) action.run();
		return true;
	}
}
