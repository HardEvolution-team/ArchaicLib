package com.electrifiedded.archaiclib.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.IFluidBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * World checks for NPCs that dig: liquids next to a block about to be mined, lava around an entity,
 * safe places to stand, quenching lava like a water bucket would, and whether a tool actually harvests
 * a block. Works with vanilla liquids and Forge fluid blocks.
 */
public final class DigSafety {

    private DigSafety() {}

    public static boolean isLiquid(IBlockState s) {
        return s.getMaterial().isLiquid() || s.getBlock() instanceof IFluidBlock;
    }

    public static boolean isLava(IBlockState s) {
        if (s.getMaterial() == Material.LAVA) return true;
        Block b = s.getBlock();
        if (b instanceof IFluidBlock) {
            net.minecraftforge.fluids.Fluid f = ((IFluidBlock) b).getFluid();
            return f != null && f.getTemperature() >= 1000;
        }
        return false;
    }

    /** Dangerous to touch: lava, hot fluids, fire, magma. */
    public static boolean isHot(IBlockState s) {
        Block b = s.getBlock();
        return isLava(s) || b == Blocks.FIRE || b == Blocks.MAGMA || s.getMaterial() == Material.FIRE;
    }

    /** A still (source) liquid block. */
    public static boolean isSource(World world, BlockPos pos) {
        IBlockState s = world.getBlockState(pos);
        Block b = s.getBlock();
        if (b instanceof BlockLiquid) return s.getValue(BlockLiquid.LEVEL) == 0;
        if (b instanceof IFluidBlock) return ((IFluidBlock) b).canDrain(world, pos);
        return false;
    }

    /** Liquid blocks touching {@code pos} on any of its six sides. */
    public static List<BlockPos> liquidNeighbors(World world, BlockPos pos) {
        List<BlockPos> out = new ArrayList<>();
        for (EnumFacing f : EnumFacing.values()) {
            BlockPos n = pos.offset(f);
            if (world.isBlockLoaded(n) && isLiquid(world.getBlockState(n))) out.add(n);
        }
        return out;
    }

    /** Hot blocks (lava, fire, magma) within {@code grow} blocks of an entity box. */
    public static List<BlockPos> hotAround(World world, AxisAlignedBB box, int grow) {
        List<BlockPos> out = new ArrayList<>();
        BlockPos min = new BlockPos(MathHelper.floor(box.minX) - grow, MathHelper.floor(box.minY) - 1, MathHelper.floor(box.minZ) - grow);
        BlockPos max = new BlockPos(MathHelper.floor(box.maxX) + grow, MathHelper.floor(box.maxY) + grow, MathHelper.floor(box.maxZ) + grow);
        for (BlockPos p : BlockPos.getAllInBoxMutable(min, max)) {
            if (world.isBlockLoaded(p) && isHot(world.getBlockState(p))) out.add(p.toImmutable());
        }
        return out;
    }

    public static boolean hotNear(World world, AxisAlignedBB box, int grow) {
        return !hotAround(world, box, grow).isEmpty();
    }

    /**
     * Feet position with a solid, non-falling-into floor, two free non-liquid blocks above it and no hot
     * block within one block of the body (sides, floor, above the head).
     */
    public static boolean isSafeToStand(World world, BlockPos feet) {
        if (!world.isBlockLoaded(feet)) return false;
        IBlockState floor = world.getBlockState(feet.down());
        if (!floor.getMaterial().blocksMovement() || floor.getMaterial() == Material.LEAVES || isHot(floor)) return false;
        for (int h = 0; h < 2; h++) {
            IBlockState s = world.getBlockState(feet.up(h));
            if (s.getMaterial().blocksMovement() || isLiquid(s) || isHot(s)) return false;
        }
        for (BlockPos p : BlockPos.getAllInBoxMutable(feet.add(-1, -1, -1), feet.add(1, 2, 1))) {
            if (isHot(world.getBlockState(p))) return false;
        }
        return true;
    }

    /**
     * Nearest (to {@code prefer}) safe standing spot within {@code radius} blocks horizontally and
     * {@code dy} vertically of {@code around} that also passes {@code extra} (may be null).
     */
    public static BlockPos safeStandSpot(World world, BlockPos around, int radius, int dy, BlockPos prefer, Predicate<BlockPos> extra) {
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        BlockPos ref = prefer == null ? around : prefer;
        for (BlockPos p : BlockPos.getAllInBoxMutable(around.add(-radius, -dy, -radius), around.add(radius, dy, radius))) {
            double d = p.distanceSq(ref);
            if (d >= bd) continue;
            if (!isSafeToStand(world, p)) continue;
            BlockPos imm = p.toImmutable();
            if (extra != null && !extra.test(imm)) continue;
            best = imm;
            bd = d;
        }
        return best;
    }

    /**
     * What pouring water on a lava block does: a source turns into obsidian, flowing lava into cobblestone.
     * Plays the fizz and smoke. Returns false when the block is not lava.
     */
    public static boolean quench(World world, BlockPos pos) {
        IBlockState s = world.getBlockState(pos);
        if (!isLava(s)) return false;
        world.setBlockState(pos, isSource(world, pos) ? Blocks.OBSIDIAN.getDefaultState() : Blocks.COBBLESTONE.getDefaultState());
        fizz(world, pos);
        return true;
    }

    public static void fizz(World world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 0.5F,
                2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F);
        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_LARGE, pos.getX() + 0.5, pos.getY() + 0.8,
                    pos.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0.0);
        }
    }

    /** Sand, gravel, concrete powder... anything that falls when the block under it is removed. */
    public static boolean falls(IBlockState s) {
        return s.getBlock() instanceof BlockFalling;
    }

    /**
     * Whether breaking {@code state} with {@code tool} yields drops, the way it does for a player
     * (stone needs a pickaxe, obsidian a diamond one...). Hardness and the block's harvest tool/level are used.
     */
    public static boolean canHarvest(IBlockState state, ItemStack tool) {
        if (state.getMaterial().isToolNotRequired()) return true;
        Block b = state.getBlock();
        String type = b.getHarvestTool(state);
        if (type == null) return !tool.isEmpty() && tool.canHarvestBlock(state);
        if (tool.isEmpty()) return false;
        int level = tool.getItem().getHarvestLevel(tool, type, null, state);
        if (level < 0) return tool.canHarvestBlock(state);
        return level >= b.getHarvestLevel(state);
    }
}
