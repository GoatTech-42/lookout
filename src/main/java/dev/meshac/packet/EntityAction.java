package dev.meshac.packet;

/** Ported wrapper record for the entity-action (sneak/sprint/elytra) packet. */
public record EntityAction(Action action, int jumpBoost) {

	public enum Action {
		START_SNEAKING, STOP_SNEAKING, START_SPRINTING, STOP_SPRINTING,
		START_JUMP_HORSE, STOP_JUMP_HORSE, OPEN_INVENTORY, START_FLYING_WITH_ELYTRA
	}

	public Action getAction() { return action; }
	public int getJumpBoost() { return jumpBoost; }
}
