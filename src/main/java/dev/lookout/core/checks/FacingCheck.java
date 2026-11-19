package dev.lookout.core.checks;

import dev.lookout.core.AlertSink;
import dev.lookout.core.LookoutConfig;
import dev.lookout.core.ObservedPlayer;
import dev.lookout.core.ObservedSample;
import dev.lookout.core.WorldProbe;

/**
 * KillAura facing: a player keeps damaging someone they are not looking at.
 * Evidence comes from damage packets, which name the hurt entity and the entity
 * that caused it, so lookout never guesses a victim: no damage packet, no judgment.
 * Off by default (opt-in in lookout.json).
 * FP notes: only melee damage counts (cause and direct ids match). The angle
 * tolerance is wide and positions are lag-lerped, so single odd hits decay; it
 * needs a run of off-facing hits. The buffer also decays over time, so spaced-out
 * incidents never add up.
 */
public class FacingCheck extends LookoutCheck {

    @Override public String id() { return "killaura"; }

    static double yawToBearingDiff(float yaw, double bearingDeg) {
        double diff = (bearingDeg - yaw) % 360.0;
        if (diff > 180) diff -= 360;
        if (diff < -180) diff += 360;
        return Math.abs(diff);
    }

    static double distance3d(ObservedSample a, ObservedSample b) {
        return Math.sqrt((a.x() - b.x()) * (a.x() - b.x())
                + (a.y() - b.y()) * (a.y() - b.y())
                + (a.z() - b.z()) * (a.z() - b.z()));
    }

    @Override
    public void onSample(ObservedPlayer p, ObservedSample s, ObservedSample prev, WorldProbe w,
                         LookoutConfig cfg, AlertSink sink) {
        decay(0.02); // old incidents fade even without swings
    }

    @Override
    public void onHitEvidence(ObservedPlayer p, ObservedPlayer victim, long tick,
                              LookoutConfig cfg, AlertSink sink) {
        ObservedSample a = p.prev, v = victim.prev;
        if (a == null || v == null || a.gamemodeExempt()) return;
        double dist = distance3d(a, v);
        if (dist > cfg.killauraRange) return; // too far to read angles; reach owns long hits
        double dx = v.x() - a.x(), dz = v.z() - a.z();
        // a victim nearly straight over or under the attacker has no bearing to
        // compare against (atan2(0,0)); vertical fights are legit, so skip
        if (dx * dx + dz * dz < 0.25) return;
        double bearing = Math.toDegrees(Math.atan2(-dx, dz));
        double off = yawToBearingDiff(a.yaw(), bearing);
        if (off > cfg.killauraFacingDegrees) {
            buffer += 1.0;
            if (buffer > cfg.killauraBufferMax) {
                fail(p, a, sink, "hits landing while facing away (off by " + Math.round(off) + " degrees)");
            }
        } else {
            decay(1.0);
        }
    }
}
