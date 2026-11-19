package dev.lookout.core.checks;

import dev.lookout.core.AlertSink;
import dev.lookout.core.LookoutConfig;
import dev.lookout.core.ObservedPlayer;
import dev.lookout.core.ObservedSample;
import dev.lookout.core.WorldProbe;

/**
 * Rough reach: repeated swings lined up on a player who is too far away to hit.
 * Vanilla survival reach is about 3 blocks; the bar here is far past that plus the
 * lag-lerp margin.
 * FP notes: lookout cannot see mobs, so someone fighting a mob next to a player can
 * read as swinging at that player; the facing alignment requirement makes that rarer,
 * not impossible. Positions arrive smoothed over 3 ticks, so the distance bar carries
 * a wide margin and single long swings decay. This is the roughest check in lookout
 * and its notes deserve the most skepticism.
 */
public class ReachCheck extends LookoutCheck {

    @Override public String id() { return "reach"; }

    @Override
    public void onSwing(ObservedPlayer p, long tick, LookoutConfig cfg, AlertSink sink) {
        ObservedSample s = p.prev;
        if (s == null) return;
        double dist = p.lastSwingNearestDist;
        if (dist < 0 || dist > 16) { decay(0.5); return; } // nobody around, or too far to call
        double off = FacingCheck.yawToBearingDiff(s.yaw(), p.lastSwingBearing);
        if (dist > cfg.reachBlocks && off < cfg.reachFacingDegrees) {
            buffer += 1.0;
            if (buffer > cfg.reachBufferMax) {
                fail(p, s, sink, "swinging at someone " + String.format("%.1f", dist) + " blocks away");
            }
        } else {
            decay(0.5);
        }
    }
}
