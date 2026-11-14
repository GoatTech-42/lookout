package dev.meshac.data;

import dev.meshac.MeshServices;
import dev.meshac.config.MeshVersion;
import java.util.UUID;
import net.minecraft.network.Connection;

/**
 * Optional translator-aware client version resolution. Feeds PlayerData#setClientVersion (the
 * PARITY hook point) through soft, compile-only references to the public Via/Geyser/Floodgate API
 * jars: on a server without those mods the nested impl classes are never loaded, nothing is
 * registered at runtime, and resolution is exactly the old handshake path.
 *
 * Order and fail-safes:
 *   1. Bedrock (Geyser/Floodgate): a Bedrock player is not any Java Edition version, so it always
 *      resolves UNKNOWN; the handshake must not relabel it as the server version.
 *   2. ViaVersion: when loaded, its answer is AUTHORITATIVE. A translated client's handshake
 *      carries the SERVER's protocol, so a Via miss, lookup error or unmapped protocol stays
 *      UNKNOWN and never falls back to the handshake.
 *   3. Handshake fallback, only when no Via is loaded: ClientVersionResolver, UNKNOWN on mismatch.
 * Presence is tri-state: ABSENT (class missing or translator not running), PRESENT, or BROKEN
 * (class failed to load / state check errored). BROKEN and lookup errors are indeterminate and
 * close to UNKNOWN. This class never registers, enables or sends anything.
 *
 * Lifecycle: the first (login-send HEAD) call is PROVISIONAL. When any translator CLASS is
 * present, even not yet loaded, registration for this player can lag that point, so a provisional call defers to UNKNOWN and
 * leaves the captured handshake untouched; the definitive Fabric JOIN call re-resolves fully.
 * A provisional KNOWN result is only ever produced when no translator CLASS exists at all, so
 * a provisional known version is final and never needs re-derivation.
 */
public final class TranslatorHooks {

	/** Optional-dependency presence: absent, present, or present-but-failed (indeterminate). */
	enum Presence { ABSENT, PRESENT, BROKEN }

	private static volatile boolean loggedLookupError;

	private TranslatorHooks() { }

	/**
	 * Resolves the joining player's client version; never throws, never invents a version.
	 * provisional = the early login-send HEAD stage: with any translator CLASS present the result defers
	 * to UNKNOWN and the handshake is NOT consumed, because Bedrock/Via registration for this
	 * player can lag that point. The definitive JOIN stage re-resolves fully.
	 */
	public static MeshVersion resolve(UUID playerId, Connection connection, boolean provisional) {
		Boolean bedrock = Boolean.FALSE;
		boolean viaLoaded = false;
		MeshVersion via = MeshVersion.UNKNOWN;
		Presence bedrockPresence = Presence.ABSENT;
		Presence viaPresence = Presence.ABSENT;
		Presence viaClassPresence = Presence.ABSENT;
		if (playerId != null) {
			bedrockPresence = BedrockLookup.presence();
			viaPresence = ViaLookup.presence();
			viaClassPresence = ViaLookup.classPresence();
			// Deferral keys on CLASS presence, not loaded state: a Via jar on the classpath whose
			// runtime is not loaded yet at login must still defer, or the provisional stage would
			// return a false native KNOWN that the JOIN stage never re-derives.
			if (provisional && (bedrockPresence != Presence.ABSENT || viaClassPresence != Presence.ABSENT)) {
				// Deferred: registration may lag login; do not consume the handshake, do not guess.
				return MeshVersion.UNKNOWN;
			}
			if (bedrockPresence == Presence.BROKEN) {
				bedrock = null; // indeterminate: never guess
			} else if (bedrockPresence == Presence.PRESENT) {
				bedrock = BedrockLookup.check(playerId);
			}
			if (viaPresence == Presence.BROKEN) {
				viaLoaded = true; // block the handshake fallback: it is unreliable when Via is broken
			} else if (viaPresence == Presence.PRESENT) {
				viaLoaded = true;
				via = ViaLookup.resolve(playerId);
			}
		}
		return choose(provisional, bedrockPresence, viaClassPresence, bedrock, viaLoaded, via, ClientVersionResolver.resolve(connection));
	}

	/** Pure decision, unit-tested directly: provisional deferral, Bedrock veto, loaded-Via
	 *  authority, handshake fallback. */
	static MeshVersion choose(boolean provisional, Presence bedrockPresence, Presence viaClassPresence,
			Boolean bedrock, boolean viaLoaded, MeshVersion via, MeshVersion handshake) {
		// Provisional with any translator CLASS present (loaded or not): defer to UNKNOWN; a
		// provisional KNOWN is only possible with no translator class at all and is therefore final.
		if (provisional && (bedrockPresence != Presence.ABSENT || viaClassPresence != Presence.ABSENT)) return MeshVersion.UNKNOWN;
		// Indeterminate (null) and Bedrock (TRUE) both stay UNKNOWN: never guess a JE version for a
		// client we could not classify; the handshake must not relabel a Bedrock player.
		if (bedrock == null || bedrock.booleanValue()) return MeshVersion.UNKNOWN;
		// A loaded Via is authoritative: misses, errors and unmapped protocols stay UNKNOWN because
		// the handshake then carries the SERVER's protocol, not the client's.
		if (viaLoaded) return via == null ? MeshVersion.UNKNOWN : via;
		return handshake == null ? MeshVersion.UNKNOWN : handshake;
	}

	/** Presence gates: the impl classes below load only when their translator jar is present. */
	private static final class BedrockLookup {
		private static final Presence GEYSER = present("org.geysermc.geyser.api.GeyserApi");
		private static final Presence FLOODGATE = present("org.geysermc.floodgate.api.FloodgateApi");

		static Presence presence() {
			if (GEYSER == Presence.BROKEN || FLOODGATE == Presence.BROKEN) return Presence.BROKEN;
			return (GEYSER == Presence.PRESENT || FLOODGATE == Presence.PRESENT) ? Presence.PRESENT : Presence.ABSENT;
		}

		/** TRUE = Bedrock, FALSE = verified not Bedrock, null = indeterminate (lookup error). */
		static Boolean check(UUID playerId) {
			try {
				if (GEYSER == Presence.PRESENT && GeyserImpl.isBedrock(playerId)) return Boolean.TRUE;
				if (FLOODGATE == Presence.PRESENT && FloodgateImpl.isBedrock(playerId)) return Boolean.TRUE;
				return Boolean.FALSE;
			} catch (Throwable t) {
				logLookupError("Bedrock detection", t);
				return null;
			}
		}
	}

	/** Direct Geyser API references; loaded only when the GeyserApi class is present. */
	private static final class GeyserImpl {
		static boolean isBedrock(UUID playerId) {
			// Exact bytecode, base-api 1.0.3 org.geysermc.api.Geyser: the ONLY benign absence is the
			// message below. The same call throws RuntimeException with OTHER messages ("Api was not
			// an instance of ...") on API-class mismatch; those must propagate as indeterminate.
			final org.geysermc.geyser.api.GeyserApi api;
			try {
				api = org.geysermc.geyser.api.GeyserApi.api();
			} catch (RuntimeException e) {
				if ("Api has not been registered yet!".equals(e.getMessage())) return false;
				throw e;
			}
			return api.connectionByUuid(playerId) != null;
		}
	}

	/** Direct Floodgate API references; loaded only when the FloodgateApi class is present. */
	private static final class FloodgateImpl {
		static boolean isBedrock(UUID playerId) {
			// Probed against floodgate api 2.2.4-20250930.181800-5: getInstance() returns null when
			// Floodgate is not running. An isFloodgatePlayer error propagates as indeterminate.
			final org.geysermc.floodgate.api.FloodgateApi api = org.geysermc.floodgate.api.FloodgateApi.getInstance();
			return api != null && api.isFloodgatePlayer(playerId);
		}
	}

	private static final class ViaLookup {
		private static final Presence CLASS = present("com.viaversion.viaversion.api.Via");

		/** Class presence alone, independent of runtime loaded state; gates provisional deferral. */
		static Presence classPresence() {
			return CLASS;
		}

		/** ABSENT = no Via translating (handshake is the client's real protocol); PRESENT = loaded
		 *  and authoritative; BROKEN = present but its state check failed (indeterminate). */
		static Presence presence() {
			if (CLASS != Presence.PRESENT) return CLASS;
			try {
				return ViaImpl.isLoaded() ? Presence.PRESENT : Presence.ABSENT;
			} catch (Throwable t) {
				logLookupError("ViaVersion state check", t);
				return Presence.BROKEN;
			}
		}

		/** Authoritative Via answer: the mapped client version, UNKNOWN on any miss or error. */
		static MeshVersion resolve(UUID playerId) {
			try {
				return ViaImpl.protocol(playerId);
			} catch (Throwable t) {
				logLookupError("ViaVersion lookup", t);
				return MeshVersion.UNKNOWN;
			}
		}
	}

	/** Direct ViaVersion API references; loaded only when the Via class is present. */
	private static final class ViaImpl {
		static boolean isLoaded() {
			return com.viaversion.viaversion.api.Via.isLoaded();
		}
		static MeshVersion protocol(UUID playerId) {
			return MeshVersion.fromProtocol(com.viaversion.viaversion.api.Via.getAPI().getPlayerVersion(playerId));
		}
	}

	/** Class-presence probe: ClassNotFoundException is absence; any other load failure is a broken
	 *  (incompatible/incomplete) present translator and must resolve indeterminate, not absent. */
	static Presence present(String className) {
		try {
			Class.forName(className, false, TranslatorHooks.class.getClassLoader());
			return Presence.PRESENT;
		} catch (ClassNotFoundException absent) {
			return Presence.ABSENT;
		} catch (Throwable broken) {
			logLookupError("optional translator class " + className + " failed to load", broken);
			return Presence.BROKEN;
		}
	}

	private static void logLookupError(String what, Throwable t) {
		if (!loggedLookupError) {
			loggedLookupError = true;
			MeshServices.log("[meshac] " + what + " failed (" + t.getClass().getSimpleName()
					+ "); affected client versions resolve UNKNOWN (version-dependent checks stay closed).");
		}
	}
}
