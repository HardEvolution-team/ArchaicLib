package com.electrifiedded.archaiclib.legacy.client;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The six 1.7.10 {@code RenderBlocks.renderFaceXXX(block, x, y, z, icon)} calls on {@link LegacyTessellator}, with
 * render bounds (default full block) and the 1.7 vertex order / UV layout. Use inside
 * {@code startDrawingQuads()/draw()} with the block atlas bound.
 */
@SideOnly(Side.CLIENT)
public final class LegacyRenderBlocks {
    public double minX = 0, minY = 0, minZ = 0, maxX = 1, maxY = 1, maxZ = 1;
    private final LegacyTessellator t;

    public LegacyRenderBlocks() {
        this(LegacyTessellator.instance());
    }

    public LegacyRenderBlocks(LegacyTessellator t) {
        this.t = t;
    }

    public LegacyRenderBlocks setRenderBounds(double x1, double y1, double z1, double x2, double y2, double z2) {
        minX = x1;
        minY = y1;
        minZ = z1;
        maxX = x2;
        maxY = y2;
        maxZ = z2;
        return this;
    }

    private static double u(TextureAtlasSprite s, double f) {
        return s.getInterpolatedU(f * 16.0);
    }

    private static double v(TextureAtlasSprite s, double f) {
        return s.getInterpolatedV(f * 16.0);
    }

    public void renderFaceYNeg(double x, double y, double z, TextureAtlasSprite s) {
        double u0 = u(s, minX), u1 = u(s, maxX), v0 = v(s, minZ), v1 = v(s, maxZ);
        t.addVertexWithUV(x + minX, y + minY, z + maxZ, u0, v1);
        t.addVertexWithUV(x + minX, y + minY, z + minZ, u0, v0);
        t.addVertexWithUV(x + maxX, y + minY, z + minZ, u1, v0);
        t.addVertexWithUV(x + maxX, y + minY, z + maxZ, u1, v1);
    }

    public void renderFaceYPos(double x, double y, double z, TextureAtlasSprite s) {
        double u0 = u(s, minX), u1 = u(s, maxX), v0 = v(s, minZ), v1 = v(s, maxZ);
        t.addVertexWithUV(x + maxX, y + maxY, z + maxZ, u1, v1);
        t.addVertexWithUV(x + maxX, y + maxY, z + minZ, u1, v0);
        t.addVertexWithUV(x + minX, y + maxY, z + minZ, u0, v0);
        t.addVertexWithUV(x + minX, y + maxY, z + maxZ, u0, v1);
    }

    public void renderFaceZNeg(double x, double y, double z, TextureAtlasSprite s) {
        double u0 = u(s, 1.0 - maxX), u1 = u(s, 1.0 - minX), v0 = v(s, 1.0 - maxY), v1 = v(s, 1.0 - minY);
        t.addVertexWithUV(x + minX, y + maxY, z + minZ, u1, v0);
        t.addVertexWithUV(x + maxX, y + maxY, z + minZ, u0, v0);
        t.addVertexWithUV(x + maxX, y + minY, z + minZ, u0, v1);
        t.addVertexWithUV(x + minX, y + minY, z + minZ, u1, v1);
    }

    public void renderFaceZPos(double x, double y, double z, TextureAtlasSprite s) {
        double u0 = u(s, minX), u1 = u(s, maxX), v0 = v(s, 1.0 - maxY), v1 = v(s, 1.0 - minY);
        t.addVertexWithUV(x + minX, y + maxY, z + maxZ, u0, v0);
        t.addVertexWithUV(x + minX, y + minY, z + maxZ, u0, v1);
        t.addVertexWithUV(x + maxX, y + minY, z + maxZ, u1, v1);
        t.addVertexWithUV(x + maxX, y + maxY, z + maxZ, u1, v0);
    }

    public void renderFaceXNeg(double x, double y, double z, TextureAtlasSprite s) {
        double u0 = u(s, minZ), u1 = u(s, maxZ), v0 = v(s, 1.0 - maxY), v1 = v(s, 1.0 - minY);
        t.addVertexWithUV(x + minX, y + maxY, z + maxZ, u1, v0);
        t.addVertexWithUV(x + minX, y + maxY, z + minZ, u0, v0);
        t.addVertexWithUV(x + minX, y + minY, z + minZ, u0, v1);
        t.addVertexWithUV(x + minX, y + minY, z + maxZ, u1, v1);
    }

    public void renderFaceXPos(double x, double y, double z, TextureAtlasSprite s) {
        double u0 = u(s, 1.0 - maxZ), u1 = u(s, 1.0 - minZ), v0 = v(s, 1.0 - maxY), v1 = v(s, 1.0 - minY);
        t.addVertexWithUV(x + maxX, y + minY, z + maxZ, u0, v1);
        t.addVertexWithUV(x + maxX, y + minY, z + minZ, u1, v1);
        t.addVertexWithUV(x + maxX, y + maxY, z + minZ, u1, v0);
        t.addVertexWithUV(x + maxX, y + maxY, z + maxZ, u0, v0);
    }

    /** All six faces, one sprite. */
    public void renderCube(double x, double y, double z, TextureAtlasSprite s) {
        renderFaceYNeg(x, y, z, s);
        renderFaceYPos(x, y, z, s);
        renderFaceZNeg(x, y, z, s);
        renderFaceZPos(x, y, z, s);
        renderFaceXNeg(x, y, z, s);
        renderFaceXPos(x, y, z, s);
    }
}
