package dev.meshac.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Replaces org.bukkit.event.block.BlockPlaceEvent. */
public final class MeshBlockPlaceEvent extends MeshEvent {

	private final ServerPlayer player;
	private final MeshBlock block;
	private final MeshBlock blockAgainst;
	private final InteractionHand hand;
	private final ItemStack item;

	public MeshBlockPlaceEvent(ServerPlayer player, MeshBlock block, MeshBlock blockAgainst, InteractionHand hand, ItemStack item) {
		this.player = player;
		this.block = block;
		this.blockAgainst = blockAgainst;
		this.hand = hand;
		this.item = item;
	}

	public ServerPlayer getPlayer() { return player; }
	public MeshBlock getBlock() { return block; }
	public MeshBlock getBlockPlaced() { return block; }
	public MeshBlock getBlockAgainst() { return blockAgainst; }
	public InteractionHand getHand() { return hand; }
	public ItemStack getItemInHand() { return item; }
	public boolean canBuild() { return true; }
}
