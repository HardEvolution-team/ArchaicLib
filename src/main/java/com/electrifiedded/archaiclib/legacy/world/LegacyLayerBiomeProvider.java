package com.electrifiedded.archaiclib.legacy.world;

import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.GenLayerVoronoiZoom;
import net.minecraft.world.gen.layer.GenLayerZoom;
import net.minecraft.world.gen.layer.IntCache;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.util.Arrays;
import java.util.List;

/**
 * The common 1.7 mod-dimension WorldChunkManager: a GenLayer that picks a random biome out of a fixed list per cell,
 * zoomed {@code zooms} times plus a voronoi zoom (1.7 "GenLayerBiomesX + 5x GenLayerZoom"). Spawn biomes = the list.
 */
public class LegacyLayerBiomeProvider extends BiomeProvider {
    public LegacyLayerBiomeProvider(long seed, Biome[] biomes, int zooms) {
        super();
        GenLayer layer = new Pick(1L, biomes);
        for (int i = 0; i < zooms; i++) layer = new GenLayerZoom(1000L + i, layer);
        GenLayer voronoi = new GenLayerVoronoiZoom(10L, layer);
        layer.initWorldGenSeed(seed);
        voronoi.initWorldGenSeed(seed);
        ObfuscationReflectionHelper.setPrivateValue(BiomeProvider.class, this, layer, "field_76944_d");
        ObfuscationReflectionHelper.setPrivateValue(BiomeProvider.class, this, voronoi, "field_76945_e");
        List<Biome> spawn = getBiomesToSpawnIn();
        spawn.clear();
        spawn.addAll(Arrays.asList(biomes));
    }

    /** Random biome out of a list for every cell. */
    public static class Pick extends GenLayer {
        private final int[] ids;
        public Pick(long seed, Biome[] biomes) {
            super(seed);
            ids = new int[biomes.length];
            for (int i = 0; i < biomes.length; i++) ids[i] = Biome.getIdForBiome(biomes[i]);
        }
        @Override
        public int[] getInts(int x, int z, int w, int h) {
            int[] out = IntCache.getIntCache(w * h);
            for (int dz = 0; dz < h; dz++)
                for (int dx = 0; dx < w; dx++) {
                    initChunkSeed(dx + x, dz + z);
                    out[dx + dz * w] = ids[nextInt(ids.length)];
                }
            return out;
        }
    }
}
