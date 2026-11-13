package dev.meshac.check.impl.movement.noslow;

import dev.meshac.packet.MeshPacketEvent;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.check.CheckUtils;
import dev.meshac.check.ExemptType;
import dev.meshac.player.CollisionState;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "No Slow", type = 'C', complexType = "Web", description = "Moving too quickly in a web.")
public class NoSlowC extends AbstractCheck {

    public NoSlowC(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isPositionUpdatePacket(packetEvent)) {
            CollisionState collisionState = this.playerData.getCollisionState();
            double deltaXZ = collisionState.getDeltaXZ();
            double maxSpeed = CheckUtils.getMaxSpeed(this.playerData, 0.09F);
            boolean touchingWeb = this.playerData.getCollisionState().isTouchingWeb();
            boolean exempt = this.isExempt(new ExemptType[] { ExemptType.FLIGHT, ExemptType.CREATIVE, ExemptType.VELOCITY, ExemptType.TELEPORT, ExemptType.ENDER_PEARL, ExemptType.CHORUS_FRUIT, ExemptType.RIPTIDE, ExemptType.ELYTRA, ExemptType.GLIDING, ExemptType.DEPTH_STRIDER, ExemptType.SERVER_POSITION_FAST, ExemptType.ELYTRA, ExemptType.GLIDING, ExemptType.SWIMMING, ExemptType.TRAPDOOR });
            boolean condition = touchingWeb && deltaXZ > maxSpeed;
            if (condition && !exempt) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail("deltaXZ=" + deltaXZ + " max=" + maxSpeed);
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
