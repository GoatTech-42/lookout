package dev.lookout.core.checks;

import dev.lookout.core.AlertSink;
import dev.lookout.core.LookoutConfig;
import dev.lookout.core.ObservedPlayer;
import dev.lookout.core.ObservedSample;
import dev.lookout.core.WorldProbe;

/**
 * Step: gaining more height than vanilla stepping allows (0.6) over a short window
 * without ever leaving the ground. Uses a 3-tick ground-to-ground gain so the
 * client-side position smoothing cannot hide it.
 * FP notes: every cell of the window (start, middle, end) is probed for stairs,
 * slabs and climbables, at foot level and below. Knockback and teleports reset it.
 * Horses and boats are vehicles (exempt). A jump always leaves the ground, so it
 * never reads here. Known FP risk, needs the buffer to repeat: two legit 0.6 ledges
 * inside 3 ticks with no stair/slab block anywhere in the window.
 */
public class StepCheck extends LookoutCheck {

    @Override public String id() { return "step"; }

    private boolean stairsNear(ObservedSample s, WorldProbe w) {
        return w.isStairLikeAt(s.x(), s.y() - 0.51, s.z()) || w.isStairLikeAt(s.x(), s.y(), s.z())
                || w.isClimbableAt(s.x(), s.y(), s.z());
    }

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
        // red team S2: an onGround=false flicker every other tick used to skip the
        // check entirely. two of three grounded flags with no real airtime is
        // still a grounded window; a genuinely airborne tick (airTicks > 0) is not
        int grounded = (a.onGround() ? 1 : 0) + (b.onGround() ? 1 : 0) + (c.onGround() ? 1 : 0);
        if (grounded < 2 || p.airTicks > 0) {
            decay(0.25);
            return;
        }
        if (stairsNear(a, w) || stairsNear(b, w) || stairsNear(c, w)) {
            decay(0.5);
            return;
        }
        double gain = c.y() - a.y();
        if (gain > cfg.stepGain) {
            buffer += 2.0;
            if (buffer > cfg.stepBufferMax) {
                fail(p, s, sink, "stepped up " + String.format("%.2f", gain) + " blocks without jumping");
            }
        } else {
            decay(0.5);
        }
    }
}
