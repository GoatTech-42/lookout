package dev.lookout.core.checks;

import dev.lookout.core.AlertSink;
import dev.lookout.core.LookoutConfig;
import dev.lookout.core.ObservedPlayer;
import dev.lookout.core.ObservedSample;
import dev.lookout.core.WorldProbe;

/**
 * FastBow: near-full-power arrows released after almost no draw. Spam-clicking a bow
 * makes weak, slow arrows; a full-power arrow off a two-tick draw is a machine.
 * FP notes: arrow power comes from the spawn velocity in the S2C add-entity packet,
 * and the shooter comes from that packet's owner field, so bystanders are never
 * blamed; arrows with an unknown or untracked owner are skipped. One draw fuels one
 * counted release, so a multi-arrow burst on a single draw counts once. It still
 * needs three fast full-power shots inside the memory window.
 */
public class FastBowCheck extends LookoutCheck {

    private long drawStart = -1000;
    private long countedDraw = -1000;
    private int fastCount;
    private long lastFast = -1000;

    @Override public String id() { return "fastbow"; }

    @Override
    public void onSample(ObservedPlayer p, ObservedSample s, ObservedSample prev, WorldProbe w,
                         LookoutConfig cfg, AlertSink sink) {
        if (s.usingBow() && (prev == null || !prev.usingBow())) drawStart = s.tick();
    }

    @Override
    public void onProjectile(ObservedPlayer p, long tick, LookoutConfig cfg, AlertSink sink) {
        if (tick - lastFast > cfg.fastbowMemoryTicks) fastCount = 0;
        if (drawStart == countedDraw) return; // one draw fuels one counted release
        long draw = tick - drawStart;
        if (p.lastProjectileSpeed >= cfg.fastbowFullPowerSpeed && draw <= cfg.fastbowMaxDrawTicks) {
            countedDraw = drawStart;
            fastCount++;
            lastFast = tick;
            if (fastCount >= cfg.fastbowCount) {
                sink.alert(p.entityId, p.name, id(),
                        "near-full-power arrows after ~" + draw + " ticks of draw x" + fastCount);
                fastCount = 0;
            }
        }
    }
}
