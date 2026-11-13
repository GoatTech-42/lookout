package dev.meshac.event;

import net.minecraft.server.level.ServerPlayer;

/** Replaces org.bukkit.event.block.BlockBreakEvent. */
public final class MeshBlockBreakEvent extends MeshEvent {

	private final ServerPlayer player;
	private final MeshBlock block;

	public MeshBlockBreakEvent(ServerPlayer player, MeshBlock block) {
		this.player = player;
		this.block = block;
	}

	public ServerPlayer getPlayer() { return player; }
	public MeshBlock getBlock() { return block; }
}
