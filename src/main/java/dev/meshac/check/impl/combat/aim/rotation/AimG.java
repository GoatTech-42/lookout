package dev.meshac.check.impl.combat.aim.rotation;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.data.PlayerDataManager;
import dev.meshac.MeshServices;
import dev.meshac.data.PlayerData;
import dev.meshac.player.PlayerToggles;
import dev.meshac.tracker.RelativeMoveTracker;
import dev.meshac.tracker.RotationTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;

@CheckInfo(name = "Aim", type = 'G', complexType = "Ratio", description = "Too large yaw change.")
public class AimG extends AbstractCheck {

    public AimG(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if ((this.isRotationUpdatePacket(packetEvent)) && (this.hasRecentAttack(2))) {
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
            PlayerToggles playerToggles = playerData.getPlayerToggles();
            boolean exempt = playerToggles.isExempt(ExemptType.VEHICLE);
            boolean condition = deltaYaw > 15.0F && deltaPitch < 0.1 && Math.abs(pitch) < 65.0F && attackDistance > 1.0;
            if (condition && !exempt) {
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
