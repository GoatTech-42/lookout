package dev.meshac.check.impl.movement.elytra;

import dev.meshac.packet.MeshPacketEvent;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.player.CollisionState;
import dev.meshac.player.MotionState;
import dev.meshac.data.PlayerData;
import dev.meshac.tracker.RotationTracker;
import dev.meshac.tracker.VelocityTracker;

@CheckInfo(name = "Elytra", type = 'L', complexType = "Ascending", description = "Moving up wrongly.")
public class ElytraL extends AbstractCheck {

    public ElytraL(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        boolean condition;
        if ((this.isPositionUpdatePacket(packetEvent)) && (!this.isTeleporting())) {
            MotionState motionState = this.playerData.getMotionState();
            if (!motionState.isGliding()) {
                return;
            }
            double deltaY;
            float pitch;
            CollisionState collisionState = this.playerData.getCollisionState();
            deltaY = collisionState.getDeltaY();
            RotationTracker rotationTracker = this.playerData.getRotationTracker();
            pitch = rotationTracker.getPitch();
            condition = !((!this.isExempt(new ExemptType[] { ExemptType.DRAGON_DAMAGE, ExemptType.EXPLOSION, ExemptType.FIREWORK, ExemptType.TELEPORT, ExemptType.FISHING_ROD, ExemptType.RIPTIDE, ExemptType.BUBBLE_COLUMN, ExemptType.LIQUID, ExemptType.SLIME, ExemptType.PISTON, ExemptType.WORLD_CHANGE, ExemptType.SWIMMING, ExemptType.BED, ExemptType.LEVITATION, ExemptType.FLIGHT })) && (this.playerData.getCollisionState().getTicksSinceEntityCramming() >= 200));
            boolean condition2 = condition;
            double amount = 1.45;
            VelocityTracker velocityTracker = this.playerData.getVelocityTracker();
            if (velocityTracker.getTicksSinceVelocity() < 30) {
                VelocityTracker velocityTracker2 = this.playerData.getVelocityTracker();
                amount += velocityTracker2.getMaximumVerticalVelocity();
            }
            VelocityTracker velocityTracker3 = this.playerData.getVelocityTracker();
            if (velocityTracker3.getTicksSinceExplosion() < 30) {
                VelocityTracker velocityTracker4 = this.playerData.getVelocityTracker();
                amount += velocityTracker4.getLastExplosionY();
            }
            MotionState motionState2 = this.playerData.getMotionState();
            if (motionState2.getJumpBoostAmplifier() > 0) {
                MotionState motionState3 = this.playerData.getMotionState();
                amount += motionState3.getJumpBoostAmplifier() * 0.125;
            }
            int ticksSinceRiptide = this.playerData.getCollisionState().getTicksSinceRiptide();
            boolean recentRiptide = ticksSinceRiptide < 400;
            boolean condition4 = deltaY > amount && pitch > -20.0F;
            if (condition4 && !condition2 && !recentRiptide) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail("deltaY=" + deltaY + " pitch=" + pitch + " rt=" + ticksSinceRiptide);
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
