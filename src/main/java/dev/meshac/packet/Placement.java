package dev.meshac.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;

/** Ported wrapper record for the block-placement packet. */
public record Placement(BlockPos blockPosition, Direction face, InteractionHand hand, float cursorX, float cursorY, float cursorZ, boolean insideBlock, int sequence) {

	public BlockPos getBlockPosition() { return blockPosition; }
	public Direction getFace() { return face; }
	public InteractionHand getHand() { return hand; }
	public float getCursorX() { return cursorX; }
	public float getCursorY() { return cursorY; }
	public float getCursorZ() { return cursorZ; }
	public boolean isInsideBlock() { return insideBlock; }
	public int getSequence() { return sequence; }
}
