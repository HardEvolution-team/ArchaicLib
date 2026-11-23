package com.electrifiedded.archaiclib.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A see-through "hologram" of blocks that are not there yet (blueprint projections, schematic previews).
 * The block models are baked once into a display list (with the ghost blocks as neighbours, so inner faces are
 * culled and everything is fully lit) and drawn with constant alpha. Blocks drawn by tile entity renderers
 * (chests, beds, signs) have no model: they get a box outline instead.
 * <p>
 * Use one instance per projection; call {@link #rebuild} when the set of blocks changes, {@link #render} from a
 * TESR or a world render event, and {@link #dispose} when the projection goes away.
 */
@SideOnly(Side.CLIENT)
public final class GhostBlockRenderer {

    private int list = -1;
    private BlockPos origin = BlockPos.ORIGIN;
    private final List<AxisAlignedBB> outlines = new ArrayList<>();
    private int count;

    public boolean isEmpty() {
        return count == 0 && outlines.isEmpty();
    }

    /** Bakes {@code blocks} (world positions); {@code origin} is the point the list is drawn relative to. */
    public void rebuild(Map<BlockPos, IBlockState> blocks, BlockPos origin) {
        this.origin = origin;
        outlines.clear();
        count = 0;
        if (list < 0) list = GLAllocation.generateDisplayLists(1);
        BlockRendererDispatcher d = Minecraft.getMinecraft().getBlockRendererDispatcher();
        Access access = new Access(blocks);
        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        GlStateManager.glNewList(list, GL11.GL_COMPILE);
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        b.setTranslation(-origin.getX(), -origin.getY(), -origin.getZ());
        for (Map.Entry<BlockPos, IBlockState> e : blocks.entrySet()) {
            IBlockState s = e.getValue();
            if (s.getBlock() == Blocks.AIR) continue;
            boolean drawn = false;
            if (s.getRenderType() != EnumBlockRenderType.ENTITYBLOCK_ANIMATED && s.getRenderType() != EnumBlockRenderType.INVISIBLE) {
                try {
                    drawn = d.renderBlock(s, e.getKey(), access, b);
                } catch (RuntimeException ignored) {
                    drawn = false;
                }
            }
            if (drawn) count++;
            else outlines.add(new AxisAlignedBB(e.getKey()).offset(-origin.getX(), -origin.getY(), -origin.getZ()).shrink(0.0625));
        }
        b.setTranslation(0, 0, 0);
        t.draw();
        GlStateManager.glEndList();
    }

    /**
     * Draws the hologram; {@code x, y, z} is where {@code origin} is relative to the camera (for a TESR at
     * {@code tePos}: {@code x + origin.x - tePos.x}, ...).
     */
    public void render(double x, double y, double z, float alpha) {
        if (list < 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        RenderHelper.disableStandardItemLighting();
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_CONSTANT_ALPHA, GL11.GL_ONE_MINUS_CONSTANT_ALPHA);
        GL14.glBlendColor(1F, 1F, 1F, alpha);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.enableCull();
        GlStateManager.depthMask(false);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.callList(list);
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL14.glBlendColor(0F, 0F, 0F, 0F);
        if (!outlines.isEmpty()) {
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GL11.glLineWidth(1.5F);
            for (AxisAlignedBB bb : outlines) RenderGlobal.drawSelectionBoundingBox(bb, 0.6F, 0.85F, 1.0F, Math.min(1F, alpha + 0.3F));
            GlStateManager.enableTexture2D();
        }
        GlStateManager.disableBlend();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    public void dispose() {
        if (list >= 0) GLAllocation.deleteDisplayLists(list);
        list = -1;
        count = 0;
        outlines.clear();
    }

    /** The ghost blocks as their own little world: everything else is air, all of it fully lit. */
    private static final class Access implements IBlockAccess {
        private final Map<BlockPos, IBlockState> blocks;

        Access(Map<BlockPos, IBlockState> blocks) {
            this.blocks = blocks;
        }

        @Override
        public TileEntity getTileEntity(BlockPos pos) {
            return null;
        }

        @Override
        public int getCombinedLight(BlockPos pos, int lightValue) {
            return 15 << 20 | 15 << 4;
        }

        @Override
        public IBlockState getBlockState(BlockPos pos) {
            IBlockState s = blocks.get(pos);
            return s == null ? Blocks.AIR.getDefaultState() : s;
        }

        @Override
        public boolean isAirBlock(BlockPos pos) {
            return getBlockState(pos).getBlock() == Blocks.AIR;
        }

        @Override
        public Biome getBiome(BlockPos pos) {
            return Biomes.PLAINS;
        }

        @Override
        public int getStrongPower(BlockPos pos, EnumFacing direction) {
            return 0;
        }

        @Override
        public WorldType getWorldType() {
            return WorldType.DEFAULT;
        }

        @Override
        public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
            return getBlockState(pos).isSideSolid(this, pos, side);
        }
    }
}
