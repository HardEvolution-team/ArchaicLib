package com.electrifiedded.archaiclib.ai.path;

import net.minecraft.util.math.BlockPos;

/** Where a path search should end (feet cell of the mob). */
public interface Goal {

    boolean isEnd(int x, int y, int z);

    /** Estimated cost (ticks) from feet cell x,y,z to the goal. */
    double heuristic(int x, int y, int z);

    /** Octile walking estimate plus climbing/falling for {@code dy} (positive = goal above). */
    static double estimate(double dx, double dy, double dz) {
        dx = Math.abs(dx);
        dz = Math.abs(dz);
        double straight = Math.abs(dx - dz), diag = Math.min(dx, dz);
        double h = (straight + diag * Math.sqrt(2)) * PathCosts.WALK;
        if (dy > 0) h += dy * (PathCosts.JUMP + PathCosts.WALK * 0.5);
        else h += -dy * 1.5;
        return h;
    }

    /** Feet within {@code reach} of the centre of {@code target} (like "near the chest"). */
    static Goal near(BlockPos target, double reach) {
        final double tx = target.getX() + 0.5, ty = target.getY() + 0.5, tz = target.getZ() + 0.5;
        final double r2 = reach * reach;
        return new Goal() {
            @Override
            public boolean isEnd(int x, int y, int z) {
                double dx = x + 0.5 - tx, dy = y - ty, dz = z + 0.5 - tz;
                return dx * dx + dy * dy + dz * dz <= r2;
            }

            @Override
            public double heuristic(int x, int y, int z) {
                double dx = x + 0.5 - tx, dy = ty - 0.5 - y, dz = z + 0.5 - tz;
                double d = Math.sqrt(dx * dx + dz * dz);
                double k = d < 1e-6 ? 0 : Math.max(0, d - reach) / d;
                return estimate(dx * k, dy, dz * k);
            }

            @Override
            public String toString() {
                return "near " + target.getX() + "," + target.getY() + "," + target.getZ() + " r" + reach;
            }
        };
    }

    /** Exactly this feet cell. */
    static Goal at(BlockPos feet) {
        final int fx = feet.getX(), fy = feet.getY(), fz = feet.getZ();
        return new Goal() {
            @Override
            public boolean isEnd(int x, int y, int z) {
                return x == fx && y == fy && z == fz;
            }

            @Override
            public double heuristic(int x, int y, int z) {
                return estimate(fx - x, fy - y, fz - z);
            }

            @Override
            public String toString() {
                return "at " + fx + "," + fy + "," + fz;
            }
        };
    }
}
