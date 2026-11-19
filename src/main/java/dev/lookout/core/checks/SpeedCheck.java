package dev.lookout.core.checks;

import dev.lookout.core.AlertSink;
import dev.lookout.core.LookoutConfig;
import dev.lookout.core.ObservedPlayer;
import dev.lookout.core.ObservedSample;
import dev.lookout.core.WorldProbe;

/**
 * Speed: sustained ground speed past what sprint + the visible speed effect allow.
 * Windowed average so per-tick lerp noise cancels out.
 * FP notes: every sample in the window must qualify on its own: grounded, not in a
 * vehicle, not gliding, not in water, not swimming pose, not gamemode-exempt, and its
 * own ground must not be ice or soul ground with glinting boots. A window that straddles
 * an exit (off ice, out of water) is skipped, not averaged. The cap scales with the
 * strongest speed effect visible in the window. Stale windows (samples older than the
 * window span, e.g. after a stall) are skipped. Knockback and teleports reset.
 * Deliberate miss: subtle speed (5-15%) stays under the cap; from outside the client
 * cannot separate it from legit bhop reliably, and meshac owns that fight server-side.
 */
public class SpeedCheck extends LookoutCheck {

    @Override public String id() { return "speed"; }

    @Override
    public void onSample(ObservedPlayer p, ObservedSample s, ObservedSample prev, WorldProbe w,
                         LookoutConfig cfg, AlertSink sink) {
        if (prev == null) return;
        int n = Math.min(cfg.speedWindowTicks, p.samples.size());
        if (n < cfg.speedWindowTicks) return;
        ObservedSample[] last = p.samples.toArray(new ObservedSample[0]);
        int size = last.length;
        ObservedSample windowStart = last[size - n];
        if (s.tick() - windowStart.tick() > n + 10) {
            decay(0.25); // stale history: samples span far more wall time than the window
            return;
        }
        if (p.lastVelocityTick >= windowStart.tick() || p.lastTeleportTick >= windowStart.tick()) {
            decay(0.5);
            return;
        }
        double dist = 0;
        int maxSpeedLevel = 0;
        for (int i = size - n; i < size; i++) {
            ObservedSample cur = last[i];
            if (!cur.onGround() || cur.inVehicle() || cur.gliding() || cur.inWater()
                    || cur.swimmingPose() || cur.gamemodeExempt()) {
                decay(0.25);
                return;
            }
            if (w.isSlipperyAt(cur.x(), cur.y() - 0.5, cur.z())
                    || (cur.glintingBoots() && w.isSoulGroundAt(cur.x(), cur.y() - 0.5, cur.z()))) {
                decay(0.5);
                return;
            }
            maxSpeedLevel = Math.max(maxSpeedLevel, cur.speedLevel());
            if (i > 0) dist += Math.hypot(cur.x() - last[i - 1].x(), cur.z() - last[i - 1].z());
        }
        double avg = dist / n;
        double cap = cfg.speedMax * (1.0 + 0.2 * maxSpeedLevel);
        if (avg > cap) {
            buffer += 1.0;
            if (buffer > cfg.speedBufferMax) {
                fail(p, s, sink, "moving fast (" + String.format("%.1f", avg * 20) + " m/s on the ground)");
            }
        } else {
            decay(0.5);
        }
    }
}
