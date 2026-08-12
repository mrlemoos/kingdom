package dev.mrlemoos.kingdom.police;

/**
 * The geometry of the bench: which way the magistrate looks, and where the dock stands. The yaw
 * sited with the court does double duty — it aims the magistrate and fixes the dock in front of
 * them.
 */
public final class CourtBench {

    /** Blocks between the bench and the dock. */
    public static final int DOCK_STAND_OFF = 2;

    private CourtBench() {
    }

    /** Yaw from the bench towards a point, holding {@code currentYaw} when the point is the bench itself. */
    public static float yawTowards(double benchX, double benchZ, double targetX, double targetZ, float currentYaw) {
        double dx = targetX - benchX;
        double dz = targetZ - benchZ;
        if (dx == 0.0 && dz == 0.0) {
            return currentYaw;
        }
        return normaliseYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
    }

    public static float yawTowards(double benchX, double benchZ, double targetX, double targetZ) {
        return yawTowards(benchX, benchZ, targetX, targetZ, 0f);
    }

    /** Block offset of the dock, {@code standOff} blocks along the bench's yaw. */
    public static int[] dockOffset(float yaw, int standOff) {
        double radians = Math.toRadians(yaw);
        return new int[] {
            (int) Math.round(-Math.sin(radians) * standOff), (int) Math.round(Math.cos(radians) * standOff)
        };
    }

    public static float normaliseYaw(float yaw) {
        float wrapped = yaw % 360f;
        if (wrapped > 180f) {
            wrapped -= 360f;
        }
        if (wrapped < -180f) {
            wrapped += 360f;
        }
        return wrapped;
    }
}
