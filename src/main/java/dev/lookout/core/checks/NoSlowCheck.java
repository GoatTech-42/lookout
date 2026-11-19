package dev.lookout.core.checks;

import dev.lookout.core.AlertSink;
import dev.lookout.core.LookoutConfig;
import dev.lookout.core.ObservedPlayer;
import dev.lookout.core.ObservedSample;
import dev.lookout.core.WorldProbe;

/**
 * NoSlow: moving at full speed while eating, drinking, drawing a bow or blocking.
 * Experimental, configurable heuristic: the cap is a rough line, not a sourced
 * vanilla maximum. Vanilla slows item use heavily; keeping sprint pace through it
 * is a cheat.
 * FP notes: a grace covers the momentum you carry into the use (sprint, then eat),
 * and slippery ground exempts outright (sliding on ice while eating is legit and
 * reading the difference from outside is guesswork). Vehicles, elytra, water,
 * knockback and teleports exempt. Needs a sustained run of fast using-ticks, so
 * the tick you start eating while still coasting never flags.
 */
public class NoSlowCheck extends LookoutCheck {

    private long useStart = -1000;
    private int releaseGap = 1000;
    private int streak;

    @Override public String id() { return "noslow"; }

    @Override
    public void onSample(ObservedPlayer p, ObservedSample s, ObservedSample prev, WorldProbe w,
                         LookoutConfig cfg, AlertSink sink) {
        if (prev == null) return;
        // red team N1: a one-tick release every grace period used to restart the
        // grace forever. a gap up to noslowReleaseGapTicks holds state instead;
        // only a real break re-arms it
        if (s.usingItem()) {
            if (releaseGap > cfg.noslowReleaseGapTicks) { useStart = s.tick(); streak = 0; }
            releaseGap = 0;
        } else {
            releaseGap++;
            if (releaseGap > cfg.noslowReleaseGapTicks) { useStart = -1000; streak = 0; }
            return;
        }
        if (useStart == -1000) useStart = s.tick(); // first observed use-tick
        // red team N2: one airborne flag every few ticks used to reset the streak
        // forever. this reads the engine-owned, support-derived airTicks
        // (real airtime with no ground support), NOT a count of airborne flags:
        // a lone flicker over solid ground is just not evaluated, genuine
        // airtime (2+ ticks) exempts and resets
        if (!s.onGround() && p.airTicks > 1) { streak = 0; return; }
        if (!s.onGround()) return;
        boolean exempt = s.gamemodeExempt() || s.inVehicle() || s.gliding() || s.inWater()
                || w.isClimbableAt(s.x(), s.y(), s.z())
                || w.isSlipperyAt(s.x(), s.y() - 0.5, s.z())
                || p.ticksSince(p.lastVelocityTick, s.tick()) < 20
                || p.ticksSince(p.lastTeleportTick, s.tick()) < 5
                || s.tick() - useStart < cfg.noslowGraceTicks;
        if (exempt) { streak = 0; return; }
        double cap = cfg.noslowMaxSpeed * (1 + 0.2 * s.speedLevel()); // speed effect scales the crawl too
        if (s.horizontalSpeed(prev) > cap) {
            streak++;
            if (streak >= cfg.noslowSustainTicks) {
                fail(p, s, sink, "moving at full speed while using an item");
                streak = 0;
            }
        } else {
            streak = 0;
        }
    }
}
