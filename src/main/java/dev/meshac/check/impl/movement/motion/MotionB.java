package dev.meshac.check.impl.movement.motion;

import dev.meshac.packet.MeshPacketEvent;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.player.CollisionState;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Motion", type = 'B', complexType = "Inverse", description = "Repeated vertical motions.")
public class MotionB extends AbstractCheck {

    private static final String VIOLATION_DETAIL_PREFIX = "deltaY=";

    public MotionB(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if ((this.isPositionUpdatePacket(packetEvent)) && (!this.isTeleporting()) && (!this.isSetbackPending())) {
            double deltaY;
            double previousDeltaY;
            boolean recentChorusFruit;
            boolean condition2;
            CollisionState collisionState = this.playerData.getCollisionState();
            deltaY = collisionState.getDeltaY();
            previousDeltaY = this.playerData.getCollisionState().getPreviousDeltaY();
            recentChorusFruit = this.playerData.getMotionState().getTicksSinceChorusFruit() < 50;
            condition2 = !((!this.playerData.getCollisionState().isLectern()) && (!this.playerData.getCollisionState().isNearRepeater()));
            boolean condition3 = condition2;
            boolean exempt = this.isExempt(new ExemptType[] { ExemptType.WEB, ExemptType.LILY_PAD, ExemptType.JOINED, ExemptType.ELYTRA, ExemptType.SIGN, ExemptType.ANVIL, ExemptType.SHULKER, ExemptType.SHULKER_BOX, ExemptType.PLACED_WEB, ExemptType.TELEPORT, ExemptType.RIPTIDE, ExemptType.CREATIVE, ExemptType.SPECTATOR, ExemptType.BOAT, ExemptType.WORLD_CHANGE, ExemptType.GLIDING, ExemptType.ENDER_PEARL, ExemptType.LIQUID, ExemptType.CHORUS_FRUIT, ExemptType.TRAPDOOR, ExemptType.CHUNK, ExemptType.PISTON, ExemptType.ILLEGAL_BLOCK, ExemptType.VELOCITY, ExemptType.LENIENT_SCAFFOLDING, ExemptType.CHORUS_FRUIT, ExemptType.FARMLAND, ExemptType.COLLIDING_VERTICALLY, ExemptType.TRAPDOOR, ExemptType.STAIRS, ExemptType.SLAB, ExemptType.SLOW_FALLING, ExemptType.BED });
            boolean condition4 = deltaY > 0.0 && deltaY == -previousDeltaY;
            if (condition4 && !exempt && !recentChorusFruit && !condition3) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail(VIOLATION_DETAIL_PREFIX + deltaY);
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
