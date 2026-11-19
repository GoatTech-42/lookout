package dev.lookout.core;

import dev.lookout.core.checks.ElytraCheck;
import dev.lookout.core.checks.FacingCheck;
import dev.lookout.core.checks.FastBowCheck;
import dev.lookout.core.checks.FlightCheck;
import dev.lookout.core.checks.JesusCheck;
import dev.lookout.core.checks.MultiAuraCheck;
import dev.lookout.core.checks.NoSlowCheck;
import dev.lookout.core.checks.ReachCheck;
import dev.lookout.core.checks.TimerCheck;
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

    private final java.util.function.LongSupplier wallClock;

    public LookoutEngine(LookoutConfig config, AlertSink sink) {
        this(config, sink, () -> System.nanoTime() / 1_000_000L); // monotonic, never the adjustable wall clock
    }

    public LookoutEngine(LookoutConfig config, AlertSink sink, java.util.function.LongSupplier wallClock) {
        this.config = config;
        this.sink = sink;
        this.wallClock = wallClock;
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
            if (config.timer) list.add(new TimerCheck());
            if (config.multiAura) list.add(new MultiAuraCheck());
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

    // Relative move packets cap at 8 blocks per packet, so an unsignaled relocation
    // arrives as one sync jump or a couple of ~7-block relative moves; the client lerp
    // can also slice a big jump into ~4-5 block samples. 4.0 stays clear of legit
    // per-tick movement (bunching ~1.4, boosted elytra ~2) and of knockback, which
    // arrives with a velocity packet and gates separately.
    private static final double UNSIGNALED_TELEPORT_BLOCKS = 4.0;

    private static double distance3d(ObservedSample a, ObservedSample b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        double dz = a.z() - b.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public void tick(int entityId, String name, ObservedSample sample, WorldProbe probe) {
        if (!config.enabled) return;
        ObservedPlayer p = players.computeIfAbsent(entityId, ObservedPlayer::new);
        if (name != null) p.name = name;
        p.lastWallMillis = wallClock.getAsLong();
        ObservedSample prev = p.prev;
        p.add(sample);
        // Lane-run finding: at join the server can relocate a player with no S2C teleport
        // event, and the client lerps the jump over a few ticks. Without this gate the
        // windowed checks read it as huge speed. Treat any unsignaled single-sample jump
        // past 8 blocks as a teleport: same reset as onTeleport.
        if (prev != null && distance3d(prev, sample) > UNSIGNALED_TELEPORT_BLOCKS) {
            p.lastTeleportTick = prev.tick();
            p.airTicks = 0;
        }
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
     * A damage packet says causeId hurt victimId. Only player-attack damage counts:
     * thorns and other types reuse the single-entity shape (equal cause and direct
     * ids), so the type check is what separates real melee hits from them.
     */
    public void onDamage(int victimId, int causeId, int directId, String typeKey) {
        if (!config.enabled) return;
        if (!"minecraft:player_attack".equals(typeKey)) return;
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
        seenProjectileIds.clear(); // ids can be reused on the next connection
    }
}
