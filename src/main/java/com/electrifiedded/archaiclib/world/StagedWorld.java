package com.electrifiedded.archaiclib.world;

import com.electrifiedded.archaiclib.legacy.world.LegacyBlockIds;
import com.electrifiedded.archaiclib.legacy.world.LegacyWorld;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.Random;

/**
 * Drop-in replacement for the {@link LegacyWorld} calls of old structure generators. Inside a
 * {@link ChunkStagedGen} job everything goes to the staged session (background thread, no chunk is loaded,
 * generated or populated for the structure); outside a job it is the plain {@link LegacyWorld} behaviour.
 * Entities go through {@link ChunkStagedGen#spawnEntity} (spawned once their chunk really exists).
 */
public final class StagedWorld {
    private StagedWorld() {}

    private static ChunkStagedGen.Session s() { return ChunkStagedGen.current(); }

    public static boolean staged() { return s() != null; }

    /** Runs a structure job staged (worlds on the server) or right away (client / other). */
    public static void submit(World w, Runnable job) { ChunkStagedGen.submit(w, job); }

    public static void onMain(Runnable r) { ChunkStagedGen.onMain(r); }

    // ---------------------------------------------------------------- blocks

    public static IBlockState getState(World w, BlockPos p) {
        ChunkStagedGen.Session s = s();
        return s != null ? s.getBlockState(p) : w.getBlockState(p);
    }

    public static IBlockState getState(World w, int x, int y, int z) { return getState(w, new BlockPos(x, y, z)); }

    public static Block getBlock(World w, int x, int y, int z) { return getState(w, x, y, z).getBlock(); }

    public static int getBlockMetadata(World w, int x, int y, int z) {
        IBlockState st = getState(w, x, y, z);
        return st.getBlock().getMetaFromState(st);
    }

    public static boolean isAirBlock(World w, int x, int y, int z) {
        if (s() == null) return LegacyWorld.isAirBlock(w, x, y, z);
        return getState(w, x, y, z).getMaterial() == Material.AIR;
    }

    public static boolean setState(World w, BlockPos p, IBlockState st, int flags) {
        ChunkStagedGen.Session s = s();
        return s != null ? s.setBlockState(p, st) : w.setBlockState(p, st, flags);
    }

    public static boolean setBlock(World w, int x, int y, int z, Block b) { return setBlock(w, x, y, z, b, 0, 3); }

    public static boolean setBlock(World w, int x, int y, int z, Block b, int meta, int flags) {
        return setState(w, new BlockPos(x, y, z), LegacyBlockIds.state(b, meta), flags);
    }

    public static boolean setBlockToAir(World w, int x, int y, int z) {
        return setState(w, new BlockPos(x, y, z), Blocks.AIR.getDefaultState(), 3);
    }

    public static boolean setBlockMetadataWithNotify(World w, int x, int y, int z, int meta, int flags) {
        return setBlock(w, x, y, z, getBlock(w, x, y, z), meta, flags);
    }

    /** Inside a job: detached tile entity of a block this job placed (copied in when the chunk gets its blocks). */
    public static TileEntity getTileEntity(World w, int x, int y, int z) {
        ChunkStagedGen.Session s = s();
        BlockPos p = new BlockPos(x, y, z);
        return s != null ? s.getTileEntity(p) : w.getTileEntity(p);
    }

    public static boolean blockExists(World w, int x, int y, int z) {
        return s() != null || LegacyWorld.blockExists(w, x, y, z);
    }

    // ---------------------------------------------------------------- height / sky / biome

    public static int getHeightValue(World w, int x, int z) {
        ChunkStagedGen.Session s = s();
        return s != null ? s.getHeight(x, z) : LegacyWorld.getHeightValue(w, x, z);
    }

    /** 1.7 {@code getTopBlock}: walks up from sea level to the last non-air block. */
    public static Block getTopBlock(World w, int x, int z) {
        if (s() == null) return LegacyWorld.getTopBlock(w, x, z);
        int y = 63;
        while (y < 255 && !isAirBlock(w, x, y + 1, z)) y++;
        return getBlock(w, x, y, z);
    }

    /** 1.7 {@code getTopSolidOrLiquidBlock} (y of the first free block above solid ground or liquid, leaves ignored). */
    public static int getTopSolidOrLiquidBlock(World w, int x, int z) {
        if (s() == null) return LegacyWorld.getTopSolidOrLiquidBlock(w, x, z);
        int y = Math.min(255, getHeightValue(w, x, z) + 1);
        for (; y >= 0; y--) {
            IBlockState st = getState(w, x, y, z);
            Material m = st.getMaterial();
            if (m.blocksMovement() && m != Material.LEAVES || m.isLiquid()) {
                return y + 1;
            }
        }
        return 0;
    }

    public static boolean canBlockSeeTheSky(World w, int x, int y, int z) {
        ChunkStagedGen.Session s = s();
        return s != null ? s.canSeeSky(new BlockPos(x, y, z)) : LegacyWorld.canBlockSeeTheSky(w, x, y, z);
    }

    public static Biome getBiome(World w, int x, int z) {
        ChunkStagedGen.Session s = s();
        return s != null ? s.getBiome(x, z) : w.getBiome(new BlockPos(x, 0, z));
    }

    /** 1.7 {@code canBlockFreeze} (temperature + still water, light ignored inside a job). */
    public static boolean canBlockFreeze(World w, int x, int y, int z, boolean byWater) {
        if (s() == null) return LegacyWorld.canBlockFreeze(w, x, y, z, byWater);
        BlockPos p = new BlockPos(x, y, z);
        if (getBiome(w, x, z).getTemperature(p) >= 0.15F || y < 0 || y >= 256) return false;
        IBlockState st = getState(w, p);
        if ((st.getBlock() != Blocks.WATER && st.getBlock() != Blocks.FLOWING_WATER) || st.getValue(BlockLiquid.LEVEL) != 0) return false;
        if (!byWater) return true;
        boolean allWater = isWater(w, x - 1, y, z) && isWater(w, x + 1, y, z) && isWater(w, x, y, z - 1) && isWater(w, x, y, z + 1);
        return !allWater;
    }

    private static boolean isWater(World w, int x, int y, int z) { return getState(w, x, y, z).getMaterial() == Material.WATER; }

    // ---------------------------------------------------------------- growth

    public static final String OP_SAPLING = "archaiclib:grow_sapling";
    public static final String OP_GROW = "archaiclib:grow";

    static {
        ChunkStagedGen.registerOp(OP_SAPLING, (w, p, r) -> LegacyWorld.growSapling(w, p.getX(), p.getY(), p.getZ(), r));
        ChunkStagedGen.registerOp(OP_GROW, (w, p, r) -> LegacyWorld.grow(w, r, p.getX(), p.getY(), p.getZ()));
    }

    /** Call once during init so stored operations of a saved world can run before any structure code. */
    public static void init() { ChunkStagedGen.register(); }

    /**
     * 1.7 sapling grow with the real vanilla tree generator. Inside a job it runs on the real world as soon as every
     * chunk the tree can reach is loaded and has the structure's blocks (no chunk is loaded for it).
     */
    public static void growSapling(World w, int x, int y, int z, Random rand) {
        if (s() == null) { LegacyWorld.growSapling(w, x, y, z, rand); return; }
        ChunkStagedGen.deferOp(w, OP_SAPLING, new BlockPos(x, y, z), 16, rand.nextLong());
    }

    /** 1.7 {@code IGrowable} grow (bone meal) with the real vanilla code; staged like {@link #growSapling}. */
    public static void grow(World w, Random rand, int x, int y, int z) {
        if (s() == null) { LegacyWorld.grow(w, rand, x, y, z); return; }
        ChunkStagedGen.deferOp(w, OP_GROW, new BlockPos(x, y, z), 8, rand.nextLong());
    }

    // ---------------------------------------------------------------- entities

    public static EntityPainting newPainting(World w, int x, int y, int z, int direction, String title) {
        return LegacyWorld.newPainting(w, x, y, z, direction, title);
    }

    /** Spawns when the entity's chunk exists (inside a job: after the blocks are committed). */
    public static boolean spawnEntity(World w, Entity e) {
        ChunkStagedGen.spawnEntity(w, e);
        return true;
    }
}
