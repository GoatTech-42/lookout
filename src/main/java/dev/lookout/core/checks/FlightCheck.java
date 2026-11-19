package dev.lookout.core.checks;

import dev.lookout.core.AlertSink;
import dev.lookout.core.LookoutConfig;
import dev.lookout.core.ObservedPlayer;
import dev.lookout.core.ObservedSample;
import dev.lookout.core.WorldProbe;

/**
 * Flight / hover: in the air with no support far longer than a jump allows,
 * holding level, or rising again after the opening ticks of a jump.
 * FP notes: exempts vehicles, elytra glide pose, water, climbables, levitation and
 * slow-falling effects, recent knockback/velocity (20 ticks) and recent teleports (5 ticks).
 * Support means solid, liquid or climbable within a half block of the feet, or stairs below.
 * Lag spikes arrive as teleports and reset the clock. A legit jump with jump boost 2 stays
 * under the grace window; the grace window is the main FP dial.
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
        // rising again after the opening of a jump
        if (p.airTicks >= 4 && dy > 0.05) buffer += 1.5;
        // still airborne far past what jump boost explains
        int maxAir = cfg.flightGraceTicks + 12 * s.jumpBoostLevel();
        if (p.airTicks > maxAir) buffer += 0.5;

        if (buffer > cfg.flightBufferMax) {
            fail(p, s, sink, "flight-like movement (air " + String.format("%.1f", p.airTicks / 20.0)
                    + "s, no support)");
        }
    }
}
