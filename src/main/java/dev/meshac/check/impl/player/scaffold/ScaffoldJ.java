package dev.meshac.check.impl.player.scaffold;

import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.data.PlayerData;
import dev.meshac.tracker.RotationTracker;
import dev.meshac.util.MathUtils;
import dev.meshac.event.MeshEvent;
import dev.meshac.event.MeshBlockPlaceEvent;

@CheckInfo(name = "Scaffold", type = 'J', complexType = "Acceleration [2]", experimental = false, description = "Invalid pitch change.")
public class ScaffoldJ extends AbstractCheck {

    public ScaffoldJ(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public void update() {
    }

    public void handleEvent(Event event2) {
        if ((event2 instanceof MeshBlockPlaceEvent) && (this.playerData.getMotionState().isLastPlacementValid())) {
            double deltaXZChange = this.playerData.getCollisionState().getDeltaXZChange();
            RotationTracker rotationTracker = this.playerData.getRotationTracker();
            float deltaPitch = rotationTracker.getDeltaPitch();
            boolean condition = MathUtils.isSmallScientificNotation(deltaXZChange) && deltaPitch > 18.0F;
            if (condition) {
                this.fail("acceleration=" + deltaXZChange + " deltaPitch=" + deltaPitch);
            }
        }
    }
}
