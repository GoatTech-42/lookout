package dev.meshac.check.impl.combat.autoblock;

import dev.meshac.packet.InteractEntity;
import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Auto Block", type = 'B', complexType = "Packet", description = "Attacked while sending BlockDig packet.")
public class AutoBlockB extends AbstractCheck {

    public AutoBlockB(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isAttackPacket(packetEvent)) {
            if (this.playerData.getLastInteractionPacket().getAction() == InteractEntity.Action.ATTACK) {
                boolean releaseUseItemReceived = this.playerData.getMotionState().isReleaseUseItemReceived();
                boolean exempt = this.isExempt(new ExemptType[] { ExemptType.CLIENT_VERSION });
                if (releaseUseItemReceived && !exempt) {
                    this.fail();
                }
            }
        }
    }

    @Override
    public void update() {
    }
}
