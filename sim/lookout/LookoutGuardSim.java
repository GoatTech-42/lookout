import dev.lookout.core.AlertSink;
import dev.lookout.core.LookoutConfig;
import dev.lookout.core.LookoutEngine;
import dev.lookout.core.ObservedSample;
import dev.lookout.core.WorldProbe;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * False-positive guard fixtures for reach, multiaura, timer and killaura facing.
 * Synthetic branch fixtures, not recorded traffic. No check or threshold is changed:
 * every guard is paired with a red control that differs from it by the one thing the
 * guard is about (a boundary value, an exemption, a decay, a gate), and the red control
 * must alert. A guard that cannot go red would prove nothing.
 */
public class LookoutGuardSim {

    static List<String> alerts;
    static final long[] WALL = {0};
    static final String PA = "minecraft:player_attack";

    static final WorldProbe FLAT = new WorldProbe() {
        public boolean isSolidAt(double x, double y, double z) { return Math.floor(y) <= 63; }
        public boolean isLiquidAt(double x, double y, double z) { return false; }
        public boolean isClimbableAt(double x, double y, double z) { return false; }
        public boolean isStairLikeAt(double x, double y, double z) { return false; }
        public boolean isSlipperyAt(double x, double y, double z) { return false; }
        public boolean isSoulGroundAt(double x, double y, double z) { return false; }
    };

    /** only one check on, no alert cooldown, fake wall clock */
    static LookoutEngine only(String check) {
        alerts = new ArrayList<>();
        WALL[0] = 0;
        LookoutConfig c = new LookoutConfig();
        c.flight = c.jesus = c.step = c.speed = c.totem = c.fastbow = c.noslow = c.elytra = false;
        c.killaura = c.reach = c.timer = c.multiAura = false;
        switch (check) {
            case "reach" -> c.reach = true;
            case "multiaura" -> c.multiAura = true;
            case "timer" -> c.timer = true;
            case "killaura" -> c.killaura = true;
            default -> throw new IllegalArgumentException(check);
        }
        c.alertCooldownSeconds = 0;
        return new LookoutEngine(c, (id, name, chk, msg) -> alerts.add(chk + " " + name + ": " + msg), () -> WALL[0]);
    }

    static ObservedSample smp(long t, double x, double z, float yaw, boolean exempt, int speedLevel) {
        return new ObservedSample(t, x, 64, z, true, true, false, false, false, false,
                false, false, false, 0, speedLevel, false, exempt, yaw, 0f, false);
    }

    static ObservedSample smp(long t, double x, double z, float yaw) { return smp(t, x, z, yaw, false, 0); }

    static long count(String check) { return alerts.stream().filter(a -> a.startsWith(check + " ")).count(); }

    static int guards, reds;

    /** guard must raise exactly guardExact alerts of the check; red must raise at least redMin. */
    static void pair(String name, String check, int guardExact, Consumer<LookoutEngine> guard,
                     int redMin, Consumer<LookoutEngine> red) {
        LookoutEngine g = only(check);
        guard.accept(g);
        if (count(check) != guardExact) {
            throw new AssertionError("FAIL guard [" + name + "] expected " + guardExact + " " + check + " alerts, got " + alerts);
        }
        guards++;
        System.out.println("guard ok: " + name + (guardExact == 0 ? "" : " (exactly " + guardExact + ")"));
        LookoutEngine r = only(check);
        red.accept(r);
        if (count(check) < redMin) {
            throw new AssertionError("FAIL red control [" + name + "] expected >= " + redMin + " " + check + " alerts, got " + alerts);
        }
        reds++;
        System.out.println("  red caught: " + name + " -> " + count(check) + " " + check + " alert(s)");
    }

    static void pair(String name, String check, Consumer<LookoutEngine> guard, Consumer<LookoutEngine> red) {
        pair(name, check, 0, guard, 1, red);
    }

    // ---------------- reach: attacker 1 at x=500 facing +x (yaw -90), victim 2 at x=500+dist ----------------

    /** hits hits-times, one every spacing ticks; dist(hitIndex) gives the distance per hit. */
    static void reachRun(LookoutEngine e, java.util.function.IntToDoubleFunction dist, int hits, int spacing,
                         boolean attackerExempt, boolean victimTracked, boolean victimStopsEarly, boolean projectileIds) {
        reachRun(e, dist, hits, spacing, 1, attackerExempt, victimTracked, victimStopsEarly, projectileIds);
    }

    static void reachRun(LookoutEngine e, java.util.function.IntToDoubleFunction dist, int hits, int spacing,
                         long firstHitTick, boolean attackerExempt, boolean victimTracked, boolean victimStopsEarly,
                         boolean projectileIds) {
        int done = 0;
        double d = dist.applyAsDouble(0);
        for (long t = 0; done < hits; t++) {
            WALL[0] += 50;
            e.tick(1, "steve", smp(t, 500, 0, -90f, attackerExempt, 0), FLAT);
            if (victimTracked && !(victimStopsEarly && t >= 5)) e.tick(2, "sam", smp(t, 500 + d, 0, 0f), FLAT);
            if (t >= firstHitTick && (t - firstHitTick) % spacing == 0) {
                d = dist.applyAsDouble(done);
                e.onDamage(2, 1, projectileIds ? 99 : 1, PA);
                done++;
            }
        }
    }

    static void reachFixtures() {
        pair("reach: just under the 4.6 bar (4.59) stays silent, just over (4.61) does not", "reach",
                e -> reachRun(e, i -> 4.59, 20, 1, false, true, false, false),
                e -> reachRun(e, i -> 4.61, 20, 1, false, true, false, false));
        pair("reach: four far hits sit at the buffer cap and stay silent", "reach",
                e -> reachRun(e, i -> 5.5, 4, 1, false, true, false, false),
                e -> reachRun(e, i -> 5.5, 5, 1, false, true, false, false));
        pair("reach: near hits between far hits drain the buffer (far,near x20)", "reach",
                e -> reachRun(e, i -> (i % 2 == 0) ? 5.5 : 3.0, 40, 1, false, true, false, false),
                e -> reachRun(e, i -> (i % 3 == 2) ? 3.0 : 5.5, 60, 1, false, true, false, false));
        pair("reach: far hits spaced 100 ticks apart decay away", "reach",
                e -> reachRun(e, i -> 5.5, 12, 100, false, true, false, false),
                e -> reachRun(e, i -> 5.5, 12, 10, false, true, false, false));
        pair("reach: creative or spectator attacker is exempt", "reach",
                e -> reachRun(e, i -> 6.0, 20, 1, true, true, false, false),
                e -> reachRun(e, i -> 6.0, 20, 1, false, true, false, false));
        pair("reach: untracked victim gives no evidence", "reach",
                e -> reachRun(e, i -> 6.0, 20, 1, false, false, false, false),
                e -> reachRun(e, i -> 6.0, 20, 1, false, true, false, false));
        // the victim stops being observed at tick 5 and every hit lands at tick 50+,
        // so its freshest observation is 45+ ticks old at hit time: past the 40-tick
        // drop in onDamage, no hit counts. red side keeps the victim observed.
        pair("reach: victim observations older than 40 ticks give no evidence", "reach",
                e -> reachRun(e, i -> 6.0, 12, 10, 50, false, true, true, false),
                e -> reachRun(e, i -> 6.0, 12, 10, 50, false, true, false, false));
        pair("reach: projectile-shaped damage (cause != direct id) is not a melee hit", "reach",
                e -> reachRun(e, i -> 6.0, 20, 1, false, true, false, true),
                e -> reachRun(e, i -> 6.0, 20, 1, false, true, false, false));
    }

    // ---------------- multiaura: attacker 1, victims 2 and 3 ----------------

    static void tickThree(LookoutEngine e, long t, boolean exempt) {
        WALL[0] += 50;
        e.tick(1, "steve", smp(t, 500, 0, -90f, exempt, 0), FLAT);
        e.tick(2, "sam", smp(t, 502.5, 0, 0f), FLAT);
        e.tick(3, "ivy", smp(t, 501, 1.5, 0f), FLAT);
    }

    /** a burst: both victims hit on the same tick. bursts happen at tick = first + k*gap, k < n. */
    static void bursts(LookoutEngine e, int n, int gap, boolean exempt) {
        long end = 5 + (long) n * gap + 5;
        for (long t = 0; t < end; t++) {
            tickThree(e, t, exempt);
            if (t >= 5 && (t - 5) % gap == 0 && (t - 5) / gap < n) {
                e.onDamage(2, 1, 1, PA);
                e.onDamage(3, 1, 1, PA);
            }
        }
    }

    static void multiFixtures() {
        pair("multiaura: bursts one tick past the 8-tick rapid gap never build a streak", "multiaura",
                e -> bursts(e, 12, 9, false),
                e -> bursts(e, 12, 8, false));
        pair("multiaura: a burst spread over two adjacent ticks counts once, spaced 12 apart", "multiaura",
                e -> splitBursts(e, 10, 12),
                e -> splitBursts(e, 10, 5));
        pair("multiaura: single victim hit every tick is not a multi-target hit", "multiaura",
                e -> { for (long t = 0; t < 60; t++) { tickThree(e, t, false); if (t >= 2) e.onDamage(2, 1, 1, PA); } },
                e -> { for (long t = 0; t < 60; t++) { tickThree(e, t, false); if (t >= 2) e.onDamage(t % 2 == 0 ? 2 : 3, 1, 1, PA); } });
        pair("multiaura: both victims hit on three consecutive ticks is one burst (every 20 ticks)", "multiaura",
                e -> longBursts(e, 10, 20),
                e -> bursts(e, 10, 4, false));
        pair("multiaura: creative or spectator attacker is exempt", "multiaura",
                e -> bursts(e, 6, 4, true),
                e -> bursts(e, 6, 4, false));
        pair("multiaura: streak resets after an alert (5 rapid bursts, one alert; 6 bursts, two)", "multiaura", 1,
                e -> bursts(e, 5, 4, false),
                2, e -> bursts(e, 6, 4, false));
    }

    /** each burst: both victims hit on three consecutive ticks. */
    static void longBursts(LookoutEngine e, int n, int gap) {
        long end = 5 + (long) n * gap + 5;
        for (long t = 0; t < end; t++) {
            tickThree(e, t, false);
            long k = t >= 5 ? (t - 5) / gap : -1;
            if (k >= 0 && k < n && (t - 5) % gap < 3) {
                e.onDamage(2, 1, 1, PA);
                e.onDamage(3, 1, 1, PA);
            }
        }
    }

    static void splitBursts(LookoutEngine e, int n, int gap) {
        long end = 5 + (long) n * gap + 5;
        for (long t = 0; t < end; t++) {
            tickThree(e, t, false);
            long k = t >= 5 ? (t - 5) / gap : -1;
            if (k >= 0 && k < n) {
                long r = (t - 5) % gap;
                if (r == 0) e.onDamage(2, 1, 1, PA);
                if (r == 1) e.onDamage(3, 1, 1, PA);
            }
        }
    }

    // ---------------- timer: attacker-free, player 1 sprinting along x at dPerTick for the whole run ----------------

    static void timerRun(LookoutEngine e, int ticks, double d, int msPerTick, boolean exempt, int speedLevel,
                         java.util.function.IntConsumer perTick) {
        double x = 500;
        for (int t = 0; t < ticks; t++) {
            WALL[0] += msPerTick;
            x += d;
            e.tick(1, "alex", smp(t, x, 0, -90f, exempt, speedLevel), FLAT);
            if (perTick != null) perTick.accept(t);
        }
    }

    /** 10 slow ticks, a tick-number jump (client stall), then n fast ticks at 50 ms. */
    static void gapRun(LookoutEngine e, int fastAfterGap) {
        double x = 500;
        long t = 0;
        for (int i = 0; i < 10; i++) { WALL[0] += 50; x += 0.1; e.tick(1, "alex", smp(t++, x, 0, -90f), FLAT); }
        t += 20; // tick jump: the evenly-paced run is broken
        for (int i = 0; i < fastAfterGap + 1; i++) { // +1: the first post-gap sample only clears the window
            WALL[0] += 50; x += 0.6;
            e.tick(1, "alex", smp(t++, x, 0, -90f), FLAT);
        }
    }

    static void timerFixtures() {
        pair("timer: a tick gap restarts the window (39 fast ticks after it stay silent)", "timer",
                e -> gapRun(e, 39),
                e -> gapRun(e, 40));
        pair("timer: repeated 600 ms client freezes keep the window from ever filling", "timer",
                e -> timerRun(e, 200, 0.6, 50, false, 0, t -> { if (t % 30 == 29) WALL[0] += 600; }),
                e -> timerRun(e, 200, 0.6, 50, false, 0, null));
        pair("timer: a 600 ms freeze resets the window even when the rest of the window reads as 19 tps", "timer",
                e -> timerRun(e, 200, 0.6, 36, false, 0, t -> { if (t % 30 == 29) WALL[0] += 600; }),
                e -> timerRun(e, 200, 0.6, 50, false, 0, null));
        pair("timer: slow local client (55 ms per tick, 18 tps) makes wall-clock math unreadable", "timer",
                e -> timerRun(e, 80, 0.6, 55, false, 0, null),
                e -> timerRun(e, 80, 0.6, 50, false, 0, null));
        pair("timer: catch-up client (45 ms per tick, 22 tps) is not real pacing", "timer",
                e -> timerRun(e, 80, 0.6, 45, false, 0, null),
                e -> timerRun(e, 80, 0.6, 50, false, 0, null));
        pair("timer: exactly 10.0 m/s on the wall clock is at the cap, not past it", "timer",
                e -> timerRun(e, 80, 0.5, 50, false, 0, null),
                e -> timerRun(e, 80, 0.55, 50, false, 0, null));
        pair("timer: knockback velocity packets every 15 ticks keep the run exempt", "timer",
                e -> timerRun(e, 100, 0.6, 50, false, 0, t -> { if (t % 15 == 0) e.onVelocity(1); }),
                e -> timerRun(e, 100, 0.6, 50, false, 0, null));
        pair("timer: creative or spectator player is exempt", "timer",
                e -> timerRun(e, 80, 0.6, 50, true, 0, null),
                e -> timerRun(e, 80, 0.6, 50, false, 0, null));
    }

    // ---------------- killaura facing: attacker 1 at origin with a chosen yaw, victim 2 at (vx, vz) ----------------

    static void facingRun(LookoutEngine e, float yaw, double vx, double vz, int hits, boolean exempt) {
        for (long t = 0; t < hits + 2; t++) {
            WALL[0] += 50;
            e.tick(1, "steve", smp(t, 0, 0, yaw, exempt, 0), FLAT);
            e.tick(2, "sam", smp(t, vx, vz, 0f), FLAT);
            if (t >= 1) e.onDamage(2, 1, 1, PA);
        }
    }

    /** victim at 2.5 blocks on the given bearing (degrees, same convention as the check) */
    static double bx(double bearing) { return -Math.sin(Math.toRadians(bearing)) * 2.5; }
    static double bz(double bearing) { return Math.cos(Math.toRadians(bearing)) * 2.5; }

    static void facingFixtures() {
        pair("killaura: yaw wraps across +-180 (yaw 175, victim bearing -175, 10 degrees off)", "killaura",
                e -> facingRun(e, 175f, bx(-175), bz(-175), 12, false),
                e -> facingRun(e, 175f, bx(0), bz(0), 12, false));
        pair("killaura: 54 degrees off stays inside the 55 degree tolerance", "killaura",
                e -> facingRun(e, 0f, bx(54), bz(54), 12, false),
                e -> facingRun(e, 0f, bx(56), bz(56), 12, false));
        pair("killaura: victim past the 4.6 range gate is left to reach, even facing away", "killaura",
                e -> facingRun(e, 0f, 0, -4.7, 12, false),
                e -> facingRun(e, 0f, 0, -4.5, 12, false));
        pair("killaura: creative or spectator attacker facing away is exempt", "killaura",
                e -> facingRun(e, 0f, 0, -2.5, 12, true),
                e -> facingRun(e, 0f, 0, -2.5, 12, false));
        pair("killaura: off, on, off, on facing hits drain the buffer (net zero)", "killaura",
                e -> alternatingFacing(e, 40, 2),
                e -> alternatingFacing(e, 60, 3));
    }

    /** attacker yaw 0 faces +z. victim alternates between ahead (+z) and behind (-z); every period-th hit is the facing one. */
    static void alternatingFacing(LookoutEngine e, int hits, int period) {
        for (long t = 0; t < hits + 2; t++) {
            WALL[0] += 50;
            boolean ahead = t % period == 0;
            e.tick(1, "steve", smp(t, 0, 0, 0f), FLAT);
            e.tick(2, "sam", smp(t, 0, ahead ? 2.5 : -2.5, 0f), FLAT);
            if (t >= 1) e.onDamage(2, 1, 1, PA);
        }
    }

    public static void main(String[] args) {
        reachFixtures();
        multiFixtures();
        timerFixtures();
        facingFixtures();
        System.out.println(guards + " guard fixtures ok, " + reds + " of " + guards + " red-controlled");
        if (reds != guards) throw new AssertionError("a guard has no red control");
        System.out.println("LookoutGuardSim: PASS");
    }
}
