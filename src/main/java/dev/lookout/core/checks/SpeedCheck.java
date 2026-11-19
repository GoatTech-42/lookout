package dev.lookout.core.checks;

import dev.lookout.core.AlertSink;
import dev.lookout.core.LookoutConfig;
import dev.lookout.core.ObservedPlayer;
import dev.lookout.core.ObservedSample;
import dev.lookout.core.WorldProbe;

/**
 * Speed: sustained ground speed past what sprint + the visible speed effect allow.
 * Windowed average so per-tick lerp noise cancels out.
 * FP notes: only counts grounded ticks, and skips ice (sprint-slide), soul ground with
 * glinting boots (soul speed), water, vehicles, elytra, and anything near knockback or a
 * teleport. The base cap is set well past good bhop (~0.30-0.35/tick windowed), so this
 * only catches gross speed; subtle 5-10% speed is a documented miss on purpose.
 */
public class SpeedCheck extends LookoutCheck {

    @Override public String id() { return "speed"; }

    @Override
    public void onSample(ObservedPlayer p, ObservedSample s, ObservedSample prev, WorldProbe w,
                         LookoutConfig cfg, AlertSink sink) {
        if (prev == null) return;
        if (s.gamemodeExempt() || s.inVehicle() || s.gliding() || s.inWater() || s.swimmingPose()
                || p.ticksSince(p.lastVelocityTick, s.tick()) < 10
                || p.ticksSince(p.lastTeleportTick, s.tick()) < 5) {
            decay(0.5);
            return;
        }
        int n = Math.min(cfg.speedWindowTicks, p.samples.size());
        if (n < cfg.speedWindowTicks) return;
        ObservedSample[] last = p.samples.toArray(new ObservedSample[0]);
        int size = last.length;
        double dist = 0;
        int groundTicks = 0;
        for (int i = size - n; i < size; i++) {
            ObservedSample cur = last[i];
            if (!cur.onGround()) continue;
            groundTicks++;
            if (i > 0) dist += Math.hypot(cur.x() - last[i - 1].x(), cur.z() - last[i - 1].z());
        }
        if (groundTicks < n * 0.8) {
            decay(0.25);
            return;
        }
        if (w.isSlipperyAt(s.x(), s.y() - 0.5, s.z())
                || (s.glintingBoots() && w.isSoulGroundAt(s.x(), s.y() - 0.5, s.z()))) {
            decay(0.5);
            return;
        }
        double avg = dist / n;
        double cap = cfg.speedMax * (1.0 + 0.2 * s.speedLevel());
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
