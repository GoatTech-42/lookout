package dev.meshac.packet;

/** Ported wrapper record for the held-item-change packet. */
public record HeldItemChange(int slot) {

	public int getSlot() {
		return slot;
	}
}
