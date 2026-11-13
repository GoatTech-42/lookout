package dev.meshac.check.impl.combat.killaura;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.packet.InteractEntity;
import dev.meshac.packet.InteractEntity.Action;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.player.ClickTimingData;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Kill Aura", type = 'J', complexType = "Frequency", experimental = false, description = "Attack frequency.")
public class KillAuraJ extends AbstractCheck {

    private int movementsSinceAttack;

    private int previousAttackMovements;

    private int attackSamples;

    private int identicalAttackSamples;

    private static final String VIOLATION_DETAIL_PREFIX = "invalid=";

    public KillAuraJ(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isAttackPacket(packetEvent)) {
            InteractEntity lastInteractionPacket = this.playerData.getLastInteractionPacket();
            if (lastInteractionPacket.getAction() == InteractEntity.Action.ATTACK) {
                ClickTimingData clickTimingData = this.playerData.getClickTimingData();
                boolean condition = clickTimingData.getClicksPerSecond() > 7.2 && this.movementsSinceAttack < 4 && this.previousAttackMovements < 4;
                if (condition) {
                    boolean condition2 = this.movementsSinceAttack == this.previousAttackMovements;
                    if (condition2) {
                        this.identicalAttackSamples++;
                    }
                    if (++this.attackSamples == 30) {
                        if (this.identicalAttackSamples > 28) {
                            if (this.increaseBuffer() > this.maximumBuffer) {
                                this.fail(VIOLATION_DETAIL_PREFIX + this.identicalAttackSamples);
                            }
                        } else {
                            this.decayBuffer();
                        }
                        this.identicalAttackSamples = 0;
                        this.attackSamples = 0;
                    }
                }
                this.previousAttackMovements = this.movementsSinceAttack;
                this.movementsSinceAttack = 0;
            }
        } else if (this.isMovementPacket(packetEvent)) {
            this.movementsSinceAttack++;
        }
    }

    @Override
    public void update() {
    }
}
