package dev.meshac.check.impl.player.timer;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.config.MeshVersion;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.check.ExemptionManager;
import dev.meshac.config.MeshSettings;
import dev.meshac.data.PlayerData;
import dev.meshac.tracker.VehicleTracker;

@CheckInfo(name = "Timer", type = 'D', complexType = "Balance", description = "Increased game speed.")
public class TimerD extends AbstractCheck {

    private long lastMovementAtMillis;

    private long balanceMillis;

    private int movementsSinceBalanceReset;

    public TimerD(PlayerData playerData) {
        super(playerData);
        this.lastMovementAtMillis = 0L;
        this.balanceMillis = 0L;
        this.movementsSinceBalanceReset = 0;
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (!((!this.isMovementPacket(packetEvent)) && (!this.isVehicleMovePacket(packetEvent)))) {
            if (!this.isSetbackPending()) {
                long currentTimeMillisDifference = System.currentTimeMillis() - this.lastMovementAtMillis;
                boolean exempt = this.isExempt(new ExemptType[] { ExemptType.JOINED, ExemptType.TPS, ExemptType.VEHICLE });
                if (this.lastMovementAtMillis != 0L && !exempt) {
                    this.balanceMillis += 50L;
                    this.balanceMillis -= currentTimeMillisDifference;
                    if (this.balanceMillis > MeshSettings.checksPlayerTimerDMaxBalance) {
                        VehicleTracker vehicleTracker = this.playerData.getVehicleTracker();
                        if (vehicleTracker.getEntityType() == null) {
                            if (this.increaseBuffer() > this.maximumBuffer) {
                                this.fail("balance=" + this.balanceMillis + " delay=" + currentTimeMillisDifference);
                            } else {
                                this.decayBuffer();
                            }
                            this.balanceMillis = -200L;
                        }
                    }
                    if (this.playerData.getClientVersion().isNewerThanOrEquals(MeshVersion.V_1_9)) {
                        if (this.movementsSinceBalanceReset % 400 == 0) {
                            this.balanceMillis = -200L;
                            this.movementsSinceBalanceReset = 0;
                        }
                    } else if (this.movementsSinceBalanceReset % 3000 == 0) {
                        this.balanceMillis = -200L;
                        this.movementsSinceBalanceReset = 0;
                    }
                }
                this.movementsSinceBalanceReset++;
                this.lastMovementAtMillis = System.currentTimeMillis();
                return;
            }
        }
        if (this.isServerPositionPacket(packetEvent)) {
            if (ExemptionManager.isAtLeast1_9()) {
                this.balanceMillis -= 150L;
            } else {
                this.balanceMillis -= 50L;
            }
        }
    }

    @Override
    public void update() {
    }
}
