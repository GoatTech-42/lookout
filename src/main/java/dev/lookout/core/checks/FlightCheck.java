package dev.lookout.core.checks;

import dev.lookout.core.AlertSink;
import dev.lookout.core.LookoutConfig;
import dev.lookout.core.ObservedPlayer;
import dev.lookout.core.ObservedSample;
import dev.lookout.core.WorldProbe;

/**
 * Flight / hover: holding level or rising while airborne with no support, past what
 * a jump explains.
 * FP notes: exempts vehicles, elytra glide pose, water, climbables, levitation and
 * slow-falling effects, recent knockback/velocity (20 ticks) and recent teleports (5
 * ticks). Support means solid, liquid or climbable within a half block of the feet, or
 * stairs below. Lag spikes arrive as teleports and reset the clock. The long-airtime
 * rule only engages when the player is NOT falling at speed (a real fall from any
 * height passes it), so ordinary drops never flag. Unloaded chunks read as support.
 * Known hole, deliberate: gravity-mimicking fly that keeps falling slowly stays quiet.
 */
public class FlightCheck extends LookoutCheck {

    @Override public String id() { return "flight"; }

    @Override
    public void onSample(ObservedPlayer p, ObservedSample s, ObservedSample prev, WorldProbe w,
                         LookoutConfig cfg, AlertSink sink) {
        if (prev == null) return;
        boolean exempt = s.gamemodeExempt() || s.inVehicle() || s.gliding() || s.inWater()
                || s.levitation() || s.slowFalling() || s.swimmingPose()
                || p.ticksSince(p.lastVelocityTick, s.tick()) < 20
                || p.ticksSince(p.lastTeleportTick, s.tick()) < 5;
        boolean supported = s.onGround()
                || w.isSolidAt(s.x(), s.y() - 0.5, s.z())
                || w.isLiquidAt(s.x(), s.y() - 0.5, s.z())
                || w.isClimbableAt(s.x(), s.y(), s.z())
                || w.isStairLikeAt(s.x(), s.y() - 0.51, s.z());
        if (exempt || supported) {
            p.airTicks = 0;
            decay(0.75);
            return;
        }

        p.airTicks++;
        double dy = s.y() - prev.y();

        // hovering dead level well past jump time
        if (p.airTicks >= cfg.flightGraceTicks && Math.abs(dy) < 0.004) buffer += 1.5;
        // rising again after the opening ticks of a jump
        if (p.airTicks >= 4 && dy > 0.05) buffer += 1.5;
        // long airtime only counts when not genuinely falling; a real fall from any
        // height is moving down faster than 1 block/tick well before the grace ends
        int maxAir = cfg.flightGraceTicks + 12 * s.jumpBoostLevel();
        if (p.airTicks > maxAir && dy > -1.0) buffer += 0.5;

        if (buffer > cfg.flightBufferMax) {
            fail(p, s, sink, "flight-like movement (air " + String.format("%.1f", p.airTicks / 20.0)
                    + "s, no support)");
        }
    }
}
