package com.electrifiedded.archaiclib.structure;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBanner;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockPressurePlate;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockReed;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockTripWire;
import net.minecraft.block.BlockTripWireHook;
import net.minecraft.block.BlockVine;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.IFluidBlock;

/**
 * How a block of a blueprint is built by hand, survival style: which item it takes, when the block already in
 * the world counts as done, and in which order blocks go in (attached blocks after their support, liquids last).
 */
public final class BlockItems {

    /** Build order of a block. */
    public enum Phase {
        /** nothing to place (air, or a block nobody can place by hand) */
        NONE,
        /** full and half blocks, built bottom-up */
        SOLID,
        /** blocks that hang on or stand on something: torches, doors, ladders, crops, carpets... */
        ATTACHED,
        /** water and lava from buckets */
        LIQUID
    }

    private BlockItems() {}

    public static boolean isAir(IBlockState s) {
        return s.getBlock() == Blocks.AIR || s.getMaterial() == Material.AIR;
    }

    public static boolean isLiquid(IBlockState s) {
        return s.getBlock() instanceof BlockLiquid || s.getBlock() instanceof IFluidBlock;
    }

    /** Blocks a hand cannot place: they are left as they are (neither required nor cleared). */
    public static boolean isUnbuildable(IBlockState s) {
        Block b = s.getBlock();
        return b == Blocks.BEDROCK || b == Blocks.BARRIER || b == Blocks.STRUCTURE_BLOCK || b == Blocks.STRUCTURE_VOID
                || b == Blocks.COMMAND_BLOCK || b == Blocks.CHAIN_COMMAND_BLOCK || b == Blocks.REPEATING_COMMAND_BLOCK
                || b == Blocks.MOB_SPAWNER || b == Blocks.PORTAL || b == Blocks.END_PORTAL || b == Blocks.END_GATEWAY
                || b == Blocks.FIRE || b == Blocks.PISTON_EXTENSION || b == Blocks.PISTON_HEAD || b == Blocks.MONSTER_EGG
                || b == Blocks.END_PORTAL_FRAME;
    }

    /** Whether the block is placed by placing another one (upper door half, bed head, upper half of tall grass). */
    public static boolean isCompanion(IBlockState s) {
        Block b = s.getBlock();
        if (b instanceof BlockDoor) return s.getValue(BlockDoor.HALF) == BlockDoor.EnumDoorHalf.UPPER;
        if (b instanceof BlockBed) return s.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD;
        if (b instanceof BlockDoublePlant) return s.getValue(BlockDoublePlant.HALF) == BlockDoublePlant.EnumBlockHalf.UPPER;
        return false;
    }

    public static Phase phase(IBlockState s) {
        if (isAir(s) || isUnbuildable(s) || isCompanion(s)) return Phase.NONE;
        Block b = s.getBlock();
        if (isLiquid(s)) {
            // only the sources are put down; flowing water comes by itself
            if (b instanceof BlockLiquid && s.getValue(BlockLiquid.LEVEL) != 0) return Phase.NONE;
            return Phase.LIQUID;
        }
        if (b instanceof BlockTorch || b instanceof BlockLadder || b instanceof BlockDoor || b instanceof BlockBed
                || b instanceof BlockCarpet || b instanceof BlockButton || b instanceof BlockLever || b instanceof BlockPressurePlate
                || b instanceof BlockBush || b instanceof BlockRedstoneWire || b instanceof BlockRailBase || b instanceof BlockSign
                || b instanceof BlockTrapDoor || b instanceof BlockFlowerPot || b instanceof BlockSnow || b instanceof BlockVine
                || b instanceof BlockCocoa || b instanceof BlockTripWire || b instanceof BlockTripWireHook || b instanceof BlockBanner
                || b instanceof BlockSkull || b instanceof BlockCactus || b instanceof BlockReed || b instanceof BlockRedstoneDiode
                || b == Blocks.CAKE || b == Blocks.WEB) {
            return Phase.ATTACHED;
        }
        return Phase.SOLID;
    }

    /**
     * Whether {@code current} already is what the blueprint asks for at that spot. Lenient where survival play
     * cannot be exact: grass may be dirt, crops of any age, farmland of any moisture, leaves without the decay
     * bits, any water where water is wanted, doors open or shut.
     */
    public static boolean matches(IBlockState current, IBlockState wanted) {
        Block w = wanted.getBlock(), c = current.getBlock();
        if (isAir(wanted)) return isAir(current);
        if (isLiquid(wanted)) {
            return isLiquid(current) && current.getMaterial() == wanted.getMaterial();
        }
        if (w == Blocks.GRASS) return c == Blocks.GRASS || c == Blocks.DIRT && current.getValue(net.minecraft.block.BlockDirt.VARIANT) == net.minecraft.block.BlockDirt.DirtType.DIRT;
        if (w == Blocks.FARMLAND) return c == Blocks.FARMLAND;
        if (c != w) {
            // the two stone slab blocks, lit and unlit furnaces and lamps
            return w == Blocks.LIT_FURNACE && c == Blocks.FURNACE || w == Blocks.FURNACE && c == Blocks.LIT_FURNACE
                    || w == Blocks.LIT_REDSTONE_LAMP && c == Blocks.REDSTONE_LAMP || w == Blocks.REDSTONE_LAMP && c == Blocks.LIT_REDSTONE_LAMP;
        }
        if (w instanceof BlockCrops || w instanceof BlockRedstoneWire || w instanceof BlockDoublePlant || w instanceof BlockReed
                || w instanceof BlockCactus || w == Blocks.FIRE || w instanceof BlockBed) {
            return true;
        }
        if (w instanceof BlockDoor) return current.getValue(BlockDoor.HALF) == wanted.getValue(BlockDoor.HALF)
                && (wanted.getValue(BlockDoor.HALF) == BlockDoor.EnumDoorHalf.UPPER || current.getValue(BlockDoor.FACING) == wanted.getValue(BlockDoor.FACING));
        if (w instanceof BlockLeaves) return (w.getMetaFromState(current) & 3) == (w.getMetaFromState(wanted) & 3);
        if (w instanceof BlockTrapDoor) return current.getValue(BlockTrapDoor.FACING) == wanted.getValue(BlockTrapDoor.FACING)
                && current.getValue(BlockTrapDoor.HALF) == wanted.getValue(BlockTrapDoor.HALF);
        if (w instanceof net.minecraft.block.BlockFenceGate) return current.getValue(net.minecraft.block.BlockFenceGate.FACING).getAxis()
                == wanted.getValue(net.minecraft.block.BlockFenceGate.FACING).getAxis();
        if (w instanceof BlockLever || w instanceof BlockButton || w instanceof BlockPressurePlate || w instanceof BlockRedstoneDiode) {
            // powered or not does not matter
            return true;
        }
        return w.getMetaFromState(current) == w.getMetaFromState(wanted);
    }

    /** Plain natural ground a wall or foundation may stay made of (below the ground line nobody rebuilds it). */
    public static boolean isNaturalGround(IBlockState s) {
        Block b = s.getBlock();
        return b == Blocks.STONE || b == Blocks.DIRT || b == Blocks.GRASS || b == Blocks.COBBLESTONE || b == Blocks.SANDSTONE
                || b == Blocks.CLAY || b == Blocks.HARDENED_CLAY || b == Blocks.STAINED_HARDENED_CLAY || b == Blocks.NETHERRACK
                || b == Blocks.MYCELIUM || b == Blocks.COAL_ORE || b == Blocks.IRON_ORE;
    }

    /** Whether a wanted block is plain enough that natural ground in its place is accepted underground. */
    public static boolean acceptsGroundInstead(IBlockState wanted) {
        Block b = wanted.getBlock();
        return b == Blocks.COBBLESTONE || b == Blocks.STONE || b == Blocks.DIRT || b == Blocks.GRASS || b == Blocks.GRAVEL
                || b == Blocks.SANDSTONE || b == Blocks.SAND || b == Blocks.MOSSY_COBBLESTONE;
    }

    /**
     * Plants that grow on farmland (wheat, carrots, potatoes, beetroot, melon/pumpkin stems, nether wart and mod
     * crops of the "Crop" plant type). A builder makes the field, sowing is the farmer's job: these are neither
     * required nor cleared.
     */
    public static boolean isFarmCrop(IBlockState s) {
        Block b = s.getBlock();
        if (b instanceof BlockCrops || b instanceof net.minecraft.block.BlockStem || b instanceof net.minecraft.block.BlockNetherWart) return true;
        if (b instanceof net.minecraftforge.common.IPlantable && !(b instanceof BlockReed) && !(b instanceof BlockCactus)) {
            try {
                return ((net.minecraftforge.common.IPlantable) b).getPlantType(null, BlockPos.ORIGIN) == net.minecraftforge.common.EnumPlantType.Crop;
            } catch (RuntimeException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * The block a player actually puts down for {@code wanted}: dirt where farmland, a grass path or grass goes
     * (tilled with a hoe, flattened with a shovel or left to grow over afterwards), the block itself otherwise.
     */
    public static IBlockState placedState(IBlockState wanted) {
        Block b = wanted.getBlock();
        if (b == Blocks.FARMLAND || b == Blocks.GRASS_PATH || b == Blocks.GRASS || b == Blocks.MYCELIUM) return Blocks.DIRT.getDefaultState();
        return wanted;
    }

    /** A still source block of water or lava (level 0). */
    public static boolean isSource(IBlockState s, Material material) {
        return s.getMaterial() == material && s.getBlock() instanceof BlockLiquid && s.getValue(BlockLiquid.LEVEL) == 0;
    }

    /**
     * Whether a bucket may be filled here without using the water up: a water source with two source neighbours
     * and solid ground or another source under it (vanilla refills such a block at once).
     */
    public static boolean isInfiniteWater(World world, BlockPos p) {
        if (!isSource(world.getBlockState(p), Material.WATER)) return false;
        int n = 0;
        for (net.minecraft.util.EnumFacing f : net.minecraft.util.EnumFacing.Plane.HORIZONTAL) {
            if (isSource(world.getBlockState(p.offset(f)), Material.WATER)) n++;
        }
        if (n < 2) return false;
        IBlockState below = world.getBlockState(p.down());
        return below.getMaterial().isSolid() || isSource(below, Material.WATER);
    }

    /** Blocks in the way that go without a tool in a moment and never drop anything worth keeping. */
    public static boolean isSoftClutter(IBlockState s) {
        Block b = s.getBlock();
        return b == Blocks.TALLGRASS || b == Blocks.DEADBUSH || b == Blocks.YELLOW_FLOWER || b == Blocks.RED_FLOWER
                || b == Blocks.DOUBLE_PLANT || b == Blocks.SNOW_LAYER || b == Blocks.VINE
                || b == Blocks.FIRE;
    }

    /**
     * The item a player would use to put this block down ({@link ItemStack#EMPTY} when none: companion halves,
     * flowing liquid, unbuildable blocks). Double slabs take two slabs, sources take a full bucket, grass and
     * farmland take dirt (turned into grass/farmland on the spot), crops take their seed.
     */
    public static ItemStack itemFor(World world, BlockPos pos, IBlockState s) {
        if (phase(s) == Phase.NONE) return ItemStack.EMPTY;
        Block b = s.getBlock();
        if (b == Blocks.WATER || b == Blocks.FLOWING_WATER) return new ItemStack(Items.WATER_BUCKET);
        if (b == Blocks.LAVA || b == Blocks.FLOWING_LAVA) return new ItemStack(Items.LAVA_BUCKET);
        if (b instanceof IFluidBlock) {
            ItemStack bucket = net.minecraftforge.fluids.FluidUtil.getFilledBucket(new net.minecraftforge.fluids.FluidStack(((IFluidBlock) b).getFluid(), 1000));
            return bucket == null ? ItemStack.EMPTY : bucket;
        }
        if (b == Blocks.GRASS || b == Blocks.FARMLAND || b == Blocks.GRASS_PATH || b == Blocks.MYCELIUM) return new ItemStack(Blocks.DIRT);
        if (b == Blocks.LIT_FURNACE) return new ItemStack(Blocks.FURNACE);
        if (b == Blocks.LIT_REDSTONE_LAMP) return new ItemStack(Blocks.REDSTONE_LAMP);
        if (b == Blocks.LIT_REDSTONE_ORE) return new ItemStack(Blocks.REDSTONE_ORE);
        if (b instanceof BlockBed) return new ItemStack(Items.BED, 1, net.minecraft.item.EnumDyeColor.RED.getMetadata());
        ItemStack st;
        try {
            st = b.getItem(world, pos, s);
        } catch (RuntimeException e) {
            st = ItemStack.EMPTY;
        }
        if (st == null || st.isEmpty()) {
            Item i = Item.getItemFromBlock(b);
            if (i == net.minecraft.init.Items.AIR) return ItemStack.EMPTY;
            st = new ItemStack(i, 1, b.damageDropped(s));
        }
        st = st.copy();
        st.setCount(b instanceof BlockSlab && ((BlockSlab) b).isDouble() ? 2 : 1);
        return st;
    }

    /** Whether {@code have} can be used where {@code need} is asked for (beds of any colour, same item and meta otherwise). */
    public static boolean fits(ItemStack have, ItemStack need) {
        if (have.isEmpty() || need.isEmpty() || have.getItem() != need.getItem()) return false;
        if (need.getItem() == Items.BED) return true;
        return !have.getHasSubtypes() || have.getMetadata() == need.getMetadata();
    }

    /** Gravity blocks need something under them first. */
    public static boolean falls(IBlockState s) {
        return s.getBlock() instanceof BlockFalling;
    }

    /** Puts the block down with its companion part (upper door half, bed head, upper plant half). */
    public static void place(World world, BlockPos pos, IBlockState s, IBlockState companion, int bedColor) {
        Block b = s.getBlock();
        if (b instanceof BlockDoor) {
            IBlockState up = companion != null && companion.getBlock() == b ? companion
                    : b.getDefaultState().withProperty(BlockDoor.HALF, BlockDoor.EnumDoorHalf.UPPER);
            world.setBlockState(pos, s, 2);
            world.setBlockState(pos.up(), up, 2);
            world.notifyNeighborsOfStateChange(pos, b, false);
            world.notifyNeighborsOfStateChange(pos.up(), b, false);
            return;
        }
        if (b instanceof BlockDoublePlant) {
            world.setBlockState(pos, s, 2);
            world.setBlockState(pos.up(), b.getDefaultState().withProperty(BlockDoublePlant.HALF, BlockDoublePlant.EnumBlockHalf.UPPER), 2);
            return;
        }
        if (b instanceof BlockBed) {
            BlockPos head = pos.offset(s.getValue(BlockBed.FACING));
            IBlockState h = companion != null && companion.getBlock() == b ? companion
                    : s.withProperty(BlockBed.PART, BlockBed.EnumPartType.HEAD);
            world.setBlockState(pos, s.withProperty(BlockBed.OCCUPIED, false), 10);
            world.setBlockState(head, h.withProperty(BlockBed.OCCUPIED, false), 10);
            for (BlockPos p : new BlockPos[]{pos, head}) {
                net.minecraft.tileentity.TileEntity te = world.getTileEntity(p);
                if (te instanceof net.minecraft.tileentity.TileEntityBed) {
                    ((net.minecraft.tileentity.TileEntityBed) te).setColor(net.minecraft.item.EnumDyeColor.byMetadata(bedColor));
                }
            }
            world.notifyNeighborsRespectDebug(pos, b, false);
            world.notifyNeighborsRespectDebug(head, b, false);
            return;
        }
        world.setBlockState(pos, s, 3);
    }

    /** Where the companion of a block sits (null when it has none). */
    public static BlockPos companionPos(BlockPos pos, IBlockState s) {
        Block b = s.getBlock();
        if (b instanceof BlockDoor || b instanceof BlockDoublePlant) return pos.up();
        if (b instanceof BlockBed) return pos.offset(s.getValue(BlockBed.FACING));
        return null;
    }
}
