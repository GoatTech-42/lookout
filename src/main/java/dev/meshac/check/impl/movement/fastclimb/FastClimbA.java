package dev.meshac.check.impl.movement.fastclimb;

import dev.meshac.packet.MeshPacketEvent;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.check.ExemptionManager;
import dev.meshac.player.CollisionState;
import dev.meshac.player.MotionState;
import dev.meshac.data.PlayerData;
import dev.meshac.tracker.VelocityTracker;
import dev.meshac.world.BlockUtils;
import net.minecraft.world.level.block.state.BlockState;

@CheckInfo(name = "Fast Climb", type = 'A', complexType = "Limit", description = "Climbing too quickly")
public class FastClimbA extends AbstractCheck {

    public FastClimbA(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isPositionUpdatePacket(packetEvent)) {
            int ticksOnLadder = this.playerData.getCollisionState().getTicksOnLadder();
            CollisionState collisionState = this.playerData.getCollisionState();
            double deltaY = collisionState.getDeltaY();
            boolean ladderOrVine = this.playerData.getCollisionState().isLadderOrVine();
            MotionState motionState = this.playerData.getMotionState();
            if (motionState.getGravity() < 0.08) {
                return;
            }
            if ((this.playerData.getMotionState().getDistanceFromBrokenBlock() < 1.25) && (this.playerData.getMotionState().getDistanceFromBrokenBlock() > 0.0)) {
                return;
            }
            boolean condition = false;
            if (ExemptionManager.isNewerThan1_13()) {
                CollisionState collisionState2 = this.playerData.getCollisionState();
                if ((collisionState2.getGroundMaterial() != null) && (this.playerData.getCollisionState().getSubGroundMaterial() != null)) {
                    BlockState material = this.playerData.getCollisionState().getGroundMaterial();
                    BlockState material2 = this.playerData.getCollisionState().getSubGroundMaterial();
                    if (BlockUtils.isClimbable(material)) {
                        if (BlockUtils.isClimbable(material2)) {
                            condition = true;
                        }
                    }
                }
            } else {
                CollisionState collisionState3 = this.playerData.getCollisionState();
                if ((collisionState3.getGroundMaterialLegacy() != null) && (this.playerData.getCollisionState().getSubGroundMaterialLegacy() != null)) {
                    BlockState material3 = this.playerData.getCollisionState().getGroundMaterialLegacy();
                    BlockState material4 = this.playerData.getCollisionState().getSubGroundMaterialLegacy();
                    if (BlockUtils.isClimbable(material3)) {
                        if (BlockUtils.isClimbable(material4)) {
                            condition = true;
                        }
                    }
                }
            }
            double amount = 0.118;
            VelocityTracker velocityTracker = this.playerData.getVelocityTracker();
            if (velocityTracker.getTicksSinceVelocity() < 20) {
                return;
            }
            VelocityTracker velocityTracker2 = this.playerData.getVelocityTracker();
            if (velocityTracker2.getTicksSinceExplosion() < 20) {
                return;
            }
            if (!condition) {
                return;
            }
            boolean condition2 = deltaY > amount && ticksOnLadder > 3 && ladderOrVine;
            boolean exempt = this.isExempt(new ExemptType[] { ExemptType.JUMP_BOOST, ExemptType.FLIGHT, ExemptType.CREATIVE, ExemptType.GLIDING, ExemptType.LEVITATION, ExemptType.SLEEPING, ExemptType.ENDER_PEARL, ExemptType.TELEPORT, ExemptType.WORLD_CHANGE, ExemptType.DEATH, ExemptType.SCAFFOLDING, ExemptType.LIQUID, ExemptType.WATERLOGGED, ExemptType.BLOCK_PLACE, ExemptType.RIPTIDE, ExemptType.LIQUID, ExemptType.BUBBLE_COLUMN, ExemptType.SLIME, ExemptType.PROJECTILE_DAMAGE, ExemptType.WALL });
            if (condition2 && !exempt) {
                if (this.increaseBuffer() > this.maximumBuffer || deltaY > 1.0) {
                    this.fail("deltaY=" + deltaY + " ticks=" + ticksOnLadder);
                }
            } else {
                this.decayBuffer();
            }
        }
    }

    @Override
    public void update() {
    }
}
