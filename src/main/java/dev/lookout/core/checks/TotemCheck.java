package dev.lookout.core.checks;

import dev.lookout.core.AlertSink;
import dev.lookout.core.LookoutConfig;
import dev.lookout.core.ObservedPlayer;
import dev.lookout.core.ObservedSample;
import dev.lookout.core.WorldProbe;

/**
 * AutoTotem: a totem pops and a fresh one appears in the offhand almost instantly,
 * over and over. One fast restock is human; three instant ones in a row is a machine.
 * FP notes: needs three instant restocks inside the memory window, so a single clutch
 * swap never flags. Latency only makes the client see the restock later, which lowers
 * sensitivity, it never creates instant restocks that did not happen.
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
        if (tick - lastPop <= cfg.totemRestockTicks) {
            instantCount++;
            lastInstant = tick;
            if (instantCount >= cfg.totemCount) {
                sink.alert(p.entityId, p.name, id(),
                        "instant totem restock x" + instantCount + " (" + (tick - lastPop) + " ticks after a pop)");
                instantCount = 0;
            }
        }
    }

    @Override
    public void onSample(ObservedPlayer p, ObservedSample s, ObservedSample prev, WorldProbe w,
                         LookoutConfig cfg, AlertSink sink) {}
}
