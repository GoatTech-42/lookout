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
    private final java.util.Set<Integer> seenProjectileIds = new java.util.LinkedHashSet<>();

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
        AlertSink gated = gatedSink();
        for (LookoutCheck check : checksFor(entityId)) check.onSwing(p, p.prev.tick(), config, gated);
    }

    /**
     * A damage packet says causeId hurt victimId. Only melee counts here: when cause
     * and direct ids differ the damage came through a projectile or another carrier,
     * which facing and reach do not judge.
     */
    public void onDamage(int victimId, int causeId, int directId) {
        if (!config.enabled) return;
        if (causeId <= 0 || causeId != directId) return;
        ObservedPlayer attacker = players.get(causeId);
        ObservedPlayer victim = players.get(victimId);
        if (attacker == null || victim == null || attacker.prev == null || victim.prev == null) return;
        if (Math.abs(attacker.prev.tick() - victim.prev.tick()) > 40) return; // stale observation
        long tick = Math.max(attacker.prev.tick(), victim.prev.tick());
        AlertSink gated = gatedSink();
        for (LookoutCheck check : checksFor(causeId)) check.onHitEvidence(attacker, victim, tick, config, gated);
    }

    /**
     * An arrow (or spectral arrow) just spawned. The owner comes from the add-entity
     * packet's data field; arrows with an unknown or untracked owner are skipped, so
     * bystanders are never blamed for someone else's shot. Projectile ids are deduped.
     */
    public void onProjectileSpawn(double x, double y, double z, double speed, int projectileId, int ownerId) {
        if (!config.enabled) return;
        if (seenProjectileIds.size() > 1024) seenProjectileIds.clear();
        if (ownerId <= 0 || !seenProjectileIds.add(projectileId)) return;
        ObservedPlayer owner = players.get(ownerId);
        if (owner == null || owner.prev == null) return;
        owner.lastProjectileSpeed = speed;
        owner.lastProjectileTick = owner.prev.tick();
        AlertSink gated = gatedSink();
        for (LookoutCheck check : checksFor(ownerId)) check.onProjectile(owner, owner.prev.tick(), config, gated);
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
