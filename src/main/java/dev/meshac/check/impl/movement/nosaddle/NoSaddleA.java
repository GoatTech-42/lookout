package dev.meshac.check.impl.movement.nosaddle;

import me.frep.vulcan.spigot.check.AbstractCheck;
import me.frep.vulcan.spigot.check.api.CheckInfo;
import dev.meshac.check.ExemptionManager;
import dev.meshac.config.MeshSettings;
import dev.meshac.data.PlayerData;
import dev.meshac.MeshServices;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.server.level.ServerPlayer;
import dev.meshac.event.MeshEvent;
import dev.meshac.event.MeshMoveEvent;

@CheckInfo(name = "No Saddle", type = 'A', complexType = "Spoof", description = "Controlling an entity without a saddle.")
public class NoSaddleA extends AbstractCheck {

    public NoSaddleA(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public void update() {
    }

    @Override
    public void handleEvent(Event event) {
        if (event instanceof MeshMoveEvent)
            checkVehicleMovement();
    }

    private void checkVehicleMovement() {
        if (ExemptionManager.isBefore1_9())
            return;
        Player player = playerData.getPlayer();
        if (player == null || !(player.getVehicle() instanceof Horse horse) || horse.isLeashed())
            return;
        AttributeInstance movementSpeed = horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (movementSpeed == null)
            return;
        double horizontalSpeed = playerData.getCollisionState().getVehicleHorizontalSpeed();
        double difference = movementSpeed.getBaseValue() * 2.5 + 0.1 - horizontalSpeed;
        if (horse.getInventory().getSaddle() == null && difference < 0.3 && horizontalSpeed > 0.15 && playerData.getCollisionState().getTicksInVehicle() > 5) {
            if (increaseBuffer() > maximumBuffer) {
                fail("diff=" + difference + " deltaXZ=" + horizontalSpeed);
                if (MeshSettings.checksPlayerBadpacketsVKickOut)
                    dev.meshac.MeshServices.mainThread(() -> {
                        Player currentPlayer = playerData.getPlayer();
                        if (currentPlayer != null)
                            currentPlayer.leaveVehicle();
                    });
            }
        } else
            decayBuffer();
    }
}
