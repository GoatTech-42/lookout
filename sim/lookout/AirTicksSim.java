import dev.lookout.core.*;
import java.util.*;

/** Extra tests for the NoSlow / FlightCheck shared ObservedPlayer.airTicks interaction (lookout 8998af6). Synthetic, core only.
 *  Cheater uses an item and moves 0.3 b/t on the ground (cap 0.1). Patterns: G<n> = n ground ticks, A<n> = n unsupported airborne ticks.
 *  T1 was RED-NOW at 8998af6 (noslow verdict depended on the flight toggle); it is a guard now that airTicks is engine-owned.
 *  GUARD tests pin today's intended behavior and should pass; mutants must make at least one fail. */
public class AirTicksSim {
    static List<String> alerts; static final long[] WALL = {0};
    static final WorldProbe FLAT = new WorldProbe() {
        public boolean isSolidAt(double x, double y, double z) { return Math.floor(y) <= 63; }
        public boolean isLiquidAt(double x, double y, double z) { return false; }
        public boolean isClimbableAt(double x, double y, double z) { return false; }
        public boolean isStairLikeAt(double x, double y, double z) { return false; }
        public boolean isSlipperyAt(double x, double y, double z) { return false; }
        public boolean isSoulGroundAt(double x, double y, double z) { return false; }
    };
    static LookoutEngine eng(boolean flight) {
        alerts = new ArrayList<>(); WALL[0] = 0;
        LookoutConfig c = new LookoutConfig();
        c.flight = flight; c.jesus = c.step = c.speed = c.totem = c.fastbow = c.elytra = false;
        c.killaura = c.reach = c.timer = c.multiAura = false; c.noslow = true;
        c.alertCooldownSeconds = 0;
        return new LookoutEngine(c, (id, n, chk, m) -> alerts.add(chk), () -> WALL[0]);
    }
    static ObservedSample s(long t, double x, double y, boolean ground, boolean slowFall) {
        return new ObservedSample(t, x, y, 0, ground, false, false, false, true, false, false, false, slowFall, 0, 0, false, false, 0f, 0f, false);
    }
    /** segs like "G14 A2" repeated cycles times after an optional prefix; returns noslow alert count */
    static long run(boolean flight, boolean slowFall, String prefix, String cycle, int cycles) {
        LookoutEngine e = eng(flight);
        long[] t = {0}; double[] x = {0};
        Runnable none = () -> {};
        List<String> segs = new ArrayList<>(Arrays.asList(prefix.isBlank() ? new String[0] : prefix.trim().split(" ")));
        for (int i = 0; i < cycles; i++) segs.addAll(Arrays.asList(cycle.trim().split(" ")));
        // 10 ground ticks first: all inside the use grace (10 ticks), so none count
        List<String> all = new ArrayList<>(); all.add("G10"); all.addAll(segs);
        for (String sg : all) {
            boolean ground = sg.charAt(0) == 'G'; int n = Integer.parseInt(sg.substring(1));
            for (int k = 0; k < n; k++) { x[0] += 0.3; WALL[0] += 50;
                e.tick(1, "steve", s(t[0]++, x[0], ground ? 64 : 66, ground, !ground && slowFall), FLAT); }
        }
        return alerts.stream().filter(a -> a.equals("noslow")).count();
    }

    /** hovering airborne ticks with flight on; optional velocity event after the first n ticks. returns flight alerts */
    static long hover(int before, boolean velocity, int exemptAir, int after) {
        LookoutEngine e = eng(true); long t = 0;
        for (int k = 0; k < before; k++) { WALL[0] += 50; e.tick(1, "steve", s(t++, 0, 66, false, false), FLAT); }
        if (velocity) e.onVelocity(1);
        for (int k = 0; k < exemptAir; k++) { WALL[0] += 50; e.tick(1, "steve", s(t++, 0, 66, false, false), FLAT); }
        for (int k = 0; k < after; k++) { WALL[0] += 50; e.tick(1, "steve", s(t++, 0, 66, false, false), FLAT); }
        return alerts.stream().filter(x -> x.equals("flight")).count();
    }
    /** hovering airborne ticks with flight on; an unsignaled 4-block relocation inside
     *  the join grace after the first n ticks. returns flight alerts */
    static long joinHover(int before, int after) {
        LookoutEngine e = eng(true); long t = 0; double x = 0;
        for (int k = 0; k < before; k++) { WALL[0] += 50; e.tick(1, "steve", s(t++, x, 66, false, false), FLAT); }
        x += 4.0; // unsignaled relocation, inside the 100-tick join grace
        WALL[0] += 50; e.tick(1, "steve", s(t++, x, 66, false, false), FLAT);
        for (int k = 0; k < after; k++) { WALL[0] += 50; e.tick(1, "steve", s(t++, x, 66, false, false), FLAT); }
        return alerts.stream().filter(a -> a.equals("flight")).count();
    }
    static int fails; static final List<String> failed = new ArrayList<>();
    static void check(String id, boolean expectPass, boolean ok, String detail) {
        String tag = ok ? "PASS" : "FAIL";
        System.out.println(tag + " " + id + (expectPass ? "" : " (RED-NOW: expected FAIL at 8998af6)") + " " + detail);
        if (expectPass && !ok) { fails++; failed.add(id); }
    }
    public static void main(String[] a) {
        // T1 RED-NOW: same cheat, flight check on vs off, noslow verdict must match
        long on = run(true, false, "", "G14 A2", 20), off = run(false, false, "", "G14 A2", 20);
        check("T1-noslow-independent-of-flight-toggle", true, on == off, "flight on=" + on + " off=" + off);
        // T2 GUARD: a 2-tick airborne run resets the streak (silent); a 1-tick flicker holds it (alerts)
        long run2 = run(true, false, "", "G14 A2", 20), flick = run(true, false, "", "G14 A1", 20);
        check("T2a-airborne-run-2-resets", true, run2 == 0, "variant=" + run2);
        check("T2b-lone-flicker-held", true, flick > 0, "control=" + flick);
        // T3 GUARD: flight-exempt airtime (slow falling) leaves airTicks at 0, so the noslow streak is held, unlike plain airtime
        long sf = run(true, true, "", "G14 A3", 20), plain = run(true, false, "", "G14 A3", 20);
        check("T3-flight-exempt-air-does-not-reset-noslow", true, sf > 0 && plain == 0, "slowfall=" + sf + " plain=" + plain);
        // T4 GUARD: a long air run, then ground, must clear airTicks so later 1-tick flickers are held again
        long afterLong = run(true, false, "A5 G2", "G12 A1", 20);
        check("T4-airTicks-cleared-by-ground-after-long-air", true, afterLong > 0, "alerts=" + afterLong);
        // T5 GUARD (flight side of the shared field): a flight-exempt window (knockback) clears airTicks, so hover time before it does not count after it
        long hv = hover(10, true, 20, 30), hc = hover(10, false, 20, 30), hlong = hover(0, false, 0, 70);
        check("T5-exempt-window-clears-airTicks", true, hv == 0 && hc > 0 && hlong > 0, "velocity=" + hv + " no-velocity-same-length=" + hc + " long-hover=" + hlong);
        // T6 GUARD: an unsignaled relocation inside the join grace zeroes airTicks, so
        // hover time before it does not count after it (kills the M8 join-grace hole)
        long jr = joinHover(29, 29), jc = joinHover(29, 45);
        check("T6-join-grace-relocation-zeroes-airTicks", true, jr == 0 && jc > 0, "reloc=" + jr + " control=" + jc);
        System.out.println(fails == 0 ? "SUITE: all guards pass" : "SUITE: guard failures " + failed);
        System.exit(fails == 0 ? 0 : 1);
    }
}
