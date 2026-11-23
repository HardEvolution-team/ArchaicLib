package com.electrifiedded.archaiclib.ai.path;

import net.minecraft.util.math.BlockPos;

import static com.electrifiedded.archaiclib.ai.path.CellCache.*;
import static com.electrifiedded.archaiclib.ai.path.PathCosts.*;

/**
 * Cost of each {@link Move} between two feet cells for a mob {@code h} cells tall and at most one block wide.
 * Shared by the search (expansion) and the executor (re-checking the next moves against the world as it is
 * now). After a call {@link #places} tells how many helper blocks that move puts down.
 */
public final class Moves {

    final CellCache c;
    final int h;
    /** helper blocks the last evaluated move places */
    public int places;

    public Moves(CellCache cells, int height) {
        this.c = cells;
        this.h = Math.max(1, height);
    }

    private boolean water(int x, int y, int z) {
        return (c.type(x, y, z) & WATER) != 0;
    }

    private boolean climb(int x, int y, int z) {
        return (c.type(x, y, z) & CLIMB) != 0;
    }

    /** A door in the cell blocks travel along the other axis. */
    private boolean doorAcross(int x, int y, int z, boolean axisX) {
        int t = c.type(x, y, z);
        return (t & DOOR) != 0 && ((t & AXIS_X) != 0) != axisX;
    }

    private double body(int x, int y, int z, Boolean axisX) {
        double sum = 0;
        for (int i = 0; i < h; i++) {
            sum += c.cellCost(x, y + i, z, axisX);
            if (sum >= INF) return INF;
        }
        return sum;
    }

    private boolean bodyFree(int x, int y, int z) {
        for (int i = 0; i < h; i++) if (c.cellCost(x, y + i, z, null) != 0) return false;
        return true;
    }

    private double place(int x, int y, int z, boolean pillar) {
        int t = c.type(x, y, z);
        if ((t & PASS) == 0 || (t & (WATER | DEADLY | DOOR | CLIMB | UNLOADED)) != 0) return INF;
        double p = c.rules.placeCost(c.world, new BlockPos(x, y, z), pillar);
        if (p < INF) places++;
        return p;
    }

    private double fin(double cost, int x, int y, int z) {
        if (cost >= INF) return INF;
        return cost + c.rules.penalty(c.world, x, y, z, c.type(x, y, z), c.type(x, y - 1, z));
    }

    /** Cost of {@code m} from a to b, INF if impossible (b of FALL must be the landing cell). */
    public double cost(Move m, int ax, int ay, int az, int bx, int by, int bz) {
        places = 0;
        switch (m) {
            case TRAVERSE: return traverse(ax, ay, az, bx, bz);
            case DIAGONAL: return diagonal(ax, ay, az, bx, bz);
            case ASCEND: return ascend(ax, ay, az, bx, bz);
            case DESCEND: return descend(ax, ay, az, bx, bz);
            case FALL: return fall(ax, ay, az, bx, by, bz);
            case DOWNWARD: return downward(ax, ay, az);
            case PILLAR: return pillar(ax, ay, az);
            case CLIMB_UP: return climbUp(ax, ay, az);
            case CLIMB_DOWN: return climbDown(ax, ay, az);
            case SWIM_UP: return swimUp(ax, ay, az);
            case SWIM_DOWN: return swimDown(ax, ay, az);
            default: return 0;
        }
    }

    double traverse(int ax, int ay, int az, int bx, int bz) {
        places = 0;
        boolean axisX = bx != ax;
        if (doorAcross(ax, ay, az, axisX) || doorAcross(ax, ay + 1, az, axisX)) return INF;
        double cost = body(bx, ay, bz, axisX);
        if (cost >= INF) return INF;
        if (!c.standable(bx, ay, bz)) {
            cost += place(bx, ay - 1, bz, false); // bridge the gap
            if (cost >= INF) return INF;
        }
        cost += water(ax, ay, az) || water(bx, ay, bz) ? SWIM : WALK;
        return fin(cost, bx, ay, bz);
    }

    double diagonal(int ax, int ay, int az, int bx, int bz) {
        places = 0;
        if (!bodyFree(bx, ay, az) || !bodyFree(ax, ay, bz) || !bodyFree(bx, ay, bz)) return INF;
        if (!c.standable(bx, ay, bz) || climb(bx, ay, bz)) return INF;
        double base = water(ax, ay, az) || water(bx, ay, bz) ? SWIM * Math.sqrt(2) : WALK_DIAGONAL;
        return fin(base, bx, ay, bz);
    }

    double ascend(int ax, int ay, int az, int bx, int bz) {
        places = 0;
        boolean axisX = bx != ax;
        if (doorAcross(ax, ay, az, axisX)) return INF;
        double cost = c.cellCost(ax, ay + h, az, null); // headroom for the jump
        if (cost >= INF) return INF;
        cost += body(bx, ay + 1, bz, axisX);
        if (cost >= INF) return INF;
        if (!c.standable(bx, ay + 1, bz)) {
            int t = c.type(bx, ay, bz);
            if ((t & TALL) != 0) return INF;
            cost += place(bx, ay, bz, false); // build the step
            if (cost >= INF) return INF;
        }
        return fin(cost + WALK + JUMP, bx, ay + 1, bz);
    }

    double descend(int ax, int ay, int az, int bx, int bz) {
        places = 0;
        boolean axisX = bx != ax;
        if (doorAcross(ax, ay, az, axisX)) return INF;
        if (!c.standable(bx, ay - 1, bz)) return INF;
        double cost = c.cellCost(bx, ay + h - 1, bz, axisX); // walking over the edge at the old height
        if (cost >= INF) return INF;
        cost += body(bx, ay - 1, bz, axisX);
        return fin(cost + WALK + PathCosts.fall(1), bx, ay - 1, bz);
    }

    double fall(int ax, int ay, int az, int bx, int by, int bz) {
        places = 0;
        int drop = ay - by;
        if (drop < 2) return INF;
        boolean axisX = bx != ax;
        for (int i = 0; i < h; i++) if (c.cellCost(bx, ay + i, bz, axisX) != 0 && c.cellCost(bx, ay + i, bz, axisX) != DOOR_OPEN) return INF;
        for (int y = ay - 1; y >= by; y--) if (!c.open(bx, y, bz)) return INF;
        if (!c.standable(bx, by, bz)) return INF;
        boolean wet = water(bx, by, bz);
        if (!wet && drop > c.rules.maxFall()) return INF;
        return fin(WALK + PathCosts.fall(drop) + (wet ? 0 : drop * 2.0), bx, by, bz);
    }

    /** Landing cell when walking off toward bx,bz from level ay, or Integer.MIN_VALUE. */
    int fallLanding(int ay, int bx, int bz) {
        for (int y = ay - 1; y >= Math.max(0, ay - 24); y--) {
            if (!c.open(bx, y, bz)) return Integer.MIN_VALUE;
            if (y <= ay - 2 && c.standable(bx, y, bz)) return y;
        }
        return Integer.MIN_VALUE;
    }

    double downward(int ax, int ay, int az) {
        places = 0;
        if (water(ax, ay, az) || climb(ax, ay, az)) return INF;
        int floor = c.type(ax, ay - 1, az);
        if ((floor & FLOOR) == 0) return INF;
        if ((c.type(ax, ay - 2, az) & (FLOOR | WATER)) == 0) return INF;
        double cost = c.cellCost(ax, ay - 1, az, null);
        return fin(cost + PathCosts.fall(1), ax, ay - 1, az);
    }

    double pillar(int ax, int ay, int az) {
        places = 0;
        if (water(ax, ay, az) || climb(ax, ay, az)) return INF;
        double cost = c.cellCost(ax, ay + h, az, null);
        if (cost >= INF) return INF;
        cost += place(ax, ay, az, true);
        return fin(cost + JUMP + WALK, ax, ay + 1, az);
    }

    double climbUp(int ax, int ay, int az) {
        places = 0;
        if (!climb(ax, ay, az) || !climb(ax, ay + 1, az)) return INF;
        if (c.cellCost(ax, ay + h, az, null) != 0) return INF;
        return fin(LADDER_UP, ax, ay + 1, az);
    }

    double climbDown(int ax, int ay, int az) {
        places = 0;
        if (!climb(ax, ay - 1, az)) return INF;
        return fin(LADDER_DOWN, ax, ay - 1, az);
    }

    double swimUp(int ax, int ay, int az) {
        places = 0;
        if (!water(ax, ay, az) || !water(ax, ay + 1, az)) return INF;
        if (!c.open(ax, ay + h, az)) return INF;
        return fin(SWIM, ax, ay + 1, az);
    }

    double swimDown(int ax, int ay, int az) {
        places = 0;
        if (!water(ax, ay, az) || !water(ax, ay - 1, az)) return INF;
        return fin(SWIM, ax, ay - 1, az);
    }
}
