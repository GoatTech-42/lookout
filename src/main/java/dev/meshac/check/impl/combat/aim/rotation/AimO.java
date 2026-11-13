package dev.meshac.check.impl.combat.aim.rotation;

import dev.meshac.packet.MeshPacketEvent;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.data.PlayerDataManager;
import dev.meshac.MeshServices;
import dev.meshac.player.CollisionState;
import dev.meshac.data.PlayerData;
import dev.meshac.tracker.RelativeMoveTracker;
import dev.meshac.tracker.RotationTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;

@CheckInfo(name = "Aim", type = 'O', complexType = "Small Yaw", description = "Too small pitch change.")
public class AimO extends AbstractCheck {

    public AimO(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if ((this.isRotationUpdatePacket(packetEvent)) && (this.hasRecentAttack(2))) {
            RotationTracker rotationTracker = this.playerData.getRotationTracker();
            float deltaYaw = rotationTracker.getDeltaYaw();
            RotationTracker rotationTracker2 = this.playerData.getRotationTracker();
            float deltaPitch = rotationTracker2.getDeltaPitch();
            boolean condition = deltaPitch < 0.05 && deltaPitch > 0.0F && deltaYaw == 0.0F;
            RelativeMoveTracker relativeMoveTracker = this.playerData.getRelativeMoveTracker();
            Entity entity = relativeMoveTracker.getLastAttackedEntity();
            if (entity == null || !(entity instanceof Player player)) {
                return;
            }
            PlayerData playerData = PlayerDataManager.get(player);
            if (playerData == null) {
                return;
            }
            CollisionState collisionState = playerData.getCollisionState();
            if (collisionState.getDeltaXZ() < 0.03) {
                return;
            }
            if (condition) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail("deltaPitch=" + deltaPitch + " deltaYaw=" + deltaYaw);
                }
            } else {
                this.decayBuffer();
            }
        }
    }

    @Override
    public void update() {
    }
}
