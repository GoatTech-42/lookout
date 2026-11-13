package dev.meshac.check.impl.combat.velocity;

import dev.meshac.packet.MeshPacketEvent;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.player.CollisionState;
import dev.meshac.data.PlayerData;
import dev.meshac.tracker.VelocityTracker;
import dev.meshac.util.MathUtils;

@CheckInfo(name = "Velocity", type = 'B', complexType = "Horizontal", description = "Horizontal velocity modifications.")
public class VelocityB extends AbstractCheck {

    public VelocityB(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        boolean movementPacket = this.isMovementPacket(packetEvent);
        if (movementPacket) {
            CollisionState collisionState = this.playerData.getCollisionState();
            double deltaXZ = collisionState.getDeltaXZ();
            double velocityX = this.playerData.getVelocityTracker().getVelocityX();
            VelocityTracker velocityTracker = this.playerData.getVelocityTracker();
            double velocityXZ = MathUtils.hypot(velocityX, velocityTracker.getVelocityZ());
            if (velocityXZ == 0.01) {
                return;
            }
            double scaledDeltaXZ = deltaXZ / velocityXZ;
            VelocityTracker velocityTracker2 = this.playerData.getVelocityTracker();
            boolean empty = velocityTracker2.getPendingVelocities().isEmpty();
            if (!(scaledDeltaXZ <= 0.21)) {
                this.fail("percent=" + scaledDeltaXZ * 100.0 + "% deltaXZ=" + deltaXZ + " velocityXZ=" + velocityXZ);
                this.decayBuffer();
            }
        }
    }

    @Override
    public void update() {
    }
}
