package dev.meshac.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Replaces org.bukkit.event.entity.ProjectileLaunchEvent. */
public final class MeshProjectileLaunchEvent extends MeshEvent {

	private final ServerPlayer shooter;
	private final Entity entity;

	public MeshProjectileLaunchEvent(ServerPlayer shooter, Entity entity) {
		this.shooter = shooter;
		this.entity = entity;
	}

	public ServerPlayer getShooter() { return shooter; }
	public Entity getEntity() { return entity; }
}
