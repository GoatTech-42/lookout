package dev.meshac.check.impl.combat.reach;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.packet.InteractEntity;
import dev.meshac.packet.InteractEntity.Action;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.config.MeshSettings;
import dev.meshac.data.PlayerDataManager;
import dev.meshac.MeshServices;
import dev.meshac.player.CollisionState;
import dev.meshac.player.MotionState;
import dev.meshac.data.PlayerData;
import dev.meshac.player.PlayerToggles;
import dev.meshac.MeshServices;
import dev.meshac.tracker.ConnectionTracker;
import dev.meshac.tracker.RelativeMoveTracker;
import dev.meshac.collection.tuple.Pair;
import dev.meshac.util.MathUtils;
import dev.meshac.util.Location;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.server.level.ServerPlayer;
import dev.meshac.util.Vector;
import org.geysermc.floodgate.api.FloodgateApi;

@CheckInfo(name = "Reach", type = 'A', complexType = "History", description = "Hit from too far away.")
public class ReachA extends AbstractCheck {

    private boolean pendingAttack = true;

    public ReachA(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        boolean condition;
        if (this.isAttackPacket(packetEvent)) {
            InteractEntity lastInteractionPacket = this.playerData.getLastInteractionPacket();
            if (lastInteractionPacket.getAction() != InteractEntity.Action.ATTACK) {
                return;
            }
            RelativeMoveTracker relativeMoveTracker = this.playerData.getRelativeMoveTracker();
            if (!(relativeMoveTracker.getLastAttackedEntity() instanceof Player)) {
                return;
            }
            this.pendingAttack = true;
        } else if (this.isMovementPacket(packetEvent) && this.pendingAttack) {
            TickProcessor tickProcessor = PluginServices.INSTANCE.getTickProcessor();
            int totalTicks = tickProcessor.getTotalTicks();
            int index = MathUtils.pingCompensationTicks(this.playerData);
            CollisionState collisionState = this.playerData.getCollisionState();
            double x = collisionState.getX();
            CollisionState collisionState2 = this.playerData.getCollisionState();
            double z = collisionState2.getZ();
            Vector vector = new Vector(x, 0.0, z);
            RelativeMoveTracker relativeMoveTracker2 = this.playerData.getRelativeMoveTracker();
            double distance2 = relativeMoveTracker2.getTargetPositions().stream().filter(sample -> ReachA.isRecentTargetPosition(totalTicks, index, sample)).mapToDouble(sample -> ReachA.calculateTargetDistance(vector, sample)).min().orElse(-1.0);
            if (distance2 == -1.0) {
                this.decreaseBuffer(0.025);
            } else {
                RelativeMoveTracker relativeMoveTracker3 = this.playerData.getRelativeMoveTracker();
                PlayerData playerData = PlayerDataManager.get(relativeMoveTracker3.getPlayer());
                if (playerData == null) {
                    return;
                }
                if (playerData.getPlayer() == null) {
                    return;
                }
                if (MeshSettings.settingsIgnoreFlagsGeyserClientBrand && playerData.getClientBrand().toLowerCase().contains("geyser")) {
                    return;
                }
                if (MeshSettings.settingsIgnoreGeyserUuids && playerData.getUniqueId().toString().startsWith("00000")) {
                    return;
                }
                if ((MeshSettings.settingsIgnoreGeyserPrefixes) && (playerData.getPlayerName().startsWith(MeshSettings.settingsIgnoreGeyserPrefix))) {
                    return;
                }
                if ((MeshSettings.settingsIgnoreFloodgate && false) && (org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(playerData.getPlayer().getUniqueId()))) {
                    return;
                }
                if (MeshSettings.settingsIgnoreFloodgate && false) {
                    FloodgateApi instance = FloodgateApi.getInstance();
                    if (instance.isFloodgatePlayer(playerData.getPlayer().getUniqueId())) {
                        return;
                    }
                }
                condition = ((this.playerData.getPlayer() != null) && (this.dev.meshac.util.EnchantUtil.level(playerData.getPlayer().getMainHandItem(), net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK) > 0));
                boolean condition2 = condition;
                boolean exempt = this.isExempt(new ExemptType[] { ExemptType.CREATIVE, ExemptType.VEHICLE, ExemptType.BOAT });
                PlayerToggles playerToggles = playerData.getPlayerToggles();
                boolean anyExempt = playerToggles.isAnyExempt(new ExemptType[] { ExemptType.VEHICLE, ExemptType.BOAT });
                MotionState motionState = this.playerData.getMotionState();
                double adjustedEntityInteractionRange = motionState.getEntityInteractionRange() + (MeshSettings.checksCombatReachAMaxReach - 3.0);
                long currentTimeMillis2 = System.currentTimeMillis();
                ConnectionTracker connectionTracker = this.playerData.getConnectionTracker();
                if (currentTimeMillis2 - connectionTracker.getLastFastMovementAtMillis() < 1500L) {
                    adjustedEntityInteractionRange++;
                }
                if (this.isExempt(new ExemptType[] { ExemptType.LIQUID })) {
                    adjustedEntityInteractionRange += 0.1;
                }
                if (distance2 > adjustedEntityInteractionRange && distance2 < 8.0 && !condition2 && !exempt && !anyExempt) {
                    if (this.increaseBuffer() > this.maximumBuffer) {
                        this.fail("distance=" + distance2);
                    }
                } else {
                    this.decayBuffer();
                }
            }
            this.pendingAttack = false;
        }
    }

    @Override
    public void update() {
    }

    private static double calculateTargetDistance(Vector vector, Pair pair) {
        Vector vector2 = ((Location) pair.getFirst()).toVector().setY(0);
        return vector.distance(vector2) - 0.52;
    }

    private static boolean isRecentTargetPosition(int index, int index2, Pair pair) {
        return Math.abs(index - (Integer) pair.getSecond() - index2) < 3;
    }
}
