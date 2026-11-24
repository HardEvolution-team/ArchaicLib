package com.electrifiedded.archaiclib.legacy.world;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Numeric 1.7 block id + metadata (schematics, structure .dat files, configs) to 1.12 block states, and
 * the 1.7 coordinate-style world calls ({@code getBlock(x,y,z)}, {@code setBlock(x,y,z,b,meta,flags)}).
 * Vanilla ids 0..197 kept their numbers in 1.12, so {@link Block#getBlockById} is correct for them.
 */
public final class LegacyBlockIds {
    private LegacyBlockIds() {}

    @SuppressWarnings("deprecation")
    public static IBlockState state(int id, int meta) {
        Block b = Block.getBlockById(id);
        if (b == null) return Blocks.AIR.getDefaultState();
        try {
            return b.getStateFromMeta(meta & 15);
        } catch (RuntimeException e) {
            return b.getDefaultState();
        }
    }

    @SuppressWarnings("deprecation")
    public static IBlockState state(Block b, int meta) {
        if (b == null) return Blocks.AIR.getDefaultState();
        meta &= 15;
        IBlockState[] arr = STATE_CACHE.get(b);
        if (arr == null) {
            arr = new IBlockState[16];
            IBlockState[] prev = STATE_CACHE.putIfAbsent(b, arr);
            if (prev != null) arr = prev;
        }
        IBlockState st = arr[meta];
        if (st == null) {
            try {
                st = b.getStateFromMeta(meta);
            } catch (RuntimeException e) {
                st = b.getDefaultState();
            }
            arr[meta] = st;
        }
        return st;
    }

    /** Block -> 16 meta states, filled lazily (getStateFromMeta is pure; races only recompute). */
    private static final java.util.concurrent.ConcurrentHashMap<Block, IBlockState[]> STATE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    public static Block getBlock(World world, int x, int y, int z) {
        return world.getBlockState(LegacyWorld.rpos(x, y, z)).getBlock();
    }

    public static int getMeta(World world, int x, int y, int z) {
        IBlockState s = world.getBlockState(LegacyWorld.rpos(x, y, z));
        return s.getBlock().getMetaFromState(s);
    }

    /** 1.7 {@code world.setBlock(x, y, z, block, meta, flags)}. */
    public static boolean setBlock(World world, int x, int y, int z, Block block, int meta, int flags) {
        return LegacyWorld.setBlock(world, x, y, z, block, meta, flags);
    }

    public static boolean setBlock(World world, int x, int y, int z, Block block) {
        return setBlock(world, x, y, z, block, 0, 3);
    }
}
