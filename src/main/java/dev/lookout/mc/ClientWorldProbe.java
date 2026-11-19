package dev.lookout.mc;

import dev.lookout.core.WorldProbe;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/** World probe over the client's loaded chunks. */
public class ClientWorldProbe implements WorldProbe {

    private final ClientLevel level;

    public ClientWorldProbe(ClientLevel level) {
        this.level = level;
    }

    private BlockState stateAt(double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        if (!level.isLoaded(pos)) return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        return level.getBlockState(pos);
    }

    @Override
    public boolean isSolidAt(double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        if (!level.isLoaded(pos)) return true; // unloaded reads as support: never flag on missing chunks
        BlockState state = level.getBlockState(pos);
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    @Override
    public boolean isLiquidAt(double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        return level.isLoaded(pos) && !level.getFluidState(pos).isEmpty();
    }

    @Override
    public boolean isClimbableAt(double x, double y, double z) {
        return stateAt(x, y, z).is(BlockTags.CLIMBABLE);
    }

    @Override
    public boolean isStairLikeAt(double x, double y, double z) {
        BlockState state = stateAt(x, y, z);
        return state.getBlock() instanceof StairBlock || state.getBlock() instanceof SlabBlock;
    }

    @Override
    public boolean isSlipperyAt(double x, double y, double z) {
        return stateAt(x, y, z).is(BlockTags.ICE);
    }

    @Override
    public boolean isSoulGroundAt(double x, double y, double z) {
        return stateAt(x, y, z).is(BlockTags.SOUL_SPEED_BLOCKS);
    }

    @Override
    public boolean isWebAt(double x, double y, double z) {
        // 26.3: WebBlock slows to Vec3(0.25, 0.05, 0.25) and resets fall distance
        return stateAt(x, y, z).getBlock() instanceof net.minecraft.world.level.block.WebBlock;
    }
}
