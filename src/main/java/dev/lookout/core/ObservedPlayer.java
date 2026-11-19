package dev.lookout.core;

import java.util.ArrayDeque;
import java.util.Deque;

/** Rolling observation of one other player, built from S2C traffic only. */
public class ObservedPlayer {
    public final int entityId;
    public String name = "?";

    public final Deque<ObservedSample> samples = new ArrayDeque<>();
    public ObservedSample prev;

    public int airTicks;
    public long lastVelocityTick = -1000;
    public long lastTeleportTick = -1000;
    public long lastTotemPopTick = -1000;

    // join/reacquisition state: set on the first observed sample; a fresh
    // ObservedPlayer (genuine despawn + re-add) is the only re-arm
    public long firstSampleTick = -1;
    /** unsignaled relocation jumps inside the appear grace gate lateral windows only */
    public long lastJoinRelocationTick = -1000;
    // unsignaled-jump cluster tracking for the discontinuity finding
    public long lastJumpTick = -1000;
    public long jumpClusterStartTick = -1000;
    public final Deque<Long> recentJumpTicks = new ArrayDeque<>();

    public long lastWallMillis;

    // filled by the engine right before the projectile event fires
    public double lastProjectileSpeed;
    public long lastProjectileTick = -1000;

    public ObservedPlayer(int entityId) {
        this.entityId = entityId;
    }

    public void add(ObservedSample s) {
        samples.addLast(s);
        while (samples.size() > 60) samples.removeFirst();
        prev = s;
    }

    public ObservedSample at(long tick) {
        for (ObservedSample s : samples) if (s.tick() == tick) return s;
        return null;
    }

    public long ticksSince(long eventTick, long now) {
        return now - eventTick;
    }
}
