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

    private static final ThreadLocal<BlockPos.MutableBlockPos> READ_POS = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

    /** Per-thread reusable position for READ-ONLY calls (never store it, never pass it to setBlockState). */
    public static BlockPos rpos(int x, int y, int z) {
        return READ_POS.get().setPos(x, y, z);
    }

    public static Block getBlock(IBlockAccess world, int x, int y, int z) {
        return world.getBlockState(rpos(x, y, z)).getBlock();
    }

    public static int getBlockMetadata(IBlockAccess world, int x, int y, int z) {
        IBlockState s = world.getBlockState(rpos(x, y, z));
        return s.getBlock().getMetaFromState(s);
    }

    public static TileEntity getTileEntity(IBlockAccess world, int x, int y, int z) {
        return world.getTileEntity(new BlockPos(x, y, z));
    }

    public static boolean setBlock(World world, int x, int y, int z, Block block) {
        return setBlock(world, x, y, z, block, 0, 3);
    }

    public static boolean setBlock(World world, int x, int y, int z, Block block, int meta, int flags) {
        if (y < 0 || y > 255) return false; // skip BlockPos alloc; vanilla rejects these anyway
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
        return world.isAirBlock(rpos(x, y, z));
    }

    public static boolean blockExists(World world, int x, int y, int z) {
        return world.isBlockLoaded(rpos(x, y, z));
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

    /** 1.7 {@code World.doesBlockHaveSolidTopSurface(world, x, y, z)} (static func_147466_a). */
    public static boolean doesBlockHaveSolidTopSurface(IBlockAccess world, int x, int y, int z) {
        BlockPos p = new BlockPos(x, y, z);
        return world.getBlockState(p).isSideSolid(world, p, EnumFacing.UP);
    }

    // ---- 1.7 height / sky queries (added 1.38.0) ----------------------------------------------------------------

    /** 1.7 {@code World.getHeightValue(x, z)}: first block above the heightmap top. */
    public static int getHeightValue(World world, int x, int z) {
        return world.getHeight(x, z);
    }

    /** 1.7 {@code World.getTopBlock(x, z)}: walks up from sea level (63) to the last non-air block and returns it. */
    public static Block getTopBlock(World world, int x, int z) {
        int y = 63;
        while (y < 255 && !world.isAirBlock(new BlockPos(x, y + 1, z))) y++;
        return getBlock(world, x, y, z);
    }

    /** 1.7 {@code World.getTopSolidOrLiquidBlock(x, z)} (y of the first free block above solid ground or liquid). */
    public static int getTopSolidOrLiquidBlock(World world, int x, int z) {
        return world.getTopSolidOrLiquidBlock(new BlockPos(x, 0, z)).getY();
    }

    /** 1.7 {@code World.canBlockSeeTheSky(x, y, z)}. */
    public static boolean canBlockSeeTheSky(World world, int x, int y, int z) {
        return world.canSeeSky(new BlockPos(x, y, z));
    }

    /** 1.7 {@code World.canBlockFreeze(x, y, z, byWater)}. */
    public static boolean canBlockFreeze(World world, int x, int y, int z, boolean byWater) {
        return world.canBlockFreeze(new BlockPos(x, y, z), byWater);
    }

    /** 1.7 {@code ((BlockSapling) Blocks.sapling).func_149878_d(world, x, y, z, rand)}: grows the sapling placed there into a tree. */
    public static void growSapling(World world, int x, int y, int z, java.util.Random rand) {
        BlockPos p = new BlockPos(x, y, z);
        IBlockState s = world.getBlockState(p);
        if (s.getBlock() instanceof net.minecraft.block.BlockSapling)
            ((net.minecraft.block.BlockSapling) s.getBlock()).generateTree(world, p, s, rand);
    }

    /** 1.7 {@code IGrowable.func_149853_b(world, rand, x, y, z)} on whatever growable block is there (grass: flowers and tall grass). */
    public static void grow(World world, java.util.Random rand, int x, int y, int z) {
        BlockPos p = new BlockPos(x, y, z);
        IBlockState s = world.getBlockState(p);
        if (s.getBlock() instanceof net.minecraft.block.IGrowable)
            ((net.minecraft.block.IGrowable) s.getBlock()).grow(world, rand, p, s);
    }

    /**
     * 1.7 {@code new EntityPainting(world, x, y, z, direction, title)}; x/y/z is the block the painting hangs on and
     * direction 0=S 1=W 2=N 3=E (same as {@link EnumFacing#byHorizontalIndex}). Not spawned yet.
     */
    public static net.minecraft.entity.item.EntityPainting newPainting(World world, int x, int y, int z, int direction, String title) {
        return new net.minecraft.entity.item.EntityPainting(world, new BlockPos(x, y, z), EnumFacing.byHorizontalIndex(direction & 3), title);
    }
}
