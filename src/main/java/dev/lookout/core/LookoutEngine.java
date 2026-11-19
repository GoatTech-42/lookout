package dev.lookout.core;

import dev.lookout.core.checks.FlightCheck;
import dev.lookout.core.checks.JesusCheck;
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
        AlertSink gated = gatedSink();
        for (LookoutCheck check : checksFor(entityId)) check.onSwing(p, p.prev.tick(), config, gated);
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
