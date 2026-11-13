package dev.meshac.check.impl.combat.velocity;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Velocity", type = 'C', complexType = "Ignored Vertical", description = "Vertical velocity modifications.")
public class VelocityC extends AbstractCheck {

    public VelocityC(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        this.isMovementPacket(packetEvent);
    }

    @Override
    public void update() {
    }
}
