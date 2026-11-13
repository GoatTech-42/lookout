package dev.meshac.check.impl.combat.autoclicker.distribution;

import dev.meshac.packet.MeshPacketEvent;
import java.util.Collection;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.collection.EvictingList;
import dev.meshac.config.MeshSettings;
import dev.meshac.player.ClickTimingData;
import dev.meshac.data.PlayerData;
import dev.meshac.util.MathUtils;

@CheckInfo(name = "Auto Clicker", type = 'T', complexType = "Kurtosis", description = "Too low kurtosis")
public class AutoClickerT extends AbstractCheck {

    private final EvictingList<Long> clickIntervals = new EvictingList<>(25);

    private static AbstractCheck[] abstractChecks;

    private static final String VIOLATION_DETAIL_PREFIX;

    public AutoClickerT(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        dev.meshac.collection.EvictingList<Long> evictingList2;
        AbstractCheck[] abstractChecks2;
        abstractChecks2 = getAbstractChecks();
        boolean armAnimationPacket = this.isArmAnimationPacket(packetEvent);
        if (abstractChecks2 == null) {
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
        if (abstractChecks2 == null) {
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
        double scaledKurtosis = MathUtils.kurtosis((Collection) evictingList2) / 1000.0;
        if (abstractChecks2 == null) {
            ClickTimingData clickTimingData2 = this.playerData.getClickTimingData();
            clickTimingData2.setKurtosis(scaledKurtosis);
            if (scaledKurtosis < MeshSettings.checksCombatAutoclickerTMinKurtosis) {
                if (!(this.increaseBuffer() > this.maximumBuffer)) {
                    return;
                }
                this.fail(VIOLATION_DETAIL_PREFIX + scaledKurtosis);
                return;
            }
        }
        this.decayBuffer();
    }

    @Override
    public void update() {
    }

    public static void setAbstractChecks(AbstractCheck[] abstractChecks2) {
        abstractChecks = abstractChecks2;
    }

    public static AbstractCheck[] getAbstractChecks() {
        return abstractChecks;
    }

    static {
        if (getAbstractChecks() != null) {
            setAbstractChecks(new AbstractCheck[2]);
        }
        VIOLATION_DETAIL_PREFIX = "kurtosis=";
    }
}
