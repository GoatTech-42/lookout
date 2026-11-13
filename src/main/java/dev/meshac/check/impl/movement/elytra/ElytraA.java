package dev.meshac.check.impl.movement.elytra;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.check.ExemptionManager;
import dev.meshac.config.MeshSettings;
import dev.meshac.player.CollisionState;
import dev.meshac.player.MotionState;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Elytra", type = 'A', complexType = "Limit", description = "Gliding too quickly on X/Z axis.")
public class ElytraA extends AbstractCheck {

    private static final String VIOLATION_DETAIL_PREFIX = "deltaXZ=";

    public ElytraA(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        boolean condition;
        if ((this.isPositionUpdatePacket(packetEvent)) && (!this.isTeleporting())) {
            if (ExemptionManager.isBefore1_9()) {
                return;
            }
            MotionState motionState = this.playerData.getMotionState();
            if (!motionState.isGliding()) {
                return;
            }
            double deltaXZ;
            CollisionState collisionState = this.playerData.getCollisionState();
            deltaXZ = collisionState.getDeltaXZ();
            condition = !((!this.isExempt(new ExemptType[] { ExemptType.RIPTIDE, ExemptType.TELEPORT, ExemptType.WORLD_CHANGE, ExemptType.DEATH, ExemptType.ENDER_PEARL, ExemptType.CHORUS_FRUIT, ExemptType.SERVER_POSITION_FAST, ExemptType.DRAGON_DAMAGE, ExemptType.DOLPHINS_GRACE })) && (this.playerData.getCollisionState().getTicksSinceEntityCramming() >= 200));
            boolean condition2 = condition;
            boolean recentRiptide = this.playerData.getCollisionState().getTicksSinceRiptide() < 400;
            if (deltaXZ > MeshSettings.checksMovementElytraAMaxSpeed && !condition2 && !recentRiptide) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail(VIOLATION_DETAIL_PREFIX + deltaXZ);
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
