package dev.lookout.core.checks;

import dev.lookout.core.AlertSink;
import dev.lookout.core.LookoutConfig;
import dev.lookout.core.ObservedPlayer;
import dev.lookout.core.ObservedSample;
import dev.lookout.core.WorldProbe;

/**
 * Step: gaining more height than vanilla stepping allows (0.6) over a short window
 * without ever leaving the ground. Uses a 3-tick ground-to-ground gain so the
 * client-side position smoothing (3-tick lerp) cannot hide it.
 * FP notes: stairs, slabs and climbables exempt the whole window. Knockback and
 * teleports reset it. Horses and boats are vehicles (exempt). A jump always leaves
 * the ground, so it never reads here.
 */
public class StepCheck extends LookoutCheck {

    @Override public String id() { return "step"; }

    @Override
    public void onSample(ObservedPlayer p, ObservedSample s, ObservedSample prev, WorldProbe w,
                         LookoutConfig cfg, AlertSink sink) {
        if (p.samples.size() < 3) return;
        if (s.gamemodeExempt() || s.inVehicle()
                || p.ticksSince(p.lastVelocityTick, s.tick()) < 10
                || p.ticksSince(p.lastTeleportTick, s.tick()) < 5) {
            decay(0.5);
            return;
        }
        ObservedSample[] last = p.samples.toArray(new ObservedSample[0]);
        int n = last.length;
        ObservedSample a = last[n - 3], b = last[n - 2], c = last[n - 1];
        boolean allGrounded = a.onGround() && b.onGround() && c.onGround() && p.airTicks == 0;
        if (!allGrounded) {
            decay(0.25);
            return;
        }
        // stairs and slabs in or under the window explain big legit gains
        boolean stairs = w.isStairLikeAt(c.x(), c.y() - 0.51, c.z()) || w.isStairLikeAt(c.x(), c.y(), c.z())
                || w.isStairLikeAt(a.x(), a.y(), a.z()) || w.isClimbableAt(c.x(), c.y(), c.z());
        double gain = c.y() - a.y();
        if (!stairs && gain > cfg.stepGain) {
            buffer += 2.0;
            if (buffer > cfg.stepBufferMax) {
                fail(p, s, sink, "stepped up " + String.format("%.2f", gain) + " blocks without jumping");
            }
        } else {
            decay(0.5);
        }
    }
}
