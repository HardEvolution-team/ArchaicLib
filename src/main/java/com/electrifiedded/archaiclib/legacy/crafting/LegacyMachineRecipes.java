package com.electrifiedded.archaiclib.legacy.crafting;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The private "RecipesXxx" smelting lists of 1.7 custom furnaces (input stack with wildcard meta 32767 -> output,
 * plus experience). Order of insertion is kept; the first matching input wins like the 1.7 HashMap loop.
 */
public class LegacyMachineRecipes {
    public static final int WILDCARD = 32767;
    private final Map<ItemStack, ItemStack> recipes = new LinkedHashMap<>();
    private final Map<ItemStack, Float> experience = new LinkedHashMap<>();

    public LegacyMachineRecipes add(Item input, ItemStack output, float xp) {
        return add(new ItemStack(input, 1, WILDCARD), output, xp);
    }

    public LegacyMachineRecipes add(Block input, ItemStack output, float xp) {
        return add(Item.getItemFromBlock(input), output, xp);
    }

    public LegacyMachineRecipes add(ItemStack input, ItemStack output, float xp) {
        this.recipes.put(input, output);
        this.experience.put(output, xp);
        return this;
    }

    private static boolean matches(ItemStack stack, ItemStack key) {
        return key.getItem() == stack.getItem() && (key.getMetadata() == WILDCARD || key.getMetadata() == stack.getMetadata());
    }

    /** @return the output (do not modify, copy it) or {@link ItemStack#EMPTY} */
    public ItemStack getResult(ItemStack input) {
        if (input == null || input.isEmpty()) return ItemStack.EMPTY;
        for (Map.Entry<ItemStack, ItemStack> e : this.recipes.entrySet()) {
            if (matches(input, e.getKey())) return e.getValue();
        }
        return ItemStack.EMPTY;
    }

    public float getExperience(ItemStack output) {
        for (Map.Entry<ItemStack, Float> e : this.experience.entrySet()) {
            if (e.getKey().getItem() == output.getItem()) return e.getValue();
        }
        return 0.0F;
    }

    public Map<ItemStack, ItemStack> getRecipes() {
        return Collections.unmodifiableMap(this.recipes);
    }
}
