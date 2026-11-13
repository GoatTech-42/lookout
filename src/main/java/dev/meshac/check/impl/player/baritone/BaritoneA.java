package dev.meshac.check.impl.player.baritone;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.packet.Digging.Action;
import dev.meshac.packet.Digging;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.data.PlayerData;
import dev.meshac.tracker.RotationTracker;

@CheckInfo(name = "Baritone", type = 'A', complexType = "Rotations", description = "Checks for Baritone like rotations.")
public class BaritoneA extends AbstractCheck {

    private int rotationsSinceDigCancellation;

    public BaritoneA(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isRotationUpdatePacket(packetEvent) && ++this.rotationsSinceDigCancellation < 10) {
            RotationTracker rotation = this.playerData.getRotationTracker();
            float deltaYaw = rotation.getDeltaYaw();
            float deltaPitch = rotation.getDeltaPitch();
            boolean cinematic = rotation.isCinematic();
            if (deltaYaw < 0.005 && deltaPitch < 0.005 && deltaYaw > 0.0 && deltaPitch > 0.0 && !cinematic) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail("deltaYaw=" + deltaYaw + " deltaPitch=" + deltaPitch);
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
