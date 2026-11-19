import dev.lookout.core.AlertSink;
import dev.lookout.core.LookoutConfig;
import dev.lookout.core.LookoutEngine;
import dev.lookout.core.ObservedSample;
import dev.lookout.core.WorldProbe;
import java.util.ArrayList;
import java.util.List;

/**
 * Headless lookout proof: scripted legit movement must produce zero alerts, scripted
 * cheat movement must alert. Pure synthetic samples against dev.lookout.core only.
 */
public class LookoutSim {

    static List<String> alerts;
    static final AlertSink SINK = (id, name, check, message) -> alerts.add(check + " " + name + ": " + message);

    // flat world: solid at y<=63; water surface at y=63 for x in [100,120); stairs x in [200,220);
    // ice x in [300,320); frosted (solid+liquid) x in [400,420)
    static final WorldProbe PROBE = new WorldProbe() {
        public boolean isSolidAt(double x, double y, double z) {
            int bx = (int) Math.floor(x), by = (int) Math.floor(y);
            if (by > 63) return false;
            if (by == 63 && bx >= 100 && bx < 120) return false; // open water surface
            return true;
        }
        public boolean isLiquidAt(double x, double y, double z) {
            int bx = (int) Math.floor(x), by = (int) Math.floor(y);
            return by == 63 && ((bx >= 100 && bx < 120) || (bx >= 400 && bx < 420));
        }
        public boolean isClimbableAt(double x, double y, double z) { return false; }
        public boolean isStairLikeAt(double x, double y, double z) {
            int bx = (int) Math.floor(x);
            return bx >= 200 && bx < 220 && Math.floor(y) >= 63 && Math.floor(y) <= 75;
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

    // a legit jump arc from ground level, ballistic, 3-tick-lerped like a real client sees it
    static void jumpArc(LookoutEngine e, int id, long t0, double x, double jumpVel, int groundBefore, int groundAfter) {
        double y = 64;
        for (int i = 0; i < groundBefore; i++) { x += 0.28; e.tick(id, "alex", mk(t0++, x, y, 0, true), PROBE); }
        List<Double> raw = new ArrayList<>();
        double dy = jumpVel, yy = y;
        while (true) {
            yy += dy;
            raw.add(yy);
            dy = (dy - 0.08) * 0.98;
            if (dy < 0 && yy <= 64) break;
        }
        // client lerp: each rendered tick moves a third of the way to the target
        double lerped = y;
        List<Double> seen = new ArrayList<>();
        for (double target : raw) {
            for (int k = 0; k < 3; k++) {
                lerped = lerped + (target - lerped) / 3.0;
                seen.add(lerped);
            }
        }
        for (double sy : seen) { x += 0.31; e.tick(id, "alex", mk(t0++, x, sy, 0, sy <= 64.01), PROBE); }
        for (int i = 0; i < groundAfter; i++) { x += 0.28; e.tick(id, "alex", mk(t0++, x, 64, 0, true), PROBE); }
    }

    public static void main(String[] args) {
        // ---- legit scenarios: zero alerts expected ----
        legit("flat sprint", e -> {
            long t = 0; double x = 0;
            for (int i = 0; i < 60; i++) { x += 0.281; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
        });
        legit("bhop chain", e -> {
            long t = 0; double x = 0;
            for (int j = 0; j < 4; j++) { jumpArc(e, 1, t, x, 0.42, 1, 0); t += 40; x += 6; }
            for (int i = 0; i < 30; i++) { x += 0.28; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
        });
        legit("jump boost 2 arcs", e -> {
            long t = 0; double x = 0;
            for (int j = 0; j < 2; j++) {
                double y = 64; x += 0.28; e.tick(1, "alex", mk(t++, x, y, 0, true), PROBE);
                double dy = 0.42 + 0.1 * 2; List<Double> raw = new ArrayList<>();
                double yy = y;
                while (true) { yy += dy; raw.add(yy); dy = (dy - 0.08) * 0.98; if (dy < 0 && yy <= 64) break; }
                for (double sy : raw) {
                    x += 0.2;
                    e.tick(1, "alex", new ObservedSample(t++, x, sy, 0, sy <= 64.01, true, false, false, false, false,
                            false, false, false, 2, 0, false, false), PROBE);
                }
            }
            for (int i = 0; i < 20; i++) { x += 0.28; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
        });
        legit("stair climb", e -> {
            long t = 0; double y = 64;
            for (int i = 0; i < 10; i++) { y += 0.5; e.tick(1, "alex", mk(t++, 200 + i, y, 0, true), PROBE); }
        });
        legit("water swim", e -> {
            long t = 0;
            for (int i = 0; i < 30; i++) {
                e.tick(1, "alex", new ObservedSample(t++, 100 + i * 0.2, 63.4, 0, false, false, false, true, false, false,
                        true, false, false, 0, 0, false, false), PROBE);
            }
        });
        legit("boat on water", e -> {
            long t = 0;
            for (int i = 0; i < 30; i++) {
                e.tick(1, "alex", new ObservedSample(t++, 100 + i * 0.3, 63.9, 0, false, false, false, false, false, true,
                        false, false, false, 0, 0, false, false), PROBE);
            }
        });
        legit("frost walker", e -> {
            long t = 0;
            for (int i = 0; i < 30; i++) { e.tick(1, "alex", mk(t++, 400 + i * 0.28, 64, 0, true), PROBE); }
        });
        legit("ice slide", e -> {
            long t = 0; double x = 300; double v = 0.45;
            for (int i = 0; i < 40; i++) { x += v; v *= 0.99; e.tick(1, "alex", mk(t++, x, 64, 0, true), PROBE); }
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
        legit("human totem restock", e -> {
            long t = 0;
            for (int i = 0; i < 10; i++) e.tick(1, "alex", mk(t++, 0, 64, 0, true), PROBE);
            e.onTotemPop(1);
            for (int i = 0; i < 6; i++) e.tick(1, "alex", mk(t++, 0, 64, 0, true), PROBE);
            e.onTotemEquip(1);
            for (int i = 0; i < 10; i++) e.tick(1, "alex", mk(t++, 0, 64, 0, true), PROBE);
        });
        legit("creative flyby", e -> {
            long t = 0;
            for (int i = 0; i < 60; i++) {
                e.tick(1, "alex", new ObservedSample(t++, i * 0.5, 80, 0, false, false, false, false, false, false,
                        false, false, false, 0, 0, false, true), PROBE);
            }
        });

        // ---- cheat scenarios: an alert expected ----
        cheat("hover", "flight", e -> {
            long t = 0;
            for (int i = 0; i < 10; i++) e.tick(1, "steve", mk(t++, 0, 64, 0, true), PROBE);
            double y = 66;
            for (int i = 0; i < 60; i++) e.tick(1, "steve", mk(t++, 0, y, 0, false), PROBE);
        });
        cheat("rising fly", "flight", e -> {
            long t = 0;
            for (int i = 0; i < 5; i++) e.tick(1, "steve", mk(t++, 0, 64, 0, true), PROBE);
            double y = 64;
            for (int i = 0; i < 40; i++) { y += 0.2; e.tick(1, "steve", mk(t++, 0, y, 0, false), PROBE); }
        });
        cheat("jesus run", "jesus", e -> {
            long t = 0;
            for (int i = 0; i < 30; i++) e.tick(1, "steve", mk(t++, 100 + i * 0.4, 64, 0, false), PROBE);
        });
        cheat("one-block step spam", "step", e -> {
            long t = 0; double y = 64;
            for (int j = 0; j < 4; j++) {
                for (int i = 0; i < 3; i++) { y += 0.34; e.tick(1, "steve", mk(t++, j * 10, y, 0, true), PROBE); }
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
        if (!alerts.isEmpty()) throw new AssertionError("FAIL legit [" + name + "] flagged: " + alerts);
        System.out.println("legit ok: " + name);
    }

    static void cheat(String name, String check, java.util.function.Consumer<LookoutEngine> scenario) {
        LookoutEngine e = fresh();
        scenario.accept(e);
        if (alerts.stream().noneMatch(a -> a.startsWith(check + " "))) {
            throw new AssertionError("FAIL cheat [" + name + "] produced no " + check + " alert (got: " + alerts + ")");
        }
        System.out.println("cheat caught: " + name + " -> " + alerts);
    }
}
