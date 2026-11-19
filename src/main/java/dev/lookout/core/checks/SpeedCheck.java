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
 * Deliberate miss: anything under the cap (0.44 b/t, ~57% over sprint's ~0.28) stays
 * silent. The gap is that wide because legit bhop momentum sits between sprint and
 * the cap, and from outside the client cannot separate a small cheat bonus from it;
 * meshac owns the sub-cap fight server-side. A lone spike (rubber-band, unsignaled
 * relocation) is trimmed, not averaged in.
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
        if (p.lastVelocityTick >= windowStart.tick() || p.lastTeleportTick >= windowStart.tick()
                || p.lastJoinRelocationTick >= windowStart.tick()) {
            decay(0.5);
            return;
        }
        double dist = 0;
        double maxDelta = 0;
        int maxSpeedLevel = 0;
        int airFlickers = 0;
        for (int i = size - n; i < size; i++) {
            ObservedSample cur = last[i];
            if (cur.inVehicle() || cur.gliding() || cur.inWater()
                    || cur.swimmingPose() || cur.gamemodeExempt()) {
                decay(0.25);
                return;
            }
            // red team P1: one airborne flag per window used to skip the check
            // forever. a lone flicker is tolerated (the hop's delta is real
            // movement and still counts); a window full of hops is bhop, skipped
            if (!cur.onGround()) airFlickers++;
            if (w.isSlipperyAt(cur.x(), cur.y() - 0.5, cur.z())
                    || (cur.glintingBoots() && w.isSoulGroundAt(cur.x(), cur.y() - 0.5, cur.z()))) {
                decay(0.5);
                return;
            }
            maxSpeedLevel = Math.max(maxSpeedLevel, cur.speedLevel());
            if (i > 0) {
                double d = Math.hypot(cur.x() - last[i - 1].x(), cur.z() - last[i - 1].z());
                dist += d;
                if (d > maxDelta) maxDelta = d;
            }
        }
        if (airFlickers > cfg.speedAirFlickerTicks) {
            decay(0.25);
            return;
        }
        // red team P3: a single unsignaled relocation under the teleport bar (2.9
        // blocks in one tick) used to poison the whole window. drop the largest
        // per-tick delta: a lone spike is absorbed, sustained speed is untouched
        double avg = (dist - maxDelta) / (n - 1);
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
