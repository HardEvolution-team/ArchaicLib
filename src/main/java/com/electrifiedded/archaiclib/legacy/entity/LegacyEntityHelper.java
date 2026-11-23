package com.electrifiedded.archaiclib.legacy.entity;

import com.electrifiedded.archaiclib.legacy.world.LegacyMOP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * 1.7.10 entity idioms on 1.12: single rider/mount, held item that may be "null",
 * and the iChunUtil style {@code getEntityLook} ray trace returning a {@link LegacyMOP}.
 */
public final class LegacyEntityHelper {
    private LegacyEntityHelper() {}

    /** 1.7 {@code entity.riddenByEntity}: first passenger or null. */
    public static Entity getRider(Entity e) {
        if (e == null) return null;
        List<Entity> p = e.getPassengers();
        return p.isEmpty() ? null : p.get(0);
    }

    /** 1.7 {@code getHeldItem()/getCurrentEquippedItem()}: main hand stack, null when empty. */
    public static ItemStack getHeld(EntityLivingBase e) {
        if (e == null) return null;
        ItemStack s = e.getHeldItemMainhand();
        return s.isEmpty() ? null : s;
    }

    public static void setHeld(EntityLivingBase e, ItemStack s) {
        e.setHeldItem(EnumHand.MAIN_HAND, s == null ? ItemStack.EMPTY : s);
    }

    /** null -> EMPTY. */
    public static ItemStack nn(ItemStack s) {
        return s == null ? ItemStack.EMPTY : s;
    }

    /** EMPTY -> null. */
    public static ItemStack orNull(ItemStack s) {
        return s == null || s.isEmpty() ? null : s;
    }

    public static Vec3d eyes(Entity e, float partial) {
        if (partial == 1.0F) return new Vec3d(e.posX, e.posY + e.getEyeHeight(), e.posZ);
        return new Vec3d(e.prevPosX + (e.posX - e.prevPosX) * partial,
                e.prevPosY + (e.posY - e.prevPosY) * partial + e.getEyeHeight(),
                e.prevPosZ + (e.posZ - e.prevPosZ) * partial);
    }

    /** iChunUtil {@code EntityHelperBase.getEntityLook(ent, d)}. */
    public static LegacyMOP getEntityLook(Entity ent, double d) {
        return getEntityLook(ent, d, false, 1.0F);
    }

    public static LegacyMOP getEntityLook(Entity ent, double d, boolean ignoreEntities) {
        return getEntityLook(ent, d, ignoreEntities, 1.0F);
    }

    /**
     * Ray trace from the entity's eyes along its look vector for {@code d} blocks; the closest entity
     * (unless ignored) wins over a block that is further away.
     */
    public static LegacyMOP getEntityLook(Entity ent, double d, boolean ignoreEntities, float partial) {
        if (ent == null || ent.world == null) return null;
        Vec3d start = eyes(ent, partial);
        Vec3d look = ent.getLook(partial);
        Vec3d end = start.add(look.x * d, look.y * d, look.z * d);
        RayTraceResult blockHit = ent.world.rayTraceBlocks(start, end, false, false, true);
        LegacyMOP mop = LegacyMOP.of(blockHit);
        double blockDist = mop != null ? mop.hitVec.distanceTo(start) : d;
        if (ignoreEntities) return mop;

        Entity best = null;
        Vec3d bestHit = null;
        double bestDist = blockDist;
        AxisAlignedBB area = ent.getEntityBoundingBox().expand(look.x * d, look.y * d, look.z * d).grow(1.0D);
        for (Entity e : ent.world.getEntitiesWithinAABBExcludingEntity(ent, area)) {
            if (!e.canBeCollidedWith()) continue;
            float border = e.getCollisionBorderSize();
            AxisAlignedBB box = e.getEntityBoundingBox().grow(border);
            RayTraceResult hit = box.calculateIntercept(start, end);
            if (box.contains(start)) {
                if (bestDist >= 0.0D) {
                    best = e;
                    bestHit = hit == null ? start : hit.hitVec;
                    bestDist = 0.0D;
                }
            } else if (hit != null) {
                double dist = start.distanceTo(hit.hitVec);
                if (dist < bestDist || bestDist == 0.0D && best == null) {
                    if (e == ent.getRidingEntity() && !e.canRiderInteract()) {
                        if (bestDist == 0.0D) {
                            best = e;
                            bestHit = hit.hitVec;
                        }
                    } else {
                        best = e;
                        bestHit = hit.hitVec;
                        bestDist = dist;
                    }
                }
            }
        }
        if (best != null) return new LegacyMOP(best, bestHit);
        return mop;
    }

    private static java.lang.reflect.Method deathSound;

    /** Protected {@code EntityLivingBase.getDeathSound()} (1.7 code played it by name), null if unavailable. */
    public static net.minecraft.util.SoundEvent getDeathSound(EntityLivingBase e) {
        try {
            if (deathSound == null) {
                deathSound = net.minecraftforge.fml.common.ObfuscationReflectionHelper.findMethod(EntityLivingBase.class, "func_184615_bR", net.minecraft.util.SoundEvent.class);
            }
            return (net.minecraft.util.SoundEvent) deathSound.invoke(e);
        } catch (Exception ex) {
            return null;
        }
    }

    /** Play the entity's own death sound at it (server side broadcast). */
    public static void playDeathSound(EntityLivingBase e, float volume, float pitch) {
        net.minecraft.util.SoundEvent s = getDeathSound(e);
        if (s != null) e.world.playSound(null, e.posX, e.posY, e.posZ, s, e.getSoundCategory(), volume, pitch);
    }

    /** 1.7 {@code EntityPlayer.getCommandSenderName()} / {@code getDisplayName} semantics. */
    public static String name(Entity e) {
        return e == null ? "" : e.getName();
    }

    public static boolean isCreative(Entity e) {
        return e instanceof EntityPlayer && ((EntityPlayer) e).capabilities.isCreativeMode;
    }
}
