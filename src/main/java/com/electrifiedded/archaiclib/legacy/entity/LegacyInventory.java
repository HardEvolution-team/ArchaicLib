package com.electrifiedded.archaiclib.legacy.entity;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class LegacyInventory {
    private LegacyInventory() {}

    public static boolean consumeInventoryItem(InventoryPlayer inventory, Item item) {
        for (int i = 0; i < inventory.mainInventory.size(); ++i) {
            ItemStack stack = inventory.mainInventory.get(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                stack.shrink(1);
                if (stack.isEmpty()) inventory.mainInventory.set(i, ItemStack.EMPTY);
                return true;
            }
        }
        for (int i = 0; i < inventory.offHandInventory.size(); ++i) {
            ItemStack stack = inventory.offHandInventory.get(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                stack.shrink(1);
                if (stack.isEmpty()) inventory.offHandInventory.set(i, ItemStack.EMPTY);
                return true;
            }
        }
        return false;
    }

    public static boolean hasItem(InventoryPlayer inventory, Item item) {
        for (ItemStack stack : inventory.mainInventory) {
            if (!stack.isEmpty() && stack.getItem() == item) return true;
        }
        for (ItemStack stack : inventory.offHandInventory) {
            if (!stack.isEmpty() && stack.getItem() == item) return true;
        }
        return false;
    }

    public static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.isEmpty();
    }
}
