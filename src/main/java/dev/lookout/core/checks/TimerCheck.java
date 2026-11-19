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
 * past what sprint, effects and terrain explain. Low multipliers are invisible to
 * it. Off by default (opt-in).
 * FP notes: the cap scales with the strongest speed effect seen in the window, and
 * ice or soul-speed ground disqualifies samples, same as the speed check. The
 * cadence gate measures our own client's sampling rate: the window must be one
 * unbroken run of consecutive samples, wall-clock pauses and tick gaps reset it,
 * and the local rate must sit between 19 and 21 tps (a slow client makes
 * wall-clock math unreadable; a fast one means catch-up processing after a stall).
 * The wall clock is monotonic (System.nanoTime in the client glue), never the
 * adjustable system clock.
 */
public class TimerCheck extends LookoutCheck {

    private final Deque<double[]> window = new ArrayDeque<>(); // tick, wallMillis, x, z, speedLevel
    private long lastTick = -1;
    private long lastWall = -1;

    @Override public String id() { return "timer"; }

    @Override
    public void onSample(ObservedPlayer p, ObservedSample s, ObservedSample prev, WorldProbe w,
                         LookoutConfig cfg, AlertSink sink) {
        if (prev == null) return;
        long wall = p.lastWallMillis;
        boolean exempt = s.gamemodeExempt() || s.inVehicle() || s.gliding() || s.inWater()
                || !s.onGround() || s.swimmingPose()
                || w.isClimbableAt(s.x(), s.y(), s.z())
                || w.isSlipperyAt(s.x(), s.y() - 0.5, s.z())
                || (s.glintingBoots() && w.isSoulGroundAt(s.x(), s.y() - 0.5, s.z()))
                || p.ticksSince(p.lastVelocityTick, s.tick()) < 20
                || p.ticksSince(p.lastTeleportTick, s.tick()) < 5;
        // reject anything but an unbroken, evenly-paced sample run
        boolean gap = lastTick >= 0 && (s.tick() - lastTick != 1 || wall - lastWall > 500);
        lastTick = s.tick();
        lastWall = wall;
        if (exempt || gap) { window.clear(); return; }
        window.addLast(new double[]{s.tick(), wall, s.x(), s.z(), s.speedLevel()});
        while (window.size() > cfg.timerWindowTicks) window.removeFirst();
        if (window.size() < cfg.timerWindowTicks) return;

        double[] first = window.peekFirst(), last = window.peekLast();
        double ticks = last[0] - first[0];
        double wallMs = last[1] - first[1];
        if (ticks < 30 || wallMs < 500) return;
        double localTps = ticks * 1000.0 / wallMs;
        if (localTps < cfg.timerMinLocalTps || localTps > cfg.timerMaxLocalTps) return;
        double maxEffect = 0;
        for (double[] e : window) maxEffect = Math.max(maxEffect, e[4]);
        double cap = cfg.timerMaxWallSpeed * (1 + 0.2 * maxEffect);
        double dist = Math.hypot(last[2] - first[2], last[3] - first[3]);
        double wallSpeed = dist * 1000.0 / wallMs; // m/s in real time
        if (wallSpeed > cap) {
            fail(p, s, sink, "moving faster than game time allows (timer-like, "
                    + String.format("%.1f", wallSpeed) + " m/s real-time)");
            window.clear();
        }
    }
}
