package com.electrifiedded.archaiclib.v1710.api.client;

import com.electrifiedded.archaiclib.v1710.client.renderer.ArchaicTEISR;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Map;

@SideOnly(Side.CLIENT)
public final class ArchaicMinecraftForgeClient {
    private static final Map<Item, IItemRenderer> RENDERERS = new Object2ObjectOpenHashMap<>();

    private ArchaicMinecraftForgeClient() {}

    public static void registerItemRenderer(Item item, IItemRenderer renderer) {
        RENDERERS.put(item, renderer);
        item.setTileEntityItemStackRenderer(ArchaicTEISR.INSTANCE);
    }

    public static IItemRenderer getRenderer(Item item) {
        return RENDERERS.get(item);
    }

    public static boolean hasCustomRenderer(ResourceLocation itemRegName) {
        Item item = Item.REGISTRY.getObject(itemRegName);
        return item != null && RENDERERS.containsKey(item);
    }
}
