package dev.meshac.check.impl.combat.aim.pattern;

import dev.meshac.packet.MeshPacketEvent;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.data.PlayerData;
import dev.meshac.tracker.RotationTracker;

@CheckInfo(name = "Aim", type = 'X', complexType = "Analysis", description = "Generic rotation analysis heuristic.")
public class AimX extends AbstractCheck {

    public AimX(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if ((this.isRotationUpdatePacket(packetEvent)) && (this.getAttackTicks() < 3)) {
            RotationTracker rotationTracker = this.playerData.getRotationTracker();
            float deltaYaw = rotationTracker.getDeltaYaw();
            RotationTracker rotationTracker2 = this.playerData.getRotationTracker();
            float deltaPitch = rotationTracker2.getDeltaPitch();
            RotationTracker rotationTracker3 = this.playerData.getRotationTracker();
            int sensitivity = rotationTracker3.getSensitivity();
            boolean exempt = this.isExempt(new ExemptType[] { ExemptType.CINEMATIC, ExemptType.SPECTATOR });
            boolean condition = sensitivity > 205 && deltaYaw > 1.25F && deltaPitch > 1.25F;
            if (condition && !exempt) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail("sens=" + sensitivity + " deltaYaw=" + deltaYaw + " deltaPitch=" + deltaPitch);
                }
            } else {
                this.decayBuffer();
            }
        }
    }

    @Override
    public void update() {
    }
}
