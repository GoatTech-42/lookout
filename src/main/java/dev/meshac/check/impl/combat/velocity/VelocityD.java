package dev.meshac.check.impl.combat.velocity;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.packet.InteractEntity;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.player.MotionState;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Velocity", type = 'D', complexType = "Horizontal", description = "Horizontal velocity modifications.", experimental = true)
public class VelocityD extends AbstractCheck {

    public VelocityD(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isMovementPacket(packetEvent)) {
            InteractEntity lastInteractionPacket = this.playerData.getLastInteractionPacket();
            lastInteractionPacket.getAction();
        }
    }

    @Override
    public void update() {
    }

    public float getRatio(PlayerData playerData) {
        MotionState motionState = playerData.getMotionState();
        double movementSpeed = motionState.getMovementSpeed();
        MotionState motionState2 = playerData.getMotionState();
        movementSpeed += motionState2.getSpeedAmplifier() * 0.2 * movementSpeed;
        return (float) movementSpeed;
    }
}
