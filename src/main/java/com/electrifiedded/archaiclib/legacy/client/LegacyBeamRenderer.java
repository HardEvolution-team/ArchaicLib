package com.electrifiedded.archaiclib.legacy.client;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * The common 1.7 "glowing translucent box" beam pattern (lasers, tractor beams, force fields):
 * untextured, unlit, full-bright, alpha-blended, no depth write, double sided.
 * Call {@link #preBeam()}, any number of {@link #drawBox}, then {@link #postBeam()}.
 */
@SideOnly(Side.CLIENT)
public final class LegacyBeamRenderer {
    private LegacyBeamRenderer() {}

    private static float lastX, lastY;

    public static void preBeam() {
        preBeam(240.0F);
    }

    /** @param light lightmap coordinate (1.7 code often used 160 or 240) */
    public static void preBeam(float light) {
        lastX = OpenGlHelper.lastBrightnessX;
        lastY = OpenGlHelper.lastBrightnessY;
        GlStateManager.pushMatrix();
        RenderHelper.disableStandardItemLighting();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light, light);
        GlStateManager.disableAlpha();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
    }

    public static void postBeam() {
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.enableAlpha();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.popMatrix();
    }

    /** All six faces of {@code b} in one colour (coordinates in the current GL space, e.g. TESR-relative). */
    public static void drawBox(AxisAlignedBB b, float r, float g, float bl, float a) {
        Tessellator t = Tessellator.getInstance();
        BufferBuilder buf = t.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        // north (min z)
        v(buf, b.minX, b.minY, b.minZ, r, g, bl, a); v(buf, b.minX, b.maxY, b.minZ, r, g, bl, a);
        v(buf, b.maxX, b.maxY, b.minZ, r, g, bl, a); v(buf, b.maxX, b.minY, b.minZ, r, g, bl, a);
        // south (max z)
        v(buf, b.minX, b.minY, b.maxZ, r, g, bl, a); v(buf, b.maxX, b.minY, b.maxZ, r, g, bl, a);
        v(buf, b.maxX, b.maxY, b.maxZ, r, g, bl, a); v(buf, b.minX, b.maxY, b.maxZ, r, g, bl, a);
        // down
        v(buf, b.minX, b.minY, b.minZ, r, g, bl, a); v(buf, b.maxX, b.minY, b.minZ, r, g, bl, a);
        v(buf, b.maxX, b.minY, b.maxZ, r, g, bl, a); v(buf, b.minX, b.minY, b.maxZ, r, g, bl, a);
        // up
        v(buf, b.minX, b.maxY, b.minZ, r, g, bl, a); v(buf, b.minX, b.maxY, b.maxZ, r, g, bl, a);
        v(buf, b.maxX, b.maxY, b.maxZ, r, g, bl, a); v(buf, b.maxX, b.maxY, b.minZ, r, g, bl, a);
        // west (min x)
        v(buf, b.minX, b.minY, b.minZ, r, g, bl, a); v(buf, b.minX, b.minY, b.maxZ, r, g, bl, a);
        v(buf, b.minX, b.maxY, b.maxZ, r, g, bl, a); v(buf, b.minX, b.maxY, b.minZ, r, g, bl, a);
        // east (max x)
        v(buf, b.maxX, b.minY, b.minZ, r, g, bl, a); v(buf, b.maxX, b.maxY, b.minZ, r, g, bl, a);
        v(buf, b.maxX, b.maxY, b.maxZ, r, g, bl, a); v(buf, b.maxX, b.minY, b.maxZ, r, g, bl, a);
        t.draw();
    }

    private static void v(BufferBuilder buf, double x, double y, double z, float r, float g, float b, float a) {
        buf.pos(x, y, z).color(r, g, b, a).endVertex();
    }
}
