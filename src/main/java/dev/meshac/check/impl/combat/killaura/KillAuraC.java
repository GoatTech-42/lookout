package dev.meshac.check.impl.combat.killaura;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.player.CollisionState;
import dev.meshac.data.PlayerData;
import dev.meshac.tracker.RelativeMoveTracker;
import dev.meshac.tracker.RotationTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

@CheckInfo(name = "Kill Aura", type = 'C', complexType = "Head Snap", experimental = false, description = "Invalid acceleration.")
public class KillAuraC extends AbstractCheck {

    public KillAuraC(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if ((this.isPositionUpdatePacket(packetEvent)) && (this.getAttackTicks() < 3)) {
            double deltaXZChange = this.playerData.getCollisionState().getDeltaXZChange();
            CollisionState collisionState = this.playerData.getCollisionState();
            double deltaXZ = collisionState.getDeltaXZ();
            RotationTracker rotationTracker = this.playerData.getRotationTracker();
            float deltaYaw = rotationTracker.getDeltaYaw();
            RotationTracker rotationTracker2 = this.playerData.getRotationTracker();
            float deltaPitch = rotationTracker2.getDeltaPitch();
            RelativeMoveTracker relativeMoveTracker = this.playerData.getRelativeMoveTracker();
            boolean condition = relativeMoveTracker.getLastAttackedEntity() instanceof Player;
            boolean condition2 = deltaXZChange < 0.001 && deltaYaw > 10.0F && deltaPitch > 26.5 && condition && deltaXZ > 0.0;
            boolean exempt = this.isExempt(new ExemptType[] { ExemptType.SPECTATOR });
            if (condition2 && !exempt) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail("accel=" + deltaXZChange + " deltaYaw=" + deltaYaw);
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
