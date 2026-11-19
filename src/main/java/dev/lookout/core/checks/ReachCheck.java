package dev.lookout.core.checks;

import dev.lookout.core.AlertSink;
import dev.lookout.core.LookoutConfig;
import dev.lookout.core.ObservedPlayer;
import dev.lookout.core.ObservedSample;
import dev.lookout.core.WorldProbe;

/**
 * Rough reach: a player keeps damaging someone too far away to hit. Evidence comes
 * from damage packets, which name the hurt entity and the entity that caused it, so
 * bystanders, mob fights and mining never enter the picture. Off by default (opt-in
 * in lookout.json).
 * FP notes: the distance bar sits well past vanilla's roughly-3-block melee reach
 * plus a margin for lag-lerped positions, single long hits decay, and the buffer
 * fades over time. Still the roughest check in lookout: the bar is heuristic, and
 * both ends' positions arrive smoothed.
 */
public class ReachCheck extends LookoutCheck {

    @Override public String id() { return "reach"; }

    @Override
    public void onSample(ObservedPlayer p, ObservedSample s, ObservedSample prev, WorldProbe w,
                         LookoutConfig cfg, AlertSink sink) {
        decay(0.02);
    }

    @Override
    public void onHitEvidence(ObservedPlayer p, ObservedPlayer victim, long tick,
                              LookoutConfig cfg, AlertSink sink) {
        ObservedSample a = p.prev, v = victim.prev;
        if (a == null || v == null || a.gamemodeExempt()) return;
        double dist = FacingCheck.distance3d(a, v);
        if (dist > cfg.reachBlocks) {
            buffer += 1.0;
            if (buffer > cfg.reachBufferMax) {
                fail(p, a, sink, "hits landing from " + String.format("%.1f", dist) + " blocks away");
            }
        } else {
            decay(1.0);
        }
    }
}
