package dev.meshac.check.impl.combat.autoblock;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.packet.InteractEntity.Action;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Auto Block", type = 'A', complexType = "Sequence", description = "Attacked while sending BlockPlace packet.")
public class AutoBlockA extends AbstractCheck {

    public AutoBlockA(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isAttackPacket(packetEvent)) {
            if (this.playerData.getLastInteractionPacket().getAction() == InteractEntity.Action.ATTACK) {
                boolean itemUseReceived = this.playerData.getMotionState().isItemUseReceived();
                boolean exempt = this.isExempt(new ExemptType[] { ExemptType.CLIENT_VERSION });
                if (itemUseReceived && !exempt) {
                    this.fail();
                }
            }
        }
    }

    @Override
    public void update() {
    }
}
