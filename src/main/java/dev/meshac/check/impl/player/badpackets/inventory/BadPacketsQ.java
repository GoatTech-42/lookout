package dev.meshac.check.impl.player.badpackets.inventory;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.packet.HeldItemChange;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Bad Packets", type = 'Q', complexType = "Hotbar", description = "Too big HeldItemSlot packet.")
public class BadPacketsQ extends AbstractCheck {

    private static final String VIOLATION_DETAIL_PREFIX = "slot=";

    public BadPacketsQ(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isHeldItemChangePacket(packetEvent)) {
            HeldItemChange lastHeldItemPacket = this.playerData.getLastHeldItemPacket();
            int slot = lastHeldItemPacket.getSlot();
            if (slot > 8) {
                this.fail(VIOLATION_DETAIL_PREFIX + slot);
            }
        }
    }

    @Override
    public void update() {
    }
}
