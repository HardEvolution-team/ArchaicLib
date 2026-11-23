package com.electrifiedded.archaiclib.legacy.item;

import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;

/**
 * 1.7/1.8 potion damage values (Items.potionitem + meta) -> 1.12 potion stacks.
 * Bits 0-3 effect, bit 5 (32) level II, bit 6 (64) extended, bit 14 (16384) splash; meta 0 = water bottle.
 */
public final class LegacyPotions {
    private LegacyPotions() {}

    public static PotionType type(int meta) {
        if (meta == 0) return PotionTypes.WATER;
        int eff = meta & 15;
        boolean strong = (meta & 32) != 0;
        boolean longer = (meta & 64) != 0;
        switch (eff) {
            case 1: return strong ? PotionTypes.STRONG_REGENERATION : longer ? PotionTypes.LONG_REGENERATION : PotionTypes.REGENERATION;
            case 2: return strong ? PotionTypes.STRONG_SWIFTNESS : longer ? PotionTypes.LONG_SWIFTNESS : PotionTypes.SWIFTNESS;
            case 3: return longer ? PotionTypes.LONG_FIRE_RESISTANCE : PotionTypes.FIRE_RESISTANCE;
            case 4: return strong ? PotionTypes.STRONG_POISON : longer ? PotionTypes.LONG_POISON : PotionTypes.POISON;
            case 5: return strong ? PotionTypes.STRONG_HEALING : PotionTypes.HEALING;
            case 6: return longer ? PotionTypes.LONG_NIGHT_VISION : PotionTypes.NIGHT_VISION;
            case 8: return longer ? PotionTypes.LONG_WEAKNESS : PotionTypes.WEAKNESS;
            case 9: return strong ? PotionTypes.STRONG_STRENGTH : longer ? PotionTypes.LONG_STRENGTH : PotionTypes.STRENGTH;
            case 10: return longer ? PotionTypes.LONG_SLOWNESS : PotionTypes.SLOWNESS;
            case 11: return strong ? PotionTypes.STRONG_LEAPING : longer ? PotionTypes.LONG_LEAPING : PotionTypes.LEAPING;
            case 12: return strong ? PotionTypes.STRONG_HARMING : PotionTypes.HARMING;
            case 13: return longer ? PotionTypes.LONG_WATER_BREATHING : PotionTypes.WATER_BREATHING;
            case 14: return longer ? PotionTypes.LONG_INVISIBILITY : PotionTypes.INVISIBILITY;
            default: return (meta & 16) != 0 ? PotionTypes.AWKWARD : PotionTypes.MUNDANE;
        }
    }

    public static boolean isSplash(int meta) {
        return (meta & 16384) != 0;
    }

    public static ItemStack stack(int meta, int count) {
        Item item = isSplash(meta) ? Items.SPLASH_POTION : Items.POTIONITEM;
        return PotionUtils.addPotionToItemStack(new ItemStack(item, Math.max(1, count)), type(meta));
    }
}
