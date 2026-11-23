package com.electrifiedded.archaiclib.client.texture;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A texture built at runtime from another (possibly foreign, e.g. another mod's) texture: the base image is put on a
 * larger canvas and extra regions are "painted" from it — copied, recoloured, or filled as a full model-box UV
 * layout whose pixels are sampled from a region of the base (so a model can get new bones that match the original
 * art without shipping any image). Rebuilt on every resource reload, follows HD resource packs (all coordinates are
 * in base texels and scaled by the actual base resolution).
 *
 * <pre>
 * CompositeTexture.builder(new ResourceLocation("othermod", "textures/entity/mob.png"), 128, 128)
 *     .canvas(128, 256)
 *     .box(0, 128, 6, 16, 6, 104, 0, 24, 16, 0xFFFFFFFF)   // new bone UV block painted from the arm region
 *     .register(new ResourceLocation("mymod", "dynamic/mob_ext"));
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class CompositeTexture extends AbstractTexture {

    /** One painting step; coordinates in base texels, {@code s} = actual pixels per texel. */
    public interface Op {
        void apply(BufferedImage base, BufferedImage dst, int s, Random rand);
    }

    private final ResourceLocation base;
    private final int baseW, baseH, canvasW, canvasH;
    private final List<Op> ops;
    private final long seed;

    private CompositeTexture(Builder b) {
        this.base = b.base;
        this.baseW = b.baseW;
        this.baseH = b.baseH;
        this.canvasW = b.canvasW;
        this.canvasH = b.canvasH;
        this.ops = new ArrayList<>(b.ops);
        this.seed = b.seed;
    }

    public static Builder builder(ResourceLocation base, int baseW, int baseH) {
        return new Builder(base, baseW, baseH);
    }

    @Override
    public void loadTexture(IResourceManager rm) throws IOException {
        deleteGlTexture();
        BufferedImage src;
        try (IResource res = rm.getResource(base); InputStream in = res.getInputStream()) {
            src = ImageIO.read(in);
        }
        if (src == null)
            throw new IOException("unreadable base texture " + base);
        int s = Math.max(1, src.getWidth() / baseW);
        BufferedImage dst = new BufferedImage(canvasW * s, canvasH * s, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < Math.min(src.getHeight(), dst.getHeight()); y++)
            for (int x = 0; x < Math.min(src.getWidth(), dst.getWidth()); x++)
                dst.setRGB(x, y, src.getRGB(x, y));
        Random rand = new Random(seed);
        for (Op op : ops)
            op.apply(src, dst, s, rand);
        TextureUtil.uploadTextureImageAllocate(getGlTextureId(), dst, false, false);
    }

    // ---------------------------------------------------------------- pixel helpers (public for custom ops)

    public static int clamp255(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    /** Multiplies an ARGB pixel by an ARGB tint and a brightness factor. */
    public static int shade(int argb, int tint, float bright) {
        int a = (argb >>> 24) * (tint >>> 24) / 255;
        int r = clamp255((int)(((argb >> 16) & 255) * ((tint >> 16) & 255) / 255 * bright));
        int g = clamp255((int)(((argb >> 8) & 255) * ((tint >> 8) & 255) / 255 * bright));
        int b = clamp255((int)((argb & 255) * (tint & 255) / 255 * bright));
        return a << 24 | r << 16 | g << 8 | b;
    }

    /** Pixel of {@code img} sampled in a texel region (wrapping), skipping fully transparent texels where possible. */
    static int sample(BufferedImage img, int s, int rx, int ry, int rw, int rh, int px, int py, Random rand) {
        int w = Math.max(1, rw * s), h = Math.max(1, rh * s);
        for (int tries = 0; tries < 6; tries++) {
            int x = rx * s + Math.floorMod(px + tries * 7, w);
            int y = ry * s + Math.floorMod(py + tries * 3, h);
            if (x >= img.getWidth() || y >= img.getHeight())
                continue;
            int c = img.getRGB(x, y);
            if ((c >>> 24) != 0)
                return c;
        }
        // whole sample area transparent: fall back to any opaque random pixel
        for (int tries = 0; tries < 64; tries++) {
            int c = img.getRGB(rand.nextInt(img.getWidth()), rand.nextInt(img.getHeight()));
            if ((c >>> 24) != 0)
                return c;
        }
        return 0xFF808080;
    }

    // ---------------------------------------------------------------- builder

    public static class Builder {
        final ResourceLocation base;
        final int baseW, baseH;
        int canvasW, canvasH;
        final List<Op> ops = new ArrayList<>();
        long seed = 1337L;

        Builder(ResourceLocation base, int baseW, int baseH) {
            this.base = base;
            this.baseW = this.canvasW = baseW;
            this.baseH = this.canvasH = baseH;
        }

        /** Canvas size in base texels (must be >= base size). The model's textureWidth/Height must match it. */
        public Builder canvas(int w, int h) {
            this.canvasW = Math.max(w, baseW);
            this.canvasH = Math.max(h, baseH);
            return this;
        }

        public Builder seed(long seed) {
            this.seed = seed;
            return this;
        }

        public Builder op(Op op) {
            ops.add(op);
            return this;
        }

        /** Copies a base region to (dx,dy), optionally mirrored and tinted. */
        public Builder copy(int sx, int sy, int w, int h, int dx, int dy, boolean flipX, int tint, float bright) {
            return op((src, dst, s, rand) -> {
                for (int y = 0; y < h * s; y++)
                    for (int x = 0; x < w * s; x++) {
                        int fx = sx * s + (flipX ? w * s - 1 - x : x), fy = sy * s + y;
                        int tx = dx * s + x, ty = dy * s + y;
                        if (fx < src.getWidth() && fy < src.getHeight() && tx < dst.getWidth() && ty < dst.getHeight())
                            dst.setRGB(tx, ty, shade(src.getRGB(fx, fy), tint, bright));
                    }
            });
        }

        /** Recolours an already painted canvas region (e.g. a darker variant of the whole texture). */
        public Builder recolor(int x0, int y0, int w, int h, int tint, float bright) {
            return op((src, dst, s, rand) -> {
                for (int y = y0 * s; y < Math.min(dst.getHeight(), (y0 + h) * s); y++)
                    for (int x = x0 * s; x < Math.min(dst.getWidth(), (x0 + w) * s); x++) {
                        int c = dst.getRGB(x, y);
                        if ((c >>> 24) != 0)
                            dst.setRGB(x, y, shade(c, tint, bright));
                    }
            });
        }

        /** Fills a rectangle with texels sampled from a base region (+ per-pixel noise). */
        public Builder fill(int x0, int y0, int w, int h, int sx, int sy, int sw, int sh, int tint, float bright, float noise) {
            return op((src, dst, s, rand) -> paintRect(src, dst, s, rand, x0, y0, w, h, sx, sy, sw, sh, tint, bright, noise, false));
        }

        /**
         * Paints the complete vanilla ModelBox UV layout of a {@code w x h x d} box at texture offset (u,v)
         * with texels sampled from the base region (sx,sy,sw,sh): top lit, bottom/back shaded, edges outlined —
         * exactly what {@code new ModelRenderer(model, u, v).addBox(.., w, h, d)} will read.
         */
        public Builder box(int u, int v, int w, int h, int d, int sx, int sy, int sw, int sh, int tint) {
            return box(u, v, w, h, d, sx, sy, sw, sh, tint, 1.0F, 1.0F);
        }

        /**
         * {@link #box(int, int, int, int, int, int, int, int, int, int)} with saturation (0 = grey, 1 = as sampled)
         * and overall brightness — e.g. paint pale bone/horn from a skin region.
         */
        public Builder box(int u, int v, int w, int h, int d, int sx, int sy, int sw, int sh, int tint, float saturation, float brightness) {
            return op((src0, dst, s, rand) -> {
                BufferedImage src = adjusted(src0, saturation, brightness);
                // top & bottom
                paintRect(src, dst, s, rand, u + d, v, w, d, sx, sy, sw, sh, tint, 1.12F, 0.08F, true);
                paintRect(src, dst, s, rand, u + d + w, v, w, d, sx, sy, sw, sh, tint, 0.70F, 0.08F, true);
                // sides: right, front, left, back
                paintRect(src, dst, s, rand, u, v + d, d, h, sx, sy, sw, sh, tint, 0.86F, 0.10F, true);
                paintRect(src, dst, s, rand, u + d, v + d, w, h, sx, sy, sw, sh, tint, 1.00F, 0.10F, true);
                paintRect(src, dst, s, rand, u + d + w, v + d, d, h, sx, sy, sw, sh, tint, 0.86F, 0.10F, true);
                paintRect(src, dst, s, rand, u + d + w + d, v + d, w, h, sx, sy, sw, sh, tint, 0.76F, 0.10F, true);
            });
        }

        /** Copy of {@code img} with saturation/brightness changed (cheap: done once per op). */
        static BufferedImage adjusted(BufferedImage img, float saturation, float brightness) {
            if (saturation == 1.0F && brightness == 1.0F)
                return img;
            BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < img.getHeight(); y++)
                for (int x = 0; x < img.getWidth(); x++) {
                    int c = img.getRGB(x, y);
                    int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
                    float l = 0.299F * r + 0.587F * g + 0.114F * b;
                    r = clamp255((int)((l + (r - l) * saturation) * brightness));
                    g = clamp255((int)((l + (g - l) * saturation) * brightness));
                    b = clamp255((int)((l + (b - l) * saturation) * brightness));
                    out.setRGB(x, y, (c & 0xFF000000) | r << 16 | g << 8 | b);
                }
            return out;
        }

        static void paintRect(BufferedImage src, BufferedImage dst, int s, Random rand, int x0, int y0, int w, int h,
                int sx, int sy, int sw, int sh, int tint, float bright, float noise, boolean outline) {
            int ox = rand.nextInt(Math.max(1, sw * s)), oy = rand.nextInt(Math.max(1, sh * s));
            for (int y = 0; y < h * s; y++)
                for (int x = 0; x < w * s; x++) {
                    int tx = x0 * s + x, ty = y0 * s + y;
                    if (tx >= dst.getWidth() || ty >= dst.getHeight())
                        continue;
                    int c = sample(src, s, sx, sy, sw, sh, ox + x, oy + y, rand);
                    float b = bright * (1.0F + (rand.nextFloat() - 0.5F) * 2.0F * noise);
                    if (outline && (x < s || y < s || x >= (w - 1) * s || y >= (h - 1) * s) && w > 2 && h > 2)
                        b *= 0.78F;
                    dst.setRGB(tx, ty, shade(c | 0xFF000000, tint, b));
                }
        }

        public CompositeTexture build() {
            return new CompositeTexture(this);
        }

        /** Builds and registers the texture under {@code id} (client thread); returns {@code id} for binding. */
        public ResourceLocation register(ResourceLocation id) {
            Minecraft.getMinecraft().getTextureManager().loadTexture(id, build());
            return id;
        }
    }
}
