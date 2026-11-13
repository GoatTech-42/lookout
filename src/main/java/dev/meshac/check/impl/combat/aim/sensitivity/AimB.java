package dev.meshac.check.impl.combat.aim.sensitivity;

import dev.meshac.packet.MeshPacketEvent;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.data.PlayerData;
import dev.meshac.tracker.RotationTracker;

@CheckInfo(name = "Aim", type = 'B', complexType = "Modulo", description = "Invalid yaw change.")
public class AimB extends AbstractCheck {

    private static final String VIOLATION_DETAIL_PREFIX = "deltaYaw=";

    public AimB(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if ((this.isRotationUpdatePacket(packetEvent)) && (this.getAttackTicks() < 3)) {
            RotationTracker rotationTracker = this.playerData.getRotationTracker();
            float deltaYaw = rotationTracker.getDeltaYaw();
            boolean condition = deltaYaw > 0.0F && (deltaYaw % 0.25 == 0.0 || deltaYaw % 0.1 == 0.0);
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
