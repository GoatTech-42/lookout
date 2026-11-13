package dev.meshac.check.impl.player.invalid;

import dev.meshac.packet.MeshPacketEvent;
import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.config.MeshSettings;
import dev.meshac.player.CollisionState;
import dev.meshac.player.MotionState;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Invalid", type = 'J', complexType = "Motion", description = "Moving too quickly.", experimental = true)
public class InvalidJ extends AbstractCheck {

    public InvalidJ(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent event) {
        if (!isPositionUpdatePacket(event))
            return;
        if (isExempt(new ExemptType[] { ExemptType.FLIGHT, ExemptType.JOINED, ExemptType.CREATIVE, ExemptType.SHULKER, ExemptType.SHULKER_BOX, ExemptType.BAMBOO, ExemptType.CAKE, ExemptType.PRESSURE_PLATE, ExemptType.CHUNK, ExemptType.DEPTH_STRIDER, ExemptType.DOLPHINS_GRACE, ExemptType.TELEPORT, ExemptType.ILLEGAL_BLOCK, ExemptType.GLIDING, ExemptType.DOOR, ExemptType.RIPTIDE, ExemptType.VEHICLE, ExemptType.BOAT, ExemptType.FISHING_ROD, ExemptType.ELYTRA, ExemptType.SIGN, ExemptType.DEATH, ExemptType.SLEEPING, ExemptType.ENDER_PEARL, ExemptType.FROZEN, ExemptType.CHORUS_FRUIT, ExemptType.SPECTATOR, ExemptType.WORLD_CHANGE, ExemptType.ANVIL, ExemptType.CANCELLED_MOVE, ExemptType.GLASS_PANE, ExemptType.FENCE, ExemptType.BED }))
            return;
        CollisionState collision = playerData.getCollisionState();
        MotionState motion = playerData.getMotionState();
        double speed = collision.getDeltaXZ();
        int groundTicks = collision.getGroundTicks();
        double maximum = 0.2975 * (motion.getMovementSpeed() / 0.1);
        if (groundTicks != 0)
            maximum += 0.5;
        if (playerData.getVelocityTracker().getTicksSinceVelocity() < 30)
            maximum += motion.getLastSpeedAmplifier() * 0.075;
        double difference = speed - maximum;
        if (difference > MeshSettings.checksMovementSpeedBMinDifference || difference == 0.5)
            fail("speed=" + speed + " max=" + maximum + " diff=" + difference + " ticks=" + groundTicks + " deltaY=" + collision.getDeltaY());
        else
            decayBuffer();
    }

    @Override
    public void update() {
    }
}
