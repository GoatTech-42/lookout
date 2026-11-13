package dev.meshac.check.impl.player.improbable;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.config.MeshSettings;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Improbable", type = 'E', complexType = "Total", description = "Too many combined violations.")
public class ImprobableE extends AbstractCheck {

    private long lastViolationAtMillis;

    private static final String VIOLATION_DETAIL_PREFIX = "totalViolations=";

    public ImprobableE(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isMovementPacket(packetEvent)) {
            int totalViolations = this.playerData.getTotalViolations();
            if (System.currentTimeMillis() - this.lastViolationAtMillis >= 60000L && totalViolations > MeshSettings.checksPlayerImprobableEMaxTotalViolations) {
                this.fail(VIOLATION_DETAIL_PREFIX + totalViolations);
                this.lastViolationAtMillis = System.currentTimeMillis();
            }
        }
    }

    @Override
    public void update() {
    }
}
