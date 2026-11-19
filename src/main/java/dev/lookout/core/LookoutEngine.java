package dev.lookout.core;

import dev.lookout.core.checks.ElytraCheck;
import dev.lookout.core.checks.FacingCheck;
import dev.lookout.core.checks.FastBowCheck;
import dev.lookout.core.checks.FlightCheck;
import dev.lookout.core.checks.JesusCheck;
import dev.lookout.core.checks.NoSlowCheck;
import dev.lookout.core.checks.ReachCheck;
import dev.lookout.core.checks.LookoutCheck;
import dev.lookout.core.checks.SpeedCheck;
import dev.lookout.core.checks.StepCheck;
import dev.lookout.core.checks.TotemCheck;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Drives the per-player checks. Pure java; the glue feeds it samples and events. */
public class LookoutEngine {

    private final LookoutConfig config;
    private final AlertSink sink;
    private final Map<Integer, ObservedPlayer> players = new ConcurrentHashMap<>();
    private final Map<Integer, List<LookoutCheck>> checks = new ConcurrentHashMap<>();
    private final Map<String, Long> lastAlertAt = new ConcurrentHashMap<>();

    public LookoutEngine(LookoutConfig config, AlertSink sink) {
        this.config = config;
        this.sink = sink;
    }

    public LookoutConfig config() {
        return config;
    }

    private List<LookoutCheck> checksFor(int entityId) {
        return checks.computeIfAbsent(entityId, id -> {
            List<LookoutCheck> list = new ArrayList<>();
            if (config.flight) list.add(new FlightCheck());
            if (config.jesus) list.add(new JesusCheck());
            if (config.step) list.add(new StepCheck());
            if (config.speed) list.add(new SpeedCheck());
            if (config.totem) list.add(new TotemCheck());
            if (config.killaura) list.add(new FacingCheck());
            if (config.fastbow) list.add(new FastBowCheck());
            if (config.noslow) list.add(new NoSlowCheck());
            if (config.reach) list.add(new ReachCheck());
            if (config.elytra) list.add(new ElytraCheck());
            return list;
        });
    }

    private AlertSink gatedSink() {
        return (id, playerName, checkId, message) -> {
            String key = id + ":" + checkId;
            long now = System.currentTimeMillis();
            Long last = lastAlertAt.get(key);
            if (last != null && now - last < config.alertCooldownSeconds * 1000) return;
            lastAlertAt.put(key, now);
            sink.alert(id, playerName, checkId, message);
        };
    }

    public void tick(int entityId, String name, ObservedSample sample, WorldProbe probe) {
        if (!config.enabled) return;
        ObservedPlayer p = players.computeIfAbsent(entityId, ObservedPlayer::new);
        if (name != null) p.name = name;
        ObservedSample prev = p.prev;
        p.add(sample);
        AlertSink gated = gatedSink();
        for (LookoutCheck check : checksFor(entityId)) {
            check.onSample(p, sample, prev, probe, config, gated);
        }
    }

    public void onVelocity(int entityId) {
        if (!config.enabled) return;
        ObservedPlayer p = players.get(entityId);
        if (p != null && p.prev != null) p.lastVelocityTick = p.prev.tick();
    }

    public void onTeleport(int entityId) {
        if (!config.enabled) return;
        ObservedPlayer p = players.get(entityId);
        if (p != null && p.prev != null) {
            p.lastTeleportTick = p.prev.tick();
            p.airTicks = 0;
        }
    }

    public void onSwing(int entityId) {
        if (!config.enabled) return;
        ObservedPlayer p = players.get(entityId);
        if (p == null || p.prev == null) return;
        ObservedPlayer nearest = nearestOther(entityId, p.prev.tick());
        if (nearest != null) {
            p.lastSwingNearestDist = distance(p.prev, nearest.prev);
            p.lastSwingBearing = bearingDegrees(p.prev, nearest.prev);
        } else {
            p.lastSwingNearestDist = -1;
        }
        AlertSink gated = gatedSink();
        for (LookoutCheck check : checksFor(entityId)) check.onSwing(p, p.prev.tick(), config, gated);
    }

    /**
     * An arrow (or spectral arrow) just spawned. Attribute it to the nearest tracked
     * player seen drawing a bow in the last 15 ticks; unclaimed arrows are ignored.
     */
    public void onProjectileSpawn(double x, double y, double z, double speed) {
        if (!config.enabled) return;
        ObservedPlayer best = null;
        double bestDist = 16; // squared, 4 blocks
        for (ObservedPlayer p : players.values()) {
            if (p.prev == null) continue;
            if (!drewBowRecently(p, 15)) continue;
            double dx = p.prev.x() - x, dy = p.prev.y() + 1.4 - y, dz = p.prev.z() - z;
            double d = dx * dx + dy * dy + dz * dz;
            if (d < bestDist) { bestDist = d; best = p; }
        }
        if (best == null) return;
        best.lastProjectileSpeed = speed;
        best.lastProjectileTick = best.prev.tick();
        AlertSink gated = gatedSink();
        for (LookoutCheck check : checksFor(best.entityId)) check.onProjectile(best, best.prev.tick(), config, gated);
    }

    private boolean drewBowRecently(ObservedPlayer p, int withinTicks) {
        for (ObservedSample s : p.samples) {
            if (s.usingBow() && p.prev != null && p.prev.tick() - s.tick() <= withinTicks) return true;
        }
        return false;
    }

    private ObservedPlayer nearestOther(int entityId, long tick) {
        ObservedPlayer self = players.get(entityId);
        if (self == null || self.prev == null) return null;
        ObservedPlayer best = null;
        double bestDist = Double.MAX_VALUE;
        for (ObservedPlayer p : players.values()) {
            if (p.entityId == entityId || p.prev == null) continue;
            if (tick - p.prev.tick() > 40) continue; // stale observation
            double d = distance(self.prev, p.prev);
            if (d < bestDist) { bestDist = d; best = p; }
        }
        return best;
    }

    private static double distance(ObservedSample a, ObservedSample b) {
        return Math.sqrt((a.x() - b.x()) * (a.x() - b.x())
                + (a.y() - b.y()) * (a.y() - b.y())
                + (a.z() - b.z()) * (a.z() - b.z()));
    }

    /** MC yaw convention: 0 is south (+z), 90 is west (-x). */
    private static double bearingDegrees(ObservedSample from, ObservedSample to) {
        return Math.toDegrees(Math.atan2(-(to.x() - from.x()), to.z() - from.z()));
    }

    public void onTotemPop(int entityId) {
        if (!config.enabled) return;
        ObservedPlayer p = players.get(entityId);
        if (p == null || p.prev == null) return;
        p.lastTotemPopTick = p.prev.tick();
        AlertSink gated = gatedSink();
        for (LookoutCheck check : checksFor(entityId)) check.onTotemPop(p, p.prev.tick(), config, gated);
    }

    public void onTotemEquip(int entityId) {
        if (!config.enabled) return;
        ObservedPlayer p = players.get(entityId);
        if (p == null || p.prev == null) return;
        AlertSink gated = gatedSink();
        for (LookoutCheck check : checksFor(entityId)) check.onTotemEquip(p, p.prev.tick(), config, gated);
    }

    /** Drop players we have not seen in a while (left render range, changed world). */
    public void sweep(long currentTick) {
        Iterator<Map.Entry<Integer, ObservedPlayer>> it = players.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ObservedPlayer> e = it.next();
            ObservedPlayer p = e.getValue();
            if (p.prev == null || currentTick - p.prev.tick() > 200) {
                it.remove();
                checks.remove(e.getKey());
            }
        }
    }

    public void clear() {
        players.clear();
        checks.clear();
        lastAlertAt.clear();
    }
}
