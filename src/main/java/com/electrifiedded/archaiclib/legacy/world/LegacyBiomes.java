package com.electrifiedded.archaiclib.legacy.world;

import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Field;

/**
 * 1.7 code read {@code BiomeGenBase.biomeName} on both sides. In 1.12 {@link Biome#getBiomeName()} is
 * client-only, so calling it from world generation crashes a dedicated server. This reads the field directly.
 */
public final class LegacyBiomes {
    private static Field nameField;

    private LegacyBiomes() {}

    /** Display name of the biome ("Plains", "Extreme Hills" ...) on either side; "" for null. */
    public static String getName(Biome biome) {
        if (biome == null) return "";
        try {
            if (nameField == null) {
                // both SRG ids are mapped to "biomeName" (Biome and Biome.BiomeProperties); take the one Biome has
                try {
                    nameField = ObfuscationReflectionHelper.findField(Biome.class, "field_185412_a");
                } catch (Throwable e) {
                    nameField = ObfuscationReflectionHelper.findField(Biome.class, "field_76791_y");
                }
            }
            Object v = nameField.get(biome);
            return v == null ? "" : (String) v;
        } catch (Throwable t) {
            return biome.getRegistryName() == null ? "" : biome.getRegistryName().getPath();
        }
    }
}
