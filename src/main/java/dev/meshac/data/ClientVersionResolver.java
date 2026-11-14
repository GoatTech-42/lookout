package dev.meshac.data;

import dev.meshac.config.MeshVersion;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.network.Connection;

/**
 * Faithful client version source: the client's own handshake protocol number,
 * captured at ClientIntentionPacket and applied at join. Only the running server's
 * protocol is mapped (computed at runtime, not a memorized table); anything else
 * stays UNKNOWN and ContextGate keeps version-dependent checks closed, which is
 * the correct fallback behavior on a vanilla-compatible server; translator-aware resolution lives in TranslatorHooks.
 */
public final class ClientVersionResolver {

	private static final Map<Connection, Integer> HANDSHAKE_PROTOCOLS = new ConcurrentHashMap<>();

	private ClientVersionResolver() { }

	public static void capture(Connection connection, int protocolVersion) {
		if (connection != null) HANDSHAKE_PROTOCOLS.put(connection, protocolVersion);
	}

	public static MeshVersion resolve(Connection connection) {
		Integer protocol = connection == null ? null : HANDSHAKE_PROTOCOLS.remove(connection);
		if (protocol == null) return MeshVersion.UNKNOWN;
		return protocol == SharedConstants.getProtocolVersion() ? MeshVersion.fromName(SharedConstants.getCurrentVersion().name()) : MeshVersion.UNKNOWN;
	}
}
