package dev.meshac.check.impl.player.badpackets.blocks;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.packet.Digging.Action;
import dev.meshac.packet.Digging;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Bad Packets", type = '6', complexType = "Nuker", description = "Invalid Block Dig packets.")
public class BadPackets6 extends AbstractCheck {

    private int digPackets = 0;

    private static final String VIOLATION_DETAIL_PREFIX = "digs=";

    public BadPackets6(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isDiggingPacket(packetEvent)) {
            Digging lastDiggingPacket = this.playerData.getLastDiggingPacket();
            if (lastDiggingPacket.getAction() == Digging.Action.START_DIGGING) {
                if (this.digPackets > 3) {
                    if (this.increaseBuffer() > this.maximumBuffer) {
                        this.fail(VIOLATION_DETAIL_PREFIX + this.digPackets);
                    }
                } else {
                    this.decayBuffer();
                }
            }
        } else if (this.isArmAnimationPacket(packetEvent)) {
            this.digPackets = 0;
        }
    }

    @Override
    public void update() {
    }
}
