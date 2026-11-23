package com.electrifiedded.archaiclib.jei;

import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * JEI/HEI recipe categories for custom crafting grids (any size), registered without touching the JEI API:
 * safe to call when JEI is not installed. The lib's JEI plugin shows them when it is.
 */
public final class ArchaicJei {

    private ArchaicJei() {}

    /** One recipe: {@code width x height} input slots (row by row, each slot a list of alternatives), one output. */
    public static final class GridRecipe {
        public final int width, height;
        public final List<List<ItemStack>> inputs;
        public final ItemStack output;
        public final boolean shapeless;

        public GridRecipe(int width, int height, List<List<ItemStack>> inputs, ItemStack output, boolean shapeless) {
            this.width = width;
            this.height = height;
            this.inputs = inputs;
            this.output = output;
            this.shapeless = shapeless;
        }

        /** Shaped recipe from a flat array of stacks (empty = no item). */
        public static GridRecipe shaped(int width, int height, ItemStack[] items, ItemStack output) {
            List<List<ItemStack>> in = new ArrayList<>();
            for (ItemStack s : items) in.add(s == null || s.isEmpty() ? Collections.emptyList() : Collections.singletonList(s.copy()));
            return new GridRecipe(width, height, in, output.copy(), false);
        }

        public static GridRecipe shapeless(List<ItemStack> items, ItemStack output) {
            List<List<ItemStack>> in = new ArrayList<>();
            for (ItemStack s : items) in.add(Collections.singletonList(s.copy()));
            return new GridRecipe(0, 0, in, output.copy(), true);
        }
    }

    /** A grid category: uid, title translation key, grid size, catalyst, recipe source and optional "+" transfer. */
    public static final class GridCategory {
        public final String uid;
        public final String modName;
        public final String titleKey;
        public final int width, height;
        public final ItemStack catalyst;
        public final Supplier<List<GridRecipe>> recipes;
        /** container for the "+" button (null: none), first grid slot index, first player inventory slot index */
        public Class<? extends Container> container;
        public int gridSlotStart, playerSlotStart;

        public GridCategory(String uid, String modName, String titleKey, int width, int height, ItemStack catalyst,
                            Supplier<List<GridRecipe>> recipes) {
            this.uid = uid;
            this.modName = modName;
            this.titleKey = titleKey;
            this.width = width;
            this.height = height;
            this.catalyst = catalyst;
            this.recipes = recipes;
        }

        public GridCategory transfer(Class<? extends Container> container, int gridSlotStart, int playerSlotStart) {
            this.container = container;
            this.gridSlotStart = gridSlotStart;
            this.playerSlotStart = playerSlotStart;
            return this;
        }
    }

    private static final List<GridCategory> CATEGORIES = new ArrayList<>();

    /** Call in pre-init, init or post-init (recipes are read when JEI starts). */
    public static void registerGridCategory(GridCategory c) {
        CATEGORIES.add(c);
    }

    static List<GridCategory> categories() {
        return CATEGORIES;
    }
}
