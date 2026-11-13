package dev.meshac.check.impl.player.improbable;

import dev.meshac.packet.MeshPacketEvent;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.config.MeshSettings;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Improbable", type = 'C', complexType = "Player", description = "Too many combined player violations.")
public class ImprobableC extends AbstractCheck {

    private long lastViolationAtMillis;

    private static final String VIOLATION_DETAIL_PREFIX = "playerViolations=";

    public ImprobableC(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isPositionUpdatePacket(packetEvent)) {
            int playerViolations = this.playerData.getPlayerViolations();
            if (System.currentTimeMillis() - this.lastViolationAtMillis >= 60000L && playerViolations > MeshSettings.checksPlayerImprobableCMaxPlayerViolations) {
                this.fail(VIOLATION_DETAIL_PREFIX + playerViolations);
                this.lastViolationAtMillis = System.currentTimeMillis();
            }
        }
    }

    @Override
    public void update() {
    }
}
