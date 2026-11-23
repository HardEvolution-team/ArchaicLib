package com.electrifiedded.archaiclib.legacy.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;

/**
 * 1.7 "shouldAttackPlayer" stare check (EntityEnderman and the many mobs that copied it):
 * the player looks at the entity's centre within {@code 1 - tolerance / distance} and can see it.
 * Unlike the 1.7 copies, this never touches Minecraft.getMinecraft() and is safe on the server.
 */
public final class LegacyStare {
    private LegacyStare() {}

    /** @param tolerance 0.025 in vanilla 1.7 enderman code */
    public static boolean isStaredAt(EntityPlayer player, Entity target, double tolerance) {
        if (player == null || target == null) return false;
        Vec3d look = player.getLook(1.0F).normalize();
        Vec3d to = new Vec3d(target.posX - player.posX,
                target.getEntityBoundingBox().minY + target.height / 2.0F - (player.posY + player.getEyeHeight()),
                target.posZ - player.posZ);
        double d = to.length();
        if (d < 1.0E-4) return false;
        double dot = look.dotProduct(to.normalize());
        return dot > 1.0 - tolerance / d && player.canEntityBeSeen(target);
    }

    /** Pumpkin-aware variant like 1.7 EntityEnderman: a carved pumpkin on the head blocks the stare. */
    public static boolean isStaredAtNoPumpkin(EntityPlayer player, Entity target, double tolerance) {
        if (player == null) return false;
        net.minecraft.item.ItemStack head = player.inventory.armorInventory.get(3);
        if (head.getItem() == net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.PUMPKIN)) return false;
        return isStaredAt(player, target, tolerance);
    }
}
