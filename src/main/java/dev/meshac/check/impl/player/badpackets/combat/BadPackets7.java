package dev.meshac.check.impl.player.badpackets.combat;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.config.MeshVersion;
import dev.meshac.packet.InteractEntity;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.check.ExemptionManager;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Bad Packets", type = '7', complexType = "No Swing", description = "Hitting without swinging.")
public class BadPackets7 extends AbstractCheck {

    private boolean swingAvailable;

    private boolean swung;

    private static final String VIOLATION_DETAIL_PREFIX = "swung=";

    public BadPackets7(PlayerData playerData) {
        super(playerData);
        this.swingAvailable = this.playerData.getClientVersion().isNewerThan(MeshVersion.V_1_8);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if ((this.playerData.getClientVersion().isOlderThan(MeshVersion.V_1_9)) && (ExemptionManager.isAtLeast1_9())) {
            return;
        }
        if (!this.isExempt(new ExemptType[] { ExemptType.SPECTATOR })) {
            if (this.isArmAnimationPacket(packetEvent)) {
                this.swingAvailable = this.swung = true;
            }
            if (this.isAttackPacket(packetEvent)) {
                InteractEntity lastInteractionPacket = this.playerData.getLastInteractionPacket();
                if (lastInteractionPacket.getAction() == InteractEntity.Action.ATTACK) {
                    if (this.playerData.isNewerThan1_9() ? !this.swingAvailable : !this.swung) {
                        this.fail(VIOLATION_DETAIL_PREFIX + this.swung);
                    }
                }
                this.swingAvailable = this.swung;
            }
        }
    }

    @Override
    public void update() {
    }
}
