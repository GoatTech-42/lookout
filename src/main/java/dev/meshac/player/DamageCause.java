package dev.meshac.player;

/** Damage causes Vulcan's MotionState switches on; fed by server damage hooks. */
public enum DamageCause {
	ENTITY_ATTACK, FALL, SUFFOCATION, FIRE, FIRE_TICK, MAGIC, POISON, CONTACT, PROJECTILE, LAVA, OTHER
}
