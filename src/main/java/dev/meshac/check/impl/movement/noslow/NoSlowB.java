package dev.meshac.check.impl.movement.noslow;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.CheckUtils;
import dev.meshac.check.ExemptType;
import dev.meshac.player.CollisionState;
import dev.meshac.player.MotionState;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "No Slow", type = 'B', complexType = "Soul Sand", description = "Moving too quickly on Soul Sand.")
public class NoSlowB extends AbstractCheck {

    public NoSlowB(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        boolean condition;
        if (this.isPositionUpdatePacket(packetEvent)) {
            CollisionState collisionState = this.playerData.getCollisionState();
            double deltaXZ = collisionState.getDeltaXZ();
            float maxSpeed = CheckUtils.getMaxSpeed(this.playerData, 0.2F);
            MotionState motionState = this.playerData.getMotionState();
            double adjustedMaxSpeed = maxSpeed + motionState.getSpeedAmplifier() * 0.0625F;
            boolean exempt;
            boolean condition2;
            CollisionState collisionState2 = this.playerData.getCollisionState();
            boolean onSoulSand = collisionState2.isOnSoulSand();
            CollisionState collisionState3 = this.playerData.getCollisionState();
            boolean groundAligned = collisionState3.isGroundAligned();
            exempt = this.isExempt(new ExemptType[] { ExemptType.FLIGHT, ExemptType.CREATIVE, ExemptType.VELOCITY, ExemptType.TELEPORT, ExemptType.ENDER_PEARL, ExemptType.CHORUS_FRUIT, ExemptType.SOUL_SPEED, ExemptType.ELYTRA, ExemptType.RIPTIDE, ExemptType.HIGH_SPEED, ExemptType.DEPTH_STRIDER });
            condition2 = onSoulSand && deltaXZ > adjustedMaxSpeed && groundAligned;
            condition = (this.playerData.getCollisionState().getWalkSpeed() > 0.11F) || ((this.playerData.getMotionState()).getMovementSpeed() > 0.11F);
            boolean condition3 = condition;
            if (condition2 && !exempt && !condition3) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail("deltaXZ=" + deltaXZ + " max=" + adjustedMaxSpeed);
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
