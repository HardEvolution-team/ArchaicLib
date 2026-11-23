package com.electrifiedded.archaiclib.v1710.api.client;

import com.electrifiedded.archaiclib.v1710.client.renderer.ArchaicTEISR;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SideOnly(Side.CLIENT)
public final class ArchaicMinecraftForgeClient {
    private static final Map<Item, IItemRenderer> RENDERERS = new Object2ObjectOpenHashMap<>();
    private static final Set<Item> HELD_ONLY = new HashSet<>();
    /** Item models that get wrapped by {@code LegacyHeldItemModel} at bake time. */
    public static final Set<ModelResourceLocation> HELD_MODELS = new HashSet<>();

    private ArchaicMinecraftForgeClient() {}

    public static void registerItemRenderer(Item item, IItemRenderer renderer) {
        RENDERERS.put(item, renderer);
        item.setTileEntityItemStackRenderer(ArchaicTEISR.INSTANCE);
    }

    /**
     * For 1.7 renderers that only handled EQUIPPED / EQUIPPED_FIRST_PERSON (the flat icon was used in the GUI, on the
     * ground and in item frames): the item keeps its normal json/generated models (pass their locations), and only the
     * hand perspectives are routed to {@code renderer}.
     */
    public static void registerHeldItemRenderer(Item item, IItemRenderer renderer, String... modelLocations) {
        registerItemRenderer(item, renderer);
        HELD_ONLY.add(item);
        for (String loc : modelLocations) {
            HELD_MODELS.add(new ModelResourceLocation(loc.toLowerCase(java.util.Locale.ROOT), "inventory"));
        }
    }

    public static IItemRenderer getRenderer(Item item) {
        return RENDERERS.get(item);
    }

    public static boolean isHeldOnly(Item item) {
        return HELD_ONLY.contains(item);
    }

    public static boolean hasCustomRenderer(ResourceLocation itemRegName) {
        Item item = Item.REGISTRY.getObject(itemRegName);
        return item != null && RENDERERS.containsKey(item) && !HELD_ONLY.contains(item);
    }
}
