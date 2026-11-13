package dev.meshac.check.impl.player.timer;

import dev.meshac.check.AbstractCheck;
import dev.meshac.check.ExemptType;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.collection.EvictingList;
import dev.meshac.config.MeshSettings;
import dev.meshac.data.PlayerData;
import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.util.MathUtils;
import java.util.Collection;

@CheckInfo(name = "Timer", type = 'A', complexType = "Average", description = "Increased game speed.")
public class TimerA extends AbstractCheck {

	private final EvictingList movementIntervals = new EvictingList(50);

	private long lastMovementAtMillis;

	public TimerA(PlayerData playerData) {
		super(playerData);
	}

	@Override
	public void handlePacket(MeshPacketEvent packetEvent) {
		if (!((!this.isMovementPacket(packetEvent)) && (!this.isVehicleMovePacket(packetEvent)))) {
			if (!this.isSetbackPending()) {
				long currentTimeMillisDifference = System.currentTimeMillis() - this.lastMovementAtMillis;
				boolean exempt = this.isExempt(new ExemptType[] { ExemptType.JOINED, ExemptType.TPS, ExemptType.VEHICLE });
				if (currentTimeMillisDifference > 4L && !exempt) {
					this.movementIntervals.add(currentTimeMillisDifference);
				}
				if (this.movementIntervals.isFull()) {
					double average2 = MathUtils.average((Collection) this.movementIntervals);
					double scaledAverage2 = 50.0 / average2;
					double speed = scaledAverage2 * 100.0;
					double checksPlayerTimerAMaxSpeed2 = MeshSettings.checksPlayerTimerAMaxSpeed;
					if (this.isExempt(new ExemptType[] { ExemptType.WEB })) {
						checksPlayerTimerAMaxSpeed2 += 0.05;
					}
					if (scaledAverage2 >= checksPlayerTimerAMaxSpeed2) {
						if (this.increaseBuffer() > this.maximumBuffer) {
							this.fail("speed=" + speed + "% delay=" + currentTimeMillisDifference);
						}
					} else {
						this.decayBuffer();
					}
				}
				this.lastMovementAtMillis = System.currentTimeMillis();
				return;
			}
		}
		if (this.isServerPositionPacket(packetEvent)) {
			this.movementIntervals.add(150L);
		} else if (this.playerData.isAtLeast1_17()) {
			if ((!this.isBlockPlacementPacket(packetEvent)) && (!this.isUseItemPacket(packetEvent))) {
				return;
			}
			this.movementIntervals.add(50L);
		}
	}

	@Override
	public void update() {
	}
}
