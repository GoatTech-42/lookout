package dev.meshac.check.impl.player.badpackets.inventory;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Bad Packets", type = 'J', complexType = "Held Item Slot", description = "Sent HeldItemSlot while placing.")
public class BadPacketsJ extends AbstractCheck {

    public BadPacketsJ(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isHeldItemChangePacket(packetEvent)) {
            boolean itemUseReceived = this.playerData.getMotionState().isItemUseReceived();
            boolean exempt = this.isExempt(new ExemptType[] { ExemptType.CLIENT_VERSION, ExemptType.SERVER_VERSION, ExemptType.CREATIVE, ExemptType.SPECTATOR });
            if (!exempt && itemUseReceived) {
                this.fail();
            }
        }
    }

    @Override
    public void update() {
    }
}
