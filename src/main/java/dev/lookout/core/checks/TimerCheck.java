package dev.lookout.core.checks;

import dev.lookout.core.AlertSink;
import dev.lookout.core.LookoutConfig;
import dev.lookout.core.ObservedPlayer;
import dev.lookout.core.ObservedSample;
import dev.lookout.core.WorldProbe;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Timer-like movement: covering ground in wall-clock seconds faster than tick
 * physics allow. Honest scope: from another client there is no separate "timer"
 * signal in positions, since both per-tick and per-second math use the same
 * samples. What this check can say is that a player's real-time ground speed stays
 * past what sprint, effects and ice explain. Low multipliers are invisible to it.
 * Off by default (opt-in).
 * FP notes: only counts while our own client ticks at full rate (a laggy local
 * client makes everyone's wall-clock speed unreadable). Only grounded, unexempt
 * ticks count, same as speed. Needs a sustained window, not a spike.
 */
public class TimerCheck extends LookoutCheck {

    private final Deque<double[]> window = new ArrayDeque<>(); // tick, wallMillis, x, z

    @Override public String id() { return "timer"; }

    @Override
    public void onSample(ObservedPlayer p, ObservedSample s, ObservedSample prev, WorldProbe w,
                         LookoutConfig cfg, AlertSink sink) {
        if (prev == null) return;
        boolean exempt = s.gamemodeExempt() || s.inVehicle() || s.gliding() || s.inWater()
                || !s.onGround() || s.swimmingPose()
                || w.isClimbableAt(s.x(), s.y(), s.z())
                || w.isSlipperyAt(s.x(), s.y() - 0.5, s.z())
                || p.ticksSince(p.lastVelocityTick, s.tick()) < 20
                || p.ticksSince(p.lastTeleportTick, s.tick()) < 5;
        if (exempt) { window.clear(); return; }
        window.addLast(new double[]{s.tick(), p.lastWallMillis, s.x(), s.z()});
        while (window.size() > cfg.timerWindowTicks) window.removeFirst();
        if (window.size() < cfg.timerWindowTicks) return;

        double[] first = window.peekFirst(), last = window.peekLast();
        double ticks = last[0] - first[0];
        double wallMs = last[1] - first[1];
        if (ticks < 30 || wallMs < 500) return;
        double localTps = ticks * 1000.0 / wallMs;
        if (localTps < cfg.timerMinLocalTps) return; // our client lagging: unreadable
        double dist = Math.hypot(last[2] - first[2], last[3] - first[3]);
        double wallSpeed = dist * 1000.0 / wallMs; // m/s in real time
        if (wallSpeed > cfg.timerMaxWallSpeed) {
            fail(p, s, sink, "moving faster than game time allows (timer-like, "
                    + String.format("%.1f", wallSpeed) + " m/s real-time)");
            window.clear();
        }
    }
}
