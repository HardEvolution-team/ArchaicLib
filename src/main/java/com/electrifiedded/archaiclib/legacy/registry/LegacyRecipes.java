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
    /** Conditional recipes and their conditions; JEI integration hides the inactive ones. */
    public static final Map<IRecipe, java.util.function.BooleanSupplier> CONDITIONAL = new java.util.LinkedHashMap<>();

    private LegacyRecipes() {}

    private static ResourceLocation nextName(ItemStack output) {
        String modid = LegacyGameRegistry.activeModId();
        String base = output.getItem().getRegistryName() == null ? "recipe" : output.getItem().getRegistryName().getPath();
        String key = modid + ":" + base;
        int n = COUNTERS.merge(key, 1, Integer::sum);
        return new ResourceLocation(modid, "legacy_" + base + "_" + n);
    }

    public static IRecipe addRecipe(ItemStack output, Object... params) {
        params = sanitizeShaped(params);
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

    /**
     * Shaped recipe that only matches while {@code condition} is true - for old mods that added/removed recipes at
     * runtime from config or per-world settings (1.12 recipes are registered once).
     */
    public static IRecipe addConditionalRecipe(java.util.function.BooleanSupplier condition, ItemStack output, Object... params) {
        params = sanitizeShaped(params);
        ResourceLocation name = nextName(output);
        IRecipe r = new ShapedOreRecipe(name, output, params) {
            @Override
            public boolean matches(net.minecraft.inventory.InventoryCrafting inv, net.minecraft.world.World world) {
                return condition.getAsBoolean() && super.matches(inv, world);
            }

            @Override
            public boolean isDynamic() {
                return true;
            }
        }.setRegistryName(name);
        ForgeRegistries.RECIPES.register(r);
        CONDITIONAL.put(r, condition);
        return r;
    }

    /** Shapeless variant of {@link #addConditionalRecipe}. */
    public static IRecipe addConditionalShapelessRecipe(java.util.function.BooleanSupplier condition, ItemStack output, Object... ingredients) {
        ResourceLocation name = nextName(output);
        IRecipe r = new ShapelessOreRecipe(name, output, ingredients) {
            @Override
            public boolean matches(net.minecraft.inventory.InventoryCrafting inv, net.minecraft.world.World world) {
                return condition.getAsBoolean() && super.matches(inv, world);
            }

            @Override
            public boolean isDynamic() {
                return true;
            }
        }.setRegistryName(name);
        ForgeRegistries.RECIPES.register(r);
        CONDITIONAL.put(r, condition);
        return r;
    }

    /**
     * 1.7 {@code GameRegistry.addShapedRecipe} tolerated sloppy recipes that 1.12 rejects with an exception:
     * a pattern char without a key (it matched an empty slot), keys that the pattern never uses, and null key values.
     * Returns params where undefined chars are spaces and unused/null keys are dropped; well-formed input is unchanged.
     */
    public static Object[] sanitizeShaped(Object[] params) {
        if (params.length == 1 && params[0] instanceof Object[]) params = (Object[]) params[0];
        java.util.List<String> rows = new java.util.ArrayList<>();
        int i = 0;
        boolean mirrored = false;
        if (i < params.length && params[i] instanceof Boolean) { mirrored = (Boolean) params[i]; i++; }
        if (i < params.length && params[i] instanceof String[]) {
            for (String r : (String[]) params[i]) rows.add(r);
            i++;
        } else {
            while (i < params.length && params[i] instanceof String) rows.add((String) params[i++]);
        }
        java.util.LinkedHashMap<Character, Object> keys = new java.util.LinkedHashMap<>();
        for (; i + 1 < params.length; i += 2) {
            if (params[i] instanceof Character && params[i + 1] != null) keys.put((Character) params[i], params[i + 1]);
        }
        java.util.Set<Character> used = new java.util.HashSet<>();
        java.util.List<String> fixed = new java.util.ArrayList<>();
        for (String r : rows) {
            StringBuilder b = new StringBuilder();
            for (char c : r.toCharArray()) {
                if (c != ' ' && !keys.containsKey(c)) c = ' ';
                if (c != ' ') used.add(c);
                b.append(c);
            }
            fixed.add(b.toString());
        }
        java.util.List<Object> out = new java.util.ArrayList<>();
        if (mirrored) out.add(Boolean.TRUE);
        out.addAll(fixed);
        for (java.util.Map.Entry<Character, Object> e : keys.entrySet()) {
            if (used.contains(e.getKey())) { out.add(e.getKey()); out.add(e.getValue()); }
        }
        return out.toArray();
    }
}
