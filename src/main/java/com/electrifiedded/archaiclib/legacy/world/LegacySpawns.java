package com.electrifiedded.archaiclib.legacy.world;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/** 1.7-style "loop over BiomeGenBase.getBiomeGenArray()" spawn helpers. Call in init/postInit (after biomes exist). */
public final class LegacySpawns {
    private LegacySpawns() {}

    public static List<Biome> biomes(Predicate<Biome> filter) {
        List<Biome> out = new ArrayList<>();
        for (Biome b : ForgeRegistries.BIOMES.getValuesCollection()) if (b != null && filter.test(b)) out.add(b);
        return out;
    }

    /** Overworld-like biomes: no Nether/End/Void. */
    public static boolean isOverworld(Biome b) {
        return !BiomeDictionary.hasType(b, BiomeDictionary.Type.NETHER)
                && !BiomeDictionary.hasType(b, BiomeDictionary.Type.END)
                && !BiomeDictionary.hasType(b, BiomeDictionary.Type.VOID);
    }

    /** What 1.7 mods did: every registered biome (including Nether/End). */
    public static void addSpawnAllBiomes(Class<? extends EntityLiving> cls, int weight, int min, int max, EnumCreatureType type) {
        add(cls, weight, min, max, type, biomes(b -> true));
    }

    public static void addSpawn(Class<? extends EntityLiving> cls, int weight, int min, int max, EnumCreatureType type, Biome... biomes) {
        add(cls, weight, min, max, type, Arrays.asList(biomes));
    }

    public static void add(Class<? extends EntityLiving> cls, int weight, int min, int max, EnumCreatureType type, List<Biome> biomes) {
        if (weight <= 0 || biomes.isEmpty()) return;
        EntityRegistry.addSpawn(cls, weight, min, max, type, biomes.toArray(new Biome[0]));
    }
}
