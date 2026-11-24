package com.electrifiedded.archaiclib.legacy.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;

import java.util.Random;

/**
 * 1.7.10 {@code StructureComponent} API on top of the 1.12 class, so decompiled 1.7 structure pieces (strongholds,
 * villages, mod dungeons) port almost line by line. Coordinates use the 1.7 {@code coordBaseMode} int (0 south, 1 west,
 * 2 north, 3 east, -1 none) and blocks are placed as {@code Block + meta} without the 1.12 mirror/rotation;
 * orientation comes from {@link #getMetadataWithOffset} exactly as in 1.7 (only the vanilla blocks it knew rotate).
 */
public abstract class LegacyStructureComponent extends StructureComponent {
    /** 1.7 coordBaseMode. */
    protected int coordBaseMode = -1;

    protected LegacyStructureComponent() {}

    protected LegacyStructureComponent(int type) { super(type); }

    /** Sets both the 1.7 int and the 1.12 facing (used by the inherited get*WithOffset). */
    public void setCoordBaseMode(int mode) {
        coordBaseMode = mode;
        setCoordBaseMode(mode < 0 ? null : EnumFacing.byHorizontalIndex(mode));
    }

    @Override
    protected void writeStructureToNBT(net.minecraft.nbt.NBTTagCompound c) {
        c.setInteger("LegacyCBM", coordBaseMode);
        writeLegacyNBT(c);
    }

    @Override
    protected void readStructureFromNBT(net.minecraft.nbt.NBTTagCompound c, net.minecraft.world.gen.structure.template.TemplateManager m) {
        coordBaseMode = c.hasKey("LegacyCBM") ? c.getInteger("LegacyCBM") : getCoordBaseMode() == null ? -1 : getCoordBaseMode().getHorizontalIndex();
        readLegacyNBT(c);
    }

    /** 1.7 writeStructureToNBT. */
    protected void writeLegacyNBT(net.minecraft.nbt.NBTTagCompound c) {}

    /** 1.7 readStructureFromNBT. */
    protected void readLegacyNBT(net.minecraft.nbt.NBTTagCompound c) {}

    protected BlockPos legacyPos(int x, int y, int z) { return new BlockPos(getXWithOffset(x, z), getYWithOffset(y), getZWithOffset(x, z)); }

    @SuppressWarnings("deprecation")
    protected static IBlockState state(Block b, int meta) { return b == null ? Blocks.AIR.getDefaultState() : b.getStateFromMeta(meta); }

    /** 1.7 placeBlockAtCurrentPosition (only inside the box, flag 2). */
    protected void placeBlockAtCurrentPosition(World w, Block b, int meta, int x, int y, int z, StructureBoundingBox bb) {
        BlockPos p = legacyPos(x, y, z);
        if (bb.isVecInside(p)) w.setBlockState(p, state(b, meta), 2);
    }

    /** 1.7 getBlockAtCurrentPosition (air outside the box). */
    protected Block getBlockAtCurrentPosition(World w, int x, int y, int z, StructureBoundingBox bb) {
        BlockPos p = legacyPos(x, y, z);
        return bb.isVecInside(p) ? w.getBlockState(p).getBlock() : Blocks.AIR;
    }

    protected void fillWithAir(World w, StructureBoundingBox bb, int x1, int y1, int z1, int x2, int y2, int z2) {
        for (int y = y1; y <= y2; y++) for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) placeBlockAtCurrentPosition(w, Blocks.AIR, 0, x, y, z, bb);
    }

    protected void fillWithBlocks(World w, StructureBoundingBox bb, int x1, int y1, int z1, int x2, int y2, int z2, Block edge, Block inside, boolean onlyReplaceSolid) {
        fillWithMetadataBlocks(w, bb, x1, y1, z1, x2, y2, z2, edge, 0, inside, 0, onlyReplaceSolid);
    }

    protected void fillWithMetadataBlocks(World w, StructureBoundingBox bb, int x1, int y1, int z1, int x2, int y2, int z2, Block edge, int edgeMeta, Block inside, int insideMeta, boolean onlyReplaceSolid) {
        for (int y = y1; y <= y2; y++)
            for (int x = x1; x <= x2; x++)
                for (int z = z1; z <= z2; z++) {
                    if (onlyReplaceSolid && getBlockAtCurrentPosition(w, x, y, z, bb).getDefaultState().getMaterial() == Material.AIR) continue;
                    if (y != y1 && y != y2 && x != x1 && x != x2 && z != z1 && z != z2) placeBlockAtCurrentPosition(w, inside, insideMeta, x, y, z, bb);
                    else placeBlockAtCurrentPosition(w, edge, edgeMeta, x, y, z, bb);
                }
    }

    protected void fillWithRandomizedBlocks(World w, StructureBoundingBox bb, int x1, int y1, int z1, int x2, int y2, int z2, boolean onlyReplaceSolid, Random r, BlockSelector sel) {
        for (int y = y1; y <= y2; y++)
            for (int x = x1; x <= x2; x++)
                for (int z = z1; z <= z2; z++) {
                    if (onlyReplaceSolid && getBlockAtCurrentPosition(w, x, y, z, bb).getDefaultState().getMaterial() == Material.AIR) continue;
                    sel.selectBlocks(r, x, y, z, y == y1 || y == y2 || x == x1 || x == x2 || z == z1 || z == z2);
                    placeBlockAtCurrentPosition(w, sel.getBlock(), sel.getSelectedBlockMetaData(), x, y, z, bb);
                }
    }

    protected void randomlyFillWithBlocks(World w, StructureBoundingBox bb, Random r, float chance, int x1, int y1, int z1, int x2, int y2, int z2, Block edge, Block inside, boolean onlyReplaceSolid) {
        for (int y = y1; y <= y2; y++)
            for (int x = x1; x <= x2; x++)
                for (int z = z1; z <= z2; z++) {
                    if (r.nextFloat() > chance) continue;
                    if (onlyReplaceSolid && getBlockAtCurrentPosition(w, x, y, z, bb).getDefaultState().getMaterial() == Material.AIR) continue;
                    if (y != y1 && y != y2 && x != x1 && x != x2 && z != z1 && z != z2) placeBlockAtCurrentPosition(w, inside, 0, x, y, z, bb);
                    else placeBlockAtCurrentPosition(w, edge, 0, x, y, z, bb);
                }
    }

    protected void randomlyPlaceBlock(World w, StructureBoundingBox bb, Random r, float chance, int x, int y, int z, Block b, int meta) {
        if (r.nextFloat() < chance) placeBlockAtCurrentPosition(w, b, meta, x, y, z, bb);
    }

    /** 1.7 clearCurrentPositionBlocksUpwards. */
    protected void clearCurrentPositionBlocksUpwards(World w, int x, int y, int z, StructureBoundingBox bb) {
        BlockPos p = legacyPos(x, y, z);
        if (!bb.isVecInside(p)) return;
        while (!w.isAirBlock(p) && p.getY() < 255) {
            w.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
            p = p.up();
        }
    }

    /** 1.7 func_151554_b: fill downwards with a block until a solid / non-replaceable block. */
    protected void fillDownwards(World w, Block b, int meta, int x, int y, int z, StructureBoundingBox bb) {
        int i = getXWithOffset(x, z), j = getYWithOffset(y), k = getZWithOffset(x, z);
        if (!bb.isVecInside(new BlockPos(i, j, k))) return;
        while (j > 1) {
            BlockPos p = new BlockPos(i, j, k);
            IBlockState s = w.getBlockState(p);
            if (!w.isAirBlock(p) && !s.getMaterial().isLiquid()) break;
            w.setBlockState(p, state(b, meta), 2);
            j--;
        }
    }

    /** 1.7 generateStructureChestContents: places a vanilla chest and fills it with the 1.7 algorithm. */
    protected boolean generateStructureChestContents(World w, StructureBoundingBox bb, Random r, int x, int y, int z, LegacyChestContent[] loot, int picks) {
        BlockPos p = legacyPos(x, y, z);
        if (!bb.isVecInside(p) || w.getBlockState(p).getBlock() == Blocks.CHEST) return false;
        w.setBlockState(p, Blocks.CHEST.correctFacing(w, p, Blocks.CHEST.getDefaultState()), 2);
        TileEntity te = w.getTileEntity(p);
        if (te instanceof IInventory inv) LegacyChestContent.generateChestContents(r, loot, inv, picks);
        return true;
    }

    /** 1.7 placeDoorAtCurrentPosition (ItemDoor.placeDoorBlock with the 1.7 meta = facing index). */
    protected void placeDoorAtCurrentPosition(World w, StructureBoundingBox bb, Random r, int x, int y, int z, int meta) {
        BlockPos p = legacyPos(x, y, z);
        if (bb.isVecInside(p)) {
            EnumFacing f = EnumFacing.byHorizontalIndex((meta + 1) & 3);
            net.minecraft.item.ItemDoor.placeDoor(w, p, f, Blocks.OAK_DOOR, false);
        }
    }

    /** 1.7 StructureComponent.getMetadataWithOffset (rails, doors, vanilla stairs, ladders, buttons, directional blocks). */
    protected int getMetadataWithOffset(Block b, int meta) {
        int m = coordBaseMode;
        if (b == Blocks.RAIL) {
            if (m == 1 || m == 3) return meta == 1 ? 0 : 1;
        } else if (b == Blocks.OAK_DOOR || b == Blocks.IRON_DOOR) {
            if (m == 0) {
                if (meta == 0) return 2;
                if (meta == 2) return 0;
            } else {
                if (m == 1) return meta + 1 & 3;
                if (m == 3) return meta + 3 & 3;
            }
        } else if (b == Blocks.STONE_STAIRS || b == Blocks.OAK_STAIRS || b == Blocks.NETHER_BRICK_STAIRS || b == Blocks.STONE_BRICK_STAIRS || b == Blocks.SANDSTONE_STAIRS) {
            if (m == 0) {
                if (meta == 2) return 3;
                if (meta == 3) return 2;
            } else if (m == 1) {
                if (meta == 0) return 2;
                if (meta == 1) return 3;
                if (meta == 2) return 0;
                if (meta == 3) return 1;
            } else if (m == 3) {
                if (meta == 0) return 2;
                if (meta == 1) return 3;
                if (meta == 2) return 1;
                if (meta == 3) return 0;
            }
        } else if (b == Blocks.LADDER) {
            if (m == 0) {
                if (meta == 2) return 3;
                if (meta == 3) return 2;
            } else if (m == 1) {
                if (meta == 2) return 4;
                if (meta == 3) return 5;
                if (meta == 4) return 2;
                if (meta == 5) return 3;
            } else if (m == 3) {
                if (meta == 2) return 5;
                if (meta == 3) return 4;
                if (meta == 4) return 2;
                if (meta == 5) return 3;
            }
        } else if (b == Blocks.STONE_BUTTON) {
            if (m == 0) {
                if (meta == 3) return 4;
                if (meta == 4) return 3;
            } else if (m == 1) {
                if (meta == 3) return 1;
                if (meta == 4) return 2;
                if (meta == 2) return 3;
                if (meta == 1) return 4;
            } else if (m == 3) {
                if (meta == 3) return 2;
                if (meta == 4) return 1;
                if (meta == 2) return 3;
                if (meta == 1) return 4;
            }
        } else if (b == Blocks.TRIPWIRE_HOOK || b instanceof BlockHorizontal && !(b instanceof net.minecraft.block.BlockChest)) {
            if (m == 0) {
                if (meta == 0 || meta == 2) return (meta + 2) & 3; // Direction.rotateOpposite
            } else if (m == 1) {
                if (meta == 2) return 1;
                if (meta == 0) return 3;
                if (meta == 1) return 2;
                if (meta == 3) return 0;
            } else if (m == 3) {
                if (meta == 2) return 3;
                if (meta == 0) return 1;
                if (meta == 1) return 2;
                if (meta == 3) return 0;
            }
        } else if (b == Blocks.PISTON || b == Blocks.STICKY_PISTON || b == Blocks.LEVER || b == Blocks.DISPENSER || b instanceof BlockDirectional) {
            if (m == 0) {
                if (meta == 2 || meta == 3) return meta ^ 1; // Facing.oppositeSide
            } else if (m == 1) {
                if (meta == 2) return 4;
                if (meta == 3) return 5;
                if (meta == 4) return 2;
                if (meta == 5) return 3;
            } else if (m == 3) {
                if (meta == 2) return 5;
                if (meta == 3) return 4;
                if (meta == 4) return 2;
                if (meta == 5) return 3;
            }
        }
        return meta;
    }

    /** 1.7 StructureComponent.BlockSelector (block + meta instead of a state). */
    public abstract static class BlockSelector {
        protected Block field_151562_a = Blocks.AIR;
        protected int selectedBlockMetaData;

        public abstract void selectBlocks(Random r, int x, int y, int z, boolean edge);

        public Block getBlock() { return field_151562_a; }
        public int getSelectedBlockMetaData() { return selectedBlockMetaData; }
    }
}
