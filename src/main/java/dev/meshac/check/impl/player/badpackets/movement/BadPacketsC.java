package dev.meshac.check.impl.player.badpackets.movement;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.data.PlayerData;
import dev.meshac.tracker.RotationTracker;

@CheckInfo(name = "Bad Packets", type = 'C', complexType = "Pitch", description = "Impossible pitch.")
public class BadPacketsC extends AbstractCheck {

    private static final String VIOLATION_DETAIL_PREFIX = "pitch=";

    public BadPacketsC(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isRotationUpdatePacket(packetEvent)) {
            RotationTracker rotationTracker = this.playerData.getRotationTracker();
            float pitch = rotationTracker.getPitch();
            if (Math.abs(pitch) > 90.0F) {
                this.fail(VIOLATION_DETAIL_PREFIX + pitch);
            }
        }
    }

    @Override
    public void update() {
    }
}
