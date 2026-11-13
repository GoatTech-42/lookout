package dev.meshac.check.impl.movement.elytra;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.player.CollisionState;
import dev.meshac.player.MotionState;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Elytra", type = 'M', complexType = "Ascending", description = "Invalid ascension pattern.")
public class ElytraM extends AbstractCheck {

    public ElytraM(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if ((this.isPositionUpdatePacket(packetEvent)) && (!this.isTeleporting())) {
            MotionState motionState = this.playerData.getMotionState();
            if (!motionState.isGliding()) {
                return;
            }
            CollisionState collisionState = this.playerData.getCollisionState();
            double deltaY = collisionState.getDeltaY();
            if (Math.abs(deltaY) > 0.01) {
                double deltaYRemainderRemainder = 0.01 % (deltaY % 0.01);
                boolean exempt = this.isExempt(new ExemptType[] { ExemptType.FLIGHT, ExemptType.SLIME, ExemptType.SLOW_FALLING, ExemptType.LEVITATION, ExemptType.LIQUID, ExemptType.BUBBLE_COLUMN, ExemptType.LIQUID, ExemptType.RIPTIDE, ExemptType.CLIMBABLE });
                if (deltaYRemainderRemainder < 1.0E-8 && !exempt) {
                    if (this.increaseBuffer() > this.maximumBuffer) {
                        this.fail("deltaY=" + deltaY + " m=" + deltaYRemainderRemainder);
                    }
                } else {
                    this.decayBuffer();
                }
            }
        }
    }

    @Override
    public void update() {
    }
}
