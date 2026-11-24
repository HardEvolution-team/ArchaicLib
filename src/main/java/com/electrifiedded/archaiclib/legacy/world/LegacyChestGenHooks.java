package com.electrifiedded.archaiclib.legacy.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraft.world.storage.loot.functions.SetCount;
import net.minecraft.world.storage.loot.functions.SetMetadata;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 1.7 {@code ChestGenHooks.addItem(category, new WeightedRandomChestContent(stack, min, max, weight))} on 1.12 loot
 * tables. Old category names (dungeonChest, mineshaftCorridor, pyramidDesertyChest, pyramidJungleChest,
 * strongholdCorridor, strongholdCrossing, strongholdLibrary, villageBlacksmith, bonusChest) map to the vanilla
 * chest tables; entries are added to the table's "main" pool when it loads. Call any time before world load.
 */
public final class LegacyChestGenHooks {
    private static final Map<String, ResourceLocation> CATEGORIES = new HashMap<>();
    private static final Map<ResourceLocation, List<Entry>> ENTRIES = new HashMap<>();
    private static boolean registered;
    private static int counter;

    static {
        CATEGORIES.put("dungeonChest", LootTableList.CHESTS_SIMPLE_DUNGEON);
        CATEGORIES.put("mineshaftCorridor", LootTableList.CHESTS_ABANDONED_MINESHAFT);
        CATEGORIES.put("pyramidDesertyChest", LootTableList.CHESTS_DESERT_PYRAMID);
        CATEGORIES.put("pyramidJungleChest", LootTableList.CHESTS_JUNGLE_TEMPLE);
        CATEGORIES.put("pyramidJungleDispenser", LootTableList.CHESTS_JUNGLE_TEMPLE_DISPENSER);
        CATEGORIES.put("strongholdCorridor", LootTableList.CHESTS_STRONGHOLD_CORRIDOR);
        CATEGORIES.put("strongholdCrossing", LootTableList.CHESTS_STRONGHOLD_CROSSING);
        CATEGORIES.put("strongholdLibrary", LootTableList.CHESTS_STRONGHOLD_LIBRARY);
        CATEGORIES.put("villageBlacksmith", LootTableList.CHESTS_VILLAGE_BLACKSMITH);
        CATEGORIES.put("bonusChest", LootTableList.CHESTS_SPAWN_BONUS_CHEST);
    }

    private LegacyChestGenHooks() {}

    private static final class Entry {
        final ItemStack stack; final int min, max, weight; final String name;
        Entry(ItemStack stack, int min, int max, int weight, String name) {
            this.stack = stack; this.min = min; this.max = max; this.weight = weight; this.name = name;
        }
    }

    /** 1.7 {@code WeightedRandomChestContent(item, meta, min, max, weight)}. */
    public static void addItem(String category, Item item, int meta, int min, int max, int weight) {
        addItem(category, new ItemStack(item, 1, meta), min, max, weight);
    }

    /** 1.7 {@code WeightedRandomChestContent(stack, min, max, weight)}; the stack's meta is kept. */
    public static void addItem(String category, ItemStack stack, int min, int max, int weight) {
        ResourceLocation table = CATEGORIES.get(category);
        if (table == null && category.indexOf(':') > 0) table = new ResourceLocation(category);
        if (table == null || stack == null || stack.isEmpty() || weight <= 0) return;
        if (!registered) {
            registered = true;
            MinecraftForge.EVENT_BUS.register(LegacyChestGenHooks.class);
        }
        ResourceLocation id = stack.getItem().getRegistryName();
        String name = "archaiclib:legacy_" + (id == null ? "item" : id.getNamespace() + "_" + id.getPath()) + "_" + (counter++);
        ENTRIES.computeIfAbsent(table, k -> new ArrayList<>()).add(new Entry(stack.copy(), Math.max(1, min), Math.max(1, Math.max(min, max)), weight, name));
    }

    /** 1.7 category name -> 1.12 chest loot table (null if unknown). */
    public static ResourceLocation table(String category) {
        ResourceLocation t = CATEGORIES.get(category);
        return t != null || category.indexOf(':') < 0 ? t : new ResourceLocation(category);
    }

    /**
     * 1.7 {@code WeightedRandomChestContent.generateChestContents(rand, ChestGenHooks.getItems(category, rand), chest, n)}:
     * the chest gets the category's loot table (filled when first opened). {@code n} is ignored (the table decides).
     */
    public static void fillChest(net.minecraft.tileentity.TileEntityLockableLoot chest, String category, java.util.Random rand) {
        ResourceLocation t = table(category);
        if (chest != null && t != null) chest.setLootTable(t, rand.nextLong());
    }

    /** Same entry in several categories at once. */
    public static void addItem(String[] categories, ItemStack stack, int min, int max, int weight) {
        for (String c : categories) addItem(c, stack, min, max, weight);
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        List<Entry> list = ENTRIES.get(event.getName());
        if (list == null) return;
        LootPool pool = event.getTable().getPool("main");
        if (pool == null) return;
        for (Entry e : list) {
            List<LootFunction> fns = new ArrayList<>();
            if (e.max > 1) fns.add(new SetCount(new LootCondition[0], new RandomValueRange(e.min, e.max)));
            if (e.stack.getMetadata() != 0) fns.add(new SetMetadata(new LootCondition[0], new RandomValueRange(e.stack.getMetadata())));
            if (pool.getEntry(e.name) == null) {
                pool.addEntry(new LootEntryItem(e.stack.getItem(), e.weight, 0, fns.toArray(new LootFunction[0]), new LootCondition[0], e.name));
            }
        }
    }
}
