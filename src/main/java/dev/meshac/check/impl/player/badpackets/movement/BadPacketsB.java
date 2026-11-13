package dev.meshac.check.impl.player.badpackets.movement;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.packet.Flying;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Bad Packets", type = 'B', complexType = "Sequence", description = "More than 20 flying packets in a row.")
public class BadPacketsB extends AbstractCheck {

    private int stationaryMovementStreak;

    private static final String VIOLATION_DETAIL_PREFIX = "streak=";

    public BadPacketsB(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isMovementPacket(packetEvent)) {
            Flying lastFlyingPacket = this.playerData.getLastFlyingPacket();
            if (!(lastFlyingPacket.hasPositionChanged())) {
                if (!((this.playerData.getPlayer() != null) && (this.playerData.getPlayer().isPassenger()))) {
                    boolean exempt = this.isExempt(new ExemptType[] { ExemptType.SERVER_VERSION, ExemptType.CLIENT_VERSION });
                    if (++this.stationaryMovementStreak > 20 && !exempt) {
                        this.fail(VIOLATION_DETAIL_PREFIX + this.stationaryMovementStreak);
                    }
                    return;
                }
            }
        } else {
            if (this.isSteerVehiclePacket(packetEvent)) {
                this.stationaryMovementStreak = 0;
            }
            return;
        }
        this.stationaryMovementStreak = 0;
    }

    @Override
    public void update() {
    }
}
