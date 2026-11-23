package com.electrifiedded.archaiclib.crafting;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A private 1.7-style crafting manager for custom workbenches (any grid size, e.g. 5x5), NOT added to the
 * vanilla recipe registry. {@link #addRecipe} takes the 1.7 GameRegistry.addRecipe argument list (pattern strings
 * may be as wide as the grid), {@link #addShapelessRecipe} the shapeless one. {@link #findMatchingRecipe} also does
 * the 1.7 two-damaged-tools repair; with {@code vanillaFallback} the vanilla recipes are tried as well.
 */
public class LegacyCraftingManager {
    private final List<IRecipe> recipes = new ArrayList<>();
    private final ResourceLocation group;
    public boolean vanillaFallback;
    public boolean repair = true;

    public LegacyCraftingManager(String id) {
        this.group = new ResourceLocation(id);
    }

    public IRecipe addRecipe(ItemStack out, Object... params) {
        IRecipe r = new ShapedOreRecipe(group, out, params);
        recipes.add(r);
        return r;
    }

    public IRecipe addShapelessRecipe(ItemStack out, Object... params) {
        if (params.length == 1 && params[0] instanceof Object[] arr) params = arr;
        IRecipe r = new ShapelessOreRecipe(group, out, params);
        recipes.add(r);
        return r;
    }

    public IRecipe add(IRecipe r) {
        recipes.add(r);
        return r;
    }

    public List<IRecipe> getRecipeList() {
        return Collections.unmodifiableList(recipes);
    }

    public ItemStack findMatchingRecipe(InventoryCrafting inv, World world) {
        if (repair) {
            ItemStack a = ItemStack.EMPTY, b = ItemStack.EMPTY;
            int n = 0;
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (s.isEmpty()) continue;
                if (n == 0) a = s; else if (n == 1) b = s;
                n++;
            }
            if (n == 2 && a.getItem() == b.getItem() && a.getCount() == 1 && b.getCount() == 1 && a.getItem().isRepairable()) {
                Item item = a.getItem();
                int left = item.getMaxDamage() - a.getItemDamage() + item.getMaxDamage() - b.getItemDamage() + item.getMaxDamage() * 5 / 100;
                return new ItemStack(item, 1, Math.max(0, item.getMaxDamage() - left));
            }
        }
        for (IRecipe r : recipes) if (r.matches(inv, world)) return r.getCraftingResult(inv);
        if (vanillaFallback) {
            IRecipe r = CraftingManager.findMatchingRecipe(inv, world);
            if (r != null) return r.getCraftingResult(inv);
        }
        return ItemStack.EMPTY;
    }
}
