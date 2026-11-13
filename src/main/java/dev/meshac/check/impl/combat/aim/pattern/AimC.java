package dev.meshac.check.impl.combat.aim.pattern;

import dev.meshac.packet.MeshPacketEvent;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.data.PlayerData;
import dev.meshac.tracker.RotationTracker;

@CheckInfo(name = "Aim", type = 'C', complexType = "Repeated", description = "Repeated yaw values.")
public class AimC extends AbstractCheck {

    private static final String VIOLATION_DETAIL_PREFIX = "deltaYaw=";

    public AimC(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if ((this.isRotationUpdatePacket(packetEvent)) && (this.hasRecentAttack(3))) {
            RotationTracker rotationTracker = this.playerData.getRotationTracker();
            float deltaYaw = rotationTracker.getDeltaYaw();
            float previousDeltaYaw = this.playerData.getRotationTracker().getPreviousDeltaYaw();
            boolean condition = deltaYaw > 1.25F && previousDeltaYaw > 1.25F && deltaYaw == previousDeltaYaw;
            if (condition) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail(VIOLATION_DETAIL_PREFIX + deltaYaw);
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
