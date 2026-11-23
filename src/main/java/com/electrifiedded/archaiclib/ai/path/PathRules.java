package com.electrifiedded.archaiclib.ai.path;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * What a mob may do to the world on its way, and what it would rather not walk on. Costs are in ticks
 * ({@link PathCosts}); {@link PathCosts#INF} forbids. Called from the path search for each new cell once
 * (results are cached per search), so keep them cheap: check the quick reasons to refuse first.
 */
public interface PathRules {

    /** Cost of breaking the block at {@code p} to get through, or INF. Default: never. */
    default double breakCost(World world, BlockPos p, IBlockState state) {
        return PathCosts.INF;
    }

    /**
     * Cost of putting a helper block at {@code p} (a bridge, a step, a pillar under itself when
     * {@code pillar}), or INF. Default: never.
     */
    default double placeCost(World world, BlockPos p, boolean pillar) {
        return PathCosts.INF;
    }

    /** How many helper blocks one path may use (blocks in the bag). */
    default int placeBudget() {
        return 0;
    }

    /**
     * Extra cost of standing in cell {@code feet} (crops trampled, a field, a leaf floor...). The default
     * keeps mobs off farmland and out of crops and tree tops when there is another way.
     */
    default double penalty(World world, int x, int y, int z, int feetType, int floorType) {
        double c = 0;
        if ((floorType & CellCache.SOFT) != 0) c += 6;
        if ((feetType & CellCache.CROP) != 0) c += 4;
        if ((floorType & CellCache.LEAVES) != 0) c += 40;
        return c;
    }

    /** Highest fall (blocks) onto solid ground; into water any depth up to 24 is fine. */
    default int maxFall() {
        return 3;
    }

    PathRules WALK_ONLY = new PathRules() {};
}
