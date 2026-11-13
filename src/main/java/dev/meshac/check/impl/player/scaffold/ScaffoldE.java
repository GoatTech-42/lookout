package dev.meshac.check.impl.player.scaffold;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.CheckUtils;
import dev.meshac.data.PlayerData;
import dev.meshac.tracker.RotationTracker;
import dev.meshac.event.MeshEvent;
import dev.meshac.event.MeshBlockPlaceEvent;

@CheckInfo(name = "Scaffold", type = 'E', complexType = "Rotations [2]", experimental = false, description = "Invalid rotations.")
public class ScaffoldE extends AbstractCheck {

    private long lastPlacementAtMillis;

    private long placementIntervalMillis;

    private boolean lastPlacementValid;

    private int movementsSincePlacement;

    public ScaffoldE(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isMovementPacket(packetEvent) && ++this.movementsSincePlacement < 3 && this.lastPlacementValid && this.placementIntervalMillis < 400L) {
            RotationTracker rotationTracker = this.playerData.getRotationTracker();
            float deltaPitch = rotationTracker.getDeltaPitch();
            RotationTracker rotationTracker2 = this.playerData.getRotationTracker();
            float deltaYaw = rotationTracker2.getDeltaYaw();
            if (deltaPitch > 8.0F && deltaYaw > 100.0F) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail("deltaYaw=" + deltaYaw + " deltaPitch=" + deltaPitch);
                }
            } else {
                this.decayBuffer();
            }
        }
    }

    public void handleEvent(MeshEvent event2) {
        if (event2 instanceof MeshBlockPlaceEvent blockPlaceEvent) {
            if (blockPlaceEvent.isCancelled()) {
                return;
            }
            this.lastPlacementValid = CheckUtils.isValidBlockPlacement(blockPlaceEvent);
            this.placementIntervalMillis = System.currentTimeMillis() - this.lastPlacementAtMillis;
            this.movementsSincePlacement = 0;
            this.lastPlacementAtMillis = System.currentTimeMillis();
        }
    }
}
