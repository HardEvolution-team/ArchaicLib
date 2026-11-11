package com.electrifiedded.archaiclib.legacy.registry;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

public final class LegacyRecipes {
    private static final Map<String, Integer> COUNTERS = new HashMap<>();

    private LegacyRecipes() {}

    private static ResourceLocation nextName(ItemStack output) {
        String modid = LegacyGameRegistry.activeModId();
        String base = output.getItem().getRegistryName() == null ? "recipe" : output.getItem().getRegistryName().getPath();
        String key = modid + ":" + base;
        int n = COUNTERS.merge(key, 1, Integer::sum);
        return new ResourceLocation(modid, "legacy_" + base + "_" + n);
    }

    public static IRecipe addRecipe(ItemStack output, Object... params) {
        ResourceLocation name = nextName(output);
        IRecipe r = new ShapedOreRecipe(name, output, params).setRegistryName(name);
        ForgeRegistries.RECIPES.register(r);
        return r;
    }

    public static IRecipe addShapelessRecipe(ItemStack output, Object... ingredients) {
        ResourceLocation name = nextName(output);
        IRecipe r = new ShapelessOreRecipe(name, output, ingredients).setRegistryName(name);
        ForgeRegistries.RECIPES.register(r);
        return r;
    }
}
