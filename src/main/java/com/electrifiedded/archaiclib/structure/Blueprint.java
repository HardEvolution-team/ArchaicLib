package com.electrifiedded.archaiclib.structure;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.common.util.Constants;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A structure to build, stored in the vanilla structure template format (the {@code .nbt} files of structure
 * blocks), so anything saved with a structure block can be used and anything captured here loads in a
 * structure block. Two optional extra keys are understood (vanilla ignores them):
 * <ul>
 *     <li>{@code archaicFront}: horizontal index of the side the entrance is on (the side that should face
 *     the player placing it); guessed from the doors when missing;</li>
 *     <li>{@code archaicGround}: how many layers of the template lie below the ground (foundation, cellar,
 *     a well shaft); 0 when missing.</li>
 * </ul>
 * Blocks are kept in template-local coordinates; {@link Placement} maps them into the world with a rotation
 * and a mirror, exactly like {@link Template#addBlocksToWorld} would. Tile entity data is read but never
 * used for building (no duplicating chest contents through a blueprint).
 */
public final class Blueprint {

    public static final class Entry {
        public final BlockPos pos;
        public final IBlockState state;

        public Entry(BlockPos pos, IBlockState state) {
            this.pos = pos;
            this.state = state;
        }
    }

    public final BlockPos size;
    public final List<Entry> blocks;
    public final EnumFacing front;
    public final int ground;
    public final String author;
    private final Map<BlockPos, IBlockState> byPos = new HashMap<>();

    public Blueprint(BlockPos size, List<Entry> blocks, EnumFacing front, int ground, String author) {
        this.size = size;
        this.blocks = Collections.unmodifiableList(blocks);
        this.front = front == null ? guessFront(size, blocks) : front;
        this.ground = Math.max(0, Math.min(ground, size.getY() - 1));
        this.author = author == null ? "" : author;
        for (Entry e : blocks) byPos.put(e.pos, e.state);
    }

    /** State at a template-local position; null outside the template or for structure void. */
    public IBlockState at(BlockPos local) {
        return byPos.get(local);
    }

    public int countNonAir() {
        int n = 0;
        for (Entry e : blocks) if (e.state.getBlock() != Blocks.AIR) n++;
        return n;
    }

    // --- reading and writing ----------------------------------------------------------------------------

    /** Reads a vanilla template compound (also older ones: the data fixer of the running game is applied). */
    public static Blueprint fromNBT(NBTTagCompound tag) {
        NBTTagCompound t = tag;
        try {
            if (!t.hasKey("DataVersion", Constants.NBT.TAG_ANY_NUMERIC)) t.setInteger("DataVersion", 500);
            t = net.minecraftforge.fml.common.FMLCommonHandler.instance().getDataFixer()
                    .process(net.minecraft.util.datafix.FixTypes.STRUCTURE, t);
        } catch (Throwable ignored) {
            t = tag;
        }
        NBTTagList sz = t.getTagList("size", Constants.NBT.TAG_INT);
        BlockPos size = new BlockPos(sz.getIntAt(0), sz.getIntAt(1), sz.getIntAt(2));
        NBTTagList pal = t.getTagList("palette", Constants.NBT.TAG_COMPOUND);
        IBlockState[] palette = new IBlockState[pal.tagCount()];
        for (int i = 0; i < palette.length; i++) palette[i] = NBTUtil.readBlockState(pal.getCompoundTagAt(i));
        NBTTagList bl = t.getTagList("blocks", Constants.NBT.TAG_COMPOUND);
        List<Entry> out = new ArrayList<>(bl.tagCount());
        for (int i = 0; i < bl.tagCount(); i++) {
            NBTTagCompound b = bl.getCompoundTagAt(i);
            NBTTagList p = b.getTagList("pos", Constants.NBT.TAG_INT);
            int id = b.getInteger("state");
            if (id < 0 || id >= palette.length) continue;
            IBlockState s = palette[id];
            if (s.getBlock() == Blocks.STRUCTURE_VOID) continue;
            out.add(new Entry(new BlockPos(p.getIntAt(0), p.getIntAt(1), p.getIntAt(2)), s));
        }
        EnumFacing front = t.hasKey("archaicFront", Constants.NBT.TAG_ANY_NUMERIC)
                ? EnumFacing.byHorizontalIndex(t.getInteger("archaicFront")) : null;
        return new Blueprint(size, out, front, t.getInteger("archaicGround"), t.getString("author"));
    }

    /** Writes a vanilla-compatible template compound (no entities, no tile entity data). */
    public NBTTagCompound toNBT() {
        NBTTagCompound t = new NBTTagCompound();
        List<IBlockState> palette = new ArrayList<>();
        Map<IBlockState, Integer> ids = new HashMap<>();
        NBTTagList bl = new NBTTagList();
        for (Entry e : blocks) {
            Integer id = ids.get(e.state);
            if (id == null) {
                id = palette.size();
                palette.add(e.state);
                ids.put(e.state, id);
            }
            NBTTagCompound b = new NBTTagCompound();
            b.setTag("pos", ints(e.pos.getX(), e.pos.getY(), e.pos.getZ()));
            b.setInteger("state", id);
            bl.appendTag(b);
        }
        NBTTagList pal = new NBTTagList();
        for (IBlockState s : palette) pal.appendTag(NBTUtil.writeBlockState(new NBTTagCompound(), s));
        t.setTag("palette", pal);
        t.setTag("blocks", bl);
        t.setTag("entities", new NBTTagList());
        t.setTag("size", ints(size.getX(), size.getY(), size.getZ()));
        t.setString("author", author);
        t.setInteger("DataVersion", 1343);
        t.setInteger("archaicFront", front.getHorizontalIndex());
        t.setInteger("archaicGround", ground);
        return t;
    }

    private static NBTTagList ints(int... v) {
        NBTTagList l = new NBTTagList();
        for (int i : v) l.appendTag(new net.minecraft.nbt.NBTTagInt(i));
        return l;
    }

    public static Blueprint read(InputStream in) throws IOException {
        return fromNBT(CompressedStreamTools.readCompressed(in));
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(toNBT(), out);
        return out.toByteArray();
    }

    public static Blueprint fromBytes(byte[] data) throws IOException {
        return read(new ByteArrayInputStream(data));
    }

    /** Copies a box of the world (vanilla structure block rules; structure void blocks are left out). */
    public static Blueprint capture(World world, BlockPos min, BlockPos size, EnumFacing front, int ground, String author) {
        Template t = new Template();
        t.takeBlocksFromWorld(world, min, size, false, Blocks.STRUCTURE_VOID);
        t.setAuthor(author == null ? "" : author);
        NBTTagCompound tag = t.writeToNBT(new NBTTagCompound());
        tag.setInteger("archaicFront", front == null ? EnumFacing.SOUTH.getHorizontalIndex() : front.getHorizontalIndex());
        tag.setInteger("archaicGround", ground);
        return fromNBT(tag);
    }

    /** The side the doors are on (outer doors: the side of the box they are closest to), else south. */
    static EnumFacing guessFront(BlockPos size, List<Entry> blocks) {
        int[] votes = new int[4];
        for (Entry e : blocks) {
            Block b = e.state.getBlock();
            if (!(b instanceof BlockDoor) || e.state.getValue(BlockDoor.HALF) != BlockDoor.EnumDoorHalf.LOWER) continue;
            int x = e.pos.getX(), z = e.pos.getZ();
            int dn = z, ds = size.getZ() - 1 - z, dw = x, de = size.getX() - 1 - x;
            int m = Math.min(Math.min(dn, ds), Math.min(dw, de));
            if (m == dn) votes[EnumFacing.NORTH.getHorizontalIndex()]++;
            else if (m == ds) votes[EnumFacing.SOUTH.getHorizontalIndex()]++;
            else if (m == dw) votes[EnumFacing.WEST.getHorizontalIndex()]++;
            else votes[EnumFacing.EAST.getHorizontalIndex()]++;
        }
        int best = EnumFacing.SOUTH.getHorizontalIndex();
        for (int i = 0; i < 4; i++) if (votes[i] > votes[best]) best = i;
        return EnumFacing.byHorizontalIndex(best);
    }

    // --- placing into the world ---------------------------------------------------------------------------

    /**
     * Where and how a blueprint sits in the world: {@code min} is the lowest corner of the rotated box. Positions
     * and states are transformed like vanilla templates do it (mirror first, then rotation).
     */
    public static final class Placement {
        public final Blueprint blueprint;
        public final BlockPos min;
        public final Rotation rotation;
        public final Mirror mirror;
        /** size of the rotated box */
        public final BlockPos size;
        private final BlockPos shift;

        public Placement(Blueprint bp, BlockPos min, Rotation rotation, Mirror mirror) {
            this.blueprint = bp;
            this.min = min;
            this.rotation = rotation;
            this.mirror = mirror;
            BlockPos far = new BlockPos(bp.size.getX() - 1, bp.size.getY() - 1, bp.size.getZ() - 1);
            BlockPos a = transform(BlockPos.ORIGIN, mirror, rotation);
            BlockPos b = transform(far, mirror, rotation);
            BlockPos lo = new BlockPos(Math.min(a.getX(), b.getX()), 0, Math.min(a.getZ(), b.getZ()));
            this.shift = lo;
            this.size = new BlockPos(Math.abs(a.getX() - b.getX()) + 1, bp.size.getY(), Math.abs(a.getZ() - b.getZ()) + 1);
        }

        /**
         * The placement that puts the blueprint's front toward {@code towards} (plus {@code extraTurns} quarter
         * turns clockwise), its box starting right in front of {@code anchor} along {@code facing}, centered on
         * it sideways (shifted by {@code side} blocks to the right), with its ground layer at {@code anchor}'s Y.
         */
        public static Placement inFrontOf(Blueprint bp, BlockPos anchor, EnumFacing facing, int extraTurns, boolean mirrored,
                                           int side, int up) {
            Mirror m = mirrored ? Mirror.LEFT_RIGHT : Mirror.NONE;
            EnumFacing wantFront = facing.getOpposite();
            EnumFacing f0 = m.mirror(bp.front);
            Rotation r = Rotation.NONE;
            for (Rotation c : Rotation.values()) if (c.rotate(f0) == wantFront) r = c;
            for (int i = 0; i < (extraTurns & 3); i++) r = r.add(Rotation.CLOCKWISE_90);
            Placement probe = new Placement(bp, BlockPos.ORIGIN, r, m);
            int sx = probe.size.getX(), sz = probe.size.getZ();
            EnumFacing right = facing.rotateY();
            // box corner nearest the anchor on the facing axis, centered sideways
            int along = facing.getAxis() == EnumFacing.Axis.X ? sx : sz;
            int across = facing.getAxis() == EnumFacing.Axis.X ? sz : sx;
            BlockPos nearCenter = anchor.offset(facing, 1).offset(right, side);
            BlockPos farCorner = nearCenter.offset(facing, along - 1);
            BlockPos leftEdge = nearCenter.offset(right, -(across - 1) / 2);
            BlockPos rightEdge = leftEdge.offset(right, across - 1);
            int minX, minZ;
            if (facing.getAxis() == EnumFacing.Axis.X) {
                minX = Math.min(nearCenter.getX(), farCorner.getX());
                minZ = Math.min(leftEdge.getZ(), rightEdge.getZ());
            } else {
                minX = Math.min(leftEdge.getX(), rightEdge.getX());
                minZ = Math.min(nearCenter.getZ(), farCorner.getZ());
            }
            return new Placement(bp, new BlockPos(minX, anchor.getY() - bp.ground + up, minZ), r, m);
        }

        private static BlockPos transform(BlockPos p, Mirror m, Rotation r) {
            return Template.transformedBlockPos(new net.minecraft.world.gen.structure.template.PlacementSettings().setMirror(m).setRotation(r), p);
        }

        public BlockPos toWorld(BlockPos local) {
            BlockPos t = transform(local, mirror, rotation);
            return min.add(t.getX() - shift.getX(), t.getY(), t.getZ() - shift.getZ());
        }

        public IBlockState stateFor(IBlockState local) {
            return local.withMirror(mirror).withRotation(rotation);
        }

        public BlockPos max() {
            return min.add(size.getX() - 1, size.getY() - 1, size.getZ() - 1);
        }

        public net.minecraft.util.math.AxisAlignedBB bounds() {
            return new net.minecraft.util.math.AxisAlignedBB(min, max().add(1, 1, 1));
        }

        public boolean contains(BlockPos p) {
            return p.getX() >= min.getX() && p.getY() >= min.getY() && p.getZ() >= min.getZ()
                    && p.getX() < min.getX() + size.getX() && p.getY() < min.getY() + size.getY() && p.getZ() < min.getZ() + size.getZ();
        }

        /** World Y of the ground the blueprint stands on (its first layer above ground). */
        public int groundY() {
            return min.getY() + blueprint.ground;
        }

        /** Every block of the blueprint, in world coordinates with transformed states. */
        public Map<BlockPos, IBlockState> worldBlocks() {
            Map<BlockPos, IBlockState> out = new java.util.LinkedHashMap<>();
            for (Entry e : blueprint.blocks) out.put(toWorld(e.pos), stateFor(e.state));
            return out;
        }
    }
}
