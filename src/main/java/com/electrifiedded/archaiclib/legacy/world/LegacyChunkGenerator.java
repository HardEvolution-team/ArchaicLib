package com.electrifiedded.archaiclib.legacy.world;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkGeneratorOverworld;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Overworld-noise terrain for 1.7 mod dimensions whose ChunkProvider was a copy of ChunkProviderGenerate with other
 * blocks: the vanilla 1.12 generator runs (biome surface, caves, ravines), then every block listed in
 * {@link #substitute} is swapped chunk-wide (e.g. stone -> mod stone, water -> mod fluid). Vanilla structures, lakes and
 * dungeons are off by default (see {@link #DEFAULT_OPTIONS}); add mod features in {@link #populate} overrides or an
 * IWorldGenerator.
 */
public class LegacyChunkGenerator extends ChunkGeneratorOverworld {
    public static final String DEFAULT_OPTIONS = "{\"useCaves\":true,\"useRavines\":true,\"useDungeons\":false,\"useWaterLakes\":false,"
            + "\"useLavaLakes\":false,\"useStrongholds\":false,\"useVillages\":false,\"useMineShafts\":false,\"useTemples\":false,"
            + "\"useMonuments\":false,\"useMansions\":false}";

    protected final World world;
    private final Map<Block, IBlockState> subst = new IdentityHashMap<>();

    public LegacyChunkGenerator(World world, long seed, String options) {
        super(world, seed, false, options == null ? DEFAULT_OPTIONS : options);
        this.world = world;
    }

    public LegacyChunkGenerator substitute(Block from, IBlockState to) {
        subst.put(from, to);
        return this;
    }

    @Override
    public Chunk generateChunk(int x, int z) {
        Chunk c = super.generateChunk(x, z);
        if (subst.isEmpty()) return c;
        for (ExtendedBlockStorage s : c.getBlockStorageArray()) {
            if (s == null || s.isEmpty()) continue;
            for (int i = 0; i < 16; i++)
                for (int j = 0; j < 16; j++)
                    for (int k = 0; k < 16; k++) {
                        IBlockState to = subst.get(s.get(i, j, k).getBlock());
                        if (to != null) s.set(i, j, k, to);
                    }
        }
        return c;
    }
}
