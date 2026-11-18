package com.electrifiedded.archaiclib.ghost;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * A container with a "ghost" crafting grid: the slots only show copies of items (a pattern), nothing is
 * moved into or out of them. Register the container class with {@link GhostRecipeTransfer#register} and
 * the JEI/HEI "+" button fills the grid from any crafting recipe, without the player owning the items.
 */
public interface IGhostGridContainer {

    /** Container slot numbers ({@code inventorySlots} indices) of the ghost grid, row-major 3x3. */
    int[] ghostGridSlots();

    /**
     * Server side: replace the grid with these stacks (one per grid cell, EMPTY for empty cells).
     * Implementations normally keep the default, which calls {@link GhostSlots#fill}.
     */
    default void setGhostGrid(EntityPlayer player, List<ItemStack> cells) {
        GhostSlots.fill((net.minecraft.inventory.Container) this, ghostGridSlots(), cells);
    }
}
