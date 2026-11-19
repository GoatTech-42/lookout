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
 * FP notes: a vanilla sweep attack can hit two or more players in one tick and
 * looks identical per event, so one burst counts ONCE no matter how many victims
 * it carries: hits inside the aggregation window extend the current burst instead
 * of stacking the streak. What separates aura is cadence across distinct bursts:
 * sweeps need a charged cooldown and slow movement, so legit bursts come seconds
 * apart. A burst after a long gap resets the streak to 1; only rapid back-to-back
 * bursts alert. Off by default (opt-in).
 */
public class MultiAuraCheck extends LookoutCheck {

    private final Deque<long[]> recentHits = new ArrayDeque<>(); // victimId, tick
    private long lastBurstTick = -100000;
    private int burstStreak;

    @Override public String id() { return "multiaura"; }

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
        if (tick - lastBurstTick < cfg.multiAuraWindowTicks) return; // same burst still forming

        if (tick - lastBurstTick <= cfg.multiAuraRapidGapTicks) burstStreak++;
        else burstStreak = 1; // a burst after a long gap restarts the cadence count
        lastBurstTick = tick;
        if (burstStreak >= cfg.multiAuraCount) {
            ObservedSample s = p.prev;
            if (s != null) {
                fail(p, s, sink, "hitting several players at once over and over");
            }
            burstStreak = 0;
        }
    }
}
