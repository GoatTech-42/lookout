package dev.meshac.check.impl.player.badpackets.inventory;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.packet.HeldItemChange;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Bad Packets", type = 'O', description = "Negative HeldItemSlot packet.")
public class BadPacketsO extends AbstractCheck {

    private static final String VIOLATION_DETAIL_PREFIX = "slot=";

    public BadPacketsO(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isHeldItemChangePacket(packetEvent)) {
            HeldItemChange lastHeldItemPacket = this.playerData.getLastHeldItemPacket();
            int slot = lastHeldItemPacket.getSlot();
            if (slot < 0) {
                this.fail(VIOLATION_DETAIL_PREFIX + slot);
            }
        }
    }

    @Override
    public void update() {
    }
}
