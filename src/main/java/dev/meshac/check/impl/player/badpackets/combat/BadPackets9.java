package dev.meshac.check.impl.player.badpackets.combat;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.config.MeshVersion;
import dev.meshac.packet.InteractEntity;
import dev.meshac.packet.InteractEntity.Action;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Bad Packets", type = '9', complexType = "No Swing", description = "Hitting without swinging.")
public class BadPackets9 extends AbstractCheck {

    private boolean swung = false;

    private boolean attacked = false;

    public BadPackets9(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if ((!this.playerData.getClientVersion().isOlderThan(MeshVersion.V_1_21_2)) && (!this.isExempt(new ExemptType[] { ExemptType.SPECTATOR }))) {
            if (this.isArmAnimationPacket(packetEvent)) {
                this.swung = true;
            } else if (this.isAttackPacket(packetEvent)) {
                InteractEntity lastInteractionPacket = this.playerData.getLastInteractionPacket();
                if (lastInteractionPacket.getAction() != InteractEntity.Action.ATTACK) {
                    return;
                }
                this.attacked = true;
            } else if (this.isClientTickEndPacket(packetEvent)) {
                if (this.attacked && !this.swung) {
                    this.fail("attacked=" + this.attacked + " swung=" + this.swung);
                }
                this.attacked = false;
                this.swung = false;
            }
            return;
        }
    }

    @Override
    public void update() {
    }
}
