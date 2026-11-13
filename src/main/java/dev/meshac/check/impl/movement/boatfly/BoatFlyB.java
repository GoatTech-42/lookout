package dev.meshac.check.impl.movement.boatfly;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.check.ExemptionManager;
import dev.meshac.config.MeshSettings;
import dev.meshac.MeshServices;
import dev.meshac.MeshServices;
import dev.meshac.player.CollisionState;
import dev.meshac.data.PlayerData;
import net.minecraft.world.entity.vehicle.boat.Boat;
import dev.meshac.event.MeshEvent;
import dev.meshac.event.MeshMoveEvent;

@CheckInfo(name = "Boat Fly", type = 'B', complexType = "Vertical", description = "Moving too quickly horizontally.")
public class BoatFlyB extends AbstractCheck {

    public BoatFlyB(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isVehicleMovePacket(packetEvent)) {
            if (false) {
                return;
            }
            this.checkVehicleMovement();
        }
    }

    public void handleEvent(MeshEvent event2) {
        if (event2 instanceof MeshMoveEvent) {
            this.checkVehicleMovement();
        }
    }

    private void checkVehicleMovement() {
        if ((!ExemptionManager.isBefore1_9()) && (this.playerData.getPlayer() != null)) {
            if (!(this.playerData.getPlayer().getVehicle() instanceof Boat)) {
                return;
            }
            int ticksInVehicle = this.playerData.getCollisionState().getTicksInVehicle();
            if (ticksInVehicle <= 5) {
                return;
            }
            Boat vehicle = (Boat) this.playerData.getPlayer().getVehicle();
            double horizontalSpeed = this.playerData.getCollisionState().getVehicleHorizontalSpeed();
            double vehicleDeltaY = this.playerData.getCollisionState().getVehicleDeltaY();
            boolean recentSurroundingIce = this.playerData.getCollisionState().getTicksSinceSurroundingIce() < 100;
            CollisionState collisionState = this.playerData.getCollisionState();
            boolean recentSurroundingLiquid = collisionState.getTicksSinceSurroundingLiquid() < 100;
            boolean recentSurroundingSlime = this.playerData.getCollisionState().getTicksSinceSurroundingSlime() < 100;
            boolean recentSurroundingBubbleColumn = this.playerData.getCollisionState().getTicksSinceSurroundingBubbleColumn() < 100;
            boolean recentSurroundingPiston = this.playerData.getCollisionState().getTicksSinceSurroundingPiston() < 100;
            int nearbyBoatCount = this.playerData.getCollisionState().getNearbyBoatCount();
            if (ExemptionManager.isNewerThan1_13() && !vehicle.isNoGravity()) {
                return;
            }
            if (dev.meshac.MeshServices.hookedBoatTicks().containsKey(vehicle.getId())) {
                return;
            }
            if (this.playerData.getCollisionState().hasLivingEntityNearby()) {
                return;
            }
            double amount = 0.25;
            if (this.isExempt(new ExemptType[] { ExemptType.WATERLOGGED })) {
                amount += 0.2;
            }
            boolean condition6 = horizontalSpeed > amount && vehicleDeltaY > -0.05 && !recentSurroundingLiquid && !recentSurroundingSlime && !recentSurroundingBubbleColumn && !recentSurroundingIce && !recentSurroundingPiston;
            if (condition6 && nearbyBoatCount == 1) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail("deltaXZ=" + horizontalSpeed + " deltaY=" + vehicleDeltaY);
                    if (MeshSettings.checksMovementBoatflyBKickOut) {
                        dev.meshac.MeshServices.mainThread(this::ejectPlayer);
                    }
                }
            } else {
                this.decayBuffer();
            }
            return;
        }
    }

    private void ejectPlayer() {
        dev.meshac.safety.Effects.ejectVehicle(this.playerData.getPlayer());
    }
}
