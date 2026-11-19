package dev.lookout.core.checks;

import dev.lookout.core.AlertSink;
import dev.lookout.core.LookoutConfig;
import dev.lookout.core.ObservedPlayer;
import dev.lookout.core.ObservedSample;
import dev.lookout.core.WorldProbe;

/** One detector watching one player. Reusable buffer/decay shape from meshac. */
public abstract class LookoutCheck {
    protected double buffer;

    public abstract String id();

    public void onSample(ObservedPlayer p, ObservedSample s, ObservedSample prev, WorldProbe w,
                         LookoutConfig cfg, AlertSink sink) {}

    public void onSwing(ObservedPlayer p, long tick, LookoutConfig cfg, AlertSink sink) {}

    public void onTotemPop(ObservedPlayer p, long tick, LookoutConfig cfg, AlertSink sink) {}

    public void onTotemEquip(ObservedPlayer p, long tick, LookoutConfig cfg, AlertSink sink) {}

    protected void fail(ObservedPlayer p, ObservedSample s, AlertSink sink, String message) {
        sink.alert(p.entityId, p.name, id(), message);
        buffer *= 0.5;
    }

    protected void decay(double amount) {
        buffer = Math.max(0.0, buffer - amount);
    }
}
