package com.electrifiedded.archaiclib.crafting;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.crafting.IShapedRecipe;
import net.minecraftforge.oredict.OreDictionary;

import java.util.List;

/**
 * Crafting without a player or a real crafting table: places items from a plain list into a 3x3 grid
 * in the recipe's own layout and runs the recipe the way a crafting table does
 * ({@code matches}, {@code getCraftingResult}, {@code getRemainingItems}). That way modded recipes keep
 * their special behaviour: charge/NBT transferred to the result (IC2 energy crystals), reusable crafting
 * tools coming back damaged (IC2 forge hammer and cutter, GregTech-style tools), buckets returned, etc.
 */
public final class RecipeGrid {

    private RecipeGrid() {}

    private static final class NoContainer extends Container {
        @Override
        public boolean canInteractWith(EntityPlayer player) {
            return false;
        }

        @Override
        public void onCraftMatrixChanged(net.minecraft.inventory.IInventory inv) {}
    }

    /** A 3x3 crafting inventory that does not notify any container. */
    public static InventoryCrafting newGrid() {
        return new InventoryCrafting(new NoContainer(), 3, 3);
    }

    /**
     * The recipe's ingredients placed on a 3x3 grid (row-major, 9 entries, {@link Ingredient#EMPTY} for
     * empty cells). Handles vanilla/ore shaped recipes (width x height lists), recipes that already list
     * nine cells (IC2 AdvRecipe) and shapeless recipes.
     */
    public static Ingredient[] layout(IRecipe recipe) {
        Ingredient[] grid = new Ingredient[9];
        java.util.Arrays.fill(grid, Ingredient.EMPTY);
        List<Ingredient> ings = recipe.getIngredients();
        if (ings.size() == 9) {
            for (int i = 0; i < 9; i++) grid[i] = ings.get(i);
            return grid;
        }
        if (recipe instanceof IShapedRecipe) {
            int w = ((IShapedRecipe) recipe).getRecipeWidth(), h = ((IShapedRecipe) recipe).getRecipeHeight();
            if (w > 0 && h > 0 && w <= 3 && h <= 3 && w * h == ings.size()) {
                for (int i = 0; i < ings.size(); i++) grid[(i / w) * 3 + i % w] = ings.get(i);
                return grid;
            }
        }
        int slot = 0;
        for (Ingredient ing : ings) {
            if (isEmpty(ing)) continue;
            if (slot >= 9) break;
            grid[slot++] = ing;
        }
        return grid;
    }

    public static boolean isEmpty(Ingredient ing) {
        return ing == null || ing == Ingredient.EMPTY || ing.getMatchingStacks().length == 0;
    }

    /** A concrete stack for an ingredient (wildcard metadata replaced by 0), or EMPTY. */
    public static ItemStack example(Ingredient ing) {
        if (isEmpty(ing)) return ItemStack.EMPTY;
        ItemStack s = ing.getMatchingStacks()[0].copy();
        if (s.getMetadata() == OreDictionary.WILDCARD_VALUE) s.setItemDamage(0);
        s.setCount(1);
        return s;
    }

    // --- reusable crafting tools ------------------------------------------------------------------------

    /**
     * Whether crafting with this stack gives the same item back (hammer, cutter, wrench-like tools) instead
     * of consuming it. Buckets and bottles are not reusable: they come back as a different item.
     */
    public static boolean isReusable(ItemStack stack) {
        if (stack.isEmpty() || !stack.getItem().hasContainerItem(stack)) return false;
        ItemStack probe = stack.copy();
        probe.setCount(1);
        if (probe.getMetadata() == OreDictionary.WILDCARD_VALUE) probe.setItemDamage(0);
        ItemStack back;
        try {
            back = ForgeHooks.getContainerItem(probe);
        } catch (RuntimeException e) {
            return false;
        }
        return !back.isEmpty() && back.getItem() == probe.getItem();
    }

    /** An ingredient is reusable when every item it accepts is a reusable tool. */
    public static boolean isReusable(Ingredient ing) {
        if (isEmpty(ing)) return false;
        for (ItemStack s : ing.getMatchingStacks()) if (!isReusable(s)) return false;
        return true;
    }

    /** How many crafts one fresh item of a reusable ingredient lasts (Integer.MAX_VALUE if it never breaks). */
    public static int usesPerItem(Ingredient ing) {
        ItemStack s = example(ing);
        if (s.isEmpty() || !s.isItemStackDamageable()) return Integer.MAX_VALUE;
        return Math.max(1, s.getMaxDamage() + 1);
    }

    /** How many items of an ingredient a batch of {@code times} crafts needs. */
    public static int amountForBatch(Ingredient ing, int times) {
        if (!isReusable(ing)) return times;
        int uses = usesPerItem(ing);
        return uses == Integer.MAX_VALUE ? 1 : (times + uses - 1) / uses;
    }

    // --- crafting from a list of items --------------------------------------------------------------------

    public static final class Result {
        /** crafted stack, EMPTY when it failed */
        public final ItemStack output;
        /** grid cell whose ingredient was missing from the source, or -1 */
        public final int missingCell;
        /** the missing ingredient, or null */
        public final Ingredient missing;
        /** items were all there, but the recipe refused the grid (NBT-sensitive input etc.) */
        public final boolean refused;

        private Result(ItemStack output, int missingCell, Ingredient missing, boolean refused) {
            this.output = output;
            this.missingCell = missingCell;
            this.missing = missing;
            this.refused = refused;
        }

        public boolean ok() {
            return !output.isEmpty();
        }
    }

    /**
     * Crafts {@code recipe} once from the items in {@code source}. On success the used items are taken
     * out of the list and whatever the recipe leaves behind (damaged tools, empty buckets) is added to it;
     * the result is returned, not added. On failure the list is left exactly as it was.
     * Reusable tools: the most worn matching stack is used first so fresh ones last.
     */
    public static Result craftOnce(IRecipe recipe, World world, List<ItemStack> source) {
        Ingredient[] layout = layout(recipe);
        InventoryCrafting grid = newGrid();
        ItemStack[] taken = new ItemStack[9];
        int[] from = new int[9];
        for (int cell = 0; cell < 9; cell++) {
            Ingredient ing = layout[cell];
            if (isEmpty(ing)) continue;
            int idx = pick(source, ing);
            if (idx < 0) {
                putBack(source, taken, from);
                return new Result(ItemStack.EMPTY, cell, ing, false);
            }
            taken[cell] = source.get(idx).splitStack(1);
            from[cell] = idx;
            grid.setInventorySlotContents(cell, taken[cell]);
        }
        if (!recipe.matches(grid, world)) {
            putBack(source, taken, from);
            return new Result(ItemStack.EMPTY, -1, null, true);
        }
        ItemStack out = recipe.getCraftingResult(grid).copy();
        if (out.isEmpty()) {
            putBack(source, taken, from);
            return new Result(ItemStack.EMPTY, -1, null, true);
        }
        NonNullList<ItemStack> rest = recipe.getRemainingItems(grid);
        source.removeIf(ItemStack::isEmpty);
        for (ItemStack r : rest) if (!r.isEmpty()) source.add(r.copy());
        return new Result(out, -1, null, false);
    }

    private static int pick(List<ItemStack> source, Ingredient ing) {
        boolean reusable = isReusable(ing);
        int best = -1;
        for (int i = 0; i < source.size(); i++) {
            ItemStack s = source.get(i);
            if (s.isEmpty() || !ing.apply(s)) continue;
            if (!reusable) return i;
            if (best < 0 || s.getItemDamage() > source.get(best).getItemDamage()) best = i;
        }
        return best;
    }

    private static void putBack(List<ItemStack> source, ItemStack[] taken, int[] from) {
        for (int i = 0; i < 9; i++) {
            if (taken[i] == null || taken[i].isEmpty()) continue;
            ItemStack orig = source.get(from[i]);
            if (orig.isEmpty()) source.set(from[i], taken[i]);
            else orig.grow(taken[i].getCount());
            taken[i] = null;
        }
    }
}
