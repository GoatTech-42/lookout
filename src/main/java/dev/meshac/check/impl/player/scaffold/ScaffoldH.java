package dev.meshac.check.impl.player.scaffold;

import dev.meshac.packet.MeshPacketEvent;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.check.CheckUtils;
import dev.meshac.check.ExemptType;
import dev.meshac.player.CollisionState;
import dev.meshac.data.PlayerData;
import dev.meshac.tracker.RotationTracker;
import dev.meshac.event.MeshEvent;
import dev.meshac.event.MeshBlockPlaceEvent;

@CheckInfo(name = "Scaffold", type = 'H', complexType = "Rotations [3]", experimental = false, description = "Invalid rotations.")
public class ScaffoldH extends AbstractCheck {

    private long lastPlacementAtMillis;

    private long placementIntervalMillis;

    private boolean lastPlacementValid;

    private int rotationsSincePlacement;

    public ScaffoldH(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isRotationUpdatePacket(packetEvent) && ++this.rotationsSincePlacement < 5 && this.lastPlacementValid && this.placementIntervalMillis < 400L) {
            double sensitivityEstimate;
            float deltaPitch;
            boolean condition;
            double deltaXZ;
            boolean condition2;
            RotationTracker rotationTracker = this.playerData.getRotationTracker();
            sensitivityEstimate = rotationTracker.getSensitivityEstimate();
            RotationTracker rotationTracker2 = this.playerData.getRotationTracker();
            deltaPitch = rotationTracker2.getDeltaPitch();
            condition = sensitivityEstimate < -2.0 || sensitivityEstimate > 200.0 && deltaPitch > 0.55F;
            CollisionState collisionState = this.playerData.getCollisionState();
            deltaXZ = collisionState.getDeltaXZ();
            condition2 = (this.isExempt(new ExemptType[] { ExemptType.CINEMATIC })) || ((this.playerData.getMotionState()).isSwiftSneak());
            boolean condition3 = condition2;
            if (condition && !condition3 && deltaXZ > 0.25) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail("final=" + sensitivityEstimate + " dp=" + deltaPitch);
                }
            } else {
                this.decayBuffer();
            }
        }
    }

    public void handleEvent(Event event2) {
        if (event2 instanceof MeshBlockPlaceEvent blockPlaceEvent) {
            if (blockPlaceEvent.isCancelled()) {
                return;
            }
            this.lastPlacementValid = CheckUtils.isValidBlockPlacement(blockPlaceEvent);
            this.placementIntervalMillis = System.currentTimeMillis() - this.lastPlacementAtMillis;
            this.rotationsSincePlacement = 0;
            this.lastPlacementAtMillis = System.currentTimeMillis();
        }
    }
}
