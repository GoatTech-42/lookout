package dev.meshac.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Ported wrapper record for the player digging packet. */
public record Digging(Action action, BlockPos blockPosition, Direction blockFace, int sequence) {

	public enum Action {
		CHANGE_DESTROY_DIRECTION, STAB, START_DIGGING, CANCELLED_DIGGING, FINISHED_DIGGING, RELEASE_USE_ITEM, DROP_ITEM_STACK, DROP_ITEM, SWAP_ITEM_WITH_OFFHAND
	}

	public Action getAction() { return action; }
	public BlockPos getBlockPosition() { return blockPosition; }
	public Direction getBlockFace() { return blockFace; }
	public int getSequence() { return sequence; }
}
