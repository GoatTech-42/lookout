package dev.meshac.check.impl.combat.criticals;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.player.CollisionState;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Criticals", type = 'B', complexType = "Modulo", description = "Tried to hit Critical hit on ground.")
public class CriticalsB extends AbstractCheck {

    private static final String VIOLATION_DETAIL_PREFIX;

    public CriticalsB(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if ((this.isAttackPacket(packetEvent)) && (this.getAttackTicks() < 2)) {
            CollisionState collisionState = this.playerData.getCollisionState();
            double yRemainder = collisionState.getY() % 0.015625;
            boolean condition = yRemainder < 1.0E-5 && yRemainder > 0.0;
            boolean exempt = this.isExempt(new ExemptType[] { ExemptType.LIQUID, ExemptType.TRAPDOOR, ExemptType.COLLIDING_VERTICALLY, ExemptType.BOAT, ExemptType.TELEPORT, ExemptType.WEB, ExemptType.ENDER_PEARL, ExemptType.CHORUS_FRUIT, ExemptType.SERVER_POSITION, ExemptType.VEHICLE, ExemptType.FLIGHT, ExemptType.SLAB, ExemptType.STAIRS });
            if (condition && !exempt) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail(VIOLATION_DETAIL_PREFIX + yRemainder);
                }
            } else {
                this.decayBuffer();
            }
        }
    }

    @Override
    public void update() {
    }

    static {
        VIOLATION_DETAIL_PREFIX = "modulo=";
    }
}
