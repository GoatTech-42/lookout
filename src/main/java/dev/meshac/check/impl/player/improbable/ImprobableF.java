package dev.meshac.check.impl.player.improbable;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.config.MeshSettings;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Improbable", type = 'F', complexType = "Scaffold", description = "Too many combined Scaffold violations.")
public class ImprobableF extends AbstractCheck {

    private long lastViolationAtMillis;

    private static final String VIOLATION_DETAIL_PREFIX;

    public ImprobableF(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isPositionUpdatePacket(packetEvent)) {
            int scaffoldViolations = this.playerData.getScaffoldViolations();
            if (System.currentTimeMillis() - this.lastViolationAtMillis >= 60000L && scaffoldViolations > MeshSettings.checksPlayerImprobableFMaxScaffoldViolations) {
                this.fail(VIOLATION_DETAIL_PREFIX + scaffoldViolations);
                this.lastViolationAtMillis = System.currentTimeMillis();
            }
        }
    }

    @Override
    public void update() {
    }

    static {
        VIOLATION_DETAIL_PREFIX = "scaffoldViolations=";
    }
}
