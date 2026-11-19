import dev.lookout.core.AlertSink;
import dev.lookout.core.LookoutConfig;
import dev.lookout.core.LookoutEngine;
import dev.lookout.core.ObservedSample;
import dev.lookout.core.WorldProbe;
import java.util.ArrayList;
import java.util.List;

/**
 * Headless lookout proof. These are synthetic branch/exemption fixtures, not recorded
 * traffic: each scenario scripts one behavior against dev.lookout.core to prove a
 * branch stays quiet (legit) or fires (cheat). Real-client validation is a separate,
 * stated gap.
 */
public class LookoutSim {

    static List<String> alerts;
    static final AlertSink SINK = (id, name, check, message) -> alerts.add(check + " " + name + ": " + message);
    static final long[] WALL = {0}; // fake wall clock, advanced by fixtures that care

    // world model: solid at y<=63 everywhere except open water (x in [100,120), block y=63
    // is liquid, not solid). frost-walker ice (x in [400,420)) is SOLID, not liquid (the
    // real behavior: frosted ice replaces the water). staircase at x in [200,220): the
    // stair block sits at 63 + (x-200)/2, solid beneath it. ice sheet x in [300,320).
    static final WorldProbe PROBE = new WorldProbe() {
        public boolean isSolidAt(double x, double y, double z) {
            int bx = (int) Math.floor(x), by = (int) Math.floor(y);
            if (bx >= 200 && bx < 220) return by <= 63 + (bx - 200) / 2;
            if (by > 63) return false;
            if (by == 63 && bx >= 100 && bx < 120) return false; // open water surface
            if (by == 63 && bx >= 0 && bx < 4 && (int) Math.floor(z) >= 0 && (int) Math.floor(z) < 4)
                return bx >= 1 && (int) Math.floor(z) >= 1; // diagonal-corner pool: only cell (1,1) solid
            return true;
        }
        public boolean isLiquidAt(double x, double y, double z) {
            int bx = (int) Math.floor(x), by = (int) Math.floor(y), bz = (int) Math.floor(z);
            if (by == 63 && bx >= 0 && bx < 4 && bz >= 0 && bz < 4) return !(bx >= 1 && bz >= 1);
            return by == 63 && bx >= 100 && bx < 120; // frosted-ice region is NOT liquid
        }
        public boolean isClimbableAt(double x, double y, double z) { return false; }
        public boolean isStairLikeAt(double x, double y, double z) {
            int bx = (int) Math.floor(x), by = (int) Math.floor(y);
            return bx >= 200 && bx < 220 && by == 63 + (bx - 200) / 2;
        }
        public boolean isSlipperyAt(double x, double y, double z) {
            int bx = (int) Math.floor(x);
            return bx >= 300 && bx < 320 && Math.floor(y) == 63;
        }
        public boolean isSoulGroundAt(double x, double y, double z) {
            int bx = (int) Math.floor(x);
            return bx >= 600 && bx < 620 && Math.floor(y) == 63;
        }
    };

    static ObservedSample mk(long t, double x, double y, double z, boolean ground) {
        return mk(t, x, y, z, ground, 0f, 0f);
    }

    static ObservedSample mk(long t, double x, double y, double z, boolean ground, float yaw, float pitch) {
        return new ObservedSample(t, x, y, z, ground, true, false, false, false, false,
                false, false, false, 0, 0, false, false, yaw, pitch, false);
    }

    /** using-item sample (eating / blocking / drawing); yaw/pitch zero, on ground */
    static ObservedSample mkUsing(long t, double x, double y, double z, boolean bow) {
        return new ObservedSample(t, x, y, z, true, true, false, false, true, false,
                false, false, false, 0, 0, false, false, 0f, 0f, bow);
    }

    static ObservedSample mkGlide(long t, double x, double y, double z) {
        return new ObservedSample(t, x, y, z, false, false, true, false, false, false,
                false, false, false, 0, 0, false, false, 0f, 0f, false);
    }

    static LookoutEngine fresh() {
        alerts = new ArrayList<>();
        return new LookoutEngine(new LookoutConfig(), SINK);
    }

    static LookoutEngine fresh(LookoutConfig cfg) {
        alerts = new ArrayList<>();
        WALL[0] = 0;
        return new LookoutEngine(cfg, SINK, () -> WALL[0]);
    }

    /** engine with the opt-in discontinuity finding turned on */
    static LookoutEngine freshTJ() { return freshTJ(new LookoutConfig()); }
    static LookoutEngine freshTJ(LookoutConfig cfg) { cfg.teleportJumps = true; return fresh(cfg); }
    static void legitTJ(String name, java.util.function.Consumer<LookoutEngine> scenario) {
        LookoutEngine e = freshTJ();
        scenario.accept(e);
        if (!alerts.isEmpty()) throw new AssertionError("FAIL legit fixture [" + name + "] flagged: " + alerts);
        System.out.println("legit ok: " + name + " (teleport finding on)");
    }

    /** timer-isolated engine: speed off so only the timer path is exercised */
    static LookoutEngine freshTimerOnly(double minLocalTps) {
        alerts = new ArrayList<>();
        WALL[0] = 0;
        LookoutConfig cfg = new LookoutConfig();
        cfg.speed = false;
        cfg.timer = true;
        cfg.timerMinLocalTps = minLocalTps;
        return new LookoutEngine(cfg, SINK, () -> WALL[0]);
    }

    /** one shared overcap laggy scenario: 11 m/s real-time at a 10 tps local rate */
    static void laggyOvercap(LookoutEngine e) {
        long t = 0; double x = 500;
        for (int i = 0; i < 45; i++) { x += 1.1; WALL[0] += 100; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
    }

    /** join relocation: the server moves a freshly-appeared player with no S2C
     *  teleport event and the client lerps the jump over ~3 ticks (lane-run FP) */
    static void scnJoinRelocate(LookoutEngine e) {
        long t = 0; double x = 0;
        for (int i = 0; i < 6; i++) { x += 0.28; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
        e.tick(1, "alex", mk(t++, x + 4.7, 64, 0, true), PROBE);
        e.tick(1, "alex", mk(t++, x + 9.4, 64, 0, true), PROBE);
        e.tick(1, "alex", mk(t++, x + 14.1, 64, 0, true), PROBE);
        x += 14.1;
        for (int i = 0; i < 30; i++) { x += 0.28; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
    }

    /** the original step-spam trace, kept unchanged as its own regression case:
     *  the 10-block x jumps between groups are incidental fixture artifacts */
    static void scnStepSpamOriginal(LookoutEngine e) {
        long t = 0; double y = 64;
        for (int j = 0; j < 4; j++) {
            for (int i = 0; i < 3; i++) { y += 0.34; e.tick(1, "steve", mk(t++, j * 10 + 50, y, 0, true), PROBE); }
        }
    }

    // ---- QA AMEND 6.2 adversarial probes: unsignaled >3-block relocation jumps
    // (continuous, periodic, and established-player blinks) layered on each of
    // speed / step / flight. Every one must still alert. ----
    static void scnSpeedContinuous(LookoutEngine e) {
        long t = 0; double x = 0;
        for (int i = 0; i < 240; i++) { x += 4.1; e.tick(1, "steve", mk(t++, x, 64, 0, true), PROBE); }
    }
    static void scnSpeedPeriodic(LookoutEngine e) {
        long t = 0; double x = 0;
        for (int i = 0; i < 240; i++) { x += 0.45 + (i % 4 == 3 ? 4.1 : 0); e.tick(1, "steve", mk(t++, x, 64, 0, true), PROBE); }
    }
    static void scnSpeedBlinks(LookoutEngine e) {
        long t = 0; double x = 0;
        for (int i = 0; i < 100; i++) { x += 0.28; e.tick(1, "steve", mk(t++, x, 64, 0, true), PROBE); }
        for (int i = 0; i < 140; i++) { x += 0.45 + (i % 30 == 29 ? 4.1 : 0); e.tick(1, "steve", mk(t++, x, 64, 0, true), PROBE); }
    }
    static void scnStepContinuous(LookoutEngine e) {
        long t = 0; double y = 64, x = 0;
        for (int i = 0; i < 240; i++) { y += 0.34; x += 4.1; e.tick(1, "steve", mk(t++, x, y, 0, true), PROBE); }
    }
    static void scnStepPeriodic(LookoutEngine e) {
        long t = 0; double y = 64, x = 0;
        for (int i = 0; i < 240; i++) { y += 0.34; x += 0.3 + (i % 4 == 3 ? 4.1 : 0); e.tick(1, "steve", mk(t++, x, y, 0, true), PROBE); }
    }
    static void scnStepBlinks(LookoutEngine e) {
        long t = 0; double y = 64, x = 0;
        for (int i = 0; i < 100; i++) { x += 0.28; e.tick(1, "steve", mk(t++, x, 64, 0, true), PROBE); }
        for (int i = 0; i < 140; i++) { y += 0.34; x += 0.3 + (i % 30 == 29 ? 4.1 : 0); e.tick(1, "steve", mk(t++, x, y, 0, true), PROBE); }
    }
    static void scnFlightContinuous(LookoutEngine e) {
        long t = 0; double x = 0;
        for (int i = 0; i < 10; i++) e.tick(1, "steve", mk(t++, x, 64, 0, true), PROBE);
        for (int i = 0; i < 230; i++) { x += 4.1; e.tick(1, "steve", mk(t++, x, 66, 0, false), PROBE); }
    }
    static void scnFlightPeriodic(LookoutEngine e) {
        long t = 0; double x = 0;
        for (int i = 0; i < 10; i++) e.tick(1, "steve", mk(t++, x, 64, 0, true), PROBE);
        for (int i = 0; i < 230; i++) { x += (i % 4 == 3 ? 4.1 : 0.0); e.tick(1, "steve", mk(t++, x, 66, 0, false), PROBE); }
    }
    static void scnFlightBlinks(LookoutEngine e) {
        long t = 0; double x = 0;
        for (int i = 0; i < 100; i++) { x += 0.28; e.tick(1, "steve", mk(t++, x, 64, 0, true), PROBE); }
        for (int i = 0; i < 140; i++) { x += (i % 30 == 29 ? 4.1 : 0.0); e.tick(1, "steve", mk(t++, x, 66, 0, false), PROBE); }
    }

    /** frozen QA 7.7.3 probe: three 4.7 relocations at ticks 10/30/50, all inside
     *  the default join grace */
    static void scnGraceJumps(LookoutEngine e) {
        long t = 0; double x = 0;
        for (int i = 0; i < 70; i++) {
            x += (t == 10 || t == 30 || t == 50) ? 4.7 : 0.28;
            e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE);
        }
    }

    /** QA 9.0 caveat probe: 100 established ticks, optional knockback at tick 120,
     *  then 3.5-block jumps at ticks 132/135/138 */
    static void scnLateKnockback(LookoutEngine e, boolean withVelocity) {
        long t = 0; double x = 0;
        for (int i = 0; i < 120; i++) { x += 0.28; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
        if (withVelocity) e.onVelocity(1);
        for (int i = 0; i < 40; i++) {
            x += (t == 132 || t == 135 || t == 138) ? 3.5 : 0.28;
            e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE);
        }
    }

    /** QA 9.0 gap probe: 120 established ticks (PAST the 100-tick join grace),
     *  then 3 relocations of `dist` blocks, each optionally signaled by an S2C
     *  teleport the tick before. Unsignaled, the 3 jumps fill the counter. */
    static void scnSignaledRelocations(LookoutEngine e, double dist, boolean signaled) {
        long t = 0; double x = 0;
        for (int i = 0; i < 120; i++) { x += 0.28; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
        for (int j = 0; j < 3; j++) {
            if (signaled) e.onTeleport(1);
            x += dist;
            e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE);
            for (int i = 0; i < 20; i++) { x += 0.28; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
        }
    }

    /** same past-grace discipline for the rapid repeated-teleport trace */
    static void scnRepeatedTeleports(LookoutEngine e, boolean signaled) {
        long t = 0; double x = 0;
        for (int i = 0; i < 120; i++) { x += 0.28; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
        for (int j = 0; j < 5; j++) {
            if (signaled) e.onTeleport(1);
            x += 8;
            e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE);
            for (int i = 0; i < 3; i++) { x += 0.28; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
        }
    }

    static void tickAll(LookoutEngine e, long t) {
        e.tick(1, "alex", mk(t, 500, 64, 0, true, -90f, 0f), PROBE);
        e.tick(2, "sam", mk(t, 502.5, 64, 0, true), PROBE);
        e.tick(3, "ivy", mk(t, 501, 64, 1.5, true), PROBE);
        e.tick(4, "max", mk(t, 501, 64, -1.5, true), PROBE);
        e.tick(5, "ria", mk(t, 503, 64, 0.5, true), PROBE);
    }

    /** engine with the opt-in stage-3 checks (timer, multi-aura) turned on */
    static LookoutEngine freshT3() {
        LookoutConfig cfg = new LookoutConfig();
        cfg.timer = true;
        cfg.multiAura = true;
        return fresh(cfg);
    }

    static void legitT3(String name, java.util.function.Consumer<LookoutEngine> scenario) {
        LookoutEngine e = freshT3();
        scenario.accept(e);
        if (!alerts.isEmpty()) throw new AssertionError("FAIL legit fixture [" + name + "] flagged: " + alerts);
        System.out.println("legit ok: " + name + " (opt-in checks on)");
    }

    static void cheatT3(String name, String check, java.util.function.Consumer<LookoutEngine> scenario) {
        LookoutEngine e = freshT3();
        scenario.accept(e);
        if (alerts.stream().noneMatch(a -> a.startsWith(check + " "))) {
            throw new AssertionError("FAIL cheat fixture [" + name + "] produced no " + check + " alert (got: " + alerts + ")");
        }
        System.out.println("cheat caught: " + name + " -> " + alerts);
    }

    /** engine with the opt-in killaura-facing and reach checks turned on */
    static LookoutEngine freshKR() {
        LookoutConfig cfg = new LookoutConfig();
        cfg.killaura = true;
        cfg.reach = true;
        return fresh(cfg);
    }

    static void legitKR(String name, java.util.function.Consumer<LookoutEngine> scenario) {
        LookoutEngine e = freshKR();
        scenario.accept(e);
        if (!alerts.isEmpty()) throw new AssertionError("FAIL legit fixture [" + name + "] flagged: " + alerts);
        System.out.println("legit ok: " + name + " (opt-in checks on)");
    }

    static void cheatKR(String name, String check, java.util.function.Consumer<LookoutEngine> scenario) {
        LookoutEngine e = freshKR();
        scenario.accept(e);
        if (alerts.stream().noneMatch(a -> a.startsWith(check + " "))) {
            throw new AssertionError("FAIL cheat fixture [" + name + "] produced no " + check + " alert (got: " + alerts + ")");
        }
        System.out.println("cheat caught: " + name + " -> " + alerts);
    }

    /**
     * A legit jump arc the way the client really sees it: the server sends one position
     * packet per tick and the client lerps a third of the way toward the newest packet
     * target each rendered tick (packet cadence, not one target per 3 ticks).
     */
    static long jumpArc(LookoutEngine e, int id, String name, long t0, double x0, double jumpVel, int jumpBoost) {
        double y = 64, x = x0, dy = jumpVel, lerped = 64;
        long t = t0;
        while (true) {
            y += dy;
            dy = (dy - 0.08) * 0.98;
            if (dy < 0 && y <= 64) break;
            lerped = lerped + (y - lerped) / 3.0;
            x += 0.31;
            e.tick(id, name, new ObservedSample(t++, x, lerped, 0, false, true, false, false, false, false,
                    false, false, false, jumpBoost, 0, false, false, 0f, 0f, false), PROBE);
        }
        return t;
    }

    public static void main(String[] args) {
        // ---- legit fixtures: zero alerts expected ----
        legit("flat sprint", e -> {
            long t = 0; double x = 0;
            for (int i = 0; i < 60; i++) { x += 0.281; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
        });
        legit("bhop chain (packet cadence)", e -> {
            long t = 0; double x = 0;
            for (int j = 0; j < 4; j++) {
                e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE);
                t = jumpArc(e, 1, "alex", t, x, 0.42, 0);
                x += 4;
            }
            for (int i = 0; i < 30; i++) { x += 0.28; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
        });
        legit("jump boost 2 arcs", e -> {
            long t = 0; double x = 0;
            e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE);
            t = jumpArc(e, 1, "alex", t, x, 0.42 + 0.1 * 2, 2);
            for (int i = 0; i < 20; i++) { x += 0.28; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
        });
        legit("long drop from y300", e -> {
            long t = 0; double y = 300, dy = 0;
            for (int i = 0; i < 300 && y > 64; i++) {
                y += dy; dy = (dy - 0.08) * 0.98;
                e.tick(1, "alex", mk(t++, 0, Math.max(y, 64), 0, y <= 64.01), PROBE);
            }
            for (int i = 0; i < 20; i++) e.tick(1, "alex", mk(t++, 0, 64, 0, true), PROBE);
        });
        legit("stair climb (real staircase)", e -> {
            long t = 0;
            for (int i = 0; i < 10; i++) {
                double x = 200 + i;
                double y = 63 + (double) (((int) x - 200) / 2) + 1.0; // standing on the stair tops
                e.tick(1, "alex", mk(t++, x, y, 0, true), PROBE);
            }
        });
        legit("water swim", e -> {
            long t = 0;
            for (int i = 0; i < 30; i++) {
                e.tick(1, "alex", new ObservedSample(t++, 100.5 + i * 0.2, 63.4, 0, false, false, false, true, false, false,
                        true, false, false, 0, 0, false, false, 0f, 0f, false), PROBE);
            }
        });
        legit("boat on water", e -> {
            long t = 0;
            for (int i = 0; i < 30; i++) {
                e.tick(1, "alex", new ObservedSample(t++, 100.5 + i * 0.3, 63.9, 0, false, false, false, false, false, true,
                        false, false, false, 0, 0, false, false, 0f, 0f, false), PROBE);
            }
        });
        legit("frost walker (solid ice, no liquid)", e -> {
            long t = 0;
            for (int i = 0; i < 30; i++) { e.tick(1, "alex", mk(t++, 400 + i * 0.28, 64, 0, true), PROBE); }
        });
        legit("water edge: diagonal corner over solid", e -> {
            // center and all four cardinal probes are over open water; only the (+x,+z)
            // diagonal corner of the footprint touches the solid cell. must stay quiet.
            long t = 0;
            for (int i = 0; i < 30; i++) {
                double x = (i % 2 == 0) ? 0.80 : 0.85; // oscillate so there is real speed
                e.tick(1, "alex", mk(t++, x, 64, 0.80, true), PROBE);
            }
        });
        legit("low glide over water", e -> {
            long t = 0;
            for (int i = 0; i < 30; i++) {
                e.tick(1, "alex", new ObservedSample(t++, 100.5 + i * 0.5, 64.2, 0, false, false, true, false, false, false,
                        false, false, false, 0, 0, false, false, 0f, 0f, false), PROBE);
            }
        });
        legit("ice slide", e -> {
            long t = 0; double x = 300; double v = 0.45;
            for (int i = 0; i < 40; i++) { x += v; v *= 0.99; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
        });
        legit("ice exit transition", e -> {
            long t = 0; double x = 310; double v = 0.5;
            for (int i = 0; i < 30; i++) { x += v; v = Math.max(0.28, v * 0.97); e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
        });
        legit("knockback recovery", e -> {
            long t = 0; double x = 0;
            for (int i = 0; i < 10; i++) { x += 0.28; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
            e.onVelocity(1);
            double y = 64, dy = 0.4;
            for (int i = 0; i < 12; i++) {
                y += dy; dy = (dy - 0.08) * 0.98; x += 0.5;
                e.tick(1, "alex", mk(t++, x, y, 0, false), PROBE);
            }
            for (int i = 0; i < 20; i++) { x += 0.28; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
        });
        legit("teleport lag correction", e -> {
            long t = 0; double x = 0;
            for (int i = 0; i < 10; i++) { x += 0.28; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
            e.onTeleport(1);
            e.tick(1, "alex", mk(t++, x + 30, 70, 0, false), PROBE);
            for (int i = 0; i < 10; i++) { x += 0.28; e.tick(1, "alex", mk(t++, x + 30, 70, 0, true), PROBE); }
        });
        legit("observed player join: big first deltas without teleport packet", LookoutSim::scnJoinRelocate);

        // ---- discontinuity-counter exemptions (QA interim on ff0e2c1): samples the
        // movement checks exempt must not feed the jump counter either ----
        legitTJ("creative relocation jumps exempt from the counter", e -> {
            long t = 0; double x = 0;
            for (int i = 0; i < 240; i++) {
                x += 0.45 + (i % 4 == 3 ? 3.1 : 0);
                e.tick(1, "alex", new ObservedSample(t++, x, 64, 0, false, false, false, false, false, false,
                        false, false, false, 0, 0, false, true, 0f, 0f, false), PROBE);
            }
        });
        legitTJ("vehicle relocation jumps exempt from the counter", e -> {
            long t = 0; double x = 0;
            for (int i = 0; i < 240; i++) {
                x += 0.45 + (i % 4 == 3 ? 3.1 : 0);
                e.tick(1, "alex", new ObservedSample(t++, x, 64, 0, true, true, false, false, false, true,
                        false, false, false, 0, 0, false, false, 0f, 0f, false), PROBE);
            }
        });
        legitTJ("glide relocation jumps exempt from the counter", e -> {
            long t = 0; double x = 0;
            for (int i = 0; i < 240; i++) { x += 1.0 + (i % 4 == 3 ? 3.1 : 0); e.tick(1, "alex", mkGlide(t++, x, 100, 0), PROBE); }
        });
        legitTJ("dive and pullout relocation jumps exempt from the counter", e -> {
            long t = 0; double x = 0;
            for (int i = 0; i < 120; i++) { // underwater burst moves (the dive)
                x += 0.3 + (i % 4 == 3 ? 3.1 : 0);
                e.tick(1, "alex", new ObservedSample(t++, x, 60, 0, false, false, false, false, false, false,
                        true, false, false, 0, 0, false, false, 0f, 0f, false), PROBE);
            }
            for (int i = 0; i < 60; i++) { x += 0.28; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); } // pullout
        });
        legitTJ("delayed knockback inside the 20-tick velocity window", e -> {
            // the hit lands, the victim keeps walking, and the knockback displacement
            // arrives 12-18 ticks late: still the hit's movement, never counted
            long t = 0; double x = 0;
            for (int i = 0; i < 10; i++) { x += 0.28; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
            e.onVelocity(1);
            for (int j = 0; j < 3; j++) {
                for (int i = 0; i < 4; i++) { x += 0.28; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
                x += 3.5; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE);
            }
        });
        // legit relocation sources: pearls, chorus fruit, portals and dimension
        // changes all arrive with an S2C teleport (signaled), so they never count;
        // respawns reappear as a fresh entity; knockback arrives with velocity;
        // bed/vehicle dismounts move under 3 blocks.
        // QA 9.0 gap: these traces establish 120 ticks BEFORE the relocation, so
        // they sit past the 100-tick join grace and the silence isolates the
        // signal; each is paired below with the same trace minus the teleport
        // packet, which must count.
        legitTJ("signaled relocation lands without counting (pearl/chorus-like, past grace)",
                e -> scnSignaledRelocations(e, 8, true));
        legitTJ("dimension-change relocation (signaled sync, past grace) silent",
                e -> scnSignaledRelocations(e, 1000, true));
        legitTJ("repeated signaled teleports never build the counter (past grace)",
                e -> scnRepeatedTeleports(e, true));
        {
            // paired mutations: the same past-grace traces with the S2C teleport
            // removed. Speed also fires on these (real lateral jumps), so the
            // pairs assert only the teleport finding.
            LookoutEngine e = freshTJ();
            scnSignaledRelocations(e, 8, false);
            if (alerts.stream().noneMatch(a -> a.startsWith("teleport ")))
                throw new AssertionError("FAIL paired mutation: unsignaled pearl-like jumps past grace did not count (got: " + alerts + ")");
            System.out.println("gate mutation red as expected (pearl-like jumps without the teleport packet): " + alerts);
            e = freshTJ();
            scnSignaledRelocations(e, 1000, false);
            if (alerts.stream().noneMatch(a -> a.startsWith("teleport ")))
                throw new AssertionError("FAIL paired mutation: unsignaled dimension-like jumps past grace did not count (got: " + alerts + ")");
            System.out.println("gate mutation red as expected (dimension-like jumps without the teleport packet): " + alerts);
            e = freshTJ();
            scnRepeatedTeleports(e, false);
            if (alerts.stream().noneMatch(a -> a.startsWith("teleport ")))
                throw new AssertionError("FAIL paired mutation: repeated unsignaled teleports past grace did not count (got: " + alerts + ")");
            System.out.println("gate mutation red as expected (repeated teleports without the packet): " + alerts);
        }
        legit("packet bunching on flat ground", e -> {
            long t = 0; double x = 0;
            for (int r = 0; r < 5; r++) {
                for (int i = 0; i < 3; i++) e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); // frozen
                x += 1.4; // then one bunched delta
                e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE);
            }
        });
        legit("stalled samples then resume (stale window)", e -> {
            long t = 0; double x = 0;
            for (int i = 0; i < 8; i++) { x += 0.5; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
            t += 100; // no samples for 100 ticks
            for (int i = 0; i < 8; i++) { x += 0.5; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
        });
        legit("human totem restock", e -> {
            long t = 0;
            for (int i = 0; i < 10; i++) e.tick(1, "alex", mk(t++, 0, 64, 0, true), PROBE);
            e.onTotemPop(1);
            for (int i = 0; i < 6; i++) e.tick(1, "alex", mk(t++, 0, 64, 0, true), PROBE);
            e.onTotemEquip(1);
            for (int i = 0; i < 10; i++) e.tick(1, "alex", mk(t++, 0, 64, 0, true), PROBE);
        });
        legit("duplicate equipment notifications, one pop", e -> {
            long t = 0;
            for (int i = 0; i < 5; i++) e.tick(1, "alex", mk(t++, 0, 64, 0, true), PROBE);
            e.onTotemPop(1);
            e.tick(1, "alex", mk(t++, 0, 64, 0, true), PROBE);
            e.onTotemEquip(1); // the restock
            e.onTotemEquip(1); // the same restock re-notified by a duplicate packet
            e.onTotemEquip(1);
            for (int i = 0; i < 10; i++) e.tick(1, "alex", mk(t++, 0, 64, 0, true), PROBE);
        });
        legit("two instant pops sharing one restock", e -> {
            long t = 0;
            for (int i = 0; i < 5; i++) e.tick(1, "alex", mk(t++, 0, 64, 0, true), PROBE);
            e.onTotemPop(1);
            e.tick(1, "alex", mk(t++, 0, 64, 0, true), PROBE);
            e.onTotemPop(1);
            e.tick(1, "alex", mk(t++, 0, 64, 0, true), PROBE);
            e.onTotemEquip(1); // one restock consumed by one pop; the other pop never restocked
            for (int i = 0; i < 10; i++) e.tick(1, "alex", mk(t++, 0, 64, 0, true), PROBE);
        });
        legit("creative flyby", e -> {
            long t = 0;
            for (int i = 0; i < 60; i++) {
                e.tick(1, "alex", new ObservedSample(t++, i * 0.5, 80, 0, false, false, false, false, false, false,
                        false, false, false, 0, 0, false, true, 0f, 0f, false), PROBE);
            }
        });

        // ---- stage 2 (combat) legit fixtures: zero alerts expected ----
        // combat fixtures use two players: id 1 acts, id 2 stands nearby as the target.
        // killaura-facing and reach are opt-in; the KR fixtures run with them enabled.
        legitKR("bystander at 2.5 blocks, facing it", e -> {
            // mining or mob fighting next to a bystander: swings alone prove nothing
            long t = 0;
            for (int j = 0; j < 6; j++) {
                for (int i = 0; i < 4; i++) {
                    e.tick(1, "alex", mk(t, 500, 64, 0, true, -90f, 0f), PROBE);
                    e.tick(2, "sam", mk(t, 502.5, 64, 0, true), PROBE);
                    t++;
                }
                e.onSwing(1);
            }
        });
        legitKR("bystander at 5.2 blocks, aligned", e -> {
            long t = 0;
            for (int j = 0; j < 6; j++) {
                for (int i = 0; i < 4; i++) {
                    e.tick(1, "alex", mk(t, 500, 64, 0, true, -90f, 0f), PROBE);
                    e.tick(2, "sam", mk(t, 505.2, 64, 0, true), PROBE);
                    t++;
                }
                e.onSwing(1);
            }
        });
        legitKR("mob fight facing away, no player damage", e -> {
            long t = 0;
            for (int j = 0; j < 6; j++) {
                for (int i = 0; i < 4; i++) {
                    e.tick(1, "alex", mk(t, 500, 64, 0, true, 60f, 0f), PROBE);
                    e.tick(2, "sam", mk(t, 502.5, 64, 0, true), PROBE);
                    t++;
                }
                e.onSwing(1);
            }
        });
        legitKR("aligned hits at 2.8 blocks", e -> {
            long t = 0;
            for (int j = 0; j < 6; j++) {
                for (int i = 0; i < 4; i++) {
                    e.tick(1, "alex", mk(t, 500, 64, 0, true, -90f, 0f), PROBE);
                    e.tick(2, "sam", mk(t, 502.8, 64, 0, true), PROBE);
                    t++;
                }
                e.onSwing(1);
                e.onDamage(2, 1, 1, "minecraft:player_attack"); // alex really hit sam, facing him, in range
            }
        });
        legitKR("fifty-degree flick hits", e -> {
            long t = 0;
            for (int j = 0; j < 6; j++) {
                for (int i = 0; i < 4; i++) {
                    e.tick(1, "alex", mk(t, 500, 64, 0, true, -90f + ((j % 2 == 0) ? 50f : -50f), 0f), PROBE);
                    e.tick(2, "sam", mk(t, 502.5, 64, 0, true), PROBE);
                    t++;
                }
                e.onSwing(1);
                e.onDamage(2, 1, 1, "minecraft:player_attack");
            }
        });
        legitKR("thorns damage is not a hit", e -> {
            // a thorns wearer damaging their attacker reuses the single-entity shape:
            // equal cause and direct ids. only the damage type tells it from melee.
            long t = 0;
            for (int j = 0; j < 6; j++) {
                for (int i = 0; i < 4; i++) {
                    e.tick(1, "alex", mk(t, 500, 64, 0, true, 10f, 0f), PROBE);
                    e.tick(2, "sam", mk(t, 502.5, 64, 0, true), PROBE);
                    t++;
                }
                e.onDamage(2, 1, 1, "minecraft:thorns"); // typed thorns: must not count as a hit
            }
        });
        legitKR("mob_attack typed damage is not a player hit", e -> {
            long t = 0;
            for (int j = 0; j < 6; j++) {
                for (int i = 0; i < 4; i++) {
                    e.tick(1, "alex", mk(t, 500, 64, 0, true, 10f, 0f), PROBE);
                    e.tick(2, "sam", mk(t, 502.5, 64, 0, true), PROBE);
                    t++;
                }
                e.onDamage(2, 1, 1, "minecraft:mob_attack");
            }
        });
        legitKR("fall damage is not a hit", e -> {
            long t = 0;
            for (int i = 0; i < 6; i++) {
                e.tick(1, "alex", mk(t, 500, 64, 0, true), PROBE);
                e.tick(2, "sam", mk(t, 502.5, 64, 0, true), PROBE);
                t++;
                e.onDamage(2, -1, -1, "minecraft:fall"); // world damage: no attacker ids
            }
        });
        legit("full-draw bow cadence", e -> {
            long t = 0;
            for (int j = 0; j < 3; j++) {
                for (int i = 0; i < 24; i++) e.tick(1, "alex", mkUsing(t++, 500, 64, 0, true), PROBE);
                e.tick(1, "alex", mk(t++, 500, 64, 0, true), PROBE); // release
                e.onProjectileSpawn(500.5, 65.4, 0, 3.0, 1000 + j * 10, 1); // full power after a full draw
                for (int i = 0; i < 6; i++) e.tick(1, "alex", mk(t++, 500, 64, 0, true), PROBE);
            }
        });
        legit("weak arrow spam", e -> {
            long t = 0;
            for (int j = 0; j < 8; j++) {
                for (int i = 0; i < 4; i++) e.tick(1, "alex", mkUsing(t++, 500, 64, 0, true), PROBE);
                e.tick(1, "alex", mk(t++, 500, 64, 0, true), PROBE);
                e.onProjectileSpawn(500.5, 65.4, 0, 1.0, 2000 + j * 10, 1); // spam-click arrows are slow
            }
        });
        legit("crossbow instant fire", e -> {
            long t = 0;
            for (int j = 0; j < 4; j++) {
                for (int i = 0; i < 10; i++) e.tick(1, "alex", mk(t++, 500, 64, 0, true), PROBE); // loading: not a bow draw
                e.onProjectileSpawn(500.5, 65.4, 0, 3.2, 3000 + j * 10, 1); // crossbows fire fast arrows instantly
            }
        });
        legit("eating walk", e -> {
            long t = 0; double x = 500;
            for (int i = 0; i < 5; i++) { x += 0.28; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
            for (int i = 0; i < 35; i++) { x += 0.05; e.tick(1, "alex", mkUsing(t++, x, 64, 0, false), PROBE); }
        });
        legit("sprint then eat, coasting in the grace", e -> {
            long t = 0; double x = 500;
            for (int i = 0; i < 10; i++) { x += 0.28; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
            for (int i = 0; i < 8; i++) { x += 0.15; e.tick(1, "alex", mkUsing(t++, x, 64, 0, false), PROBE); }
            for (int i = 0; i < 20; i++) { x += 0.05; e.tick(1, "alex", mkUsing(t++, x, 64, 0, false), PROBE); }
        });
        legit("eating while sliding on ice", e -> {
            long t = 0; double x = 300;
            for (int i = 0; i < 6; i++) { x += 0.45; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
            for (int i = 0; i < 20; i++) { x += 0.3; e.tick(1, "alex", mkUsing(t++, Math.min(x, 319.5), 64, 0, false), PROBE); }
        });
        legit("mixed shooters near a bow drawer", e -> {
            // alex does honest full draws; full-speed arrows from a skeleton (untracked
            // owner) and the local player (untracked) land right next to him
            long t = 0;
            for (int j = 0; j < 3; j++) {
                for (int i = 0; i < 24; i++) e.tick(1, "alex", mkUsing(t++, 500, 64, 0, true), PROBE);
                e.tick(1, "alex", mk(t++, 500, 64, 0, true), PROBE);
                e.onProjectileSpawn(500.5, 65.4, 0, 3.0, 5000 + j * 10, 1); // his own honest arrow
                e.onProjectileSpawn(501, 65, 0.5, 3.1, 5001 + j * 10, 999); // skeleton
                e.onProjectileSpawn(499.5, 65, 0, 3.2, 5002 + j * 10, 77); // local player
            }
        });
        legit("one draw, three full-speed spawns", e -> {
            // one 2-tick draw delivering three fast arrows on the same tick (plus a
            // re-sent spawn id) must count as ONE release, never three
            long t = 0;
            for (int i = 0; i < 2; i++) e.tick(1, "alex", mkUsing(t++, 500, 64, 0, true), PROBE);
            e.tick(1, "alex", mk(t++, 500, 64, 0, true), PROBE);
            e.onProjectileSpawn(500.5, 65.4, 0, 3.0, 6000, 1);
            e.onProjectileSpawn(500.5, 65.4, 0, 3.0, 6001, 1);
            e.onProjectileSpawn(500.5, 65.4, 0, 3.0, 6002, 1);
            e.onProjectileSpawn(500.5, 65.4, 0, 3.0, 6000, 1); // duplicate packet
            for (int i = 0; i < 10; i++) e.tick(1, "alex", mk(t++, 500, 64, 0, true), PROBE);
        });
        legit("eating while hopping", e -> {
            long t = 0; double x = 500;
            for (int i = 0; i < 30; i++) {
                boolean ground = i % 2 == 0; // airborne ticks keep hop momentum
                e.tick(1, "alex", new ObservedSample(t++, x += 0.28, 64 + (ground ? 0 : 0.4), 0, ground, true,
                        false, false, true, false, false, false, false, 0, 0, false, false, 0f, 0f, false), PROBE);
            }
        });
        legit("eating with speed 2", e -> {
            long t = 0; double x = 500;
            for (int i = 0; i < 30; i++) {
                x += 0.11;
                e.tick(1, "alex", new ObservedSample(t++, x, 64, 0, true, true,
                        false, false, true, false, false, false, false, 0, 2, false, false, 0f, 0f, false), PROBE);
            }
        });
        legit("elytra cruise", e -> {
            long t = 0; double x = 500;
            for (int i = 0; i < 45; i++) { x += 1.5; e.tick(1, "alex", mkGlide(t++, x, 100, 0), PROBE); }
        });
        legit("elytra dive burst", e -> {
            long t = 0; double x = 500; double y = 200;
            for (int i = 0; i < 25; i++) { x += 2.8; y -= 0.6; e.tick(1, "alex", mkGlide(t++, x, y, 0), PROBE); }
        });
        legit("firework-boosted level glide", e -> {
            long t = 0; double x = 500;
            for (int i = 0; i < 45; i++) { x += 1.9; e.tick(1, "alex", mkGlide(t++, x, 100, 0), PROBE); }
        });


        // ---- stage 3 legit fixtures: zero alerts expected ----
        legitT3("full-rate sprint on the wall clock", e -> {
            long t = 0; double x = 500;
            for (int i = 0; i < 60; i++) { x += 0.28; WALL[0] += 50; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
        });
        legitT3("speed 2 sprint on the wall clock", e -> {
            long t = 0; double x = 500;
            for (int i = 0; i < 60; i++) {
                x += 0.39; WALL[0] += 50;
                e.tick(1, "alex", new ObservedSample(t++, x, 64, 0, true, true, false, false, false, false,
                        false, false, false, 0, 2, false, false, 0f, 0f, false), PROBE);
            }
        });
        {
            // the SAME overcap laggy window, run twice with only the gate changed:
            // normal 19 tps floor -> silent; floored to 0 -> timer alert. speed is
            // disabled so only the timer path is exercised.
            laggyOvercap(freshTimerOnly(19.0));
            if (!alerts.isEmpty()) throw new AssertionError("FAIL gated laggy overcap flagged: " + alerts);
            System.out.println("legit ok: overcap window silent behind the 19 tps gate (timer isolated)");
            laggyOvercap(freshTimerOnly(0.0));
            if (alerts.stream().noneMatch(a -> a.startsWith("timer ")))
                throw new AssertionError("FAIL gate mutation: same window did not go red with the gate off (got: " + alerts + ")");
            System.out.println("gate mutation red as expected: " + alerts);
        }
        {
            // overcap sequence (24 m/s real-time, no effect): the 2s wall pause
            // resets the window. combined protection: the tps bounds may also
            // suppress this window, so this is not proof of gap rejection alone
            LookoutEngine e = freshTimerOnly(19.0);
            long t = 0; double x = 500;
            for (int i = 0; i < 50; i++) {
                x += 1.2;
                WALL[0] += (i == 20) ? 2000 : 50;
                e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE);
            }
            if (!alerts.isEmpty()) throw new AssertionError("FAIL freeze gap-rejection flagged: " + alerts);
            System.out.println("legit ok: local client freeze mid-window, overcap sequence rejected (timer isolated)");
        }
        {
            // overcap sequence (24 m/s, no effect) with 10 ticks unobserved in the
            // middle: the window must not straddle the hole. combined protection:
            // the tps bounds may also suppress it, as with the freeze case
            LookoutEngine e = freshTimerOnly(19.0);
            long t = 0; double x = 500;
            for (int i = 0; i < 30; i++) { x += 1.2; WALL[0] += 50; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
            t += 10; WALL[0] += 500; // player left render; position froze while unobserved
            for (int i = 0; i < 30; i++) { x += 1.2; WALL[0] += 50; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
            if (!alerts.isEmpty()) throw new AssertionError("FAIL sample-gap rejection flagged: " + alerts);
            System.out.println("legit ok: sample gap rejects an overcap window (timer isolated)");
        }
        legitT3("speed 5 sprint past the bare cap", e -> {
            // 11 m/s real-time: over the bare 10 m/s line, but the strongest effect
            // in the window scales the cap to fit. SpeedCheck stays quiet on it too.
            long t = 0; double x = 500;
            for (int i = 0; i < 60; i++) {
                x += 0.55; WALL[0] += 50;
                e.tick(1, "alex", new ObservedSample(t++, x, 64, 0, true, true, false, false, false, false,
                        false, false, false, 0, 5, false, false, 0f, 0f, false), PROBE);
            }
        });
        legitT3("soul speed on soul ground", e -> {
            long t = 0; double x = 600;
            for (int i = 0; i < 50; i++) {
                x += 0.6; WALL[0] += 50;
                e.tick(1, "alex", new ObservedSample(t++, Math.min(x, 619.5), 64, 0, true, true, false, false, false, false,
                        false, false, false, 0, 0, true, false, 0f, 0f, false), PROBE);
            }
        });
        legitT3("speed effect expires mid-window", e -> {
            long t = 0; double x = 500;
            for (int i = 0; i < 25; i++) {
                x += 0.55; WALL[0] += 50;
                e.tick(1, "alex", new ObservedSample(t++, x, 64, 0, true, true, false, false, false, false,
                        false, false, false, 0, 5, false, false, 0f, 0f, false), PROBE);
            }
            for (int i = 0; i < 25; i++) { x += 0.39; WALL[0] += 50; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
        });
        legitT3("soul ground to normal ground transition", e -> {
            long t = 0; double x = 600;
            for (int i = 0; i < 20; i++) {
                x += 0.6; WALL[0] += 50;
                e.tick(1, "alex", new ObservedSample(t++, Math.min(x, 619.5), 64, 0, true, true, false, false, false, false,
                        false, false, false, 0, 0, true, false, 0f, 0f, false), PROBE);
            }
            for (int i = 0; i < 50; i++) { x += 0.28; WALL[0] += 50; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
        });
        legitT3("single-target duel", e -> {
            long t = 0;
            for (int j = 0; j < 6; j++) {
                for (int i = 0; i < 12; i++) {
                    e.tick(1, "alex", mk(t, 500, 64, 0, true, -90f, 0f), PROBE);
                    e.tick(2, "sam", mk(t, 502.5, 64, 0, true), PROBE);
                    e.tick(3, "ivy", mk(t, 495, 64, 0, true), PROBE);
                    t++;
                }
                e.onDamage(2, 1, 1, "minecraft:player_attack"); // always just sam
            }
        });
        legitT3("sweep doubles at a legit cadence", e -> {
            // vanilla sweep can hit two players in one tick, but only with a charged
            // cooldown: seconds apart, not ticks
            long t = 0;
            for (int j = 0; j < 4; j++) {
                for (int i = 0; i < 15; i++) {
                    tickAll(e, t);
                    t++;
                }
                e.onDamage(2, 1, 1, "minecraft:player_attack");
                e.onDamage(3, 1, 1, "minecraft:player_attack");
            }
        });
        legitT3("one 4-victim sweep burst", e -> {
            // a single big sweep: four victims on one tick. must count as ONE burst,
            // not a streak
            long t = 0;
            for (int i = 0; i < 10; i++) { tickAll(e, t); t++; }
            e.onDamage(2, 1, 1, "minecraft:player_attack");
            e.onDamage(3, 1, 1, "minecraft:player_attack");
            e.onDamage(4, 1, 1, "minecraft:player_attack");
            e.onDamage(5, 1, 1, "minecraft:player_attack");
            for (int i = 0; i < 10; i++) { tickAll(e, t); t++; }
        });
        legitT3("repeated hits inside one burst", e -> {
            // the same burst re-notified (duplicate + a third victim a tick later):
            // still one burst
            long t = 0;
            for (int i = 0; i < 10; i++) { tickAll(e, t); t++; }
            e.onDamage(2, 1, 1, "minecraft:player_attack");
            e.onDamage(3, 1, 1, "minecraft:player_attack");
            tickAll(e, t); t++;
            e.onDamage(2, 1, 1, "minecraft:player_attack"); // same victim again
            e.onDamage(4, 1, 1, "minecraft:player_attack"); // one more victim
            for (int i = 0; i < 10; i++) { tickAll(e, t); t++; }
        });
        legitT3("charged sweeps with growing victim counts", e -> {
            // 2, then 3, then 4 victims, 20 ticks apart: bigger sweeps are still
            // just sweeps
            long t = 0;
            int[] counts = {2, 3, 4};
            for (int c : counts) {
                for (int i = 0; i < 20; i++) { tickAll(e, t); t++; }
                for (int v = 2; v < 2 + c; v++) e.onDamage(v, 1, 1, "minecraft:player_attack");
            }
        });
        legitT3("rapid bursts broken by a long gap", e -> {
            // bursts at +0,+3, then a 40-tick gap, then +0,+3: the gap resets the
            // cadence count, so the second pair never alerts
            long t = 0;
            for (int half = 0; half < 2; half++) {
                for (int i = 0; i < (half == 0 ? 0 : 40); i++) { tickAll(e, t); t++; }
                for (int j = 0; j < 2; j++) {
                    for (int i = 0; i < 3; i++) { tickAll(e, t); t++; }
                    e.onDamage(2, 1, 1, "minecraft:player_attack");
                    e.onDamage(3, 1, 1, "minecraft:player_attack");
                }
            }
            for (int i = 0; i < 10; i++) { tickAll(e, t); t++; }
        });
        legitT3("one double hit", e -> {
            long t = 0;
            for (int i = 0; i < 10; i++) {
                e.tick(1, "alex", mk(t, 500, 64, 0, true, -90f, 0f), PROBE);
                e.tick(2, "sam", mk(t, 502.5, 64, 0, true), PROBE);
                e.tick(3, "ivy", mk(t, 501, 64, 1.5, true), PROBE);
                t++;
            }
            e.onDamage(2, 1, 1, "minecraft:player_attack");
            e.onDamage(3, 1, 1, "minecraft:player_attack");
        });

        // reconnect: clear() must forget seen projectile ids, or the next connection's
        // reused entity ids get their arrows silently dropped
        {
            alerts = new ArrayList<>();
            LookoutEngine e = new LookoutEngine(new LookoutConfig(), SINK);
            long t = 0;
            for (int j = 0; j < 3; j++) {
                for (int i = 0; i < 2; i++) e.tick(1, "steve", mkUsing(t++, 500, 64, 0, true), PROBE);
                e.tick(1, "steve", mk(t++, 500, 64, 0, true), PROBE);
                e.onProjectileSpawn(500.5, 65.4, 0, 3.0, 7000 + j, 1);
            }
            if (alerts.stream().noneMatch(a -> a.startsWith("fastbow ")))
                throw new AssertionError("FAIL reconnect fixture: first fastbow burst never alerted");
            e.clear();
            t = 0;
            for (int j = 0; j < 3; j++) {
                for (int i = 0; i < 2; i++) e.tick(1, "steve", mkUsing(t++, 500, 64, 0, true), PROBE);
                e.tick(1, "steve", mk(t++, 500, 64, 0, true), PROBE);
                e.onProjectileSpawn(500.5, 65.4, 0, 3.0, 7000 + j, 1); // same ids, fresh connection
            }
            long fastbowAlerts = alerts.stream().filter(a -> a.startsWith("fastbow ")).count();
            if (fastbowAlerts != 2)
                throw new AssertionError("FAIL reconnect fixture: expected fastbow to fire again after clear(), got " + alerts);
            System.out.println("legit ok: projectile ids re-counted after reconnect");
        }

        // disabled engine: total silence even under cheat traffic
        {
            alerts = new ArrayList<>();
            LookoutConfig cfg = new LookoutConfig();
            cfg.enabled = false;
            LookoutEngine e = new LookoutEngine(cfg, SINK);
            long t = 0;
            for (int i = 0; i < 40; i++) e.tick(1, "steve", mk(t++, 0, 70, 0, false), PROBE);
            e.onTotemPop(1);
            e.onTotemEquip(1);
            e.onVelocity(1);
            e.onTeleport(1);
            e.onSwing(1);
            if (!alerts.isEmpty()) throw new AssertionError("FAIL disabled engine alerted: " + alerts);
            System.out.println("legit ok: disabled engine stays silent");
        }

        // alert cooldown: six instant pairs back to back must collapse to one alert
        {
            alerts = new ArrayList<>();
            LookoutEngine e = new LookoutEngine(new LookoutConfig(), SINK);
            long t = 0;
            for (int j = 0; j < 6; j++) {
                for (int i = 0; i < 5; i++) e.tick(1, "steve", mk(t++, 0, 64, 0, true), PROBE);
                e.onTotemPop(1);
                e.tick(1, "steve", mk(t++, 0, 64, 0, true), PROBE);
                e.onTotemEquip(1);
            }
            if (alerts.size() != 1) throw new AssertionError("FAIL cooldown: expected exactly 1 alert, got " + alerts);
            System.out.println("legit ok: cooldown collapses repeat alerts -> " + alerts);
        }

        // ---- cheat fixtures: an alert expected ----
        cheat("hover", "flight", e -> {
            long t = 0;
            for (int i = 0; i < 10; i++) e.tick(1, "steve", mk(t++, 0, 64, 0, true), PROBE);
            for (int i = 0; i < 60; i++) e.tick(1, "steve", mk(t++, 0, 66, 0, false), PROBE);
        });
        cheat("rising fly", "flight", e -> {
            long t = 0;
            for (int i = 0; i < 5; i++) e.tick(1, "steve", mk(t++, 0, 64, 0, true), PROBE);
            double y = 64;
            for (int i = 0; i < 40; i++) { y += 0.2; e.tick(1, "steve", mk(t++, 0, y, 0, false), PROBE); }
        });
        cheat("slow-fall fly past grace", "flight", e -> {
            long t = 0;
            for (int i = 0; i < 5; i++) e.tick(1, "steve", mk(t++, 0, 64, 0, true), PROBE);
            e.onTeleport(1); // a reposition, like a cheat toggling on mid-air
            double y = 200;
            for (int i = 0; i < 90; i++) { y -= 0.5; e.tick(1, "steve", mk(t++, 0, y, 0, false), PROBE); }
        });
        cheat("jesus run", "jesus", e -> {
            long t = 0;
            for (int i = 0; i < 30; i++) e.tick(1, "steve", mk(t++, 100.5 + i * 0.4, 64, 0, false), PROBE);
        });
        cheat("one-block step spam", "step", LookoutSim::scnStepSpamOriginal);
        cheat("one-block step spam, continuous lateral movement", "step", e -> {
            // same steps without the original trace's incidental 10-block x jumps:
            // real step spam moves continuously
            long t = 0; double y = 64; double x = 0;
            for (int j = 0; j < 4; j++) {
                for (int i = 0; i < 3; i++) { y += 0.34; x += 0.3; e.tick(1, "steve", mk(t++, x, y, 0, true), PROBE); }
            }
        });

        // ---- relocation-evasion fixtures (QA AMEND 6.2; expectations frozen before
        // the gate rework): continuous >4 moves, periodic >4 moves, and
        // established-player blinks, each across speed/step/flight, must alert ----
        cheat("evasion: continuous relocation moves (speed)", "speed", LookoutSim::scnSpeedContinuous);
        cheat("evasion: periodic relocation jumps (speed)", "speed", LookoutSim::scnSpeedPeriodic);
        cheat("evasion: established-player blinks (speed)", "speed", LookoutSim::scnSpeedBlinks);
        cheat("evasion: continuous relocation moves (step)", "step", LookoutSim::scnStepContinuous);
        cheat("evasion: periodic relocation jumps (step)", "step", LookoutSim::scnStepPeriodic);
        cheat("evasion: established-player blinks (step)", "step", LookoutSim::scnStepBlinks);
        cheat("evasion: continuous relocation moves (flight)", "flight", LookoutSim::scnFlightContinuous);
        cheat("evasion: periodic relocation jumps (flight)", "flight", LookoutSim::scnFlightPeriodic);
        cheat("evasion: established-player blinks (flight)", "flight", LookoutSim::scnFlightBlinks);
        {
            // the discontinuity finding itself: past the grace the jumps are
            // counted and surface as their own alert, next to the check they
            // failed to silence
            LookoutEngine e2 = freshTJ();
            scnSpeedPeriodic(e2);
            if (alerts.stream().noneMatch(a -> a.startsWith("teleport ")))
                throw new AssertionError("FAIL discontinuity finding: periodic relocation jumps produced no teleport alert (got: " + alerts + ")");
            if (alerts.stream().noneMatch(a -> a.startsWith("speed ")))
                throw new AssertionError("FAIL discontinuity finding: speed stayed silent through the jumps (got: " + alerts + ")");
            System.out.println("cheat caught: periodic relocation jumps -> finding + movement " + alerts);
        }
        {
            // gate mutation pairs, same scenarios with only the grace changed:
            // no grace -> the legit join relocation goes red; an unbounded grace is
            // the QA evasion itself -> the speed probe goes silent
            scnJoinRelocate(fresh());
            if (!alerts.isEmpty()) throw new AssertionError("FAIL join relocation flagged: " + alerts);
            LookoutConfig noGrace = new LookoutConfig();
            noGrace.joinGraceTicks = 0;
            scnJoinRelocate(fresh(noGrace));
            if (alerts.stream().noneMatch(a -> a.startsWith("speed ")))
                throw new AssertionError("FAIL gate mutation: join relocation did not go red with the grace off (got: " + alerts + ")");
            System.out.println("gate mutation red as expected (grace off): " + alerts);
            LookoutConfig openGrace = new LookoutConfig();
            openGrace.joinGraceTicks = Integer.MAX_VALUE;
            scnSpeedPeriodic(freshTJ(openGrace));
            if (alerts.stream().anyMatch(a -> a.startsWith("speed ") || a.startsWith("teleport ")))
                throw new AssertionError("FAIL mutation setup: unbounded grace did not silence the probe (got: " + alerts + ")");
            System.out.println("gate mutation red as expected (grace unbounded): speed probe and finding both silenced " + alerts);
        }
        {
            // frozen pair (QA 7.7.3): jumps inside the join grace are never counted.
            // three 4.7 relocations at ticks 10/30/50: silent with the grace on,
            // the finding fires with it off
            scnGraceJumps(freshTJ());
            if (!alerts.isEmpty()) throw new AssertionError("FAIL grace-window jumps counted: " + alerts);
            System.out.println("legit ok: relocations inside the join grace never count");
            LookoutConfig noGrace = new LookoutConfig();
            noGrace.joinGraceTicks = 0;
            scnGraceJumps(freshTJ(noGrace));
            if (alerts.stream().noneMatch(a -> a.startsWith("teleport ")))
                throw new AssertionError("FAIL frozen pair: grace-off jumps did not fire the finding (got: " + alerts + ")");
            System.out.println("gate mutation red as expected (grace off, frozen pair): " + alerts);
        }
        {
            // QA 9.0 caveat pair: the same after-grace jump triplet (132/135/138),
            // with and without a knockback at tick 120 (finding forced on). The
            // finding must stay silent behind the velocity window and fire without
            // it. Speed fires in both runs (the jumps are real lateral movement),
            // so the pair asserts only the finding.
            scnLateKnockback(freshTJ(), true);
            if (alerts.stream().anyMatch(a -> a.startsWith("teleport ")))
                throw new AssertionError("FAIL after-grace knockback counted: " + alerts);
            System.out.println("legit ok: after-grace jumps inside the velocity window never count (teleport finding on)");
            scnLateKnockback(freshTJ(), false);
            if (alerts.stream().noneMatch(a -> a.startsWith("teleport ")))
                throw new AssertionError("FAIL paired mutation: same jumps without velocity did not fire (got: " + alerts + ")");
            System.out.println("gate mutation red as expected (no velocity, after grace): " + alerts);
        }
        cheat("1.6x ground speed", "speed", e -> {
            long t = 0; double x = 0;
            for (int i = 0; i < 60; i++) { x += 0.45; e.tick(1, "steve", mk(t++, x, 64, 0, true), PROBE); }
        });
        cheat("instant totem x3", "totem", e -> {
            long t = 0;
            for (int j = 0; j < 3; j++) {
                for (int i = 0; i < 5; i++) e.tick(1, "steve", mk(t++, 0, 64, 0, true), PROBE);
                e.onTotemPop(1);
                e.tick(1, "steve", mk(t++, 0, 64, 0, true), PROBE);
                e.tick(1, "steve", mk(t++, 0, 64, 0, true), PROBE);
                e.onTotemEquip(1);
            }
        });


        cheatKR("hits landing while facing away", "killaura", e -> {
            long t = 0;
            for (int j = 0; j < 6; j++) {
                for (int i = 0; i < 4; i++) {
                    e.tick(1, "steve", mk(t, 500, 64, 0, true, 10f, 0f), PROBE); // ~100 off the bearing to sam
                    e.tick(2, "sam", mk(t, 502.5, 64, 0, true), PROBE);
                    t++;
                }
                e.onSwing(1);
                e.onDamage(2, 1, 1, "minecraft:player_attack"); // steve really damaged sam while looking elsewhere
            }
        });
        cheat("instant full-power arrows", "fastbow", e -> {
            long t = 0;
            for (int j = 0; j < 4; j++) {
                for (int i = 0; i < 2; i++) e.tick(1, "steve", mkUsing(t++, 500, 64, 0, true), PROBE);
                e.tick(1, "steve", mk(t++, 500, 64, 0, true), PROBE);
                e.onProjectileSpawn(500.5, 65.4, 0, 3.0, 4000 + j * 10, 1); // full power off a 2-tick draw
            }
        });
        cheat("eating at sprint speed", "noslow", e -> {
            long t = 0; double x = 500;
            for (int i = 0; i < 5; i++) { x += 0.28; e.tick(1, "steve", mk(t++, x, 64, 0, true), PROBE); }
            for (int i = 0; i < 30; i++) { x += 0.28; e.tick(1, "steve", mkUsing(t++, x, 64, 0, false), PROBE); }
        });
        cheatKR("hits landing from 5.2 blocks", "reach", e -> {
            long t = 0;
            for (int j = 0; j < 6; j++) {
                for (int i = 0; i < 4; i++) {
                    e.tick(1, "steve", mk(t, 500, 64, 0, true, -90f, 0f), PROBE);
                    e.tick(2, "sam", mk(t, 505.2, 64, 0, true), PROBE);
                    t++;
                }
                e.onSwing(1);
                e.onDamage(2, 1, 1, "minecraft:player_attack"); // steve really damaged sam from 5.2 blocks
            }
        });
        cheat("level elytra at 3.0", "elytra", e -> {
            long t = 0; double x = 500;
            for (int i = 0; i < 45; i++) { x += 3.0; e.tick(1, "steve", mkGlide(t++, x, 100, 0), PROBE); }
        });


        cheatT3("2x wall-clock speed (timer)", "timer", e -> {
            long t = 0; double x = 500;
            for (int i = 0; i < 60; i++) { x += 0.6; WALL[0] += 50; e.tick(1, "steve", mk(t++, x, 64, 0, true), PROBE); }
        });
        cheatT3("rapid multi-victim bursts", "multiaura", e -> {
            long t = 0;
            for (int j = 0; j < 4; j++) {
                for (int i = 0; i < 3; i++) {
                    e.tick(1, "steve", mk(t, 500, 64, 0, true, -90f, 0f), PROBE);
                    e.tick(2, "sam", mk(t, 502.5, 64, 0, true), PROBE);
                    e.tick(3, "ivy", mk(t, 501, 64, 1.5, true), PROBE);
                    t++;
                }
                e.onDamage(2, 1, 1, "minecraft:player_attack");
                e.onDamage(3, 1, 1, "minecraft:player_attack"); // two victims every 3 ticks
            }
        });

        System.out.println("LookoutSim: PASS");
    }

    static void legit(String name, java.util.function.Consumer<LookoutEngine> scenario) {
        LookoutEngine e = fresh();
        scenario.accept(e);
        if (!alerts.isEmpty()) throw new AssertionError("FAIL legit fixture [" + name + "] flagged: " + alerts);
        System.out.println("legit ok: " + name);
    }

    static void cheat(String name, String check, java.util.function.Consumer<LookoutEngine> scenario) {
        LookoutEngine e = fresh();
        scenario.accept(e);
        if (alerts.stream().noneMatch(a -> a.startsWith(check + " "))) {
            throw new AssertionError("FAIL cheat fixture [" + name + "] produced no " + check + " alert (got: " + alerts + ")");
        }
        System.out.println("cheat caught: " + name + " -> " + alerts);
    }
}
