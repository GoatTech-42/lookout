package dev.meshac.event;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Replaces org.bukkit.event.player.PlayerInteractEvent. */
public final class MeshInteractEvent extends MeshEvent {

	public enum Action { RIGHT_CLICK_BLOCK, RIGHT_CLICK_AIR, LEFT_CLICK_BLOCK, LEFT_CLICK_AIR }

	private final ServerPlayer player;
	private final Action action;
	private final MeshBlock clickedBlock;
	private final Direction blockFace;
	private final InteractionHand hand;
	private final ItemStack item;

	public MeshInteractEvent(ServerPlayer player, Action action, MeshBlock clickedBlock, Direction blockFace, InteractionHand hand, ItemStack item) {
		this.player = player;
		this.action = action;
		this.clickedBlock = clickedBlock;
		this.blockFace = blockFace;
		this.hand = hand;
		this.item = item;
	}

	public ServerPlayer getPlayer() { return player; }
	public Action getAction() { return action; }
	public MeshBlock getClickedBlock() { return clickedBlock; }
	public Direction getBlockFace() { return blockFace; }
	public InteractionHand getHand() { return hand; }
	public ItemStack getItem() { return item; }
}
