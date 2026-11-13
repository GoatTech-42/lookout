package dev.meshac.check.impl.player.scaffold;

import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.player.CollisionState;
import dev.meshac.data.PlayerData;
import dev.meshac.tracker.RotationTracker;
import dev.meshac.util.MathUtils;
import dev.meshac.world.BlockUtils;
import net.minecraft.core.Direction;
import dev.meshac.event.MeshEvent;
import dev.meshac.event.MeshBlockPlaceEvent;

@CheckInfo(name = "Scaffold", type = 'M', complexType = "Expand", description = "Invalid block face.")
public class ScaffoldM extends AbstractCheck {

    public ScaffoldM(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public void update() {
    }

    public void handleEvent(Event event2) {
        if (event2 instanceof MeshBlockPlaceEvent blockPlaceEvent) {
            if (blockPlaceEvent.isCancelled()) {
                return;
            }
            if (BlockUtils.isScaffolding(blockPlaceEvent.getBlock().getType())) {
                return;
            }
            double y = blockPlaceEvent.getBlockPlaced().getY();
            CollisionState collisionState = this.playerData.getCollisionState();
            boolean belowYLimit = y < collisionState.getY();
            CollisionState collisionState2 = this.playerData.getCollisionState();
            double xDifference = Math.abs(collisionState2.getX() - blockPlaceEvent.getBlockPlaced().getX());
            CollisionState collisionState3 = this.playerData.getCollisionState();
            double zDifference = Math.abs(collisionState3.getZ() - blockPlaceEvent.getBlockPlaced().getZ());
            double distance = MathUtils.hypot(xDifference, zDifference);
            Direction blockFace = blockPlaceEvent.getBlockAgainst().getFace(blockPlaceEvent.getBlock());
            RotationTracker rotationTracker = this.playerData.getRotationTracker();
            float yaw = rotationTracker.getYaw();
            Direction blockFace2 = this.getFacingFromYaw(yaw);
            boolean condition2 = belowYLimit && distance > 1.0 && distance < 6.0 && blockFace == blockFace2;
            if (condition2) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail("[1] face=" + blockFace + " direction=" + blockFace2 + " distance=" + distance);
                }
            } else {
                this.decayBuffer();
            }
        }
    }

    private Direction getFacingFromYaw(float amount) {
        float amount2 = (amount - 90.0F) % 360.0F;
        if (amount2 < 0.0F) {
            amount2 = (float) (amount2 + 360.0);
        }
        return this.getFacingFromNormalizedYaw(amount2);
    }

    private Direction getFacingFromNormalizedYaw(float amount) {
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
        return 315.0F <= amount && amount < 360.0 ? Direction.WEST : Direction.SELF;
    }
}
