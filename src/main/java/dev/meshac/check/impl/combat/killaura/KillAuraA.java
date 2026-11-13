package dev.meshac.check.impl.combat.killaura;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.packet.InteractEntity;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Kill Aura", type = 'A', complexType = "Post", description = "Post UseEntity packets.")
public class KillAuraA extends AbstractCheck {

    private long lastMovementAtMillis;

    private boolean attackAfterMovement;

    public KillAuraA(PlayerData playerData) {
        super(playerData);
        this.lastMovementAtMillis = 0L;
        this.attackAfterMovement = false;
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isMovementPacket(packetEvent)) {
            long delay = System.currentTimeMillis() - this.lastMovementAtMillis;
            if (this.attackAfterMovement) {
                boolean exempt = this.isExempt(new ExemptType[] { ExemptType.SPECTATOR });
                if (delay > 40L && delay < 100L && !exempt) {
                    if (this.increaseBuffer() > this.maximumBuffer) {
                        this.fail("delay=" + delay);
                    }
                } else {
                    this.decayBuffer();
                }
                this.attackAfterMovement = false;
            }
            this.lastMovementAtMillis = System.currentTimeMillis();
        } else if (this.isAttackPacket(packetEvent)) {
            InteractEntity lastInteractionPacket = this.playerData.getLastInteractionPacket();
            if (lastInteractionPacket.getAction() != InteractEntity.Action.ATTACK) {
                return;
            }
            long currentTimeMillisDifference = System.currentTimeMillis() - this.lastMovementAtMillis;
            if (currentTimeMillisDifference < 5L) {
                this.attackAfterMovement = true;
            }
        }
    }

    @Override
    public void update() {
    }
}
