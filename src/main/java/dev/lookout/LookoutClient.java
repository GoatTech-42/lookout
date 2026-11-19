package dev.lookout;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.lookout.core.LookoutConfig;
import dev.lookout.core.LookoutEngine;
import dev.lookout.core.ObservedSample;
import dev.lookout.mc.ChatAlerter;
import dev.lookout.mc.ClientWorldProbe;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.GameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * lookout - client-side companion to meshac. Watches other players from what the
 * server sends the client and drops the local user a chat note when someone keeps
 * moving like a cheat. No server component, never punishes anyone, private.
 */
public class LookoutClient implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("lookout");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static LookoutEngine ENGINE;
    private static long tickCounter;

    @Override
    public void onInitializeClient() {
        LookoutConfig config = loadConfig();
        ENGINE = new LookoutEngine(config, new ChatAlerter());
        LOGGER.info("[lookout] watching (client-side companion to meshac)");

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ENGINE.clear());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || client.player == null) return;
            tickCounter++;
            ClientLevel level = client.level;
            ClientWorldProbe probe = new ClientWorldProbe(level);
            for (Entity entity : level.entitiesForRendering()) {
                if (!(entity instanceof RemotePlayer other)) continue;
                ENGINE.tick(other.getId(), other.getGameProfile().name(), sample(client, other), probe);
            }
            if (tickCounter % 100 == 0) ENGINE.sweep(tickCounter);
        });
    }

    private static ObservedSample sample(Minecraft client, RemotePlayer other) {
        MobEffectInstance jump = other.getEffect(MobEffects.JUMP_BOOST);
        MobEffectInstance speed = other.getEffect(MobEffects.SPEED);
        boolean exempt = false;
        if (client.getConnection() != null) {
            PlayerInfo info = client.getConnection().getPlayerInfo(other.getUUID());
            if (info != null) {
                GameType mode = info.getGameMode();
                exempt = mode == GameType.CREATIVE || mode == GameType.SPECTATOR;
            }
        }
        return new ObservedSample(
                tickCounter,
                other.getX(), other.getY(), other.getZ(),
                other.onGround(),
                other.isSprinting(),
                other.isFallFlying(),
                other.getPose() == Pose.SWIMMING,
                other.isUsingItem(),
                other.getVehicle() != null,
                other.isInWater(),
                other.hasEffect(MobEffects.LEVITATION),
                other.hasEffect(MobEffects.SLOW_FALLING),
                jump == null ? 0 : jump.getAmplifier() + 1,
                speed == null ? 0 : speed.getAmplifier() + 1,
                other.getItemBySlot(EquipmentSlot.FEET).hasFoil(),
                exempt || other.isSpectator());
    }

    private static LookoutConfig loadConfig() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("lookout.json");
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                LookoutConfig loaded = GSON.fromJson(reader, LookoutConfig.class);
                if (loaded != null) return loaded;
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("[lookout] could not read lookout.json, using defaults", e);
            }
        }
        LookoutConfig config = new LookoutConfig();
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            LOGGER.warn("[lookout] could not write default lookout.json", e);
        }
        return config;
    }
}
