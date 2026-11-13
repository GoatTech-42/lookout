package dev.meshac.check.impl.combat.hitbox;

import dev.meshac.packet.InteractEntity;
import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.check.ExemptionManager;
import dev.meshac.config.MeshSettings;
import dev.meshac.MeshServices;
import dev.meshac.data.PlayerData;
import dev.meshac.data.PlayerDataManager;
import dev.meshac.tracker.RotationTracker;
import dev.meshac.collection.tuple.Pair;
import dev.meshac.util.MathUtils;
import dev.meshac.util.Location;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import dev.meshac.util.Vector;

@CheckInfo(name = "Hitbox", type = 'A', complexType = "History", description = "Attacked while not looking at target.")
public class HitboxA extends AbstractCheck {

    private boolean pendingAttack;

    public HitboxA(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent event) {
        if (this.isAttackPacket(event)) {
            if (this.playerData.getLastInteractionPacket().getAction() != InteractEntity.Action.ATTACK || !(this.playerData.getRelativeMoveTracker().getLastAttackedEntity() instanceof Player)) {
                return;
            }
            this.pendingAttack = true;
        } else if (this.isMovementPacket(event)) {
            if (!this.pendingAttack) {
                return;
            }
            int currentTick = dev.meshac.MeshServices.ticks();
            int latencyTicks = MathUtils.pingCompensationTicks(this.playerData);
            double x = this.playerData.getCollisionState().getX();
            double z = this.playerData.getCollisionState().getZ();
            Vector origin = new Vector(x, 0.0, z);
            double minAngle = this.playerData.getRelativeMoveTracker().getTargetPositions().stream().filter(entry -> HitboxA.isRecentTargetPosition(currentTick, latencyTicks, entry)).mapToDouble(entry -> this.calculateAimDistance(origin, entry)).min().orElse(-1.0);
            double distance = this.playerData.getRelativeMoveTracker().getTargetDistance();
            if (minAngle == -1.0) {
                this.decreaseBuffer(0.025);
                this.pendingAttack = false;
                return;
            }
            Player target = this.playerData.getRelativeMoveTracker().getPlayer();
            PlayerData targetData = PlayerDataManager.get(target);
            if (targetData == null) {
                return;
            }
            boolean vehicleExempt = targetData.getPlayerToggles().isAnyExempt(new ExemptType[] { ExemptType.VEHICLE, ExemptType.BOAT });
            double maxAngle = MeshSettings.checksCombatHitboxAMaxAngle;
            if (System.currentTimeMillis() - this.playerData.getConnectionTracker().getLastFastMovementAtMillis() < 1500L) {
                maxAngle += 0.25;
            }
            boolean exempt = this.isExempt(new ExemptType[] { ExemptType.CREATIVE, ExemptType.GLIDING, ExemptType.VEHICLE });
            double deltaXZ = this.playerData.getCollisionState().getDeltaXZ();
            float deltaYaw = this.playerData.getRotationTracker().getDeltaYaw();
            boolean outsideNearDistance = distance > (ExemptionManager.isAtLeast1_9() ? 0.25 : 0.75);
            if (minAngle > maxAngle && outsideNearDistance && distance < 10.0 && !exempt && !vehicleExempt) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail("angle=" + minAngle + " distance=" + distance + " dYaw=" + deltaYaw + " dXZ=" + deltaXZ);
                }
            } else {
                this.decayBuffer();
            }
            this.pendingAttack = false;
        }
    }

    @Override
    public void update() {
    }

    private Vector getPosition(float yaw, float pitch) {
        Vector direction = new Vector();
        double yawRadians = yaw;
        double pitchRadians = pitch;
        direction.setY(-Math.sin(Math.toRadians(pitchRadians)));
        double horizontal = Math.cos(Math.toRadians(pitchRadians));
        direction.setX(-horizontal * Math.sin(Math.toRadians(yawRadians)));
        direction.setZ(horizontal * Math.cos(Math.toRadians(yawRadians)));
        return direction;
    }

    private double calculateAimDistance(Vector origin, dev.meshac.tracker.RelativeMoveTracker.TargetPos entry) {
        Vector offset = new Vector(entry.x(), entry.y(), entry.z()).setY(0.0);
        Vector toTarget = offset.clone().subtract(origin);
        RotationTracker rotation = this.playerData.getRotationTracker();
        Vector look = this.getPosition(rotation.getYaw(), rotation.getPitch()).setY(0.0);
        return toTarget.angle(look);
    }

    private static boolean isRecentTargetPosition(int currentTick, int latencyTicks, dev.meshac.tracker.RelativeMoveTracker.TargetPos entry) {
        return Math.abs(currentTick - entry.tick() - latencyTicks) < 3;
    }
}
