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
 * stairs below, or a web block at the feet (near-zero fall speed, no collision shape).
 * Lag spikes arrive as teleports and reset the clock. The long-airtime
 * rule only engages when the player is NOT falling at speed (a real fall from any
 * height passes it), so ordinary drops never flag, while a slow-fall fly (sinking
 * gently instead of accelerating) still reads as not-falling and flags. Unloaded
 * chunks read as support. Known hole, deliberate: fly that mimics a real fall at
 * terminal-velocity-like speed (a block per tick or faster) passes the rule.
 */
public class FlightCheck extends LookoutCheck {

    @Override public String id() { return "flight"; }

    @Override
    public void onSample(ObservedPlayer p, ObservedSample s, ObservedSample prev, WorldProbe w,
                         LookoutConfig cfg, AlertSink sink) {
        if (prev == null) return;
        // airTicks is engine-owned: the engine zeroes it on exempt or supported
        // ticks and increments it otherwise, whether or not this check is enabled
        // (amp airTicks packet T1: noslow's verdict used to depend on this check's
        // toggle). 0 here means this tick was exempt or supported.
        if (p.airTicks == 0) {
            decay(0.75);
            return;
        }

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
