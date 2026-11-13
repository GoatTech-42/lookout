package dev.meshac.check.impl.player.improbable;

import dev.meshac.packet.MeshPacketEvent;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.config.MeshSettings;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Improbable", type = 'B', complexType = "Movement", experimental = false, description = "Too many combined movement violations.")
public class ImprobableB extends AbstractCheck {

    private long lastViolationAtMillis;

    private static final String VIOLATION_DETAIL_PREFIX = "movementViolations=";

    public ImprobableB(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isPositionUpdatePacket(packetEvent)) {
            int movementViolations = this.playerData.getMovementViolations();
            if (System.currentTimeMillis() - this.lastViolationAtMillis >= 60000L && movementViolations > MeshSettings.checksPlayerImprobableBMaxMovementViolations) {
                this.fail(VIOLATION_DETAIL_PREFIX + movementViolations);
                this.lastViolationAtMillis = System.currentTimeMillis();
            }
        }
    }

    @Override
    public void update() {
    }
}
