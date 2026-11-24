package com.electrifiedded.archaiclib.legacy.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.util.math.MathHelper;

/**
 * 1.7 {@code new EntityArrow(world, shooter, target, velocity, inaccuracy)} (the aimed mob constructor, gone in 1.12).
 * Pickup follows 1.12 rules (only player-shot arrows can be picked up).
 * @since 1.31.0
 */
public final class LegacyArrows {
    private LegacyArrows() {}

    /**
     * 1.7 {@code new EntityArrow(world, shooter, velocity)} (bow shot along the shooter's look; the 1.7 ctor scaled
     * velocity by 1.5 with inaccuracy 1). Not spawned. 1.7 {@code canBePickedUp = 2} = {@code PickupStatus.CREATIVE_ONLY}.
     */
    public static EntityArrow shot(net.minecraft.world.World world, EntityLivingBase shooter, float velocity) {
        EntityTippedArrow arrow = new EntityTippedArrow(world, shooter);
        arrow.shoot(shooter, shooter.rotationPitch, shooter.rotationYaw, 0.0F, velocity * 1.5F, 1.0F);
        if (shooter instanceof net.minecraft.entity.player.EntityPlayer) {
            arrow.pickupStatus = EntityArrow.PickupStatus.ALLOWED;
        }
        return arrow;
    }

    public static EntityArrow aimed(EntityLivingBase shooter, EntityLivingBase target, float velocity, float inaccuracy) {
        EntityTippedArrow arrow = new EntityTippedArrow(shooter.world, shooter);
        arrow.posY = shooter.posY + shooter.getEyeHeight() - 0.1;
        double dx = target.posX - shooter.posX;
        double dy = target.getEntityBoundingBox().minY + target.height / 3.0F - arrow.posY;
        double dz = target.posZ - shooter.posZ;
        double dh = MathHelper.sqrt(dx * dx + dz * dz);
        if (dh >= 1.0E-7) {
            float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
            float pitch = (float) (-(Math.atan2(dy, dh) * 180.0 / Math.PI));
            arrow.setLocationAndAngles(shooter.posX + dx / dh, arrow.posY, shooter.posZ + dz / dh, yaw, pitch);
            arrow.shoot(dx, dy + dh * 0.2F, dz, velocity, inaccuracy);
        }
        return arrow;
    }
}
