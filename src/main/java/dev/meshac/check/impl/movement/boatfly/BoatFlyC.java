package dev.meshac.check.impl.movement.boatfly;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptionManager;
import dev.meshac.MeshServices;
import dev.meshac.player.CollisionState;
import dev.meshac.data.PlayerData;
import net.minecraft.world.entity.vehicle.boat.Boat;
import dev.meshac.event.MeshEvent;
import dev.meshac.event.MeshMoveEvent;

@CheckInfo(name = "Boat Fly", type = 'C', complexType = "Hover", description = "Hovering in a boat.")
public class BoatFlyC extends AbstractCheck {

    public BoatFlyC(PlayerData playerData) {
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
            if (dev.meshac.MeshServices.hookedBoatTicks().containsKey(vehicle.getId())) {
                return;
            }
            double vehicleDeltaY = this.playerData.getCollisionState().getVehicleDeltaY();
            double previousVehicleDeltaY = this.playerData.getCollisionState().getPreviousVehicleDeltaY();
            double accel = Math.abs(vehicleDeltaY - previousVehicleDeltaY);
            CollisionState collisionState = this.playerData.getCollisionState();
            int ticksInOpenAir = collisionState.getTicksInOpenAir();
            boolean condition = vehicleDeltaY > -0.05 && accel < 0.025 && ticksInOpenAir > 5;
            if (this.playerData.getCollisionState().hasLivingEntityNearby()) {
                return;
            }
            if (ExemptionManager.isNewerThan1_13() && !vehicle.isNoGravity()) {
                return;
            }
            boolean recentSurroundingIce = this.playerData.getCollisionState().getTicksSinceSurroundingIce() < 50;
            CollisionState collisionState2 = this.playerData.getCollisionState();
            boolean recentSurroundingLiquid = collisionState2.getTicksSinceSurroundingLiquid() < 50;
            boolean recentSurroundingSlime = this.playerData.getCollisionState().getTicksSinceSurroundingSlime() < 50;
            boolean recentSurroundingBubbleColumn = this.playerData.getCollisionState().getTicksSinceSurroundingBubbleColumn() < 50;
            boolean recentSurroundingPiston = this.playerData.getCollisionState().getTicksSinceSurroundingPiston() < 50;
            int nearbyBoatCount = this.playerData.getCollisionState().getNearbyBoatCount();
            if (condition && !recentSurroundingIce && !recentSurroundingLiquid && !recentSurroundingSlime && !recentSurroundingBubbleColumn && !recentSurroundingPiston && nearbyBoatCount == 1) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail("accel=" + accel + " deltaY=" + vehicleDeltaY);
                }
            } else {
                this.decayBuffer();
            }
            return;
        }
    }
}
