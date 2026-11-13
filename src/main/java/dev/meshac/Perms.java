package dev.meshac;

import net.minecraft.server.level.ServerPlayer;

/** Permission checks: op-level based (no permissions plugin on Fabric). Level 2 = command user. */
public final class Perms {

	private Perms() {
	}

	public static boolean has(ServerPlayer player, String node) {
		return player != null && player.hasPermissions(2);
	}
}
