package dev.meshac.check.impl.player.scaffold;

import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.data.PlayerData;
import dev.meshac.event.MeshEvent;
import dev.meshac.event.MeshBlockPlaceEvent;

@CheckInfo(name = "Scaffold", type = 'C', complexType = "Sprint", description = "Sprinting while bridging.")
public class ScaffoldC extends AbstractCheck {

    public ScaffoldC(PlayerData playerData) {
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
            boolean validPlacement = this.playerData.getMotionState().getTicksSinceValidPlacement() < 1;
            int sprintingTicks = this.playerData.getMotionState().getSprintingTicks();
            boolean exempt = this.isExempt(new ExemptType[] { ExemptType.LIQUID, ExemptType.CREATIVE, ExemptType.FLIGHT });
            if (validPlacement && sprintingTicks > 5 && !exempt) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail("ticks=" + sprintingTicks);
                }
            } else {
                this.decayBuffer();
            }
        }
    }
}
