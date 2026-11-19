package dev.lookout.core.checks;

import dev.lookout.core.AlertSink;
import dev.lookout.core.LookoutConfig;
import dev.lookout.core.ObservedPlayer;
import dev.lookout.core.ObservedSample;
import dev.lookout.core.WorldProbe;

/**
 * Jesus: running across the surface of water without sinking in.
 * FP notes: frost walker turns water to ice, which reads solid, so it self-exempts.
 * Boats are vehicles (exempt), swimming pose and in-water both exempt. Requires ten
 * straight ticks of surface travel, so one splashy step or a lag burst does not flag.
 */
public class JesusCheck extends LookoutCheck {

    private int streak;

    @Override public String id() { return "jesus"; }

    @Override
    public void onSample(ObservedPlayer p, ObservedSample s, ObservedSample prev, WorldProbe w,
                         LookoutConfig cfg, AlertSink sink) {
        if (prev == null) return;
        boolean onSurface = !s.inWater() && !s.swimmingPose() && !s.inVehicle() && !s.gamemodeExempt()
                && w.isLiquidAt(s.x(), s.y() - 0.2, s.z())
                && !w.isSolidAt(s.x(), s.y() - 0.2, s.z());
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
