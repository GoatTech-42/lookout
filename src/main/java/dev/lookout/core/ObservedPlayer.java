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
