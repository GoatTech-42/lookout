package dev.meshac.check.impl.player.scaffold;

import java.util.ArrayList;
import java.util.List;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.check.ExemptionManager;
import dev.meshac.geometry.ray.RaySegment;
import dev.meshac.player.CollisionState;
import dev.meshac.data.PlayerData;
import dev.meshac.tracker.RotationTracker;
import dev.meshac.util.MathUtils;
import dev.meshac.world.BlockUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import dev.meshac.event.MeshBlock;
import net.minecraft.core.Direction;
import dev.meshac.event.MeshEvent;
import dev.meshac.event.MeshBlockPlaceEvent;
import dev.meshac.util.Vector;

@CheckInfo(name = "Scaffold", type = 'N', complexType = "Expand", description = "Invalid block face.", experimental = true)
public class ScaffoldN extends AbstractCheck {

    private List<dev.meshac.event.MeshBlock> recentPlacements = new ArrayList();

    public ScaffoldN(PlayerData playerData) {
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
            if (BlockUtils.isScaffolding(blockPlaceEvent.getBlock().getType()) || BlockUtils.isSlab(blockPlaceEvent.getBlock().getType())) {
                return;
            }
            if (BlockUtils.isStairs(blockPlaceEvent.getBlock().getType())) {
                return;
            }
            dev.meshac.event.MeshBlock block = blockPlaceEvent.getBlock();
            if (this.recentPlacements.contains(blockPlaceEvent.getBlockAgainst())) {
                double y = blockPlaceEvent.getBlockPlaced().getY();
                CollisionState collisionState = this.playerData.getCollisionState();
                boolean belowYLimit = y < collisionState.getY();
                Direction blockFace = blockPlaceEvent.getBlockAgainst().getFace(blockPlaceEvent.getBlock());
                BlockState material;
                if (ExemptionManager.isNewerThan1_13()) {
                    material = this.playerData.getCollisionState().getSubGroundMaterial();
                } else {
                    material = this.playerData.getCollisionState().getSubGroundMaterialLegacy();
                }
                if (material == null || blockFace == null) {
                    return;
                }
                if (blockFace == Direction.DOWN || blockFace == Direction.UP) {
                    return;
                }
                CollisionState collisionState2 = this.playerData.getCollisionState();
                double xDifference = Math.abs(collisionState2.getX() - blockPlaceEvent.getBlockPlaced().getX());
                CollisionState collisionState3 = this.playerData.getCollisionState();
                double zDifference = Math.abs(collisionState3.getZ() - blockPlaceEvent.getBlockPlaced().getZ());
                double distanceXZ = MathUtils.hypot(xDifference, zDifference);
                int blockX = blockPlaceEvent.getBlock().getLocation().getBlockX();
                int blockZ = blockPlaceEvent.getBlock().getLocation().getBlockZ();
                CollisionState collisionState4 = this.playerData.getCollisionState();
                int blockX2 = collisionState4.getBlockX();
                CollisionState collisionState5 = this.playerData.getCollisionState();
                int blockZ2 = collisionState5.getBlockZ();
                boolean condition2 = blockX == blockX2 || blockZ == blockZ2;
                boolean condition3 = BlockUtils.isAir(material) && belowYLimit && distanceXZ > 1.85 && condition2;
                boolean exempt = this.isExempt(new ExemptType[] { ExemptType.FLIGHT, ExemptType.CREATIVE });
                if (condition3 && !exempt) {
                    if (this.playerData.getPlayer() == null) {
                        return;
                    }
                    Vector vector = new dev.meshac.util.Location(this.playerData.getPlayer().level(), this.playerData.getPlayer().getX(), this.playerData.getPlayer().getY(), this.playerData.getPlayer().getZ()).toVector();
                    RotationTracker rotationTracker = this.playerData.getRotationTracker();
                    float yaw = rotationTracker.getYaw();
                    RotationTracker rotationTracker2 = this.playerData.getRotationTracker();
                    RaySegment raySegment = new RaySegment(vector, this.getPosition(yaw, rotationTracker2.getPitch()));
                    net.minecraft.server.level.ServerLevel world = this.playerData.getPlayer().level();
                    List list = raySegment.solidBlocksAlongRay(world, 3.0, 1.0);
                    if (!list.isEmpty()) {
                        if (this.increaseBuffer() > this.maximumBuffer) {
                            this.fail("distanceXZ=" + distanceXZ + " r=" + list.size());
                        }
                    } else {
                        this.decayBuffer();
                    }
                }
                this.recentPlacements.clear();
            }
            this.recentPlacements.add(block);
        }
    }

    private Direction faceFromYaw(float amount) {
        float amount2 = (amount - 90.0F) % 360.0F;
        if (amount2 < 0.0F) {
            amount2 = (float) (amount2 + 360.0);
        }
        return this.faceFromAngle(amount2);
    }

    private Direction faceFromAngle(float amount) {
        if (0.0F <= amount && amount < 45.0F) {
            return Direction.WEST;
        }
        if (45.0F <= amount && amount < 135.0F) {
            return Direction.NORTH;
        }
        if (135.0F <= amount && amount < 225.0F) {
            return Direction.EAST;
        }
        if (225.0F <= amount && amount < 315.0F) {
            return Direction.SOUTH;
        }
        return 315.0F <= amount && amount < 360.0 ? Direction.WEST : null;
    }

    public Vector getPosition(float amount, float amount2) {
        Vector vector = new Vector();
        double amount3 = amount;
        double amount4 = amount2;
        vector.setY(-Math.sin(Math.toRadians(amount4)));
        double amount5 = Math.cos(Math.toRadians(amount4));
        vector.setX(-amount5 * Math.sin(Math.toRadians(amount3)));
        vector.setZ(amount5 * Math.cos(Math.toRadians(amount3)));
        return vector;
    }
}
