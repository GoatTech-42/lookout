package dev.meshac.check.impl.movement.entityflight;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptionManager;
import dev.meshac.config.MeshSettings;
import dev.meshac.MeshServices;
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

@CheckInfo(name = "Entity Flight", type = 'A', complexType = "Ascension", description = "Ascending while riding an entity.")
public class EntityFlightA extends AbstractCheck {

    public EntityFlightA(PlayerData playerData) {
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
            if (entity2 instanceof net.minecraft.world.entity.Leashable leashable && leashable.isLeashed()) {
                return;
            }
            if (ExemptionManager.isNewerThan1_13()) {
                if (entity2.hasEffect(MobEffects.LEVITATION)) {
                    return;
                }
                if (entity2.hasEffect(MobEffects.SLOW_FALLING)) {
                    return;
                }
            }
            CollisionState collisionState2 = this.playerData.getCollisionState();
            boolean recentSurroundingLiquid = collisionState2.getTicksSinceSurroundingLiquid() < 100;
            boolean recentSurroundingSlime = this.playerData.getCollisionState().getTicksSinceSurroundingSlime() < 100;
            boolean recentSurroundingBubbleColumn = this.playerData.getCollisionState().getTicksSinceSurroundingBubbleColumn() < 100;
            boolean belowUnloadedChunkTicksLimit = this.playerData.getCollisionState().getUnloadedChunkTicks() < 60;
            int ticksInVehicle = this.playerData.getCollisionState().getTicksInVehicle();
            CollisionState collisionState3 = this.playerData.getCollisionState();
            int ticksInOpenAir = collisionState3.getTicksInOpenAir();
            double vehicleDeltaY = this.playerData.getCollisionState().getVehicleDeltaY();
            boolean condition5 = ticksInOpenAir > 30 && vehicleDeltaY > 0.0 && !recentSurroundingLiquid && !recentSurroundingSlime && !belowUnloadedChunkTicksLimit && !recentSurroundingBubbleColumn && ticksInVehicle > 5;
            if (condition5) {
                if (this.increaseBuffer() > this.maximumBuffer) {
                    this.fail("[" + text + "] deltaY=" + vehicleDeltaY + " ticks=" + ticksInOpenAir);
                    if (MeshSettings.checksMovementEntityflightAKickOut) {
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
