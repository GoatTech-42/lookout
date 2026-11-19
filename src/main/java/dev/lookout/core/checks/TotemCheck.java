package dev.lookout.core.checks;

import dev.lookout.core.AlertSink;
import dev.lookout.core.LookoutConfig;
import dev.lookout.core.ObservedPlayer;
import dev.lookout.core.ObservedSample;
import dev.lookout.core.WorldProbe;

/**
 * AutoTotem: a totem pops and a fresh one appears in the offhand almost instantly,
 * over and over. One fast restock is human; three instant ones in a row is a machine.
 * FP notes: needs three distinct pops each followed by an instant restock, so a single
 * clutch swap never flags, and one pop fuels exactly one counted restock. Each pop is
 * consumed by the first restock that follows it, so two separate instant pops cannot
 * share one restock, and duplicate restock events after a single pop cannot stack.
 */
public class TotemCheck extends LookoutCheck {

    private long lastPop = -1000;
    private int instantCount;
    private long lastInstant = -1000;

    @Override public String id() { return "totem"; }

    @Override
    public void onTotemPop(ObservedPlayer p, long tick, LookoutConfig cfg, AlertSink sink) {
        lastPop = tick;
    }

    @Override
    public void onTotemEquip(ObservedPlayer p, long tick, LookoutConfig cfg, AlertSink sink) {
        if (tick - lastInstant > cfg.totemMemoryTicks) instantCount = 0;
        long popAge = tick - lastPop;
        if (lastPop != -1000 && popAge <= cfg.totemRestockTicks) {
            instantCount++;
            lastInstant = tick;
            lastPop = -1000; // consume the pop: one pop fuels exactly one counted restock
            if (instantCount >= cfg.totemCount) {
                sink.alert(p.entityId, p.name, id(),
                        "instant totem restock x" + instantCount + " (" + popAge + " ticks after the last pop)");
                instantCount = 0;
            }
        }
    }

    @Override
    public void onSample(ObservedPlayer p, ObservedSample s, ObservedSample prev, WorldProbe w,
                         LookoutConfig cfg, AlertSink sink) {}
}
