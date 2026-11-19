package dev.lookout.core.checks;

import dev.lookout.core.AlertSink;
import dev.lookout.core.LookoutConfig;
import dev.lookout.core.ObservedPlayer;
import dev.lookout.core.ObservedSample;
import dev.lookout.core.WorldProbe;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Multi-aura: one player damaging several players at once, over and over, faster
 * than a sweep explains. Evidence is typed: only player-attack damage packets
 * count, so both the attacker and every victim are real.
 * FP notes: a vanilla sweep attack can hit two players in one tick and looks
 * identical per event. What separates aura is cadence: sweeps need a charged
 * cooldown and slow movement, so legit multi-victim hits come seconds apart, not
 * every few ticks. One double-hit never flags; the buffer needs rapid repeats and
 * fades over time. Off by default (opt-in).
 */
public class MultiAuraCheck extends LookoutCheck {

    private final Deque<long[]> recentHits = new ArrayDeque<>(); // victimId, tick
    private long lastMultiTick = -1000;
    private int streak;

    @Override public String id() { return "multiaura"; }

    @Override
    public void onSample(ObservedPlayer p, ObservedSample s, ObservedSample prev, WorldProbe w,
                         LookoutConfig cfg, AlertSink sink) {
        decay(0.02);
    }

    @Override
    public void onHitEvidence(ObservedPlayer p, ObservedPlayer victim, long tick,
                              LookoutConfig cfg, AlertSink sink) {
        if (p.prev != null && p.prev.gamemodeExempt()) return;
        recentHits.addLast(new long[]{victim.entityId, tick});
        while (!recentHits.isEmpty() && tick - recentHits.peekFirst()[1] > 100) recentHits.removeFirst();

        Set<Long> victimsNow = new HashSet<>();
        for (long[] h : recentHits) {
            if (tick - h[1] < cfg.multiAuraWindowTicks) victimsNow.add(h[0]);
        }
        if (victimsNow.size() < 2) return;

        if (tick - lastMultiTick <= cfg.multiAuraRapidGapTicks) streak++;
        else streak = 1;
        lastMultiTick = tick;
        if (streak >= cfg.multiAuraCount) {
            ObservedSample s = p.prev;
            if (s != null) {
                fail(p, s, sink, "hitting several players at once over and over");
            }
            streak = 0;
        }
    }
}
