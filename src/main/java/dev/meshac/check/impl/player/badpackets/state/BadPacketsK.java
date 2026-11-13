package dev.meshac.check.impl.player.badpackets.state;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.packet.EntityAction;
import dev.meshac.packet.EntityAction.Action;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Bad Packets", type = 'K', complexType = "Sneak", description = "Sending EntityAction packets too quickly")
public class BadPacketsK extends AbstractCheck {

    private long lastSneakAtMillis = -1L;

    private static final String VIOLATION_DETAIL_PREFIX = "delay=";

    public BadPacketsK(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isEntityActionPacket(packetEvent)) {
            EntityAction wrapperPlayClientEntityAction = this.playerData.getLastEntityActionPacket();
            if (wrapperPlayClientEntityAction.getAction() == Action.START_SNEAKING) {
                long timestamp2 = System.currentTimeMillis();
                long timestamp2Difference = timestamp2 - this.lastSneakAtMillis;
                boolean exempt = this.isExempt(new ExemptType[] { ExemptType.FAST });
                if (timestamp2Difference < 90L && !exempt) {
                    if (this.increaseBuffer() > this.maximumBuffer) {
                        this.fail(VIOLATION_DETAIL_PREFIX + timestamp2Difference);
                    }
                } else {
                    this.decayBuffer();
                }
                this.lastSneakAtMillis = timestamp2;
            }
        }
    }

    @Override
    public void update() {
    }
}
