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
 * v5 contract (B103 design review): a KNOWN version never depends on local translator READINESS,
 * because a negative or late translator answer is not proof of a native Java client.
 *   A. ANY local Bedrock API class present (or broken): resolution is ALWAYS UNKNOWN. A negative
 *      or indeterminate Geyser/Floodgate answer is not proof of Java origin (registration can lag
 *      every observable stage), and a positive Via answer only describes the Java-side connection
 *      Geyser itself creates. Detection loss for ALL players on such a server is explicit and
 *      accepted: version-dependent checks stay closed there.
 *   B. Via class present without any Bedrock API: not loaded / state error / lookup error /
 *      unmapped / -1 all stay UNKNOWN with NO handshake fallback, because a translated client's
 *      handshake carries the SERVER's protocol. A loaded Via's positive known protocol resolves,
 *      under the documented assumption that the Via answer describes a Java-origin client: it is
 *      a protocol number, not an identity proof, and this server cannot see upstream topology.
 *   C. No translator classes at all: the handshake resolves natively. It is a protocol
 *      OBSERVATION, not proof that no external Geyser/proxy translates upstream of this server;
 *      the unknown-upstream-topology boundary is documented and no universal Bedrock-detection
 *      claim is made.
 * Resolution is stage-independent: the login-send HEAD and the Fabric JOIN stage return the best
 * answer available at that moment, and the JOIN stage re-resolves an UNKNOWN so a Via that
 * loaded between the two stages is picked up. This class never registers, enables or sends
 * anything.
 */
public final class TranslatorHooks {

	/** Optional-dependency presence: absent, present, or present-but-failed (indeterminate). */
	enum Presence { ABSENT, PRESENT, BROKEN }

	private static volatile boolean loggedLookupError;

	private TranslatorHooks() { }

	/**
	 * Resolves the player's client version; never throws, never invents a version, and never lets
	 * a KNOWN result depend on translator readiness (contract A/B/C in the class javadoc).
	 */
	public static MeshVersion resolve(UUID playerId, Connection connection) {
		// No identity: never resolve a version (contract A is unconditional for a null player).
		if (playerId == null) return MeshVersion.UNKNOWN;
		Presence bedrockPresence = Presence.ABSENT;
		Presence viaClassPresence = Presence.ABSENT;
		boolean viaLoaded = false;
		MeshVersion via = MeshVersion.UNKNOWN;
		bedrockPresence = BedrockLookup.presence();
		viaClassPresence = ViaLookup.classPresence();
		// Only case B needs the Via runtime: case A is always UNKNOWN and case C has no Via.
		if (bedrockPresence == Presence.ABSENT && viaClassPresence == Presence.PRESENT) {
			Presence viaRuntime = ViaLookup.presence();
			if (viaRuntime == Presence.PRESENT) {
				viaLoaded = true;
				via = ViaLookup.resolve(playerId);
			}
			// BROKEN or not loaded: stays UNKNOWN; the handshake is never consulted under B.
		}
		// The handshake is observed only in case C (no translator classes at all); under A or B it
		// is not even consumed.
		return choose(bedrockPresence, viaClassPresence, viaLoaded, via,
				bedrockPresence == Presence.ABSENT && viaClassPresence == Presence.ABSENT
						? ClientVersionResolver.resolve(connection) : null);
	}

	/** Pure decision, unit-tested directly: the A/B/C contract. */
	static MeshVersion choose(Presence bedrockPresence, Presence viaClassPresence,
			boolean viaLoaded, MeshVersion via, MeshVersion handshake) {
		// A: any local Bedrock API class present/broken - always UNKNOWN (see class javadoc).
		if (bedrockPresence != Presence.ABSENT) return MeshVersion.UNKNOWN;
		// B: Via class present, no Bedrock API - loaded positive known protocol or UNKNOWN; never
		// the handshake.
		if (viaClassPresence != Presence.ABSENT) return viaLoaded && via != null ? via : MeshVersion.UNKNOWN;
		// C: translator-free - handshake protocol observation, UNKNOWN on mismatch/absence.
		return handshake == null ? MeshVersion.UNKNOWN : handshake;
	}

	/** Class-presence gate: the impl classes below load only when their translator jar is present. */
	private static final class BedrockLookup {
		private static final Presence GEYSER = present("org.geysermc.geyser.api.GeyserApi");
		private static final Presence FLOODGATE = present("org.geysermc.floodgate.api.FloodgateApi");

		static Presence presence() {
			if (GEYSER == Presence.BROKEN || FLOODGATE == Presence.BROKEN) return Presence.BROKEN;
			return (GEYSER == Presence.PRESENT || FLOODGATE == Presence.PRESENT) ? Presence.PRESENT : Presence.ABSENT;
		}
	}

	private static final class ViaLookup {
		private static final Presence CLASS = present("com.viaversion.viaversion.api.Via");

		/** Class presence alone, independent of runtime loaded state. */
		static Presence classPresence() {
			return CLASS;
		}

		/** ABSENT = no Via runtime; PRESENT = loaded; BROKEN = present but its state check failed
		 *  (indeterminate). Used only in case B. */
		static Presence presence() {
			if (CLASS != Presence.PRESENT) return CLASS;
			try {
				return ViaImpl.isLoaded() ? Presence.PRESENT : Presence.ABSENT;
			} catch (Throwable t) {
				logLookupError("ViaVersion state check", t);
				return Presence.BROKEN;
			}
		}

		/** Loaded Via's answer: the mapped client version, UNKNOWN on any miss or error. A
		 *  protocol number, not a Java-origin identity proof (see class javadoc, case B). */
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
