package dev.lookout.core.checks;

import dev.lookout.core.AlertSink;
import dev.lookout.core.LookoutConfig;
import dev.lookout.core.ObservedPlayer;
import dev.lookout.core.ObservedSample;
import dev.lookout.core.WorldProbe;

/**
 * Jesus: running across the surface of water without sinking in.
 * FP notes: the whole footprint must be over open water: center, all four edges and all
 * four diagonal corners are probed, so a player whose corner clips a solid block at the
 * shoreline stays quiet.
 * Frost walker freezes the surface, and ice reads solid, so it self-exempts. Boats and
 * other vehicles, elytra glide, swimming pose, in-water, recent knockback (10 ticks) and
 * recent teleports (5 ticks) all exempt. Needs ten straight fast surface ticks, so packet
 * bunching (frozen, then one big delta) resets the streak instead of feeding it.
 */
public class JesusCheck extends LookoutCheck {

    private static final double EDGE = 0.3;
    private int streak;

    @Override public String id() { return "jesus"; }

    private boolean openWaterUnderFootprint(ObservedSample s, WorldProbe w) {
        double y = s.y() - 0.2;
        double[][] points = {
                {s.x(), s.z()},
                {s.x() + EDGE, s.z()}, {s.x() - EDGE, s.z()},
                {s.x(), s.z() + EDGE}, {s.x(), s.z() - EDGE},
                {s.x() + EDGE, s.z() + EDGE}, {s.x() + EDGE, s.z() - EDGE},
                {s.x() - EDGE, s.z() + EDGE}, {s.x() - EDGE, s.z() - EDGE}};
        for (double[] pt : points) {
            if (!w.isLiquidAt(pt[0], y, pt[1])) return false;
            if (w.isSolidAt(pt[0], y, pt[1])) return false;
            // a lily pad or carpet underfoot has no collision at y-0.2 (that reads
            // the water below it) but a thin shape right under the feet; standing
            // on it over water is legit (26.3: pad top is 0.09375 over the surface)
            if (w.isSolidAt(pt[0], s.y() - 0.05, pt[1])) return false;
        }
        return true;
    }

    @Override
    public void onSample(ObservedPlayer p, ObservedSample s, ObservedSample prev, WorldProbe w,
                         LookoutConfig cfg, AlertSink sink) {
        if (prev == null) return;
        boolean exempt = s.inWater() || s.swimmingPose() || s.inVehicle() || s.gliding()
                || s.gamemodeExempt()
                || p.ticksSince(p.lastVelocityTick, s.tick()) < 10
                || p.ticksSince(p.lastTeleportTick, s.tick()) < 5;
        boolean onSurface = !exempt && openWaterUnderFootprint(s, w);
        double speed = s.horizontalSpeed(prev);
        if (onSurface && speed > 0.025) {
            streak++;
            if (streak >= 10) {
                fail(p, s, sink, "walking on water (" + streak + " ticks on the surface)");
                streak = 0;
            }
        } else {
            streak = 0;
        }
    }
}
