package com.electrifiedded.archaiclib.legacy.fluid;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

/**
 * 1.7-style fluids on 1.12: {@code new Fluid("name")} had no textures (the block supplied IIcons), 1.12 needs
 * still/flowing sprites and a colour on the Fluid itself, plus a blockstate whose LEVEL property is ignored.
 * <p>
 * Usage: {@code Fluid f = LegacyFluids.create("tsunami", LegacyFluids.WATER_STILL, LegacyFluids.WATER_FLOW, 0xFF3F76E4)},
 * register the BlockFluidClassic, then {@code LegacyFluids.ignoreLevel(block)} (safe on both sides). The port ships
 * {@code blockstates/<block>.json} with a {@code "normal"} variant - e.g. forge:fluid with {"custom":{"fluid":"name"}}
 * or any ordinary block model.
 */
public final class LegacyFluids {
    public static final ResourceLocation WATER_STILL = new ResourceLocation("minecraft", "blocks/water_still");
    public static final ResourceLocation WATER_FLOW = new ResourceLocation("minecraft", "blocks/water_flow");
    public static final ResourceLocation LAVA_STILL = new ResourceLocation("minecraft", "blocks/lava_still");
    public static final ResourceLocation LAVA_FLOW = new ResourceLocation("minecraft", "blocks/lava_flow");
    public static final ResourceLocation FIRE_0 = new ResourceLocation("minecraft", "blocks/fire_layer_0");
    public static final ResourceLocation FIRE_1 = new ResourceLocation("minecraft", "blocks/fire_layer_1");
    /** Colour vanilla 1.12 tints the grey water sprite with (1.7 water sprites were blue already). */
    public static final int WATER_COLOR = 0xFF3F76E4;

    private static final List<Block> LEVEL_IGNORED = new ArrayList<>();
    /** set by the client registrar once models are being registered; later calls apply immediately */
    public static java.util.function.Consumer<Block> lateStateMapperHook;

    private LegacyFluids() {}

    /** Create and register a fluid (or return the one already registered under that name). */
    public static Fluid create(String name, ResourceLocation still, ResourceLocation flowing, int argb) {
        String n = name.toLowerCase(Locale.ROOT);
        Fluid existing = FluidRegistry.getFluid(n);
        if (existing != null) return existing;
        Fluid f = new Fluid(n, still, flowing, (argb >>> 24) == 0 ? argb | 0xFF000000 : argb);
        FluidRegistry.registerFluid(f);
        return f;
    }

    /** Client: map every state of {@code block} to {@code <registry name>#normal}. No-op on a server. */
    public static void ignoreLevel(Block block) {
        if (lateStateMapperHook != null) lateStateMapperHook.accept(block);
        else LEVEL_IGNORED.add(block);
    }

    public static List<Block> pendingLevelIgnored() {
        return LEVEL_IGNORED;
    }

    /**
     * 1.7 {@code world.setBlockMetadataWithNotify(x, y, z, 0, flags)} on whatever block is there - many 1.7 fluids
     * did this in displaceIfPossible.
     */
    @SuppressWarnings("deprecation")
    public static void resetMeta(World world, BlockPos pos, int flags) {
        IBlockState s = world.getBlockState(pos);
        IBlockState zero = s.getBlock().getStateFromMeta(0);
        if (zero != s) world.setBlockState(pos, zero, flags);
    }
}
