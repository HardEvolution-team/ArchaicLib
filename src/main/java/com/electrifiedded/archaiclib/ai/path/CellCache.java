package com.electrifiedded.archaiclib.ai.path;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockMagma;
import net.minecraft.block.BlockVine;
import net.minecraft.block.BlockWeb;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/**
 * What each cell is, for the path search: computed once per cell from the block state (never loads chunks:
 * cells of unloaded chunks are walls), plus per-search cached break costs.
 */
public final class CellCache {

    /** the body can be in this cell (air, plants, open/wooden doors and gates, water, ladders) */
    public static final int PASS = 1;
    /** the top of this cell can be stood on */
    public static final int FLOOR = 1 << 1;
    public static final int WATER = 1 << 2;
    /** ladder or vine */
    public static final int CLIMB = 1 << 3;
    /** wooden door or fence gate: passable along {@link #AXIS_X} or Z only */
    public static final int DOOR = 1 << 4;
    public static final int AXIS_X = 1 << 5;
    /** lava, fire, cactus: never in the body */
    public static final int DEADLY = 1 << 6;
    /** higher than a full block (fences, walls): neither floor nor passable above */
    public static final int TALL = 1 << 7;
    public static final int LEAVES = 1 << 8;
    /** farmland */
    public static final int SOFT = 1 << 9;
    public static final int CROP = 1 << 10;
    public static final int WEB = 1 << 11;
    /** magma block, a floor that hurts */
    public static final int HOT = 1 << 12;
    /** a block that falls when its support goes (sand, gravel) */
    public static final int FALLING = 1 << 13;
    public static final int UNLOADED = 1 << 14;

    final World world;
    final PathRules rules;
    private final LongIntMap types = new LongIntMap(4096);
    private final LongMap<Double> breaks = new LongMap<>(256);
    private final BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
    private Chunk chunk;
    private int chunkX = Integer.MIN_VALUE, chunkZ;

    public CellCache(World world, PathRules rules) {
        this.world = world;
        this.rules = rules;
    }

    IBlockState state(int x, int y, int z) {
        if (y < 0 || y > 255) return null;
        int cx = x >> 4, cz = z >> 4;
        if (cx != chunkX || cz != chunkZ || chunk == null) {
            chunk = world.getChunkProvider().getLoadedChunk(cx, cz);
            chunkX = cx;
            chunkZ = cz;
        }
        return chunk == null ? null : chunk.getBlockState(x, y, z);
    }

    public int type(int x, int y, int z) {
        long k = LongMap.pack(x, y, z);
        int t = types.get(k);
        if (t == LongIntMap.MISSING) {
            t = compute(x, y, z);
            types.put(k, t);
        }
        return t;
    }

    private int compute(int x, int y, int z) {
        if (y < 0) return UNLOADED;
        if (y > 255) return PASS;
        IBlockState s = state(x, y, z);
        if (s == null) return UNLOADED;
        Block b = s.getBlock();
        Material m = s.getMaterial();
        if (m == Material.AIR) return PASS;
        if (m == Material.LAVA || m == Material.FIRE || b instanceof BlockCactus) return DEADLY;
        if (m == Material.WATER) return PASS | WATER;
        if (b instanceof BlockDoor) {
            if (m != Material.WOOD) return 0;
            IBlockState lower = s.getValue(BlockDoor.HALF) == BlockDoor.EnumDoorHalf.UPPER ? state(x, y - 1, z) : s;
            if (lower == null || !(lower.getBlock() instanceof BlockDoor)) return 0;
            return PASS | DOOR | (lower.getValue(BlockDoor.FACING).getAxis() == EnumFacing.Axis.X ? AXIS_X : 0);
        }
        if (b instanceof BlockFenceGate) {
            if (m != Material.WOOD) return TALL;
            return PASS | DOOR | (s.getValue(BlockFenceGate.FACING).getAxis() == EnumFacing.Axis.X ? AXIS_X : 0);
        }
        if (b instanceof BlockLadder || b instanceof BlockVine) return PASS | CLIMB;
        if (b instanceof BlockWeb) return PASS | WEB;
        int extra = 0;
        if (b instanceof BlockCrops || b == Blocks.REEDS || b == Blocks.PUMPKIN_STEM || b == Blocks.MELON_STEM) extra |= CROP;
        if (b instanceof BlockFalling) extra |= FALLING;
        mpos.setPos(x, y, z);
        AxisAlignedBB box = s.getCollisionBoundingBox(world, mpos);
        if (box == null || box == Block.NULL_AABB || box.maxY <= 0.0) return PASS | extra;
        if (box.maxY <= 0.2) return PASS | extra; // carpet, a thin snow layer, a closed bottom trapdoor
        if (box.maxY > 1.0) return TALL | extra;
        int t = FLOOR | extra;
        if (m == Material.LEAVES) t |= LEAVES;
        if (b == Blocks.FARMLAND) t |= SOFT;
        if (b instanceof BlockMagma) t |= HOT;
        return t;
    }

    /** The body may be in this cell as it is (the cell below is not a fence poking into it). */
    public boolean open(int x, int y, int z) {
        int t = type(x, y, z);
        return (t & PASS) != 0 && (t & DEADLY) == 0 && (type(x, y - 1, z) & TALL) == 0;
    }

    /** The mob can stand with its feet in this cell (floor under it, or water/ladder holding it). */
    public boolean standable(int x, int y, int z) {
        int t = type(x, y, z);
        if ((t & CLIMB) != 0) return true;
        // a swimming mob floats up: in water only the surface cell is a place to be
        if ((t & WATER) != 0) return (type(x, y + 1, z) & WATER) == 0;
        return (type(x, y - 1, z) & FLOOR) != 0;
    }

    /**
     * Cost of having the body in cell x,y,z: 0 when free, door/web extra, the break cost when it has to be
     * dug, INF when that is not allowed. {@code axisX}: direction of travel for doors (null = diagonal or
     * vertical, doors refused).
     */
    public double cellCost(int x, int y, int z, Boolean axisX) {
        int t = type(x, y, z);
        if ((t & DEADLY) != 0 || (t & UNLOADED) != 0) return PathCosts.INF;
        if ((t & PASS) != 0) {
            if ((type(x, y - 1, z) & TALL) != 0) return PathCosts.INF;
            double c = 0;
            if ((t & DOOR) != 0) {
                if (axisX == null || axisX != ((t & AXIS_X) != 0)) return PathCosts.INF;
                c += PathCosts.DOOR_OPEN;
            }
            if ((t & WEB) != 0) c += PathCosts.WEB;
            return c;
        }
        return breakCost(x, y, z);
    }

    /** Cell is free without digging (doors along {@code axisX} count as free). */
    public boolean free(int x, int y, int z, Boolean axisX) {
        return cellCost(x, y, z, axisX) < PathCosts.BREAK_EXTRA;
    }

    double breakCost(int x, int y, int z) {
        long k = LongMap.pack(x, y, z);
        Double c = breaks.get(k);
        if (c == null) {
            IBlockState s = state(x, y, z);
            if (s == null) {
                c = PathCosts.INF;
            } else {
                BlockPos p = new BlockPos(x, y, z);
                c = s.getBlockHardness(world, p) < 0 || s.getBlock().hasTileEntity(s) ? PathCosts.INF : rules.breakCost(world, p, s);
                // never dig out from under sand or gravel, nor into a liquid
                if (c < PathCosts.INF && ((type(x, y + 1, z) & FALLING) != 0 || liquidNear(x, y, z))) c = PathCosts.INF;
                // nor the block a torch, a door, a sign or a marker is fixed to
                if (c < PathCosts.INF && com.electrifiedded.archaiclib.ai.TerrainAssist.supportsSomething(world, p)) c = PathCosts.INF;
            }
            breaks.put(k, c);
        }
        return c;
    }

    private boolean liquidNear(int x, int y, int z) {
        return liquid(x, y + 1, z) || liquid(x + 1, y, z) || liquid(x - 1, y, z) || liquid(x, y, z + 1) || liquid(x, y, z - 1);
    }

    private boolean liquid(int x, int y, int z) {
        IBlockState s = state(x, y, z);
        return s != null && s.getMaterial().isLiquid();
    }

    /** Drop all cached cell types around a changed block (executor validation uses a fresh cache instead). */
    public int cachedCells() {
        return types.size();
    }
}
