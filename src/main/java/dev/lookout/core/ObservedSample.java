package dev.lookout.core;

/** One client tick of what the server told us about another player. */
public record ObservedSample(
        long tick,
        double x, double y, double z,
        boolean onGround,
        boolean sprinting,
        boolean gliding,
        boolean swimmingPose,
        boolean usingItem,
        boolean inVehicle,
        boolean inWater,
        boolean levitation,
        boolean slowFalling,
        int jumpBoostLevel,
        int speedLevel,
        boolean glintingBoots,
        boolean gamemodeExempt) {

    public double horizontalSpeed(ObservedSample prev) {
        return Math.hypot(x - prev.x, z - prev.z);
    }
}
