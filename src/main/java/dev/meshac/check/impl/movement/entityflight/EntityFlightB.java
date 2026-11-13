package dev.meshac.check.impl.movement.entityflight;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptionManager;
import dev.meshac.MeshServices;
import dev.meshac.player.CollisionState;
import dev.meshac.data.PlayerData;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.equine.Donkey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import dev.meshac.event.MeshEvent;
import dev.meshac.event.MeshMoveEvent;
import net.minecraft.world.effect.MobEffects;

@CheckInfo(name = "Entity Flight", type = 'B', complexType = "Hover", description = "Hovering while riding an entity.")
public class EntityFlightB extends AbstractCheck {

    public EntityFlightB(PlayerData playerData) {
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
        if ((!ExemptionManager.isBefore1_9()) && (this.playerData.getPlayer() != null) && (!this.playerData.getPlayer().isDeadOrDying())) {
            CollisionState collisionState = this.playerData.getCollisionState();
            if (collisionState.getVehicleY() < 0.0) {
                return;
            }
            Entity entity = this.playerData.getPlayer().getVehicle();
            if (entity == null) {
                return;
            }
            String text;
            if (entity instanceof Horse) {
                text = "Horse";
            } else if (entity instanceof Pig) {
                text = "Pig";
            } else if (ExemptionManager.isAtLeast1_9() && entity instanceof Donkey) {
                text = "Donkey";
            } else if (entity instanceof ZombieHorse) {
                text = "Zombie Horse";
            } else if (entity instanceof SkeletonHorse) {
                text = "Skeleton Horse";
            } else {
                if (!ExemptionManager.isAtLeast1_20() || !(entity instanceof Camel)) {
                    return;
                }
                text = "Camel";
            }
            LivingEntity entity2 = (LivingEntity) entity;
            if (ExemptionManager.isNewerThan1_13()) {
                if (entity2.hasEffect(MobEffects.LEVITATION)) {
                    return;
                }
                if (entity2.hasEffect(MobEffects.SLOW_FALLING)) {
                    return;
                }
            }
            if (entity2 instanceof net.minecraft.world.entity.Leashable leashable && leashable.isLeashed()) {
                return;
            }
            CollisionState collisionState2 = this.playerData.getCollisionState();
            boolean recentSurroundingLiquid = collisionState2.getTicksSinceSurroundingLiquid() < 100;
            boolean recentSurroundingSlime = this.playerData.getCollisionState().getTicksSinceSurroundingSlime() < 100;
            boolean recentSurroundingBubbleColumn = this.playerData.getCollisionState().getTicksSinceSurroundingBubbleColumn() < 100;
            int ticksInVehicle = this.playerData.getCollisionState().getTicksInVehicle();
            CollisionState collisionState3 = this.playerData.getCollisionState();
            int ticksInOpenAir = collisionState3.getTicksInOpenAir();
            double vehicleDeltaY = this.playerData.getCollisionState().getVehicleDeltaY();
            double previousVehicleDeltaY = this.playerData.getCollisionState().getPreviousVehicleDeltaY();
            double vehicleDeltaYDifference = Math.abs(vehicleDeltaY - previousVehicleDeltaY);
            boolean condition4 = ticksInOpenAir > 30 && Math.abs(vehicleDeltaY) < 5.0 && vehicleDeltaYDifference < 0.05 && !recentSurroundingLiquid && !recentSurroundingSlime && !recentSurroundingBubbleColumn && ticksInVehicle > 5 && vehicleDeltaY > -1.15;
            if (condition4) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail("[" + text + "] deltaY=" + vehicleDeltaY + " acceleration=" + vehicleDeltaYDifference + " ticks=" + ticksInVehicle);
                }
            } else {
                this.decayBuffer();
            }
            return;
        }
    }
}
