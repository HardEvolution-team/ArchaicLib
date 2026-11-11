package com.electrifiedded.archaiclib.v1710.api.item;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import java.util.Map;

public abstract class ArchaicItem extends Item {
    public static final Map<ResourceLocation, ResourceLocation> TEXTURES = new Object2ObjectOpenHashMap<>();

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
