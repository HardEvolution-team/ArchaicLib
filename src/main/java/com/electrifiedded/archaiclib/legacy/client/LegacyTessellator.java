package com.electrifiedded.archaiclib.legacy.client;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public final class LegacyTessellator {
    private static final LegacyTessellator INSTANCE = new LegacyTessellator();
    private static final int STRIDE = 13;

    public static LegacyTessellator instance() { return INSTANCE; }

    private int mode = GL11.GL_QUADS;
    private boolean drawing;
    private boolean hasColor, hasTex, hasNormal, hasBrightness, noColor;
    private int r = 255, g = 255, b = 255, a = 255;
    private float nx, ny, nz;
    private int brightness;
    private double u, v;
    private double xo, yo, zo;
    private double[] data = new double[STRIDE * 256];
    private int count;

    private LegacyTessellator() {}

    public void startDrawingQuads() { startDrawing(GL11.GL_QUADS); }

    public void startDrawing(int glMode) {
        if (drawing) draw();
        drawing = true;
        mode = glMode;
        count = 0;
        hasColor = hasTex = hasNormal = hasBrightness = noColor = false;
    }

    public boolean isDrawing() { return drawing; }

    public void setTranslation(double x, double y, double z) { xo = x; yo = y; zo = z; }
    public void addTranslation(float x, float y, float z) { xo += x; yo += y; zo += z; }

    public void setTextureUV(double u, double v) { hasTex = true; this.u = u; this.v = v; }
    public void setBrightness(int light) { hasBrightness = true; brightness = light; }
    public void setNormal(float x, float y, float z) { hasNormal = true; nx = x; ny = y; nz = z; }
    public void disableColor() { noColor = true; }

    public void setColorRGBA(int r, int g, int b, int a) {
        if (noColor) return;
        hasColor = true;
        this.r = clamp(r); this.g = clamp(g); this.b = clamp(b); this.a = clamp(a);
    }
    public void setColorOpaque(int r, int g, int b) { setColorRGBA(r, g, b, 255); }
    public void setColorRGBA_F(float r, float g, float b, float a) { setColorRGBA((int) (r * 255F), (int) (g * 255F), (int) (b * 255F), (int) (a * 255F)); }
    public void setColorOpaque_F(float r, float g, float b) { setColorRGBA_F(r, g, b, 1F); }
    public void setColorOpaque_I(int rgb) { setColorOpaque(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255); }
    public void setColorRGBA_I(int rgb, int alpha) { setColorRGBA(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, alpha); }

    public void addVertexWithUV(double x, double y, double z, double u, double v) {
        setTextureUV(u, v);
        addVertex(x, y, z);
    }

    public void addVertex(double x, double y, double z) {
        if ((count + 1) * STRIDE > data.length) {
            double[] n = new double[data.length * 2];
            System.arraycopy(data, 0, n, 0, data.length);
            data = n;
        }
        int i = count * STRIDE;
        data[i] = x + xo; data[i + 1] = y + yo; data[i + 2] = z + zo;
        data[i + 3] = u; data[i + 4] = v;
        data[i + 5] = r; data[i + 6] = g; data[i + 7] = b; data[i + 8] = a;
        data[i + 9] = brightness;
        data[i + 10] = nx; data[i + 11] = ny; data[i + 12] = nz;
        count++;
    }

    public int draw() {
        if (!drawing) return 0;
        drawing = false;
        int n = count;
        count = 0;
        if (n == 0) return 0;
        VertexFormat fmt = new VertexFormat();
        fmt.addElement(DefaultVertexFormats.POSITION_3F);
        if (hasTex) fmt.addElement(DefaultVertexFormats.TEX_2F);
        if (hasColor) fmt.addElement(DefaultVertexFormats.COLOR_4UB);
        if (hasBrightness) fmt.addElement(DefaultVertexFormats.TEX_2S);
        if (hasNormal) {
            fmt.addElement(DefaultVertexFormats.NORMAL_3B);
            fmt.addElement(DefaultVertexFormats.PADDING_1B);
        }
        Tessellator tes = Tessellator.getInstance();
        BufferBuilder buf = tes.getBuffer();
        buf.begin(mode, fmt);
        for (int k = 0; k < n; k++) {
            int i = k * STRIDE;
            buf.pos(data[i], data[i + 1], data[i + 2]);
            for (VertexFormatElement e : fmt.getElements()) {
                if (e == DefaultVertexFormats.TEX_2F) buf.tex(data[i + 3], data[i + 4]);
                else if (e == DefaultVertexFormats.COLOR_4UB) buf.color((int) data[i + 5], (int) data[i + 6], (int) data[i + 7], (int) data[i + 8]);
                else if (e == DefaultVertexFormats.TEX_2S) {
                    int l = (int) data[i + 9];
                    buf.lightmap(l >> 16 & 65535, l & 65535);
                } else if (e == DefaultVertexFormats.NORMAL_3B) buf.normal((float) data[i + 10], (float) data[i + 11], (float) data[i + 12]);
            }
            buf.endVertex();
        }
        tes.draw();
        return n;
    }

    private static int clamp(int c) { return c < 0 ? 0 : c > 255 ? 255 : c; }
}
