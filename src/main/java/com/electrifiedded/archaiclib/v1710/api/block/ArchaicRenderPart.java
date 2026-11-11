package com.electrifiedded.archaiclib.v1710.api.block;

import com.electrifiedded.archaiclib.v1710.api.client.IIcon;
import net.minecraft.util.math.AxisAlignedBB;
import org.jetbrains.annotations.Nullable;

public final class ArchaicRenderPart {
    public final boolean isBox;
    public final double[] coords;
    public final float[] uvs;
    public final @Nullable IIcon[] icons;
    public int color = 0xFFFFFF;
    public boolean doubleSided;
    public boolean cull = true;
    public boolean shade = true;

    private ArchaicRenderPart(boolean isBox, double[] coords, float[] uvs, IIcon[] icons) {
        this.isBox = isBox;
        this.coords = coords;
        this.uvs = uvs;
        this.icons = icons;
    }

    public static ArchaicRenderPart box(double x1, double y1, double z1, double x2, double y2, double z2, IIcon icon) {
        return new ArchaicRenderPart(true, new double[]{x1, y1, z1, x2, y2, z2}, null, new IIcon[]{icon, icon, icon, icon, icon, icon});
    }

    public static ArchaicRenderPart box(AxisAlignedBB bb, IIcon icon) {
        return box(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ, icon);
    }

    public static ArchaicRenderPart box(AxisAlignedBB bb, IIcon[] sideIcons) {
        return new ArchaicRenderPart(true, new double[]{bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ}, null, sideIcons.clone());
    }

    public static ArchaicRenderPart quad(IIcon icon, double[] v0, double[] v1, double[] v2, double[] v3) {
        double[] c = new double[12];
        float[] uv = new float[8];
        double[][] vs = {v0, v1, v2, v3};
        for (int i = 0; i < 4; i++) {
            c[i * 3] = vs[i][0];
            c[i * 3 + 1] = vs[i][1];
            c[i * 3 + 2] = vs[i][2];
            uv[i * 2] = (float)vs[i][3];
            uv[i * 2 + 1] = (float)vs[i][4];
        }
        return new ArchaicRenderPart(false, c, uv, new IIcon[]{icon});
    }

    public ArchaicRenderPart color(int rgb) {
        this.color = rgb & 0xFFFFFF;
        return this;
    }

    public ArchaicRenderPart color(float r, float g, float b) {
        return color(((int)(r * 255) & 0xFF) << 16 | ((int)(g * 255) & 0xFF) << 8 | ((int)(b * 255) & 0xFF));
    }

    public ArchaicRenderPart doubleSided() {
        this.doubleSided = true;
        return this;
    }

    public ArchaicRenderPart renderAllFaces() {
        this.cull = false;
        return this;
    }

    public ArchaicRenderPart noShade() {
        this.shade = false;
        return this;
    }
}
