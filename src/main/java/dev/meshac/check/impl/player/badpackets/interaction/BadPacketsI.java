package dev.meshac.check.impl.player.badpackets.interaction;

import dev.meshac.packet.MeshPacketEvent;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Bad Packets", type = 'I', complexType = "Entity Action", description = "Sent EntityAction while attacking.")
public class BadPacketsI extends AbstractCheck {

    public BadPacketsI(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isAttackPacket(packetEvent)) {
            boolean entityActionReceived = this.playerData.getMotionState().isEntityActionReceived();
            boolean exempt = this.isExempt(new ExemptType[] { ExemptType.SERVER_VERSION, ExemptType.CLIENT_VERSION, ExemptType.FAST, ExemptType.SPECTATOR, ExemptType.DEATH });
            if (entityActionReceived && !exempt) {
                this.fail();
            }
        }
    }

    @Override
    public void update() {
    }
}
