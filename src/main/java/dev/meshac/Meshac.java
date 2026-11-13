package dev.meshac;

import dev.meshac.config.MeshConfig;
import dev.meshac.config.MeshSettings;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * meshac - Vulcan-based anticheat for Fabric. Credits: Vulcan by frep / Skid-Technology OpenVulcan
 * deobfuscation (Vulcan 2.9.7.28), original meshac by Luke. Server-side only.
 */
public class Meshac implements DedicatedServerModInitializer {

	public static final String MOD_ID = "meshac";
	private static final Logger LOGGER = LoggerFactory.getLogger("meshac");

	@Override
	public void onInitializeServer() {
		LOGGER.info("[meshac] loading (Vulcan 2.9.7.28 port, credits: frep, OpenVulcan)");
		dev.meshac.safety.Containment.setLogger(MeshServices::log);
		MeshConfig.load();
		MeshSettings.loadSettings();
		ServerLifecycleEvents.SERVER_STARTED.register(MeshServices::init);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			MeshServices.tick();
			dev.meshac.veil.Veil.tick(server);
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				dev.meshac.data.PlayerDataManager.onJoin(handler.getPlayer()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				dev.meshac.data.PlayerDataManager.onQuit(handler.getPlayer()));
	}
}
