package dev.meshac.check.impl.player.ghosthand;

import java.util.ArrayList;
import java.util.List;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.config.MeshSettings;
import dev.meshac.data.PlayerData;
import dev.meshac.world.BlockUtils;
import net.minecraft.world.level.block.state.BlockState;
import dev.meshac.event.MeshBlock;
import net.minecraft.core.Direction;
import dev.meshac.event.MeshEvent;
import dev.meshac.event.MeshBlockBreakEvent;

@CheckInfo(name = "Ghost Hand", type = 'A', complexType = "Bed", description = "Invalid bed break.")
public class GhostHandA extends AbstractCheck {

    public GhostHandA(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public void update() {
    }

    public void handleEvent(MeshEvent event) {
        if (!(event instanceof MeshBlockBreakEvent)) {
            return;
        }
        MeshBlock broken = ((MeshBlockBreakEvent) event).getBlock();
        String brokenType = broken.getType().toString();
        if (!brokenType.contains("BED") || brokenType.equalsIgnoreCase("BEDROCK")) {
            return;
        }
        if (this.playerData.getCollisionState().getY() - broken.getY() < 0.0) {
            return;
        }
        Direction facing = getBedFacing(broken);
        if (facing == null) {
            return;
        }
        checkSurroundings((MeshBlockBreakEvent) event, facing);
    }

    private static Direction getBedFacing(MeshBlock broken) {
        Direction facing = null;
        if (broken.getLocation().add(0.0, 0.0, 1.0).getBlock().getType().toString().contains("BED")) {
            facing = Direction.SOUTH;
        }
        if (broken.getLocation().add(-1.0, 0.0, 0.0).getBlock().getType().toString().contains("BED")) {
            facing = Direction.WEST;
        }
        if (broken.getLocation().add(0.0, 0.0, -1.0).getBlock().getType().toString().contains("BED")) {
            facing = Direction.NORTH;
        }
        if (broken.getLocation().add(1.0, 0.0, 0.0).getBlock().getType().toString().contains("BED")) {
            facing = Direction.EAST;
        }
        return facing;
    }

    private void checkSurroundings(MeshBlockBreakEvent event, Direction facing) {
        List<BlockState> nearby = new ArrayList<>();
        for (double[] offset : neighborOffsets(facing)) {
            nearby.add(event.getBlock().getLocation().add(offset[0], offset[1], offset[2]).getBlock().getType());
        }
        boolean surrounded = nearby.stream().allMatch(BlockState::isSolid) && nearby.stream().allMatch(BlockState::canOcclude);
        nearby.remove(0);
        nearby.remove(0);
        if (!surrounded) {
            if (!nearby.stream().allMatch(BlockUtils::isGlass)) {
                return;
            }
        }
        this.fail("direction=" + facing + " blocks=" + nearby);
        if (MeshSettings.checksPlayerGhosthandACancel) {
            event.setCancelled(true);
        }
    }

    private static double[][] neighborOffsets(Direction facing) {
        switch(facing) {
            case SOUTH:
                return new double[][] { { 0.0, -1.0, 0.0 }, { 0.0, -1.0, 1.0 }, { 0.0, 0.0, -1.0 }, { -1.0, 0.0, 0.0 }, { 1.0, 0.0, 0.0 }, { -1.0, 0.0, 1.0 }, { 1.0, 0.0, 1.0 }, { 0.0, 0.0, 2.0 }, { 0.0, 1.0, 0.0 }, { 0.0, 1.0, 1.0 } };
            case NORTH:
                return new double[][] { { 0.0, -1.0, 0.0 }, { 0.0, -1.0, -1.0 }, { 0.0, 0.0, 1.0 }, { -1.0, 0.0, 0.0 }, { 1.0, 0.0, 0.0 }, { -1.0, 0.0, -1.0 }, { 1.0, 0.0, -1.0 }, { 0.0, 0.0, -2.0 }, { 0.0, 1.0, 0.0 }, { 0.0, 1.0, -1.0 } };
            case EAST:
                return new double[][] { { 0.0, -1.0, 0.0 }, { 1.0, -1.0, 0.0 }, { -1.0, 0.0, 0.0 }, { 0.0, 0.0, 1.0 }, { 0.0, 0.0, -1.0 }, { 1.0, 0.0, 1.0 }, { 1.0, 0.0, -1.0 }, { 2.0, 0.0, 0.0 }, { 0.0, 1.0, 0.0 }, { 1.0, 1.0, 0.0 } };
            case WEST:
                return new double[][] { { 0.0, -1.0, 0.0 }, { -1.0, -1.0, 0.0 }, { 1.0, 0.0, 0.0 }, { 0.0, 0.0, 1.0 }, { 0.0, 0.0, -1.0 }, { -1.0, 0.0, 1.0 }, { -1.0, 0.0, -1.0 }, { -2.0, 0.0, 0.0 }, { 0.0, 1.0, 0.0 }, { -1.0, 1.0, 0.0 } };
            default:
                return new double[0][0];
        }
    }
}
