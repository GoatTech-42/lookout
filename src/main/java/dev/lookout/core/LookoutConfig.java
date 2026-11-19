package dev.lookout.core;

/** Lookout settings. Pure values; the glue loads/saves lookout.json with gson. */
public class LookoutConfig {
    public boolean enabled = true;

    public boolean flight = true;
    public boolean jesus = true;
    public boolean step = true;
    public boolean speed = true;
    public boolean totem = true;
    public boolean killaura = false; // opt-in: roughest judgment in lookout
    public boolean fastbow = true;
    public boolean noslow = true;
    public boolean reach = false; // opt-in: roughest judgment in lookout
    public boolean elytra = true;
    public boolean timer = false; // opt-in: only readable as wall-clock overspeed
    public boolean multiAura = false; // opt-in: sweep attacks look identical per event

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
    /** only judge facing for hits inside plausible combat range; reach owns longer hits */
    public double killauraRange = 4.6;
    /** arrow speed in blocks/tick that reads as near-full power (heuristic, not a sourced maximum) */
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
    public double reachBufferMax = 4.0;
    /** level-glide speed in blocks/tick past elytra + fireworks that reads as overspeed */
    public double elytraMaxLevel = 2.2;
    public int elytraSustainTicks = 30;
    /** wall-clock ground speed in m/s past legit play that reads as timer-like (heuristic) */
    public double timerMaxWallSpeed = 10.0;
    public int timerWindowTicks = 40;
    /** our own client must tick at least this fast, or wall-clock math is meaningless */
    public double timerMinLocalTps = 19.0;
    /** faster than this means catch-up processing after a stall, not real pacing */
    public double timerMaxLocalTps = 21.0;
    /** distinct victims inside this window that read as a multi-target hit */
    public int multiAuraWindowTicks = 2;
    /** repeated multi-hits must come faster than this (legit sweep cadence is slower) */
    public int multiAuraRapidGapTicks = 8;
    public int multiAuraCount = 3;

    /** report repeated unsignaled relocation jumps as their own finding.
     *  opt-in until the 3.0 threshold is validated against measured real-client
     *  per-tick deltas (QA 7.7.2) */
    public boolean teleportJumps = false;
    /** log observed per-tick horizontal deltas past the floor for threshold measurement */
    public boolean teleportJumpStats = false;
    public double teleportJumpStatsFloor = 1.0;
    /** windowed lateral checks stay quiet about unsignaled jumps for this long after
     *  a player first appears: at join the server can relocate a fresh player with no
     *  S2C teleport event (lane-run FP). state-derived; re-arms only on genuine
     *  reacquisition (despawn + re-add = a fresh ObservedPlayer) */
    public int joinGraceTicks = 100;
    /** single-tick horizontal jump past this counts as a relocation. EXPERIMENTAL:
     *  the basis so far is sim-side (legit per-tick horizontal tops out at boosted
     *  elytra ~2.0; sprint 0.28, bunching ~1.4); replace with measured real-client
     *  deltas before turning teleportJumps on by default (QA 7.7.2). horizontal
     *  only, so big falls don't count */
    public double unsignaledJumpBlocks = 3.0;
    /** this many relocation jumps inside the window reads as its own finding */
    public int teleportJumpCount = 3;
    public long teleportJumpWindowTicks = 200;

    /** seconds before the same player+check can alert again */
    public long alertCooldownSeconds = 10;
    /** global alert rate cap across all entities: token bucket size... */
    public int alertGlobalBurst = 8; // <= 0 disables the global cap
    /** ...and refill rate in tokens per second; the burst allowance keeps one crowded fight readable */
    public double alertGlobalPerSecond = 5.0;
}
