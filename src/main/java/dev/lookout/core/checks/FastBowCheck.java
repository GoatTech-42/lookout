package dev.lookout.core.checks;

import dev.lookout.core.AlertSink;
import dev.lookout.core.LookoutConfig;
import dev.lookout.core.ObservedPlayer;
import dev.lookout.core.ObservedSample;
import dev.lookout.core.WorldProbe;

/**
 * FastBow: near-full-power arrows released after almost no draw. Vanilla spam-clicking
 * makes weak, slow arrows; vanilla full power needs a ~20 tick draw. A fast arrow off a
 * two-tick draw is a machine.
 * FP notes: arrow power comes from the spawn velocity in the S2C add-entity packet, and
 * the shooter is attributed as the nearest player seen drawing a bow, so two archers
 * side by side can cross-attribute. It needs three fast full-power shots inside the
 * memory window, and every shot must follow an observed bow draw. Crossbows load with
 * the crossbow in hand (not the bow use-animation), so they never count.
 */
public class FastBowCheck extends LookoutCheck {

    private long drawStart = -1000;
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
        long draw = tick - drawStart;
        if (p.lastProjectileSpeed >= cfg.fastbowFullPowerSpeed && draw <= cfg.fastbowMaxDrawTicks) {
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
