package dev.meshac.check.impl.player.scaffold;

import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.data.PlayerData;
import dev.meshac.event.MeshBlock;
import net.minecraft.core.Direction;
import dev.meshac.event.MeshEvent;
import dev.meshac.event.MeshInteractEvent.Action;
import dev.meshac.event.MeshInteractEvent;

@CheckInfo(name = "Scaffold", type = 'A', complexType = "Interact", description = "Interacted with the bottom of block.")
public class ScaffoldA extends AbstractCheck {

    private static final String VIOLATION_DETAIL_PREFIX = "block=";

    public ScaffoldA(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public void update() {
    }

    public void handleEvent(MeshEvent event2) {
        boolean condition;
        if (event2 instanceof MeshInteractEvent playerInteractEvent && playerInteractEvent.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            long currentTimeMillis2 = System.currentTimeMillis();
            boolean belowCurrentTimeMillis2DifferenceLimit = currentTimeMillis2 - this.playerData.getClickTimingData().getLastSwingAt() < 10000L;
            if (belowCurrentTimeMillis2DifferenceLimit) {
                return;
            }
            if (this.playerData.getPlayer() == null) {
                return;
            }
            boolean exempt;
            dev.meshac.event.MeshBlock block = new dev.meshac.util.Location(this.playerData.getPlayer().level(), this.playerData.getPlayer().getX(), this.playerData.getPlayer().getY(), this.playerData.getPlayer().getZ()).subtract(0.0, 1.0, 0.0).getBlock();
            exempt = this.isExempt(new ExemptType[] { ExemptType.TELEPORT, ExemptType.SERVER_POSITION });
            condition = ((playerInteractEvent.getClickedBlock() != null && playerInteractEvent.getClickedBlock() == block && playerInteractEvent.getBlockFace() == Direction.DOWN && playerInteractEvent.getClickedBlock().getType().isSolid()) && ((this.playerData.getPlayer().getMainHandItem().getItem() instanceof net.minecraft.world.item.BlockItem blockItem && blockItem.getBlock().defaultBlockState().isSolid())));
            boolean condition3 = condition;
            if (condition3 && !exempt) {
                this.fail(VIOLATION_DETAIL_PREFIX + playerInteractEvent.getClickedBlock().getType());
            }
        }
    }
}
