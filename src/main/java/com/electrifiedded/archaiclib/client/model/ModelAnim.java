package com.electrifiedded.archaiclib.client.model;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Small procedural-animation toolkit for {@link ModelRenderer} rigs driven by an "animation id + tick" pair
 * (AnimationAPI-style mobs, bosses with attack timers...). Poses are written as additive deltas weighted by
 * phase curves, so one attack = wind-up pose, strike pose and a timing triple.
 */
@SideOnly(Side.CLIENT)
public final class ModelAnim {
    private ModelAnim() {
    }

    public static final float PI = (float)Math.PI;

    /** 0..1 smoothstep. */
    public static float ease(float t) {
        t = MathHelper.clamp(t, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    /** Linear progress of {@code t} through [start, end], clamped to 0..1. */
    public static float window(float t, float start, float end) {
        return end <= start ? (t >= end ? 1.0F : 0.0F) : MathHelper.clamp((t - start) / (end - start), 0.0F, 1.0F);
    }

    /** Rises 0->1 over [a,b], holds, falls 1->0 over [c,d] (eased). */
    public static float pulse(float t, float a, float b, float c, float d) {
        return ease(window(t, a, b)) * (1.0F - ease(window(t, c, d)));
    }

    /**
     * Attack phases for an animation of {@code duration} ticks whose impact lands at {@code hit}.
     * Returns {wind-up weight, strike weight}; both already faded out over the recovery at the end.
     */
    public static float[] attack(float t, float hit, float duration) {
        hit = Math.max(2.0F, Math.min(hit, duration - 2.0F));
        float strikeLen = Math.max(3.0F, Math.min(10.0F, hit * 0.3F));
        float recover = Math.max(6.0F, Math.min(30.0F, (duration - hit) * 0.45F));
        float wind = ease(window(t, 0.0F, hit - strikeLen * 0.3F));
        float strike = ease(window(t, hit - strikeLen * 0.3F, hit + strikeLen * 0.7F));
        float fade = 1.0F - ease(window(t, duration - recover, duration));
        return new float[] { wind * (1.0F - strike) * fade, strike * fade };
    }

    public static void reset(ModelRenderer... parts) {
        for (ModelRenderer p : parts) {
            p.rotateAngleX = 0.0F;
            p.rotateAngleY = 0.0F;
            p.rotateAngleZ = 0.0F;
        }
    }

    public static void rot(ModelRenderer p, float x, float y, float z) {
        p.rotateAngleX = x;
        p.rotateAngleY = y;
        p.rotateAngleZ = z;
    }

    /** Adds {@code w * (x,y,z)} to the rotation. */
    public static void add(ModelRenderer p, float w, float x, float y, float z) {
        p.rotateAngleX += w * x;
        p.rotateAngleY += w * y;
        p.rotateAngleZ += w * z;
    }

    /** Mirrors a delta for the left/right twin of a bone (negates Y and Z). */
    public static void addMirrored(ModelRenderer right, ModelRenderer left, float w, float x, float y, float z) {
        add(right, w, x, y, z);
        add(left, w, x, -y, -z);
    }

    /** Snapshot of rotation points (to restore after animating offsets). */
    public static float[] savePoint(ModelRenderer p) {
        return new float[] { p.rotationPointX, p.rotationPointY, p.rotationPointZ };
    }

    public static void loadPoint(ModelRenderer p, float[] v) {
        p.rotationPointX = v[0];
        p.rotationPointY = v[1];
        p.rotationPointZ = v[2];
    }
}
