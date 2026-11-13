package dev.meshac.check.impl.movement.noslow;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.packet.Digging.Action;
import dev.meshac.packet.Digging;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "No Slow", type = 'A', complexType = "Packet", description = "Placing while digging")
public class NoSlowA extends AbstractCheck {

    public NoSlowA(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isDiggingPacket(packetEvent)) {
            Digging lastDiggingPacket = this.playerData.getLastDiggingPacket();
            boolean itemUseReceived = this.playerData.getMotionState().isItemUseReceived();
            boolean exempt = this.isExempt(new ExemptType[] { ExemptType.CLIENT_VERSION, ExemptType.SERVER_VERSION });
            boolean condition = lastDiggingPacket.getAction() == Digging.Action.RELEASE_USE_ITEM;
            if (itemUseReceived && condition && !exempt) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail();
                }
            } else {
                this.decayBuffer();
            }
        }
    }

    @Override
    public void update() {
    }
}
