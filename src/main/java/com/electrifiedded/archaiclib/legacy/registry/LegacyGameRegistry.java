package com.electrifiedded.archaiclib.legacy.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public final class LegacyGameRegistry {
    public static final class PendingModel {
        public final Item item;
        public final int meta;
        public final String location;
        public final String variant;

        PendingModel(Item item, int meta, String location, String variant) {
            this.item = item;
            this.meta = meta;
            this.location = location;
            this.variant = variant;
        }
    }

    private static final List<PendingModel> MODELS = new ArrayList<>();
    public static Consumer<PendingModel> lateModelHook;

    private LegacyGameRegistry() {}

    public static String activeModId() {
        ModContainer mc = Loader.instance().activeModContainer();
        return mc == null ? "minecraft" : mc.getModId();
    }

    public static Block registerBlock(Block block, String name) {
        return registerBlock(block, new ItemBlock(block), name);
    }

    public static Block registerBlock(Block block, Item itemBlock, String name) {
        if (block.getRegistryName() == null) block.setRegistryName(new ResourceLocation(activeModId(), name));
        ForgeRegistries.BLOCKS.register(block);
        if (itemBlock != null) {
            if (itemBlock.getRegistryName() == null) itemBlock.setRegistryName(block.getRegistryName());
            ForgeRegistries.ITEMS.register(itemBlock);
        }
        return block;
    }

    public static Item registerItem(Item item, String name) {
        if (item.getRegistryName() == null) item.setRegistryName(new ResourceLocation(activeModId(), name));
        ForgeRegistries.ITEMS.register(item);
        return item;
    }

    public static void registerItemModel(Item item, int meta, String location, String variant) {
        PendingModel p = new PendingModel(item, meta, location.toLowerCase(Locale.ROOT), variant);
        if (lateModelHook != null) lateModelHook.accept(p);
        else MODELS.add(p);
    }

    public static void registerItemModel(Block block, int meta, String location, String variant) {
        registerItemModel(Item.getItemFromBlock(block), meta, location, variant);
    }

    public static List<PendingModel> pendingModels() {
        return MODELS;
    }

    /** registerModEntity + 1.7 spawn egg in one call. */
    public static void registerModEntity(Class<? extends Entity> cls, String name, int id, Object mod, int range, int freq, boolean velocity, int eggPrimary, int eggSecondary) {
        String modid = activeModId();
        EntityRegistry.registerModEntity(new ResourceLocation(modid, name), cls, modid + "." + name, id, mod, range, freq, velocity, eggPrimary, eggSecondary);
    }

    public static void registerModEntity(Class<? extends Entity> cls, String name, int id, Object mod, int range, int freq, boolean velocity) {
        String modid = activeModId();
        EntityRegistry.registerModEntity(new ResourceLocation(modid, name), cls, modid + "." + name, id, mod, range, freq, velocity);
    }
}
