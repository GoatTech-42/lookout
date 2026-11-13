package dev.meshac.check.impl.player.scaffold;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.CheckUtils;
import dev.meshac.check.ExemptType;
import dev.meshac.data.PlayerData;
import dev.meshac.tracker.RotationTracker;
import dev.meshac.util.MathUtils;
import dev.meshac.event.MeshEvent;
import dev.meshac.event.MeshBlockPlaceEvent;

@CheckInfo(name = "Scaffold", type = 'D', complexType = "Rotations", experimental = false, description = "Invalid rotations.")
public class ScaffoldD extends AbstractCheck {

    private long lastPlacementAtMillis;

    private long placementIntervalMillis;

    private boolean lastPlacementValid;

    private int rotationsSincePlacement;

    public ScaffoldD(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isRotationUpdatePacket(packetEvent) && ++this.rotationsSincePlacement < 6 && this.lastPlacementValid && this.placementIntervalMillis < 400L) {
            float deltaPitch;
            long longValue;
            boolean condition;
            RotationTracker rotationTracker = this.playerData.getRotationTracker();
            float previousDeltaPitch = rotationTracker.getPreviousDeltaPitch();
            RotationTracker rotationTracker2 = this.playerData.getRotationTracker();
            deltaPitch = rotationTracker2.getDeltaPitch();
            long scaledRotationGcdScale = (long) (MathUtils.ROTATION_GCD_SCALE * deltaPitch);
            long scaledRotationGcdScale2 = (long) (MathUtils.ROTATION_GCD_SCALE * previousDeltaPitch);
            longValue = MathUtils.approximateGcd(scaledRotationGcdScale, scaledRotationGcdScale2);
            condition = (this.isExempt(new ExemptType[] { ExemptType.CINEMATIC })) || ((this.playerData.getMotionState()).isSwiftSneak());
            boolean condition2 = condition;
            if (longValue < 131072L && longValue > 0L && deltaPitch > 0.5 && !condition2) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail("rotation=" + longValue / 100L + " dp=" + deltaPitch);
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
            this.rotationsSincePlacement = 0;
            this.lastPlacementAtMillis = System.currentTimeMillis();
        }
    }
}
