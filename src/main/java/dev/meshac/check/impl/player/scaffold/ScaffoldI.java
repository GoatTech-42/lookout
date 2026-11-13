package dev.meshac.check.impl.player.scaffold;

import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.data.PlayerData;
import dev.meshac.tracker.RotationTracker;
import dev.meshac.event.MeshEvent;
import dev.meshac.event.MeshBlockPlaceEvent;

@CheckInfo(name = "Scaffold", type = 'I', complexType = "Acceleration", description = "Invalid acceleration.")
public class ScaffoldI extends AbstractCheck {

    public ScaffoldI(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public void update() {
    }

    public void handleEvent(MeshEvent event2) {
        if (event2 instanceof MeshBlockPlaceEvent blockPlaceEvent) {
            if (blockPlaceEvent.isCancelled()) {
                return;
            }
            if (this.playerData.getMotionState().isLastPlacementValid()) {
                double deltaXZChange = this.playerData.getCollisionState().getDeltaXZChange();
                RotationTracker rotationTracker = this.playerData.getRotationTracker();
                float deltaPitch = rotationTracker.getDeltaPitch();
                RotationTracker rotationTracker2 = this.playerData.getRotationTracker();
                float deltaYaw = rotationTracker2.getDeltaYaw();
                boolean condition = deltaXZChange < 0.01 && deltaYaw > 150.0F && deltaPitch > 10.0F;
                if (condition) {
                    this.fail("acceleration=" + deltaXZChange + " deltaYaw=" + deltaYaw + " deltaPitch=" + deltaPitch);
                }
            }
        }
    }
}
