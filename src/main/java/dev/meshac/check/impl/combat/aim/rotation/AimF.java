package dev.meshac.check.impl.combat.aim.rotation;

import dev.meshac.packet.MeshPacketEvent;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.data.PlayerDataManager;
import dev.meshac.MeshServices;
import dev.meshac.data.PlayerData;
import dev.meshac.player.PlayerToggles;
import dev.meshac.tracker.RelativeMoveTracker;
import dev.meshac.tracker.RotationTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;

@CheckInfo(name = "Aim", type = 'F', complexType = "Straight", description = "Invalid yaw change.")
public class AimF extends AbstractCheck {

    public AimF(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        boolean condition;
        if ((this.isRotationUpdatePacket(packetEvent)) && (this.hasRecentAttack(3))) {
            RotationTracker rotationTracker = this.playerData.getRotationTracker();
            float deltaYaw = rotationTracker.getDeltaYaw();
            RotationTracker rotationTracker2 = this.playerData.getRotationTracker();
            float deltaPitch = rotationTracker2.getDeltaPitch();
            RotationTracker rotationTracker3 = this.playerData.getRotationTracker();
            float pitch = rotationTracker3.getPitch();
            RelativeMoveTracker relativeMoveTracker = this.playerData.getRelativeMoveTracker();
            double attackDistance = relativeMoveTracker.getAttackDistance();
            RelativeMoveTracker relativeMoveTracker2 = this.playerData.getRelativeMoveTracker();
            Entity entity = relativeMoveTracker2.getLastAttackedEntity();
            if (entity == null || !(entity instanceof Player player)) {
                return;
            }
            PlayerData playerData = PlayerDataManager.get(player);
            if (playerData == null) {
                return;
            }
            boolean exempt;
            PlayerToggles playerToggles = playerData.getPlayerToggles();
            exempt = playerToggles.isExempt(ExemptType.VEHICLE);
            condition = (deltaPitch == 0.0F && deltaYaw > 1.825F && Math.abs(pitch) < 60.0F && attackDistance > 1.0) && ((playerData.getCollisionState()).getDeltaXZ() > 0.03);
            boolean condition2 = condition;
            if (condition2 && !exempt) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail("deltaYaw=" + deltaYaw + " deltaPitch=" + deltaPitch);
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
