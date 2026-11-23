package com.electrifiedded.archaiclib.legacy.world;

import com.electrifiedded.archaiclib.legacy.sound.LegacySounds;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.List;

/**
 * Static stand-ins for the 1.7.10 coordinate based {@code World} methods
 * ({@code world.getBlock(x, y, z)}, {@code markBlockForUpdate}, {@code isBlockIndirectlyGettingPowered} ...).
 * Ported code becomes {@code LegacyWorld.getBlock(world, x, y, z)} with the original logic kept.
 */
public final class LegacyWorld {
    private LegacyWorld() {}

    public static BlockPos pos(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }

    public static Block getBlock(IBlockAccess world, int x, int y, int z) {
        return world.getBlockState(new BlockPos(x, y, z)).getBlock();
    }

    public static int getBlockMetadata(IBlockAccess world, int x, int y, int z) {
        IBlockState s = world.getBlockState(new BlockPos(x, y, z));
        return s.getBlock().getMetaFromState(s);
    }

    public static TileEntity getTileEntity(IBlockAccess world, int x, int y, int z) {
        return world.getTileEntity(new BlockPos(x, y, z));
    }

    public static boolean setBlock(World world, int x, int y, int z, Block block) {
        return setBlock(world, x, y, z, block, 0, 3);
    }

    public static boolean setBlock(World world, int x, int y, int z, Block block, int meta, int flags) {
        return world.setBlockState(new BlockPos(x, y, z), LegacyBlockIds.state(block, meta), flags);
    }

    /** 1.7 {@code setBlockMetadataWithNotify}. */
    public static boolean setBlockMetadataWithNotify(World world, int x, int y, int z, int meta, int flags) {
        BlockPos p = new BlockPos(x, y, z);
        return world.setBlockState(p, LegacyBlockIds.state(world.getBlockState(p).getBlock(), meta), flags);
    }

    public static boolean setBlockToAir(World world, int x, int y, int z) {
        return world.setBlockToAir(new BlockPos(x, y, z));
    }

    public static boolean isAirBlock(IBlockAccess world, int x, int y, int z) {
        return world.isAirBlock(new BlockPos(x, y, z));
    }

    public static boolean blockExists(World world, int x, int y, int z) {
        return world.isBlockLoaded(new BlockPos(x, y, z));
    }

    /** 1.7 {@code markBlockForUpdate}: resend the block and its tile entity description to clients. */
    public static void markBlockForUpdate(World world, int x, int y, int z) {
        if (world == null) return;
        BlockPos p = new BlockPos(x, y, z);
        IBlockState s = world.getBlockState(p);
        world.notifyBlockUpdate(p, s, s, 3);
    }

    public static void markBlockForUpdate(World world, BlockPos p) {
        markBlockForUpdate(world, p.getX(), p.getY(), p.getZ());
    }

    /** 1.7 {@code notifyBlocksOfNeighborChange(x, y, z, block)}. */
    public static void notifyBlocksOfNeighborChange(World world, int x, int y, int z, Block block) {
        world.notifyNeighborsOfStateChange(new BlockPos(x, y, z), block, false);
    }

    public static boolean isBlockIndirectlyGettingPowered(World world, int x, int y, int z) {
        return world.isBlockPowered(new BlockPos(x, y, z));
    }

    public static int getStrongestIndirectPower(World world, int x, int y, int z) {
        return world.getRedstonePowerFromNeighbors(new BlockPos(x, y, z));
    }

    /** 1.7 side ints 0..5 -> 1.12 isSideSolid. */
    public static boolean isSideSolid(IBlockAccess world, int x, int y, int z, int side) {
        BlockPos p = new BlockPos(x, y, z);
        return world.getBlockState(p).isSideSolid(world, p, EnumFacing.byIndex(side));
    }

    public static boolean isSideSolid(IBlockAccess world, int x, int y, int z, EnumFacing side) {
        BlockPos p = new BlockPos(x, y, z);
        return world.getBlockState(p).isSideSolid(world, p, side);
    }

    /** 1.7 {@code isBlockNormalCubeDefault}. */
    public static boolean isBlockNormalCubeDefault(World world, int x, int y, int z, boolean def) {
        return world.isBlockNormalCube(new BlockPos(x, y, z), def);
    }

    public static float getBlockHardness(World world, int x, int y, int z) {
        BlockPos p = new BlockPos(x, y, z);
        return world.getBlockState(p).getBlockHardness(world, p);
    }

    /** 1.7 {@code world.canPlaceEntityOnSide(block, x, y, z, skipCollision, side, entity, stack)}. */
    public static boolean canPlaceEntityOnSide(World world, Block block, int x, int y, int z, boolean skipCollision, int side, Entity entity) {
        return world.mayPlace(block, new BlockPos(x, y, z), skipCollision, EnumFacing.byIndex(side), entity);
    }

    public static Vec3d vec(double x, double y, double z) {
        return new Vec3d(x, y, z);
    }

    public static LegacyMOP rayTraceBlocks(World world, Vec3d from, Vec3d to) {
        return LegacyMOP.of(world.rayTraceBlocks(from, to));
    }

    public static LegacyMOP rayTraceBlocks(World world, Vec3d from, Vec3d to, boolean stopOnLiquid) {
        return LegacyMOP.of(world.rayTraceBlocks(from, to, stopOnLiquid));
    }

    public static LegacyMOP rayTraceBlocks(World world, Vec3d from, Vec3d to, boolean stopOnLiquid, boolean ignoreNoBox, boolean returnLastUncollidable) {
        return LegacyMOP.of(world.rayTraceBlocks(from, to, stopOnLiquid, ignoreNoBox, returnLastUncollidable));
    }

    public static AxisAlignedBB aabb(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new AxisAlignedBB(x1, y1, z1, x2, y2, z2);
    }

    /** 1.7 {@code getEntitiesWithinAABBExcludingEntity}: returns a mutable list. */
    public static List<Entity> getEntitiesWithinAABBExcludingEntity(World world, Entity except, AxisAlignedBB box) {
        return new java.util.ArrayList<>(world.getEntitiesWithinAABBExcludingEntity(except, box));
    }

    public static void playSoundEffect(World world, double x, double y, double z, String name, float volume, float pitch) {
        LegacySounds.playSoundEffect(world, x, y, z, name, volume, pitch);
    }

    public static void playSoundAtEntity(World world, Entity entity, String name, float volume, float pitch) {
        LegacySounds.playSoundAtEntity(world, entity, name, volume, pitch);
    }

    public static void spawnParticle(World world, String name, double x, double y, double z, double vx, double vy, double vz) {
        LegacyParticles.spawn(world, name, x, y, z, vx, vy, vz);
    }

    public static boolean isAir(Block b) {
        return b == Blocks.AIR;
    }
}
