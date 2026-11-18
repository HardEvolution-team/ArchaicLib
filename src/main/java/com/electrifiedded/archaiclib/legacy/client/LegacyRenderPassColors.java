package com.electrifiedded.archaiclib.legacy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.7-style multi-pass item colouring ({@code getRenderPasses} + {@code getColorFromItemStack(stack, pass)})
 * on 1.12 item models. Give the item model one {@code layerN} per old render pass; 1.12 uses the layer index
 * as tint index, so pass N gets {@code colors[N]}. Passes beyond the array are left untinted.
 * Call from client init (item colours are registered after models are baked in preInit).
 */
@SideOnly(Side.CLIENT)
public final class LegacyRenderPassColors {

    private LegacyRenderPassColors() {}

    public static void register(Item item, int... colors) {
        final int[] c = colors.clone();
        Minecraft.getMinecraft().getItemColors().registerItemColorHandler(
                (stack, tint) -> tint >= 0 && tint < c.length ? c[tint] : -1, item);
    }
}
