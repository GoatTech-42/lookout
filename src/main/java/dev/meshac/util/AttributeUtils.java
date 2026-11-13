package dev.meshac.util;

import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** Attribute snapshot math. Ported from Vulcan's AttributeUtils. */
public final class AttributeUtils {

	private AttributeUtils() {
	}

	public static double getMovementSpeedWithoutSprint(ClientboundUpdateAttributesPacket.AttributeSnapshot snapshot) {
		double baseValue = snapshot.base();
		var modifiers = new java.util.ArrayList<>(snapshot.modifiers());
		modifiers.removeIf(AttributeUtils::isSprintModifier);
		double base = baseValue;
		for (AttributeModifier modifier : modifiers) {
			if (modifier.operation() != AttributeModifier.Operation.ADD_VALUE) continue;
			base += modifier.amount();
		}
		double modified = base;
		for (AttributeModifier modifier : modifiers) {
			if (modifier.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_BASE) continue;
			modified += base * modifier.amount();
		}
		for (AttributeModifier modifier : modifiers) {
			if (modifier.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) continue;
			modified *= 1.0 + modifier.amount();
		}
		return modified;
	}

	private static boolean isSprintModifier(AttributeModifier modifier) {
		String id = modifier.id().toString();
		return id.equals("662a6b8d-da3e-4c1c-8813-96ea6097278d") || id.toLowerCase().contains("sprint");
	}
}
