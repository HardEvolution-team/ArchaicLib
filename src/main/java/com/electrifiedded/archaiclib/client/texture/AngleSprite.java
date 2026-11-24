package com.electrifiedded.archaiclib.client.texture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.7-style "custom compass" sprite (TextureCompass / mods' TextureXCompass): an animated strip (png + .mcmeta)
 * whose frame is chosen from an angle instead of time, with the 1.7 needle damping. 1.12 removed TextureCompass
 * in favour of item property overrides; this keeps the original single-texture strip working.
 *
 * <pre>{@code
 * @SubscribeEvent void stitch(TextureStitchEvent.Pre e) {
 *     AngleSprite.register(e, new AngleSprite("mymod:items/MyCompass") {
 *         protected double targetAngle(Minecraft mc) { ... radians, or NaN to spin randomly ... }
 *     });
 * }
 * }</pre>
 * The item model must use the same texture name (layer0 = "mymod:items/MyCompass").
 */
@SideOnly(Side.CLIENT)
public abstract class AngleSprite extends TextureAtlasSprite {
    public double currentAngle, angleDelta;

    protected AngleSprite(String name) { super(name); }

    public static void register(TextureStitchEvent.Pre e, AngleSprite sprite) {
        TextureMap map = e.getMap();
        map.setTextureEntry(sprite);
    }

    /**
     * Target needle angle in radians in the 1.7 convention ({@code -((yaw - 90) * PI / 180 - atan2(dz, dx))}),
     * or {@link Double#NaN} for a randomly spinning needle. Called every client tick while the atlas animates.
     */
    protected abstract double targetAngle(Minecraft mc);

    /** Helper: the 1.7 angle that points from the viewer at (px, pz, yaw) towards (tx, tz). */
    public static double pointTo(double px, double pz, double yaw, double tx, double tz) {
        yaw %= 360.0;
        return -((yaw - 90.0) * Math.PI / 180.0 - Math.atan2(tz - pz, tx - px));
    }

    @Override
    public void updateAnimation() {
        if (framesTextureData.isEmpty()) return;
        Minecraft mc = Minecraft.getMinecraft();
        double target = mc.world != null && mc.player != null ? targetAngle(mc) : Double.NaN;
        if (Double.isNaN(target)) target = Math.random() * Math.PI * 2.0;
        double d = target - currentAngle;
        while (d < -Math.PI) d += Math.PI * 2;
        while (d >= Math.PI) d -= Math.PI * 2;
        d = Math.max(-1.0, Math.min(1.0, d));
        angleDelta += d * 0.1;
        angleDelta *= 0.8;
        currentAngle += angleDelta;
        int n = framesTextureData.size();
        int i = (int) ((currentAngle / (Math.PI * 2) + 1.0) * n) % n;
        while (i < 0) i = (i + n) % n;
        if (i != frameCounter) {
            frameCounter = i;
            TextureUtil.uploadTextureMipmap(framesTextureData.get(frameCounter), width, height, originX, originY, false, false);
        }
    }
}
