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
        public boolean isSoulGroundAt(double x, double y, double z) { return false; }
    };

    static ObservedSample mk(long t, double x, double y, double z, boolean ground) {
        return new ObservedSample(t, x, y, z, ground, true, false, false, false, false,
                false, false, false, 0, 0, false, false);
    }

    static LookoutEngine fresh() {
        alerts = new ArrayList<>();
        return new LookoutEngine(new LookoutConfig(), SINK);
    }

    static LookoutEngine fresh(LookoutConfig cfg) {
        alerts = new ArrayList<>();
        return new LookoutEngine(cfg, SINK);
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
                    false, false, false, jumpBoost, 0, false, false), PROBE);
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
                        true, false, false, 0, 0, false, false), PROBE);
            }
        });
        legit("boat on water", e -> {
            long t = 0;
            for (int i = 0; i < 30; i++) {
                e.tick(1, "alex", new ObservedSample(t++, 100.5 + i * 0.3, 63.9, 0, false, false, false, false, false, true,
                        false, false, false, 0, 0, false, false), PROBE);
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
                        false, false, false, 0, 0, false, false), PROBE);
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
                        false, false, false, 0, 0, false, true), PROBE);
            }
        });
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
        cheat("one-block step spam", "step", e -> {
            long t = 0; double y = 64;
            for (int j = 0; j < 4; j++) {
                for (int i = 0; i < 3; i++) { y += 0.34; e.tick(1, "steve", mk(t++, j * 10 + 50, y, 0, true), PROBE); }
            }
        });
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
