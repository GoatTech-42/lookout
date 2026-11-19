package dev.lookout.core.checks;

import dev.lookout.core.AlertSink;
import dev.lookout.core.LookoutConfig;
import dev.lookout.core.ObservedPlayer;
import dev.lookout.core.ObservedSample;
import dev.lookout.core.WorldProbe;

/**
 * Elytra overspeed: gliding level (or climbing) far past ordinary elytra flight.
 * Experimental, configurable heuristic: the cap approximates unpowered and
 * firework-boosted flight from the outside; it is not a sourced vanilla maximum.
 * Dives go faster but trade height for it, so only fast AND not-descending counts.
 * FP notes: pullouts keep dive speed for a moment, so the streak must sustain past
 * that. Knockback and teleports exempt. Treat borderline notes as weak.
 */
public class ElytraCheck extends LookoutCheck {

    private int streak;
    private double prevSpeed = -1;

    @Override public String id() { return "elytra"; }

    @Override
    public void onSample(ObservedPlayer p, ObservedSample s, ObservedSample prev, WorldProbe w,
                         LookoutConfig cfg, AlertSink sink) {
        if (prev == null) return;
        boolean exempt = !s.gliding() || s.gamemodeExempt() || s.inVehicle()
                || p.ticksSince(p.lastVelocityTick, s.tick()) < 20
                || p.ticksSince(p.lastTeleportTick, s.tick()) < 5;
        if (exempt) { streak = 0; prevSpeed = -1; return; }
        boolean descending = s.y() - prev.y() < -0.2; // dives buy speed with height
        double speed = s.horizontalSpeed(prev);
        // an unpowered level glide bleeds speed, so a glide still shedding dive
        // momentum is legit even above the cap; holding speed level is not
        boolean decaying = prevSpeed > 0 && speed < prevSpeed - 0.001;
        prevSpeed = speed;
        if (!descending && !decaying && speed > cfg.elytraMaxLevel) {
            streak++;
            if (streak >= cfg.elytraSustainTicks) {
                fail(p, s, sink, "gliding too fast without diving ("
                        + String.format("%.1f", s.horizontalSpeed(prev) * 20) + " m/s level)");
                streak = 0;
            }
        } else {
            streak = 0;
        }
    }
}
