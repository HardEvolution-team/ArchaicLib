package com.electrifiedded.archaiclib.crafting;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Workbench container of any square grid size bound to a custom crafting-table block (1.7 ContainerWorkbench clone).
 * Slot 0 = result, 1..size*size = grid, then 27 + 9 player slots. {@code manager == null} uses the vanilla recipes.
 */
public class LegacyCraftingContainer extends Container {
    public final InventoryCrafting craftMatrix;
    public final InventoryCraftResult craftResult = new InventoryCraftResult();
    private final World world;
    private final BlockPos pos;
    private final Block block;
    private final EntityPlayer player;
    private final LegacyCraftingManager manager;
    private final int gridSlots;

    /** resultX/Y = result slot, gridX/Y = top-left grid slot, invX/Y = top-left of the 27-slot inventory (hotbar 58 px lower). */
    public LegacyCraftingContainer(InventoryPlayer inv, World world, BlockPos pos, Block block, LegacyCraftingManager manager, int size,
                                   int resultX, int resultY, int gridX, int gridY, int invX, int invY) {
        this(inv, world, pos, block, manager, size, resultX, resultY, gridX, gridY, invX, invY, invY + 58);
    }

    /** Same, with an explicit hotbar row y. */
    public LegacyCraftingContainer(InventoryPlayer inv, World world, BlockPos pos, Block block, LegacyCraftingManager manager, int size,
                                   int resultX, int resultY, int gridX, int gridY, int invX, int invY, int hotbarY) {
        this.craftMatrix = new InventoryCrafting(this, size, size);
        this.world = world;
        this.pos = pos;
        this.block = block;
        this.player = inv.player;
        this.manager = manager;
        this.gridSlots = size * size;
        addSlotToContainer(new SlotCrafting(inv.player, craftMatrix, craftResult, 0, resultX, resultY));
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++) addSlotToContainer(new Slot(craftMatrix, c + r * size, gridX + c * 18, gridY + r * 18));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlotToContainer(new Slot(inv, c + r * 9 + 9, invX + c * 18, invY + r * 18));
        for (int c = 0; c < 9; c++) addSlotToContainer(new Slot(inv, c, invX + c * 18, hotbarY));
        onCraftMatrixChanged(craftMatrix);
    }

    @Override
    public void onCraftMatrixChanged(IInventory in) {
        if (world.isRemote) return;
        ItemStack out;
        if (manager == null) {
            IRecipe r = CraftingManager.findMatchingRecipe(craftMatrix, world);
            out = r == null ? ItemStack.EMPTY : r.getCraftingResult(craftMatrix);
        } else out = manager.findMatchingRecipe(craftMatrix, world);
        craftResult.setInventorySlotContents(0, out);
        if (player instanceof EntityPlayerMP mp) mp.connection.sendPacket(new SPacketSetSlot(windowId, 0, out));
    }

    @Override
    public void onContainerClosed(EntityPlayer p) {
        super.onContainerClosed(p);
        if (!world.isRemote) clearContainer(p, world, craftMatrix);
    }

    @Override
    public boolean canInteractWith(EntityPlayer p) {
        return world.getBlockState(pos).getBlock() == block && p.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer p, int index) {
        int invStart = 1 + gridSlots, hotStart = invStart + 27, end = hotStart + 9;
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack s = slot.getStack();
        ItemStack ret = s.copy();
        if (index == 0) {
            s.getItem().onCreated(s, world, p);
            if (!mergeItemStack(s, invStart, end, true)) return ItemStack.EMPTY;
            slot.onSlotChange(s, ret);
        } else if (index >= invStart && index < hotStart) {
            if (!mergeItemStack(s, hotStart, end, false)) return ItemStack.EMPTY;
        } else if (index >= hotStart && index < end) {
            if (!mergeItemStack(s, invStart, hotStart, false)) return ItemStack.EMPTY;
        } else if (!mergeItemStack(s, invStart, end, false)) return ItemStack.EMPTY;
        if (s.isEmpty()) slot.putStack(ItemStack.EMPTY); else slot.onSlotChanged();
        if (s.getCount() == ret.getCount()) return ItemStack.EMPTY;
        ItemStack taken = slot.onTake(p, s);
        if (index == 0) p.dropItem(taken, false);
        return ret;
    }

    @Override
    public boolean canMergeSlot(ItemStack stack, Slot slot) {
        return slot.inventory != craftResult && super.canMergeSlot(stack, slot);
    }
}
