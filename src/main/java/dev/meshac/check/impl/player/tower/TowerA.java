package dev.meshac.check.impl.player.tower;

import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.player.CollisionState;
import dev.meshac.data.PlayerData;
import dev.meshac.world.BlockUtils;
import net.minecraft.core.Direction;
import dev.meshac.event.MeshEvent;
import dev.meshac.event.MeshBlockPlaceEvent;

@CheckInfo(name = "Tower", type = 'A', complexType = "Limit", experimental = false, description = "Towering too quickly.")
public class TowerA extends AbstractCheck {

    private long lastPlacementAtMillis;

    public TowerA(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public void update() {
    }

    public void handleEvent(MeshEvent event2) {
        if (event2 instanceof MeshBlockPlaceEvent) {
            if (this.playerData.getPlayer() == null) {
                return;
            }
            MeshBlockPlaceEvent blockPlaceEvent = (MeshBlockPlaceEvent) event2;
            if (blockPlaceEvent.isCancelled() || !blockPlaceEvent.getBlock().getType().isSolid()) {
                return;
            }
            long delay = System.currentTimeMillis() - this.lastPlacementAtMillis;
            CollisionState collisionState = this.playerData.getCollisionState();
            double deltaXZ = collisionState.getDeltaXZ();
            CollisionState collisionState2 = this.playerData.getCollisionState();
            double deltaY = collisionState2.getDeltaY();
            boolean condition = new dev.meshac.util.Location(this.playerData.getPlayer().level(), this.playerData.getPlayer().getX(), this.playerData.getPlayer().getY(), this.playerData.getPlayer().getZ()).getBlockX() == blockPlaceEvent.getBlock().getLocation().getBlockX();
            boolean condition2 = new dev.meshac.util.Location(this.playerData.getPlayer().level(), this.playerData.getPlayer().getX(), this.playerData.getPlayer().getY(), this.playerData.getPlayer().getZ()).getBlockZ() == blockPlaceEvent.getBlock().getLocation().getBlockZ();
            Direction blockFace = blockPlaceEvent.getBlockAgainst().getFace(blockPlaceEvent.getBlock());
            boolean condition3 = BlockUtils.isScaffolding(blockPlaceEvent.getBlock().getType()) || BlockUtils.isTripwire(blockPlaceEvent.getBlock().getType()) || BlockUtils.isTurtleEgg(blockPlaceEvent.getBlock().getType()) || BlockUtils.isClimbable(blockPlaceEvent.getBlock().getType());
            boolean exempt = this.isExempt(new ExemptType[] { ExemptType.CREATIVE, ExemptType.FLIGHT, ExemptType.JUMP_BOOST, ExemptType.CANCELLED_PLACE, ExemptType.LIQUID, ExemptType.TURTLE_EGG, ExemptType.CLIMBABLE, ExemptType.EXPLOSION, ExemptType.ELYTRA, ExemptType.PACKET_EXPLOSION, ExemptType.SLOW_FALLING });
            boolean condition4 = blockFace == Direction.UP && condition && condition2 && delay < 250L && deltaY > 0.0 && deltaXZ < 0.1;
            if (condition4 && !exempt && !condition3) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail("delay=" + delay + " deltaY=" + deltaY);
                }
            } else {
                this.decayBuffer();
            }
            this.lastPlacementAtMillis = System.currentTimeMillis();
        }
    }
}
