package dev.meshac.event;

import dev.meshac.util.Location;
import net.minecraft.server.level.ServerPlayer;

/** Replaces org.bukkit.event.player.PlayerMoveEvent. */
public final class MeshMoveEvent extends MeshEvent {

	private final ServerPlayer player;
	private final Location from;
	private Location to;

	public MeshMoveEvent(ServerPlayer player, Location from, Location to) {
		this.player = player;
		this.from = from;
		this.to = to;
	}

	public ServerPlayer getPlayer() { return player; }
	public Location getFrom() { return from; }
	public Location getTo() { return to; }
	public void setTo(Location to) { this.to = to; }
}
