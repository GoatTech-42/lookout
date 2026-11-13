package dev.meshac.check.impl.player.badpackets.sequence;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.packet.InteractEntity;
import dev.meshac.packet.InteractEntity.Action;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Bad Packets", type = 'H', complexType = "Packet Order", description = "Invalid attack packet order.")
public class BadPacketsH extends AbstractCheck {

    private boolean swung;

    private static final String VIOLATION_DETAIL_PREFIX = "swung=";

    public BadPacketsH(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isAttackPacket(packetEvent)) {
            InteractEntity lastInteractionPacket = this.playerData.getLastInteractionPacket();
            boolean exempt = this.isExempt(new ExemptType[] { ExemptType.SERVER_VERSION, ExemptType.CLIENT_VERSION, ExemptType.FAST });
            if (lastInteractionPacket.getAction() == InteractEntity.Action.ATTACK && !exempt && !this.swung) {
                this.fail(VIOLATION_DETAIL_PREFIX + this.swung);
            }
        } else if (this.isArmAnimationPacket(packetEvent)) {
            this.swung = true;
        } else if (this.isMovementPacket(packetEvent)) {
            this.swung = false;
        }
    }

    @Override
    public void update() {
    }
}
