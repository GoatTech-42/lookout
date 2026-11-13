package dev.meshac.check.impl.combat.killaura;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.config.MeshVersion;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.CheckUtils;
import dev.meshac.check.ExemptType;
import dev.meshac.player.ClickTimingData;
import dev.meshac.player.CollisionState;
import dev.meshac.player.MotionState;
import dev.meshac.data.PlayerData;
import dev.meshac.tracker.RelativeMoveTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

@CheckInfo(name = "Kill Aura", type = 'L', complexType = "Strafe", experimental = true, description = "Impossible strafe.")
public class KillAuraL extends AbstractCheck {

    public KillAuraL(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        boolean condition;
        boolean condition2;
        if ((this.isPositionUpdatePacket(packetEvent)) && (this.getAttackTicks() < 3)) {
            if (this.playerData.getPlayer() == null) {
                return;
            }
            double deltaXZ;
            double deltaXZChange;
            float maxSpeed;
            long clickInterval;
            boolean sprinting;
            CollisionState collisionState = this.playerData.getCollisionState();
            deltaXZ = collisionState.getDeltaXZ();
            deltaXZChange = this.playerData.getCollisionState().getDeltaXZChange();
            maxSpeed = CheckUtils.getMaxSpeed(this.playerData, 0.275F);
            ClickTimingData clickTimingData = this.playerData.getClickTimingData();
            clickInterval = clickTimingData.getClickInterval();
            MotionState motionState = this.playerData.getMotionState();
            sprinting = motionState.isSprinting();
            condition2 = ((this.playerData.getRelativeMoveTracker()).getLastAttackedEntity() != null) && ((this.playerData.getRelativeMoveTracker()).getLastAttackedEntity() instanceof Player);
            boolean condition3;
            boolean olderThan;
            condition3 = condition2;
            olderThan = this.playerData.getClientVersion().isOlderThan(MeshVersion.V_1_9);
            java.util.OptionalInt ownDamageWindow = CheckUtils.maximumDamageWindowTicks(this.playerData.getPlayer());
            java.util.OptionalInt targetDamageWindow = condition3 ? CheckUtils.maximumDamageWindowTicks((Player) this.playerData.getRelativeMoveTracker().getLastAttackedEntity()) : java.util.OptionalInt.empty();
            if (ownDamageWindow.isEmpty() || (condition3 && targetDamageWindow.isEmpty())) return;
            if (ownDamageWindow.getAsInt() >= 18) {
                if (condition3) {
                    RelativeMoveTracker relativeMoveTracker3 = this.playerData.getRelativeMoveTracker();
                    if (targetDamageWindow.getAsInt() < 18) {
                        condition = true;
                    } else {
                        condition = false;
                    }
                } else {
                    condition = false;
                }
            } else {
                condition = true;
            }
            boolean condition4 = condition;
            boolean exempt = this.isExempt(new ExemptType[] { ExemptType.SOUL_SAND });
            boolean condition5 = deltaXZChange < 0.0025 && !sprinting && deltaXZ > maxSpeed && clickInterval < 500L && condition3 && olderThan;
            if (condition5 && !exempt && !condition4) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail("d=" + deltaXZ + " a=" + deltaXZChange + " s=" + clickInterval);
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
