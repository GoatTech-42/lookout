package dev.meshac.check.impl.combat.aim.rotation;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.data.PlayerDataManager;
import dev.meshac.MeshServices;
import dev.meshac.data.PlayerData;
import dev.meshac.tracker.RelativeMoveTracker;
import dev.meshac.tracker.RotationTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;

@CheckInfo(name = "Aim", type = 'D', complexType = "Straight", experimental = false, description = "Invalid pitch change.")
public class AimD extends AbstractCheck {

    public AimD(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        boolean condition;
        if ((this.isRotationUpdatePacket(packetEvent)) && (this.hasRecentAttack(3))) {
            RotationTracker rotationTracker = this.playerData.getRotationTracker();
            float pitch = rotationTracker.getPitch();
            RotationTracker rotationTracker2 = this.playerData.getRotationTracker();
            float deltaYaw = rotationTracker2.getDeltaYaw();
            RotationTracker rotationTracker3 = this.playerData.getRotationTracker();
            float deltaPitch = rotationTracker3.getDeltaPitch();
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
            condition = (deltaYaw > 2.75F && deltaPitch == 0.0F && Math.abs(pitch) < 65.0F && attackDistance > 1.0) && ((playerData.getCollisionState()).getDeltaXZ() > 0.03);
            boolean condition2 = condition;
            if (condition2) {
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
