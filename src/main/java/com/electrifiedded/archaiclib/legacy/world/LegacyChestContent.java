package com.electrifiedded.archaiclib.legacy.world;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Block;

import java.util.Random;

/**
 * 1.7 {@code WeightedRandomChestContent}: stack + min/max count + weight, and the 1.7 chest fill algorithm
 * (count picks, each split into stacks of max size, every stack into a random slot).
 */
public final class LegacyChestContent {
    public final ItemStack theItemId;
    public final int theMinimumChanceToGenerateItem, theMaximumChanceToGenerateItem, itemWeight;

    public LegacyChestContent(ItemStack stack, int min, int max, int weight) {
        theItemId = stack;
        theMinimumChanceToGenerateItem = min;
        theMaximumChanceToGenerateItem = max;
        itemWeight = weight;
    }

    public LegacyChestContent(Item item, int meta, int min, int max, int weight) { this(new ItemStack(item, 1, meta), min, max, weight); }

    public LegacyChestContent(Block block, int meta, int min, int max, int weight) { this(new ItemStack(block, 1, meta), min, max, weight); }

    public static LegacyChestContent pick(Random r, LegacyChestContent[] list) {
        int total = 0;
        for (LegacyChestContent c : list) total += c.itemWeight;
        if (total <= 0) return null;
        int n = r.nextInt(total);
        for (LegacyChestContent c : list) if ((n -= c.itemWeight) < 0) return c;
        return null;
    }

    /** WeightedRandomChestContent.generateChestContents. */
    public static void generateChestContents(Random r, LegacyChestContent[] list, IInventory inv, int picks) {
        for (int i = 0; i < picks; i++) {
            LegacyChestContent c = pick(r, list);
            if (c == null) continue;
            int count = c.theMinimumChanceToGenerateItem + r.nextInt(c.theMaximumChanceToGenerateItem - c.theMinimumChanceToGenerateItem + 1);
            ItemStack base = c.theItemId.copy();
            int max = base.getMaxStackSize();
            if (count <= max) {
                base.setCount(count);
                inv.setInventorySlotContents(r.nextInt(inv.getSizeInventory()), base);
            } else {
                for (int j = 0; j < count; j++) {
                    ItemStack one = c.theItemId.copy();
                    one.setCount(1);
                    inv.setInventorySlotContents(r.nextInt(inv.getSizeInventory()), one);
                }
            }
        }
    }
}
