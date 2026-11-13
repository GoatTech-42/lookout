package dev.meshac.check.impl.player.scaffold;

import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.data.PlayerData;
import dev.meshac.world.BlockUtils;
import net.minecraft.core.Direction;
import dev.meshac.event.MeshEvent;
import dev.meshac.event.MeshBlockPlaceEvent;

@CheckInfo(name = "Scaffold", type = 'B', complexType = "Interact", experimental = false, description = "Invalid interact.")
public class ScaffoldB extends AbstractCheck {

    private long lastPlacementAtMillis;

    public ScaffoldB(PlayerData playerData) {
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
            if (this.isExempt(new ExemptType[] { ExemptType.CANCELLED_PLACE })) {
                return;
            }
            long delay = System.currentTimeMillis() - this.lastPlacementAtMillis;
            Direction blockFace = blockPlaceEvent.getBlockAgainst().getFace(blockPlaceEvent.getBlock());
            boolean condition = BlockUtils.isTurtleEgg(blockPlaceEvent.getBlock().getType()) || BlockUtils.isSlab(blockPlaceEvent.getBlock().getType()) || BlockUtils.isTnt(blockPlaceEvent.getBlock().getType()) || BlockUtils.isConcretePowder(blockPlaceEvent.getBlock().getType());
            boolean exempt = this.isExempt(new ExemptType[] { ExemptType.LIQUID, ExemptType.CREATIVE, ExemptType.FLIGHT, ExemptType.TURTLE_EGG, ExemptType.SLAB });
            boolean condition2 = this.playerData.getMotionState().isLastPlacementValid() && false && delay < 300L;
            if (condition2 && !exempt && !condition) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail("delay=" + delay + " face=" + blockFace.toString().toLowerCase());
                }
            } else {
                this.decayBuffer();
            }
            this.lastPlacementAtMillis = System.currentTimeMillis();
        }
    }
}
