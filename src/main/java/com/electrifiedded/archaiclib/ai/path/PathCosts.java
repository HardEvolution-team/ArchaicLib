package com.electrifiedded.archaiclib.ai.path;

/**
 * Movement costs in game ticks, the way Baritone measures them: a path is the fastest one, so digging a block
 * (its real dig time) competes fairly with walking around it, and a long detour wins over a tower of blocks.
 */
public final class PathCosts {

    private PathCosts() {}

    public static final double INF = 1_000_000.0;

    /** One block of walking (a player walks 4.317 blocks/s). */
    public static final double WALK = 20.0 / 4.317;
    public static final double WALK_DIAGONAL = WALK * Math.sqrt(2);
    /** One block in water. */
    public static final double SWIM = 20.0 / 2.2;
    public static final double LADDER_UP = 20.0 / 2.35;
    public static final double LADDER_DOWN = 20.0 / 3.0;
    /** Extra for a jump up one block (the jump itself takes about this long). */
    public static final double JUMP = 2.5;
    /** Extra for going through a door or a gate (opening it). */
    public static final double DOOR_OPEN = 3.0;
    /** Cobweb: very slow. */
    public static final double WEB = 40.0;
    /** Default extra for putting a block down (time + the block it costs). */
    public static final double PLACE = 22.0;
    /** Default extra on top of a block's dig time (picking the drop up, tool wear). */
    public static final double BREAK_EXTRA = 8.0;

    private static final double[] FALL = new double[257];

    static {
        // ticks to fall n blocks from standing (vanilla gravity and drag)
        double v = 0, y = 0;
        int t = 0;
        int n = 1;
        while (n < FALL.length) {
            v = (v - 0.08) * 0.98;
            y += v;
            t++;
            while (n < FALL.length && -y >= n) FALL[n++] = t;
        }
    }

    /** Ticks to fall {@code blocks} blocks. */
    public static double fall(int blocks) {
        if (blocks <= 0) return 0;
        return FALL[Math.min(blocks, FALL.length - 1)];
    }
}
