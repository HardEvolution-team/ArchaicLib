package com.electrifiedded.archaiclib.ghost;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/** A slot that only holds a one-item copy as a pattern. Clicks are handled by {@link GhostSlots#slotClick}. */
public class GhostSlot extends Slot {

    public GhostSlot(IInventory inv, int index, int x, int y) {
        super(inv, index, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canTakeStack(EntityPlayer player) {
        return false;
    }

    @Override
    public int getSlotStackLimit() {
        return 1;
    }

    /** Sets a one-item copy (or clears the slot). */
    public void setGhost(ItemStack stack) {
        ItemStack c = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        if (!c.isEmpty()) c.setCount(1);
        putStack(c);
    }
}
