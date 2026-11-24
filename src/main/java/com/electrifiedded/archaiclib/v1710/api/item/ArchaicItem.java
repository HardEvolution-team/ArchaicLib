package com.electrifiedded.archaiclib.v1710.api.item;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import java.util.Map;

public abstract class ArchaicItem extends Item {
    public static final Map<ResourceLocation, ResourceLocation> TEXTURES = new Object2ObjectOpenHashMap<>();
    /** Extra layers (layer1, layer2, ...) of generated item models, for 1.7 multi render pass items. */
    public static final Map<ResourceLocation, java.util.List<ResourceLocation>> EXTRA_LAYERS = new Object2ObjectOpenHashMap<>();

    private final String modId;

    protected ArchaicItem() {
        ModContainer container = Loader.instance().activeModContainer();
        if (container == null) {
            throw new IllegalStateException("ArchaicItem must be constructed during mod loading");
        }
        this.modId = container.getModId();
    }

    public static void registerMetaTexture(Item item, int meta, String textureName) {
        ResourceLocation reg = item.getRegistryName();
        if (reg == null) throw new IllegalStateException("registerMetaTexture needs a registered item: " + item);
        String domain = reg.getNamespace();
        String path = textureName;
        int colonIndex = textureName.indexOf(':');
        if (colonIndex != -1) {
            domain = textureName.substring(0, colonIndex);
            path = textureName.substring(colonIndex + 1);
        }
        ResourceLocation model = new ResourceLocation(reg.getNamespace(), reg.getPath() + "/meta_" + meta);
        TEXTURES.put(model, new ResourceLocation(domain, "items/" + path));
        com.electrifiedded.archaiclib.legacy.registry.LegacyGameRegistry.registerItemModel(item, meta, model.toString(), "inventory");
    }

    /**
     * Like {@link #registerMetaTexture} but with several layers ("layer0", "layer1", ...) - the 1.7
     * {@code requiresMultipleRenderPasses()} + {@code getIconFromDamageForRenderPass} pattern (spawn eggs etc.).
     * Tint each layer with an {@code IItemColor} using the layer index as tint index.
     */
    public static void registerMetaTextureLayers(Item item, int meta, String... textureNames) {
        if (textureNames.length == 0) throw new IllegalArgumentException("no layers");
        registerMetaTexture(item, meta, textureNames[0]);
        ResourceLocation reg = item.getRegistryName();
        ResourceLocation model = new ResourceLocation(reg.getNamespace(), reg.getPath() + "/meta_" + meta);
        java.util.List<ResourceLocation> extra = new java.util.ArrayList<>();
        for (int i = 1; i < textureNames.length; i++) {
            String t = textureNames[i];
            int c = t.indexOf(':');
            extra.add(c == -1 ? new ResourceLocation(reg.getNamespace(), "items/" + t) : new ResourceLocation(t.substring(0, c), "items/" + t.substring(c + 1)));
        }
        if (extra.isEmpty()) EXTRA_LAYERS.remove(model); else EXTRA_LAYERS.put(model, extra);
    }

    @SuppressWarnings("UnusedReturnValue")
    public Item setTextureName(String textureName) {
        String domain = this.modId;
        String path = textureName;

        int colonIndex = textureName.indexOf(':');
        if (colonIndex != -1) {
            domain = textureName.substring(0, colonIndex);
            path = textureName.substring(colonIndex + 1);
        }

        if (getRegistryName() == null) {
            setRegistryName(new ResourceLocation(this.modId, path));
        }

        TEXTURES.put(getRegistryName(), new ResourceLocation(domain, "items/" + path));
        return this;
    }
}
