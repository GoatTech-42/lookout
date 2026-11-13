package dev.meshac.check.impl.player.improbable;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.config.MeshSettings;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Improbable", type = 'D', complexType = "Auto Clicker", description = "Too many combined autoclicker violations.")
public class ImprobableD extends AbstractCheck {

    private long lastViolationAtMillis;

    private static final String VIOLATION_DETAIL_PREFIX = "autoClickerViolations=";

    public ImprobableD(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if ((this.isArmAnimationPacket(packetEvent)) && (!this.isExempt(new ExemptType[] { ExemptType.AUTOCLICKER }))) {
            int autoClickerViolations = this.playerData.getAutoClickerViolations();
            if (System.currentTimeMillis() - this.lastViolationAtMillis >= 60000L && autoClickerViolations > MeshSettings.checksPlayerImprobableDMaxAutoclickerViolations) {
                this.fail(VIOLATION_DETAIL_PREFIX + autoClickerViolations);
                this.lastViolationAtMillis = System.currentTimeMillis();
            }
        }
    }

    @Override
    public void update() {
    }
}
