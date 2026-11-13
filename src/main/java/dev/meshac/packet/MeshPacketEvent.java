package dev.meshac.packet;

import net.minecraft.server.level.ServerPlayer;

/**
 * One observed packet. Checks test {@link #getType()} exactly as they tested packetevents types,
 * and read packet fields through the typed helpers on the dispatcher (dev.meshac.packet.Packets).
 */
public final class MeshPacketEvent {

	private final MeshPacketType type;
	private final ServerPlayer player;
	private final Object packet; // net.minecraft.network.protocol.Packet<?>
	private final boolean serverbound;
	private final long timestampNanos;

	public MeshPacketEvent(MeshPacketType type, ServerPlayer player, Object packet, boolean serverbound) {
		this.type = type;
		this.player = player;
		this.packet = packet;
		this.serverbound = serverbound;
		this.timestampNanos = System.nanoTime();
	}

	public MeshPacketType getType() {
		return type;
	}

	public ServerPlayer getPlayer() {
		return player;
	}

	/** The underlying 26.3 protocol packet. */
	public Object getRawPacket() {
		return packet;
	}

	@SuppressWarnings("unchecked")
	public <T> T raw() {
		return (T) packet;
	}

	public boolean isServerbound() {
		return serverbound;
	}

	/** Upstream packetevents-style predicate: serverbound (client -> server) and of this type. */
	public boolean isClient(MeshPacketType t) {
		return serverbound && type == t;
	}

	/** Clientbound (server -> client) and of this type. */
	public boolean isServer(MeshPacketType t) {
		return !serverbound && type == t;
	}

	public long getTimestampNanos() {
		return timestampNanos;
	}
}
