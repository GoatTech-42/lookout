package dev.meshac.check.impl.player.baritone;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.packet.Digging.Action;
import dev.meshac.packet.Digging;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.data.PlayerData;
import dev.meshac.check.ExemptType;
import dev.meshac.tracker.RotationTracker;

@CheckInfo(name = "Baritone", type = 'B', complexType = "Large Rotation", description = "Checks for Baritone like rotations.")
public class BaritoneB extends AbstractCheck {

    private int rotationsSinceDigCancellation = 100;

    public BaritoneB(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isRotationUpdatePacket(packetEvent) && ++this.rotationsSinceDigCancellation < 5) {
            RotationTracker rotation = this.playerData.getRotationTracker();
            float deltaYaw = rotation.getDeltaYaw();
            float deltaPitch = rotation.getDeltaPitch();
            float pitch = rotation.getPitch();
            boolean exempt = this.isExempt(new ExemptType[] { ExemptType.PISTON });
            if (deltaYaw > 95.0F && deltaPitch < 0.15 && Math.abs(pitch) < 70.0F && !exempt) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail("deltaPitch=" + deltaPitch + " deltaYaw=" + deltaYaw);
                }
            } else {
                this.decayBuffer();
            }
        } else if (this.isDiggingPacket(packetEvent)) {
            Digging lastDiggingPacket = this.playerData.getLastDiggingPacket();
            if (lastDiggingPacket.getAction() == Digging.Action.CANCELLED_DIGGING) {
                this.rotationsSinceDigCancellation = 0;
            }
        }
    }

    @Override
    public void update() {
    }
}
