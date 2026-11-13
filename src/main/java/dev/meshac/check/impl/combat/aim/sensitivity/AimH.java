package dev.meshac.check.impl.combat.aim.sensitivity;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.data.PlayerDataManager;
import dev.meshac.MeshServices;
import dev.meshac.data.PlayerData;
import dev.meshac.tracker.RelativeMoveTracker;
import dev.meshac.tracker.RotationTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;

@CheckInfo(name = "Aim", type = 'H', complexType = "Negative", experimental = false, description = "Invalid sensitivity.")
public class AimH extends AbstractCheck {

    public AimH(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        boolean condition;
        if ((this.isRotationUpdatePacket(packetEvent)) && (this.getAttackTicks() < 3)) {
            RotationTracker rotationTracker = this.playerData.getRotationTracker();
            double sensitivityEstimate = rotationTracker.getSensitivityEstimate();
            RotationTracker rotationTracker2 = this.playerData.getRotationTracker();
            double deltaPitch = rotationTracker2.getDeltaPitch();
            RelativeMoveTracker relativeMoveTracker = this.playerData.getRelativeMoveTracker();
            Entity entity = relativeMoveTracker.getLastAttackedEntity();
            if (entity == null || !(entity instanceof Player player)) {
                return;
            }
            PlayerData playerData = PlayerDataManager.get(player);
            if (playerData == null) {
                return;
            }
            boolean exempt;
            boolean hasKnownSensitivity2;
            exempt = this.isExempt(new ExemptType[] { ExemptType.CINEMATIC });
            RotationTracker rotationTracker3 = this.playerData.getRotationTracker();
            hasKnownSensitivity2 = rotationTracker3.hasKnownSensitivity();
            condition = (sensitivityEstimate < -1.0 && deltaPitch < 20.0 && deltaPitch > 0.25 && deltaPitch > 1.25) && ((playerData.getCollisionState()).getDeltaXZ() > 0.01);
            boolean condition2 = condition;
            if (condition2 && !exempt && !hasKnownSensitivity2) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail("final=" + sensitivityEstimate + " deltaPitch=" + deltaPitch);
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
