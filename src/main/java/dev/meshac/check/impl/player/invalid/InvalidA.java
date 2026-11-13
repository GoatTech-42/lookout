package dev.meshac.check.impl.player.invalid;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Invalid", type = 'A', complexType = "Invalid", description = "Possible transaction disabler. Do not ban for this check.")
public class InvalidA extends AbstractCheck {

    private int serverFlightMismatchTicks;

    public InvalidA(PlayerData playerData) {
        super(playerData);
        this.serverFlightMismatchTicks = 0;
    }

    public void handlePacket(MeshPacketEvent event) {
        if (!isPositionUpdatePacket(event))
            return;
        if (playerData.getPlayer() != null && playerData.getCollisionState().isFlightAllowed() && !playerData.getPlayer().getAbilities().mayfly) {
            serverFlightMismatchTicks++;
        } else {
            serverFlightMismatchTicks = 0;
        }
        if (serverFlightMismatchTicks > 100) {
            fail();
        }
    }

    @Override
    public void update() {
    }
}
