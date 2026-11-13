package dev.meshac.check.impl.movement.sprint;

import dev.meshac.packet.MeshPacketEvent;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.data.PlayerData;
import dev.meshac.tracker.RotationTracker;

@CheckInfo(name = "Sprint", type = 'A', complexType = "Angle", description = "Invalid sprint direction.")
public class SprintA extends AbstractCheck {

    public SprintA(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (!this.isPositionUpdatePacket(packetEvent)) {
            RotationTracker rotationTracker = this.playerData.getRotationTracker();
            float movementLookAngle = rotationTracker.getMovementLookAngle();
            int sprintingTicks = this.playerData.getMotionState().getSprintingTicks();
            this.isExempt(new ExemptType[] { ExemptType.RIPTIDE, ExemptType.TELEPORT, ExemptType.WORLD_CHANGE, ExemptType.JOINED, ExemptType.SERVER_POSITION, ExemptType.SWIMMING, ExemptType.LIQUID, ExemptType.ICE, ExemptType.SWIMMING, ExemptType.SERVER_POSITION });
            if (sprintingTicks > 0) {
                this.fail("angle=" + movementLookAngle + " ticks=" + sprintingTicks);
                this.decayBuffer();
            }
        }
    }

    @Override
    public void update() {
    }
}
