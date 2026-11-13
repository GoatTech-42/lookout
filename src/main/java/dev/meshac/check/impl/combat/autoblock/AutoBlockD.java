package dev.meshac.check.impl.combat.autoblock;

import dev.meshac.packet.MeshPacketEvent;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.data.PlayerData;
import dev.meshac.check.ExemptType;
import dev.meshac.packet.InteractEntity;
import dev.meshac.packet.InteractEntity.Action;

@CheckInfo(name = "Auto Block", type = 'D', complexType = "Order", description = "Invalid attack order.")
public class AutoBlockD extends AbstractCheck {

    private boolean interacted;

    private boolean attacked;

    public AutoBlockD(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isMovementPacket(packetEvent)) {
            this.attacked = false;
            this.interacted = false;
        } else if (this.isAttackPacket(packetEvent)) {
            InteractEntity interaction = this.playerData.getLastInteractionPacket();
            if (interaction.getAction() == InteractEntity.Action.ATTACK) {
                this.attacked = true;
            }
            if (interaction.getAction() == InteractEntity.Action.INTERACT || interaction.getAction() == InteractEntity.Action.INTERACT_AT) {
                this.interacted = true;
            }
        } else if (this.isBlockPlacementPacket(packetEvent)) {
            boolean exempt = this.isExempt(new ExemptType[] { ExemptType.CLIENT_VERSION });
            if (!exempt && this.attacked && !this.interacted) {
                this.fail("a=" + this.attacked + " i=" + this.interacted);
            }
        }
    }

    @Override
    public void update() {
    }
}
