package dev.meshac.check.impl.combat.aim.rotation;

import dev.meshac.packet.MeshPacketEvent;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.data.PlayerData;
import dev.meshac.tracker.RelativeMoveTracker;
import dev.meshac.tracker.RotationTracker;

@CheckInfo(name = "Aim", type = 'Y', complexType = "Rotation", description = "Generic rotation analysis heuristic.")
public class AimY extends AbstractCheck {

    public AimY(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if ((this.isRotationUpdatePacket(packetEvent)) && (this.getAttackTicks() < 3) && (!this.isTeleporting())) {
            float difference;
            double attackDistance;
            boolean condition;
            RotationTracker rotationTracker = this.playerData.getRotationTracker();
            float previousYawSmoothing = rotationTracker.getPreviousYawSmoothing();
            float yawSmoothing = this.playerData.getRotationTracker().getYawSmoothing();
            difference = Math.abs(yawSmoothing - previousYawSmoothing);
            RelativeMoveTracker relativeMoveTracker = this.playerData.getRelativeMoveTracker();
            attackDistance = relativeMoveTracker.getAttackDistance();
            condition = (this.isExempt(new ExemptType[] { ExemptType.TELEPORT, ExemptType.DEATH, ExemptType.WORLD_CHANGE })) || (!((this.playerData.getMotionState()).getTicksSinceServerPosition() >= 20));
            boolean condition2 = condition;
            if (condition2) {
                return;
            }
            if (attackDistance > 0.6 && difference > 20.0F && attackDistance < 10.0) {
                this.fail("diff=" + difference + " dist=" + attackDistance);
            }
        }
    }

    @Override
    public void update() {
    }
}
