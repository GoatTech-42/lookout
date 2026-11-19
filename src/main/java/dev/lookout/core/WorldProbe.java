package dev.lookout.core;

/** What the watching client can see of the world around another player. */
public interface WorldProbe {
    boolean isSolidAt(double x, double y, double z);
    boolean isLiquidAt(double x, double y, double z);
    boolean isClimbableAt(double x, double y, double z);
    boolean isStairLikeAt(double x, double y, double z);
    boolean isSlipperyAt(double x, double y, double z);
    boolean isSoulGroundAt(double x, double y, double z);
}
