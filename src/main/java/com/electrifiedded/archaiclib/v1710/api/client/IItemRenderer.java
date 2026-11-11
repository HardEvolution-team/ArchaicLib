package com.electrifiedded.archaiclib.v1710.api.client;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IItemRenderer {

    enum ItemRenderType {
        ENTITY,
        EQUIPPED,
        EQUIPPED_FIRST_PERSON,
        INVENTORY,
        FIRST_PERSON_MAP
    }

    enum ItemRendererHelper {
        ENTITY_ROTATION,
        ENTITY_BOBBING,
        EQUIPPED_BLOCK,
        BLOCK_3D,
        INVENTORY_BLOCK
    }

    boolean handleRenderType(ItemStack item, ItemRenderType type);

    default boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return false;
    }

    void renderItem(ItemRenderType type, ItemStack item, Object... data);
}
