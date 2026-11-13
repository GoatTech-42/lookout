package dev.meshac.check.impl.player.badpackets.movement;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.player.CollisionState;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Bad Packets", type = 'Y', complexType = "Invalid Position", description = "NaN X/Y/Z values.")
public class BadPacketsY extends AbstractCheck {

    public BadPacketsY(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isPositionUpdatePacket(packetEvent)) {
            CollisionState collisionState = this.playerData.getCollisionState();
            double x = collisionState.getX();
            CollisionState collisionState2 = this.playerData.getCollisionState();
            double y = collisionState2.getY();
            CollisionState collisionState3 = this.playerData.getCollisionState();
            double z = collisionState3.getZ();
            if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z) || x >= 2.147483647E9 || y >= 2.147483647E9 || z >= 2.147483647E9) {
                this.fail("x=" + x + " y=" + y + " z=" + z);
            }
        }
    }

    @Override
    public void update() {
    }
}
