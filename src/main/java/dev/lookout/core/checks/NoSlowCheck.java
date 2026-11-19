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
    private int streak;

    @Override public String id() { return "noslow"; }

    @Override
    public void onSample(ObservedPlayer p, ObservedSample s, ObservedSample prev, WorldProbe w,
                         LookoutConfig cfg, AlertSink sink) {
        if (prev == null) return;
        if (!s.usingItem()) { useStart = -1000; streak = 0; return; }
        if (!prev.usingItem()) { useStart = s.tick(); streak = 0; return; }
        boolean exempt = s.gamemodeExempt() || s.inVehicle() || s.gliding() || s.inWater()
                || !s.onGround() // hopping keeps momentum legitimately
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
