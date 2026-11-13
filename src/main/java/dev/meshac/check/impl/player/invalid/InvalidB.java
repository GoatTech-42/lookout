package dev.meshac.check.impl.player.invalid;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Invalid", type = 'B', complexType = "Invalid", description = "Spamming ghost block setbacks. Do not ban for this check.")
public class InvalidB extends AbstractCheck {

    public InvalidB(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isPositionUpdatePacket(packetEvent) && this.playerData.getCollisionState().getGhostBlockSetbacks() > 5) {
            this.fail();
        }
    }

    @Override
    public void update() {
    }
}
