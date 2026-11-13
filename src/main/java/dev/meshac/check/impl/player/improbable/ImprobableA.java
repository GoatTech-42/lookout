package dev.meshac.check.impl.player.improbable;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.packet.InteractEntity;
import dev.meshac.packet.InteractEntity.Action;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.config.MeshSettings;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Improbable", type = 'A', complexType = "Combat", description = "Too many combined combat violations.")
public class ImprobableA extends AbstractCheck {

    private long lastViolationAtMillis;

    private static final String VIOLATION_DETAIL_PREFIX = "combatViolations=";

    public ImprobableA(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isAttackPacket(packetEvent)) {
            InteractEntity lastInteractionPacket = this.playerData.getLastInteractionPacket();
            if (lastInteractionPacket.getAction() == InteractEntity.Action.ATTACK && System.currentTimeMillis() - this.lastViolationAtMillis >= 60000L) {
                int combatViolations = this.playerData.getCombatViolations();
                if (combatViolations > MeshSettings.checksPlayerImprobableAMaxCombatViolations) {
                    this.fail(VIOLATION_DETAIL_PREFIX + combatViolations);
                    this.lastViolationAtMillis = System.currentTimeMillis();
                }
            }
        }
    }

    @Override
    public void update() {
    }
}
