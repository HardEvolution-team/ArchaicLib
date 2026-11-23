package com.electrifiedded.archaiclib.legacy.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * A 1.12 block that keeps the raw 1.7 metadata (0..15) as its only state property, so ported code can keep
 * reasoning in "meta" ({@code world.getBlockMetadata}, {@code setBlockMetadataWithNotify}, meta-switched bounds)
 * instead of being rewritten around enum properties.
 * <ul>
 *   <li>{@link #getLegacyBounds(int)}: 1.7 {@code setBlockBoundsBasedOnState}, per meta, in 0..1 block units
 *       (min/max may be given in any order, as old code often did).</li>
 *   <li>{@link #hasLegacyCollision(int)}: false = old {@code getCollisionBoundingBoxFromPool} returning null.</li>
 *   <li>{@link #legacyPlacementMeta}: 1.7 {@code onBlockPlaced} return value.</li>
 *   <li>{@link #setLegacyRenderType}: -1 = TESR only (no baked model), 0 = normal model.</li>
 *   <li>{@link #setLegacyTranslucent}: 1.7 {@code getRenderBlockPass() == 1}.</li>
 * </ul>
 * Static helpers {@link #meta(IBlockAccess, BlockPos)}, {@link #setMeta(World, BlockPos, int, int)} work on any block.
 * Register a state mapper ignoring {@link #META} (or a blockstate with "meta=0..15" variants) on the client.
 */
public class LegacyMetaBlock extends Block {
    public static final PropertyInteger META = PropertyInteger.create("meta", 0, 15);

    private int legacyRenderType = 0;
    private boolean translucent = false;
    private boolean fullCube = true;

    public LegacyMetaBlock(Material material) {
        super(material);
        setDefaultState(blockState.getBaseState().withProperty(META, 0));
    }

    public LegacyMetaBlock(Material material, MapColor color) {
        super(material, color);
        setDefaultState(blockState.getBaseState().withProperty(META, 0));
    }

    // ---- configuration ------------------------------------------------------------------------------------------

    /** -1: invisible to the chunk renderer (TESR draws it); 0 / anything else: baked model. */
    public LegacyMetaBlock setLegacyRenderType(int type) {
        this.legacyRenderType = type;
        return this;
    }

    public LegacyMetaBlock setLegacyTranslucent(boolean translucent) {
        this.translucent = translucent;
        return this;
    }

    /** 1.7 {@code isOpaqueCube()/renderAsNormalBlock()} both false when {@code full == false}. */
    public LegacyMetaBlock setLegacyFullCube(boolean full) {
        this.fullCube = full;
        this.fullBlock = full;
        this.lightOpacity = full ? 255 : 0;
        return this;
    }

    // ---- legacy hooks -------------------------------------------------------------------------------------------

    /** Bounds for this meta as {minX,minY,minZ,maxX,maxY,maxZ}; null = full block. */
    @Nullable
    protected float[] getLegacyBounds(int meta) {
        return null;
    }

    protected boolean hasLegacyCollision(int meta) {
        return true;
    }

    /** 1.7 {@code onBlockPlaced(world,x,y,z,side,hitX,hitY,hitZ,itemMeta)}; side uses the 1.7 numbering (= EnumFacing index). */
    protected int legacyPlacementMeta(World world, BlockPos pos, int side, float hx, float hy, float hz, int itemMeta) {
        return itemMeta;
    }

    // ---- static helpers -----------------------------------------------------------------------------------------

    public static int meta(IBlockState state) {
        return state.getBlock().getMetaFromState(state);
    }

    public static int meta(IBlockAccess world, BlockPos pos) {
        return meta(world.getBlockState(pos));
    }

    /** 1.7 {@code setBlockMetadataWithNotify}. */
    @SuppressWarnings("deprecation")
    public static boolean setMeta(World world, BlockPos pos, int meta, int flags) {
        IBlockState s = world.getBlockState(pos);
        return world.setBlockState(pos, s.getBlock().getStateFromMeta(meta & 15), flags);
    }

    @SuppressWarnings("deprecation")
    public static IBlockState state(Block block, int meta) {
        return block.getStateFromMeta(meta & 15);
    }

    public static AxisAlignedBB box(float[] b) {
        return new AxisAlignedBB(b[0], b[1], b[2], b[3], b[4], b[5]);
    }

    // ---- 1.12 plumbing ------------------------------------------------------------------------------------------

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, META);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(META);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(META, meta & 15);
    }

    @Override
    public int damageDropped(IBlockState state) {
        return 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return state(this, legacyPlacementMeta(world, pos, facing.getIndex(), hitX, hitY, hitZ, meta));
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        float[] b = getLegacyBounds(meta(state));
        return b == null ? FULL_BLOCK_AABB : box(b);
    }

    @Nullable
    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        if (!hasLegacyCollision(meta(state))) return NULL_AABB;
        return state.getBoundingBox(world, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
        return fullCube;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state) {
        return fullCube;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return fullCube ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    @Override
    @SuppressWarnings("deprecation")
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return legacyRenderType == -1 ? EnumBlockRenderType.ENTITYBLOCK_ANIMATED : EnumBlockRenderType.MODEL;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return translucent ? BlockRenderLayer.TRANSLUCENT : (fullCube ? BlockRenderLayer.SOLID : BlockRenderLayer.CUTOUT);
    }
}
