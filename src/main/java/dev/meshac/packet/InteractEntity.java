package dev.meshac.packet;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

/** Ported wrapper record for the interact-entity packet. */
public record InteractEntity(int entityId, Action action, InteractionHand hand, Vec3 target, boolean sneaking) {

	public enum Action {
		ATTACK, INTERACT, INTERACT_AT
	}

	public Action getAction() {
		return action;
	}

	public int getEntityId() {
		return entityId;
	}

	public InteractionHand getHand() {
		return hand;
	}

	public Vec3 getTarget() {
		return target;
	}

	public boolean isSneaking() {
		return sneaking;
	}
}
