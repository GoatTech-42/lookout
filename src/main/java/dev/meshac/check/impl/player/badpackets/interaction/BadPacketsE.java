package dev.meshac.check.impl.player.badpackets.interaction;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.packet.InteractEntity;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Bad Packets", type = 'E', complexType = "Interact", description = "Interacted with themselves.")
public class BadPacketsE extends AbstractCheck {

    public BadPacketsE(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isAttackPacket(packetEvent)) {
            InteractEntity lastInteractionPacket = this.playerData.getLastInteractionPacket();
            boolean exempt = this.isExempt(new ExemptType[] { ExemptType.SPECTATOR });
            if (lastInteractionPacket.getEntityId() == this.playerData.getEntityId() && !exempt) {
                this.fail();
            }
        }
    }

    @Override
    public void update() {
    }
}
