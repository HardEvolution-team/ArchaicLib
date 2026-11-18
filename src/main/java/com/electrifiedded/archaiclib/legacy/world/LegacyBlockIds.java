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
        try {
            return b.getStateFromMeta(meta & 15);
        } catch (RuntimeException e) {
            return b.getDefaultState();
        }
    }

    public static Block getBlock(World world, int x, int y, int z) {
        return world.getBlockState(new BlockPos(x, y, z)).getBlock();
    }

    public static int getMeta(World world, int x, int y, int z) {
        IBlockState s = world.getBlockState(new BlockPos(x, y, z));
        return s.getBlock().getMetaFromState(s);
    }

    /** 1.7 {@code world.setBlock(x, y, z, block, meta, flags)}. */
    public static boolean setBlock(World world, int x, int y, int z, Block block, int meta, int flags) {
        return world.setBlockState(new BlockPos(x, y, z), state(block, meta), flags);
    }

    public static boolean setBlock(World world, int x, int y, int z, Block block) {
        return setBlock(world, x, y, z, block, 0, 3);
    }
}
