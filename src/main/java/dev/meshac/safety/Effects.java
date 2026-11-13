package dev.meshac.safety;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;

/** Minecraft-typed effect entry points; all funnel through Containment.dispatch. */
public final class Effects {

	private Effects() { }

	public static boolean disconnect(ServerPlayer player, Component reason) {
		if (player == null) return false;
		return Containment.dispatch(new Containment.Effect("disconnect", player.getName().getString(), reason.getString()),
				() -> player.connection.disconnect(reason));
	}

	public static boolean teleport(ServerPlayer player, double x, double y, double z, float yaw, float pitch) {
		if (player == null) return false;
		return Containment.dispatch(new Containment.Effect("teleport", player.getName().getString(),
						x + "," + y + "," + z),
				() -> player.connection.teleport(x, y, z, yaw, pitch));
	}

	public static boolean ejectVehicle(ServerPlayer player) {
		if (player == null) return false;
		return Containment.dispatch(new Containment.Effect("eject-vehicle", player.getName().getString(), ""), player::stopRiding);
	}

	public static boolean send(ServerPlayer player, Packet<?> packet, String purpose) {
		if (player == null) return false;
		return Containment.dispatch(new Containment.Effect("packet:" + purpose, player.getName().getString(),
						packet.getClass().getSimpleName()),
				() -> player.connection.send(packet));
	}
}
