package com.electrifiedded.archaiclib.ai.path;

import net.minecraft.util.math.BlockPos;

/** A found path: feet cells from the start (index 0) to the end and the move that reaches each one. */
public final class MovePath {

    final int[] x, y, z;
    final Move[] move;
    /** reaches the goal (false: the best partial way toward it) */
    public final boolean complete;
    public final double cost;

    MovePath(int n, boolean complete, double cost) {
        x = new int[n];
        y = new int[n];
        z = new int[n];
        move = new Move[n];
        this.complete = complete;
        this.cost = cost;
    }

    public int length() {
        return x.length;
    }

    public BlockPos pos(int i) {
        return new BlockPos(x[i], y[i], z[i]);
    }

    public Move move(int i) {
        return move[i];
    }

    public BlockPos end() {
        return pos(x.length - 1);
    }
}
