package dev.meshac.check.impl.player.scaffold;

import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.CheckUtils;
import dev.meshac.check.ExemptType;
import dev.meshac.player.MotionState;
import dev.meshac.data.PlayerData;
import dev.meshac.tracker.RotationTracker;
import dev.meshac.world.BlockUtils;
import dev.meshac.event.MeshEvent;
import dev.meshac.event.MeshBlockPlaceEvent;

@CheckInfo(name = "Scaffold", type = 'G', complexType = "Speed", experimental = false, description = "Bridging too quickly.")
public class ScaffoldG extends AbstractCheck {

    private long lastPlacementAtMillis;

    public ScaffoldG(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public void update() {
    }

    public void handleEvent(MeshEvent event2) {
        if (event2 instanceof MeshBlockPlaceEvent blockPlaceEvent) {
            if (blockPlaceEvent.isCancelled() || BlockUtils.isSlab(blockPlaceEvent.getBlock().getType())) {
                return;
            }
            MotionState motionState = this.playerData.getMotionState();
            if (motionState.isSwiftSneak()) {
                return;
            }
            if (this.isExempt(new ExemptType[] { ExemptType.SLAB })) {
                return;
            }
            boolean validBlockPlacement = CheckUtils.isValidBlockPlacement(blockPlaceEvent);
            if (validBlockPlacement) {
                long currentTimeMillisDifference = System.currentTimeMillis() - this.lastPlacementAtMillis;
                RotationTracker rotationTracker = this.playerData.getRotationTracker();
                float movementLookAngle = rotationTracker.getMovementLookAngle();
                double deltaXZChange = this.playerData.getCollisionState().getDeltaXZChange();
                boolean scaffolding = BlockUtils.isScaffolding(blockPlaceEvent.getBlock().getType());
                boolean condition = deltaXZChange < 0.001 && deltaXZChange > 0.0 && currentTimeMillisDifference < 300L && movementLookAngle < 2.0F;
                if (condition && !scaffolding) {
                    if (this.increaseBuffer() > this.maximumBuffer) {
                        this.fail("angle=" + movementLookAngle + " delay=" + currentTimeMillisDifference + " acceleration=" + deltaXZChange);
                    }
                } else {
                    this.decayBuffer();
                }
                this.lastPlacementAtMillis = System.currentTimeMillis();
            }
        }
    }
}
