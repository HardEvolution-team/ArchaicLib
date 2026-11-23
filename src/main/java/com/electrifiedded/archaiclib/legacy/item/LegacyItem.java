package com.electrifiedded.archaiclib.legacy.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * Item with the 1.7.10 method shapes, so ported item code can keep its bodies:
 * <ul>
 *     <li>{@link #onItemUseLegacy} - {@code onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ)}; the
 *     stack shrinks in place like 1.7 {@code stackSize--}</li>
 *     <li>{@link #onItemRightClickLegacy} - {@code onItemRightClick(stack, world, player)} returning the new stack</li>
 *     <li>{@link #addInformationLegacy} - tooltip with the player</li>
 *     <li>{@link #getSubItemsLegacy} - creative list without the tab check (called for every tab in
 *     {@link #getCreativeTabs()})</li>
 *     <li>{@link #setExtraCreativeTabs} - the 1.7 {@code getCreativeTabs()} override (item shown in several tabs)</li>
 * </ul>
 */
public class LegacyItem extends Item {
    private CreativeTabs[] extraTabs = new CreativeTabs[0];

    public LegacyItem setExtraCreativeTabs(CreativeTabs... tabs) {
        this.extraTabs = tabs;
        return this;
    }

    /** 1.7 maxStackSize field. */
    public LegacyItem setMaxStack(int size) {
        setMaxStackSize(size);
        return this;
    }

    @Override
    public CreativeTabs[] getCreativeTabs() {
        List<CreativeTabs> out = new ArrayList<>();
        if (getCreativeTab() != null) out.add(getCreativeTab());
        for (CreativeTabs t : extraTabs) if (t != null && !out.contains(t)) out.add(t);
        return out.toArray(new CreativeTabs[0]);
    }

    @Override
    protected boolean isInCreativeTab(CreativeTabs targetTab) {
        if (targetTab == CreativeTabs.SEARCH) return getCreativeTab() != null || extraTabs.length > 0;
        for (CreativeTabs t : getCreativeTabs()) if (t == targetTab) return true;
        return false;
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (isInCreativeTab(tab)) {
            List<ItemStack> list = new ArrayList<>();
            getSubItemsLegacy(tab, list);
            for (ItemStack s : list) if (s != null && !s.isEmpty()) items.add(s);
        }
    }

    /** Default: meta 0. */
    public void getSubItemsLegacy(CreativeTabs tab, List<ItemStack> list) {
        list.add(new ItemStack(this));
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        return onItemUseLegacy(stack, player, world, pos.getX(), pos.getY(), pos.getZ(), facing.getIndex(), hitX, hitY, hitZ)
                ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
    }

    public boolean onItemUseLegacy(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        return false;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        ItemStack out = onItemRightClickLegacy(stack, world, player);
        if (out == null) out = ItemStack.EMPTY;
        return new ActionResult<>(EnumActionResult.PASS, out);
    }

    public ItemStack onItemRightClickLegacy(ItemStack stack, World world, EntityPlayer player) {
        return stack;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        addInformationLegacy(stack, ClientPlayer.get(), tooltip, flag.isAdvanced());
    }

    @SideOnly(Side.CLIENT)
    public void addInformationLegacy(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
    }

    @SideOnly(Side.CLIENT)
    private static final class ClientPlayer {
        static EntityPlayer get() {
            return net.minecraft.client.Minecraft.getMinecraft().player;
        }
    }
}
