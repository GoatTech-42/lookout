package dev.lookout.core.checks;

import dev.lookout.core.AlertSink;
import dev.lookout.core.LookoutConfig;
import dev.lookout.core.ObservedPlayer;
import dev.lookout.core.ObservedSample;
import dev.lookout.core.WorldProbe;

/**
 * KillAura facing: swinging while looking nowhere near the closest player in range.
 * A legit hit lines up with where the attacker looks; auras that keep a server-side
 * rotation (or no rotation) swing with their back to the target.
 * FP notes: only judges when another player is within plausible combat range, so
 * swinging at air, mobs or nothing never counts. The tolerance is wide (past any
 * legit flick) and positions are lag-lerped, so single odd swings decay away; it
 * needs a run of off-target swings. Mobs are invisible to lookout: someone fighting
 * a mob with a player standing nearby can read as off-target swings, which is why
 * the alert stays hedged.
 */
public class FacingCheck extends LookoutCheck {

    @Override public String id() { return "killaura"; }

    static double yawToBearingDiff(float yaw, double bearingDeg) {
        double diff = (bearingDeg - yaw) % 360.0;
        if (diff > 180) diff -= 360;
        if (diff < -180) diff += 360;
        return Math.abs(diff);
    }

    @Override
    public void onSwing(ObservedPlayer p, long tick, LookoutConfig cfg, AlertSink sink) {
        ObservedSample s = p.prev;
        if (s == null) return;
        double dist = p.lastSwingNearestDist;
        if (dist < 0 || dist > cfg.killauraRange) { decay(0.5); return; } // nobody close enough to judge
        double off = yawToBearingDiff(s.yaw(), p.lastSwingBearing);
        if (off > cfg.killauraFacingDegrees) {
            buffer += 1.0;
            if (buffer > cfg.killauraBufferMax) {
                fail(p, s, sink, "swinging without facing the target (off by "
                        + Math.round(off) + " degrees)");
            }
        } else {
            decay(0.5);
        }
    }
}
