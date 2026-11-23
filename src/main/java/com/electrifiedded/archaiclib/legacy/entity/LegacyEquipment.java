package com.electrifiedded.archaiclib.legacy.entity;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

/**
 * 1.7 equipment indices (0 held, 1 boots, 2 legs, 3 chest, 4 helmet) for
 * {@code getEquipmentInSlot / setCurrentItemOrArmor / equipmentDropChances}, with null meaning empty.
 * @since 1.31.0
 */
public final class LegacyEquipment {
    public static final EntityEquipmentSlot[] SLOTS = {
            EntityEquipmentSlot.MAINHAND, EntityEquipmentSlot.FEET, EntityEquipmentSlot.LEGS,
            EntityEquipmentSlot.CHEST, EntityEquipmentSlot.HEAD
    };

    private LegacyEquipment() {}

    public static EntityEquipmentSlot slot(int legacyIndex) {
        return SLOTS[legacyIndex];
    }

    /** 1.7 getEquipmentInSlot: null when empty. */
    public static ItemStack get(EntityLivingBase e, int legacyIndex) {
        ItemStack s = e.getItemStackFromSlot(SLOTS[legacyIndex]);
        return s.isEmpty() ? null : s;
    }

    /** 1.7 setCurrentItemOrArmor (null clears). */
    public static void set(EntityLivingBase e, int legacyIndex, ItemStack stack) {
        e.setItemStackToSlot(SLOTS[legacyIndex], stack == null ? ItemStack.EMPTY : stack);
    }

    /** 1.7 {@code equipmentDropChances[i] = chance}. */
    public static void setDropChance(EntityLiving e, int legacyIndex, float chance) {
        e.setDropChance(SLOTS[legacyIndex], chance);
    }

    /** 1.7 EntityLiving.getArmorPosition: 0 for non-armor, else 1..4. */
    public static int armorPosition(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof ItemArmor) {
            switch (((ItemArmor) stack.getItem()).armorType) {
                case HEAD: return 4;
                case CHEST: return 3;
                case LEGS: return 2;
                case FEET: return 1;
                default: return 0;
            }
        }
        return 0;
    }
}
