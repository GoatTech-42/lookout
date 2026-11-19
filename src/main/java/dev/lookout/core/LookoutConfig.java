package dev.lookout.core;

/** Lookout settings. Pure values; the glue loads/saves lookout.json with gson. */
public class LookoutConfig {
    public boolean enabled = true;

    public boolean flight = true;
    public boolean jesus = true;
    public boolean step = true;
    public boolean speed = true;
    public boolean totem = true;
    public boolean killaura = true;
    public boolean fastbow = true;
    public boolean noslow = true;
    public boolean reach = true;
    public boolean elytra = true;

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

    /** swing angle off the target bearing that reads as killaura facing */
    public double killauraFacingDegrees = 55;
    public double killauraBufferMax = 4.0;
    /** only judge facing when someone is actually within plausible combat range */
    public double killauraRange = 4.6;
    /** arrow speed in blocks/tick that reads as near-full draw (vanilla max ~3.0) */
    public double fastbowFullPowerSpeed = 2.5;
    /** draw shorter than this with a near-full-power arrow reads as fastbow */
    public int fastbowMaxDrawTicks = 5;
    public int fastbowCount = 3;
    public long fastbowMemoryTicks = 200;
    /** ground speed in blocks/tick while using an item that reads as noslow */
    public double noslowMaxSpeed = 0.1;
    /** grace after the use starts (momentum, ice slides) before noslow math engages */
    public int noslowGraceTicks = 10;
    public int noslowSustainTicks = 15;
    /** 3D distance past which a facing-aligned swing reads as rough reach */
    public double reachBlocks = 4.6;
    public double reachFacingDegrees = 60;
    public double reachBufferMax = 4.0;
    /** level-glide speed in blocks/tick past elytra + fireworks that reads as overspeed */
    public double elytraMaxLevel = 2.2;
    public int elytraSustainTicks = 30;

    /** seconds before the same player+check can alert again */
    public long alertCooldownSeconds = 10;
}
