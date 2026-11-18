package com.electrifiedded.archaiclib.ghost;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.util.List;

/** Click handling for {@link GhostSlot}s; call from {@code Container.slotClick} and {@code transferStackInSlot}. */
public final class GhostSlots {

    private GhostSlots() {}

    /**
     * Handles a click that hits a ghost slot: left click with an item puts a copy in, empty hand or right
     * click clears it, a hotbar key copies that hotbar item. Returns true when the click was consumed
     * (the container's slotClick should then return EMPTY).
     */
    public static boolean slotClick(Container c, int slotId, int dragType, ClickType type, EntityPlayer player) {
        if (slotId < 0 || slotId >= c.inventorySlots.size()) return false;
        Slot slot = c.inventorySlots.get(slotId);
        if (!(slot instanceof GhostSlot)) return false;
        GhostSlot g = (GhostSlot) slot;
        ItemStack held = player.inventory.getItemStack();
        switch (type) {
            case PICKUP:
                if (dragType == 1 || held.isEmpty()) g.setGhost(ItemStack.EMPTY);
                else g.setGhost(held);
                break;
            case SWAP:
                if (dragType >= 0 && dragType < 9) g.setGhost(player.inventory.getStackInSlot(dragType));
                break;
            case THROW:
            case QUICK_MOVE:
                g.setGhost(ItemStack.EMPTY);
                break;
            default:
                break; // QUICK_CRAFT (drag), CLONE, PICKUP_ALL: ignored
        }
        c.detectAndSendChanges();
        return true;
    }

    /** Shift-click from the player's inventory: puts a copy into the first empty ghost cell, moves nothing. */
    public static ItemStack shiftClickInto(Container c, int[] ghostSlots, Slot from) {
        if (from == null || !from.getHasStack()) return ItemStack.EMPTY;
        for (int id : ghostSlots) {
            Slot s = c.inventorySlots.get(id);
            if (s instanceof GhostSlot && !s.getHasStack()) {
                ((GhostSlot) s).setGhost(from.getStack());
                break;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Fills the ghost slots with the given cells (missing entries clear the cell). */
    public static void fill(Container c, int[] ghostSlots, List<ItemStack> cells) {
        for (int i = 0; i < ghostSlots.length; i++) {
            Slot s = c.inventorySlots.get(ghostSlots[i]);
            ItemStack st = i < cells.size() && cells.get(i) != null ? cells.get(i) : ItemStack.EMPTY;
            if (s instanceof GhostSlot) ((GhostSlot) s).setGhost(st);
            else s.putStack(st.copy());
        }
        c.detectAndSendChanges();
    }
}
