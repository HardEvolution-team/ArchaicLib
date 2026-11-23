package com.electrifiedded.archaiclib.legacy.entity;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;

/** 1.7.10 melee helpers whose signatures changed (EnchantmentHelper took the attacker and the target). */
public final class LegacyCombat {
    private LegacyCombat() {}

    /** 1.7 {@code EnchantmentHelper.getEnchantmentModifierLiving(attacker, target)}. */
    public static float getEnchantmentModifierLiving(EntityLivingBase attacker, Entity target) {
        EnumCreatureAttribute attr = target instanceof EntityLivingBase ? ((EntityLivingBase) target).getCreatureAttribute() : EnumCreatureAttribute.UNDEFINED;
        return EnchantmentHelper.getModifierForCreature(attacker.getHeldItemMainhand(), attr);
    }

    /** 1.7 {@code EnchantmentHelper.getKnockbackModifier(attacker, target)}. */
    public static int getKnockbackModifier(EntityLivingBase attacker, Entity target) {
        return EnchantmentHelper.getKnockbackModifier(attacker);
    }
}
