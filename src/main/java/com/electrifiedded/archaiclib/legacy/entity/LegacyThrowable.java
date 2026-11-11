package com.electrifiedded.archaiclib.legacy.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.math.MathHelper;

public final class LegacyThrowable {
    private LegacyThrowable() {}

    public static void applyLegacyThrowerInit(EntityThrowable e, EntityLivingBase thrower) {
        applyLegacyThrowerInit(e, thrower, 1.5F, 1.0F, 0.0F);
    }

    public static void applyLegacyThrowerInit(EntityThrowable e, EntityLivingBase thrower, float velocity, float inaccuracy, float pitchOffset) {
        e.setLocationAndAngles(thrower.posX, thrower.posY + thrower.getEyeHeight(), thrower.posZ, thrower.rotationYaw, thrower.rotationPitch);
        float yawRad = e.rotationYaw / 180.0F * (float) Math.PI;
        float pitchRad = e.rotationPitch / 180.0F * (float) Math.PI;
        e.posX -= MathHelper.cos(yawRad) * 0.16F;
        e.posY -= 0.1F;
        e.posZ -= MathHelper.sin(yawRad) * 0.16F;
        e.setPosition(e.posX, e.posY, e.posZ);
        float f = 0.4F;
        double mx = -MathHelper.sin(yawRad) * MathHelper.cos(pitchRad) * f;
        double mz = MathHelper.cos(yawRad) * MathHelper.cos(pitchRad) * f;
        double my = -MathHelper.sin((e.rotationPitch + pitchOffset) / 180.0F * (float) Math.PI) * f;
        e.shoot(mx, my, mz, velocity, inaccuracy);
    }
}
