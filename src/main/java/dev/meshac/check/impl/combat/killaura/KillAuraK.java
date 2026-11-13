package dev.meshac.check.impl.combat.killaura;

import dev.meshac.packet.MeshPacketEvent;
import dev.meshac.packet.InteractEntity;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import dev.meshac.check.AbstractCheck;
import dev.meshac.check.api.CheckInfo;
import dev.meshac.check.ExemptType;
import dev.meshac.data.PlayerData;

@CheckInfo(name = "Kill Aura", type = 'K', complexType = "Pattern", description = "Attack pattern.")
public class KillAuraK extends AbstractCheck {

    private final List attackIntervals = Lists.newArrayList();

    private long lastAttackAtMillis;

    private static final String VIOLATION_DETAIL_PREFIX = "range=";

    public KillAuraK(PlayerData playerData) {
        super(playerData);
    }

    public void handlePacket(MeshPacketEvent packetEvent) {
        if (this.isAttackPacket(packetEvent)) {
            InteractEntity lastInteractionPacket = this.playerData.getLastInteractionPacket();
            if (lastInteractionPacket.getAction() != InteractEntity.Action.ATTACK) {
                return;
            }
            if (this.isExempt(new ExemptType[] { ExemptType.FAST })) {
                return;
            }
            long currentTimeMillisDifference = System.currentTimeMillis() - this.lastAttackAtMillis;
            this.attackIntervals.add(currentTimeMillisDifference);
            if (this.attackIntervals.size() == 10) {
                Collections.sort(this.attackIntervals);
                long longValue2 = (Long) this.attackIntervals.get(this.attackIntervals.size() - 1) - (Long) this.attackIntervals.get(0);
                if (longValue2 < 50L) {
                    if (this.increaseBuffer() > this.maximumBuffer) {
                        this.fail(VIOLATION_DETAIL_PREFIX + longValue2);
                    }
                } else {
                    this.decayBuffer();
                }
                this.attackIntervals.clear();
            }
            this.lastAttackAtMillis = System.currentTimeMillis();
        }
    }

    @Override
    public void update() {
    }
}
