package com.electrifiedded.archaiclib.legacy.world;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;
import java.util.function.Supplier;

/**
 * 1.7-style nether-portal clone for a custom dimension: any frame block, 2x3 inner area (frame 4x5, corners optional),
 * entities touching it travel between {@code homeDim} (default overworld) and {@code targetDim} through a
 * {@link LegacyFrameTeleporter}. Light it with {@link #tryToCreatePortal(World, BlockPos)} from a fire block / item.
 * meta 1 = along X, 2 = along Z (same as the 1.7 portal).
 */
public class LegacyPortalBlock extends Block {
    public static final PropertyEnum<EnumFacing.Axis> AXIS = PropertyEnum.create("axis", EnumFacing.Axis.class, EnumFacing.Axis.X, EnumFacing.Axis.Z);
    protected static final AxisAlignedBB X_AABB = new AxisAlignedBB(0.0, 0.0, 0.375, 1.0, 1.0, 0.625);
    protected static final AxisAlignedBB Z_AABB = new AxisAlignedBB(0.375, 0.0, 0.0, 0.625, 1.0, 1.0);

    protected final Supplier<Block> frame;
    protected final java.util.function.IntSupplier targetDim;
    public int homeDim = 0;
    /** players only (like the 1.7 Shadow World portals) or every non-boss entity */
    public boolean playersOnly = true;

    public LegacyPortalBlock(Supplier<Block> frame, java.util.function.IntSupplier targetDim) {
        super(Material.PORTAL);
        this.frame = frame;
        this.targetDim = targetDim;
        setDefaultState(blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.X));
        setTickRandomly(true);
        setBlockUnbreakable();
        setResistance(6000000.0F);
        setLightLevel(0.75F);
        setSoundType(SoundType.GLASS);
    }

    public Block frame() { return frame.get(); }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, AXIS); }
    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(AXIS, (meta & 3) == 2 ? EnumFacing.Axis.Z : EnumFacing.Axis.X); }
    @Override public int getMetaFromState(IBlockState s) { return s.getValue(AXIS) == EnumFacing.Axis.Z ? 2 : 1; }
    @Override public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) { return s.getValue(AXIS) == EnumFacing.Axis.Z ? Z_AABB : X_AABB; }
    @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) { return NULL_AABB; }
    @Override public boolean isOpaqueCube(IBlockState s) { return false; }
    @Override public boolean isFullCube(IBlockState s) { return false; }
    @Override public int quantityDropped(Random r) { return 0; }
    @Override public ItemStack getItem(World w, BlockPos p, IBlockState s) { return ItemStack.EMPTY; }
    @Override public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos p, EnumFacing f) { return BlockFaceShape.UNDEFINED; }
    @Override @SideOnly(Side.CLIENT) public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.TRANSLUCENT; }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockState s, IBlockAccess w, BlockPos p, EnumFacing f) {
        return w.getBlockState(p.offset(f)).getBlock() != this && super.shouldSideBeRendered(s, w, p, f);
    }

    /** @return true when a portal was created in the frame next to {@code pos} (pos = an inner air block). */
    public boolean tryToCreatePortal(World world, BlockPos pos) {
        Block fr = frame();
        int b0 = 0, b1 = 0;
        if (world.getBlockState(pos.west()).getBlock() == fr || world.getBlockState(pos.east()).getBlock() == fr) b0 = 1;
        if (world.getBlockState(pos.north()).getBlock() == fr || world.getBlockState(pos.south()).getBlock() == fr) b1 = 1;
        if (b0 == b1) return false;
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        if (world.isAirBlock(new BlockPos(x - b0, y, z - b1))) { x -= b0; z -= b1; }
        for (int l = -1; l <= 2; l++) {
            for (int i = -1; i <= 3; i++) {
                boolean edge = l == -1 || l == 2 || i == -1 || i == 3;
                if ((l != -1 && l != 2) || (i != -1 && i != 3)) {
                    BlockPos q = new BlockPos(x + b0 * l, y + i, z + b1 * l);
                    IBlockState st = world.getBlockState(q);
                    if (edge) { if (st.getBlock() != fr) return false; }
                    else if (!st.getBlock().isReplaceable(world, q) && st.getBlock() != this && !st.getMaterial().isReplaceable()) return false;
                }
            }
        }
        IBlockState st = getDefaultState().withProperty(AXIS, b0 == 1 ? EnumFacing.Axis.X : EnumFacing.Axis.Z);
        for (int l = 0; l < 2; l++)
            for (int i = 0; i < 3; i++)
                world.setBlockState(new BlockPos(x + b0 * l, y + i, z + b1 * l), st, 2);
        return true;
    }

    private boolean isMe(IBlockAccess w, int x, int y, int z) { return w.getBlockState(new BlockPos(x, y, z)).getBlock() == this; }
    private boolean isFrame(IBlockAccess w, int x, int y, int z) { return w.getBlockState(new BlockPos(x, y, z)).getBlock() == frame(); }

    @Override
    public void neighborChanged(IBlockState state, World w, BlockPos pos, Block blockIn, BlockPos fromPos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        int b0 = 0, b1 = 1;
        if (isMe(w, x - 1, y, z) || isMe(w, x + 1, y, z)) { b0 = 1; b1 = 0; }
        int i1 = y;
        while (isMe(w, x, i1 - 1, z)) i1--;
        if (!isFrame(w, x, i1 - 1, z)) { w.setBlockToAir(pos); return; }
        int j1 = 1;
        while (j1 < 4 && isMe(w, x, i1 + j1, z)) j1++;
        if (j1 == 3 && isFrame(w, x, i1 + j1, z)) {
            boolean f = isMe(w, x - 1, y, z) || isMe(w, x + 1, y, z);
            boolean f1 = isMe(w, x, y, z - 1) || isMe(w, x, y, z + 1);
            if (f && f1) w.setBlockToAir(pos);
            else if (!((isFrame(w, x + b0, y, z + b1) && isMe(w, x - b0, y, z - b1)) || (isFrame(w, x - b0, y, z - b1) && isMe(w, x + b0, y, z + b1))))
                w.setBlockToAir(pos);
        } else w.setBlockToAir(pos);
    }

    /** Teleporter for the destination world; override for custom arrival logic. */
    protected net.minecraftforge.common.util.ITeleporter teleporter(WorldServer dest) {
        return new LegacyFrameTeleporter(dest, this, frame());
    }

    /** Hook after a successful transfer (achievements etc.). */
    protected void onTravel(Entity e, int toDim) {}

    @Override
    public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (world.isRemote || entity.isRiding() || entity.isBeingRidden() || !entity.isNonBoss()) return;
        if (playersOnly && !(entity instanceof EntityPlayerMP)) return;
        if (entity.timeUntilPortal > 0) { entity.timeUntilPortal = 10; return; }
        entity.timeUntilPortal = 10;
        int target = world.provider.getDimension() == targetDim.getAsInt() ? homeDim : targetDim.getAsInt();
        WorldServer dest = world.getMinecraftServer().getWorld(target);
        entity.changeDimension(target, teleporter(dest));
        onTravel(entity, target);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World w, BlockPos pos, Random r) {
        if (r.nextInt(100) == 0)
            w.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.BLOCKS, 0.5F, r.nextFloat() * 0.4F + 0.8F, false);
        boolean xAxis = state.getValue(AXIS) == EnumFacing.Axis.X;
        for (int l = 0; l < 4; l++) {
            int dir = r.nextInt(2) * 2 - 1;
            double px = pos.getX() + r.nextFloat(), py = pos.getY() + r.nextFloat(), pz = pos.getZ() + r.nextFloat();
            double vx = (r.nextFloat() - 0.5) * 0.5, vy = (r.nextFloat() - 0.5) * 0.5, vz = (r.nextFloat() - 0.5) * 0.5;
            if (xAxis) { pz = pos.getZ() + 0.5 + 0.25 * dir; vz = r.nextFloat() * 2.0F * dir; }
            else { px = pos.getX() + 0.5 + 0.25 * dir; vx = r.nextFloat() * 2.0F * dir; }
            w.spawnParticle(EnumParticleTypes.PORTAL, px, py, pz, vx, vy, vz);
        }
    }

}
