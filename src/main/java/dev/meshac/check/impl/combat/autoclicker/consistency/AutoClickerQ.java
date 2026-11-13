package dev.meshac.check.impl.combat.autoclicker.consistency;

import dev.meshac.check.impl.combat.autoclicker.distribution.AutoClickerT;
import dev.meshac.packet.MeshPacketEvent;
import java.util.Collection;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.collection.EvictingList;
import dev.meshac.player.ClickTimingData;
import dev.meshac.data.PlayerData;
import dev.meshac.util.MathUtils;

@CheckInfo(name = "Auto Clicker", type = 'Q', complexType = "Average Deviation", description = "Too low average deviation.")
public class AutoClickerQ extends AbstractCheck {

    private final EvictingList<Long> clickIntervals = new EvictingList<>(40);

    private double previousDeviation;

    public AutoClickerQ(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        dev.meshac.collection.EvictingList<Long> evictingList2;
        AbstractCheck[] abstractChecks;
        abstractChecks = AutoClickerT.getAbstractChecks();
        boolean armAnimationPacket = this.isArmAnimationPacket(packetEvent);
        if (abstractChecks == null) {
            if (!armAnimationPacket) {
                return;
            }
            armAnimationPacket = this.isExempt(new ExemptType[] { ExemptType.AUTOCLICKER });
            if (armAnimationPacket) {
                return;
            }
        } else if (armAnimationPacket) {
            return;
        }
        PlayerData playerData = this.playerData;
        ClickTimingData clickTimingData = playerData.getClickTimingData();
        long clickInterval = clickTimingData.getClickInterval();
        long clickIntervalDifference;
        int index = (clickIntervalDifference = clickInterval - 5000L) == 0L ? 0 : (clickIntervalDifference < 0L ? -1 : 1);
        if (abstractChecks == null) {
            if (index > 0) {
                this.clickIntervals.clear();
                return;
            }
            this.clickIntervals.add(clickInterval);
            evictingList2 = this.clickIntervals;
            index = (evictingList2.isFull() ? 1 : 0);
            if (index == 0) {
                return;
            }
            evictingList2 = this.clickIntervals;
        } else {
            if (index == 0) {
                return;
            }
            evictingList2 = this.clickIntervals;
        }
        double previousDeviation2;
        previousDeviation2 = MathUtils.rootSumSquaredDeviations((Collection) evictingList2);
        double difference = Math.abs(previousDeviation2 - this.previousDeviation);
        double scaledAdjustedPreviousDeviation2 = Math.abs(previousDeviation2 + this.previousDeviation) / 2.0;
        double differenceDifference;
        int index2 = (differenceDifference = difference - 3.0) == 0.0 ? 0 : (differenceDifference < 0.0 ? -1 : 1);
        if (abstractChecks == null) {
            if (index2 >= 0) {
                this.decayBuffer();
            } else {
                double scaledAdjustedPreviousDeviation2Difference;
                index2 = (scaledAdjustedPreviousDeviation2Difference = scaledAdjustedPreviousDeviation2 - 150.0) == 0.0 ? 0 : (scaledAdjustedPreviousDeviation2Difference < 0.0 ? -1 : 1);
                if (index2 >= 0) {
                    this.decayBuffer();
                } else {
                    double previousDeviationDifference;
                    index2 = (previousDeviationDifference = this.increaseBuffer() - this.previousDeviation) == 0.0 ? 0 : (previousDeviationDifference < 0.0 ? -1 : 1);
                    if (!(index2 <= 0)) {
                        this.fail("difference=" + difference + " average=" + scaledAdjustedPreviousDeviation2);
                    }
                }
            }
        } else if (!(index2 <= 0)) {
            this.fail("difference=" + difference + " average=" + scaledAdjustedPreviousDeviation2);
            this.decayBuffer();
        }
        this.previousDeviation = previousDeviation2;
    }

    @Override
    public void update() {
    }
}
