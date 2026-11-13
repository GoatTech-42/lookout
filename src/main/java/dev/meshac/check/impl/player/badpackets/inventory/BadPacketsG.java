package dev.meshac.check.impl.player.badpackets.inventory;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.packet.HeldItemChange;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Bad Packets", type = 'G', complexType = "Hotbar", description = "Invalid HeldItemSlot packet.")
public class BadPacketsG extends AbstractCheck {

    private int lastSlot = -1;

    public BadPacketsG(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isHeldItemChangePacket(packetEvent)) {
            HeldItemChange lastHeldItemPacket = this.playerData.getLastHeldItemPacket();
            int count2 = lastHeldItemPacket.getSlot();
            if (count2 == this.lastSlot) {
                this.fail("slot=" + count2 + " lastSlot=" + this.lastSlot);
            }
            this.lastSlot = count2;
        }
    }

    @Override
    public void update() {
    }
}
