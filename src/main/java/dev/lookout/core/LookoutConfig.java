package dev.lookout.core;

/** Lookout settings. Pure values; the glue loads/saves lookout.json with gson. */
public class LookoutConfig {
    public boolean enabled = true;

    public boolean flight = true;
    public boolean jesus = true;
    public boolean step = true;
    public boolean speed = true;
    public boolean totem = true;

    /** air ticks of grace before flight math engages (covers legit jumps + lag) */
    public int flightGraceTicks = 30;
    /** buffered strike points needed before a flight note fires */
    public double flightBufferMax = 12.0;
    /** ground-to-ground y gain over 3 ticks that reads as a step hack */
    public double stepGain = 0.66;
    public double stepBufferMax = 4.0;
    /** sustained ground speed in blocks/tick that reads as a speed hack (base, before speed effect) */
    public double speedMax = 0.44; // ~8.8 m/s
    public int speedWindowTicks = 12;
    public double speedBufferMax = 6.0;
    /** totem pops with a near-instant restock needed before a note fires */
    public int totemCount = 3;
    public int totemRestockTicks = 3;
    public long totemMemoryTicks = 600;

    /** seconds before the same player+check can alert again */
    public long alertCooldownSeconds = 10;
}
