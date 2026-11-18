package com.electrifiedded.archaiclib.jei;

import com.electrifiedded.archaiclib.ghost.GhostRecipeTransfer;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Copies the recipe shown in JEI into a ghost grid. The player does not need the items: the grid is a
 * pattern. For each cell an item the player carries is preferred, otherwise the one JEI displays.
 */
public class GhostTransferHandler<C extends Container> implements IRecipeTransferHandler<C> {

    private final Class<C> containerClass;
    private final IRecipeTransferHandlerHelper helper;

    public GhostTransferHandler(Class<C> containerClass, IRecipeTransferHandlerHelper helper) {
        this.containerClass = containerClass;
        this.helper = helper;
    }

    @Override
    public Class<C> getContainerClass() {
        return containerClass;
    }

    @Override
    public IRecipeTransferError transferRecipe(C container, IRecipeLayout layout, EntityPlayer player, boolean maxTransfer, boolean doTransfer) {
        Map<Integer, ? extends IGuiIngredient<ItemStack>> all = layout.getItemStacks().getGuiIngredients();
        TreeMap<Integer, IGuiIngredient<ItemStack>> inputs = new TreeMap<>();
        for (Map.Entry<Integer, ? extends IGuiIngredient<ItemStack>> e : all.entrySet()) {
            if (e.getValue().isInput()) inputs.put(e.getKey(), e.getValue());
        }
        if (inputs.isEmpty() || inputs.size() > 9) return helper.createInternalError();
        if (!doTransfer) return null;
        List<ItemStack> cells = new ArrayList<>();
        for (IGuiIngredient<ItemStack> ing : inputs.values()) cells.add(choose(ing, player));
        while (cells.size() < 9) cells.add(ItemStack.EMPTY);
        GhostRecipeTransfer.sendToServer(container.windowId, cells);
        return null;
    }

    private static ItemStack choose(IGuiIngredient<ItemStack> ing, EntityPlayer player) {
        List<ItemStack> options = ing.getAllIngredients();
        if (options != null) {
            for (ItemStack o : options) {
                if (o == null || o.isEmpty()) continue;
                for (ItemStack inv : player.inventory.mainInventory) {
                    if (!inv.isEmpty() && inv.getItem() == o.getItem()
                            && (o.getMetadata() == 32767 || !o.getHasSubtypes() || o.getMetadata() == inv.getMetadata())) {
                        return inv.copy();
                    }
                }
            }
        }
        ItemStack d = ing.getDisplayedIngredient();
        if ((d == null || d.isEmpty()) && options != null) {
            for (ItemStack o : options) if (o != null && !o.isEmpty()) { d = o; break; }
        }
        if (d == null || d.isEmpty()) return ItemStack.EMPTY;
        ItemStack c = d.copy();
        if (c.getMetadata() == 32767) c.setItemDamage(0);
        return c;
    }
}
