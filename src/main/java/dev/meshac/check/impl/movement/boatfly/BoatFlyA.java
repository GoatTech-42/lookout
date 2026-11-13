package dev.meshac.check.impl.movement.boatfly;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptionManager;
import dev.meshac.config.MeshSettings;
import dev.meshac.MeshServices;
import dev.meshac.MeshServices;
import dev.meshac.player.CollisionState;
import dev.meshac.data.PlayerData;
import net.minecraft.world.entity.vehicle.boat.Boat;
import dev.meshac.event.MeshEvent;
import dev.meshac.event.MeshMoveEvent;

@CheckInfo(name = "Boat Fly", type = 'A', complexType = "Vertical", description = "Moving upwards in a boat.")
public class BoatFlyA extends AbstractCheck {

    public BoatFlyA(PlayerData playerData) {
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
            double vehicleDeltaY = this.playerData.getCollisionState().getVehicleDeltaY();
            CollisionState collisionState = this.playerData.getCollisionState();
            boolean recentSurroundingLiquid = collisionState.getTicksSinceSurroundingLiquid() < 100;
            boolean recentSurroundingSlime = this.playerData.getCollisionState().getTicksSinceSurroundingSlime() < 100;
            boolean recentSurroundingBubbleColumn = this.playerData.getCollisionState().getTicksSinceSurroundingBubbleColumn() < 100;
            boolean recentSurroundingPiston = this.playerData.getCollisionState().getTicksSinceSurroundingPiston() < 50;
            boolean recentSurroundingBed = this.playerData.getCollisionState().getTicksSinceSurroundingBed() < 40;
            boolean belowUnloadedChunkTicksLimit = this.playerData.getCollisionState().getUnloadedChunkTicks() < 60;
            boolean surroundedByAir = this.playerData.getCollisionState().isSurroundedByAir();
            if (this.playerData.getCollisionState().hasLivingEntityNearby()) {
                return;
            }
            if (ExemptionManager.isNewerThan1_13() && !vehicle.isNoGravity()) {
                return;
            }
            if (dev.meshac.MeshServices.hookedBoatTicks().containsKey(vehicle.getId())) {
                return;
            }
            boolean condition7 = vehicleDeltaY > 0.01 && !recentSurroundingSlime && !recentSurroundingLiquid && !recentSurroundingBubbleColumn && !recentSurroundingPiston && !recentSurroundingBed && !belowUnloadedChunkTicksLimit && surroundedByAir;
            if (condition7) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail("deltaY=" + vehicleDeltaY);
                    if (MeshSettings.checksMovementBoatflyAKickOut) {
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
        this.playerData.getPlayer().stopRiding();
    }
}
