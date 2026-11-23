package com.electrifiedded.archaiclib.legacy.client;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The classic 1.5-1.7 Techne/ModelBase tile renderer: bind texture, translate to (x+0.5, y+1.5, z+0.5),
 * rotate around Y, scale (1,-1,-1), render the model parts at 1/16. Subclasses only supply texture, rotation and
 * the actual part rendering. Optional blending for translucent "light" models, optional full-bright.
 */
@SideOnly(Side.CLIENT)
public abstract class LegacyTechneTESR<T extends TileEntity> extends TileEntitySpecialRenderer<T> {

    protected abstract ResourceLocation getTexture(T te, float partialTicks);

    /** Degrees around Y (1.7 renderers usually used {@code meta * 90}). */
    protected float getYRotation(T te) {
        return 0F;
    }

    protected abstract void renderModel(T te, float scale);

    protected boolean blend(T te) {
        return false;
    }

    protected boolean fullBright(T te) {
        return false;
    }

    /** Frame index for a texture animated by ticks (1 frame per {@code ticksPerFrame} ticks), world-time based. */
    public static int animFrame(TileEntity te, int frames, int ticksPerFrame) {
        long t = te.getWorld() == null ? 0 : te.getWorld().getTotalWorldTime();
        return (int) ((t / Math.max(1, ticksPerFrame)) % frames);
    }

    @Override
    public void render(T te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        bindTexture(getTexture(te, partialTicks));
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y + 1.5F, (float) z + 0.5F);
        GlStateManager.rotate(getYRotation(te), 0F, 1F, 0F);
        GlStateManager.scale(1F, -1F, -1F);
        boolean blend = blend(te);
        boolean bright = fullBright(te);
        if (blend) {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        }
        if (bright) {
            net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit, 240F, 240F);
        }
        GlStateManager.enableTexture2D();
        GlStateManager.color(1F, 1F, 1F, 1F);
        renderModel(te, 0.0625F);
        if (blend) GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}
