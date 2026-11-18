package com.electrifiedded.archaiclib.storage;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.tileentity.TileEntityShulkerBox;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Mod-agnostic access to item storages for AI that stores and fetches items (worker NPCs, golems, automation).
 * <p>
 * Anything with an inventory is reachable: Forge {@link IItemHandler} capabilities (side-less first, then every
 * side), plain {@link IInventory} and {@link ISidedInventory} tiles from old-style mods. On top of that
 * {@link #isStorage} tells a <i>storage</i> (vanilla/Iron chests, shulker boxes, barrels, crates, drawers...)
 * apart from a <i>machine</i> (furnaces, hoppers, IC2 machines...) so an NPC never dumps its haul into a
 * macerator. Classification: explicit rules ({@link #addRule}) &gt; vanilla classes &gt; name keywords &gt;
 * a structural test (many uniform slots that take anything). Results are cached per block + tile class.
 * <p>
 * Vanilla chests are returned as single halves (no {@code VanillaDoubleChestItemHandler}) so a double chest
 * is never counted twice when its two blocks are scanned separately.
 */
public final class StorageAccess {

    private StorageAccess() {}

    /** Lower-case keywords in a block id / tile class name that mark a storage. */
    private static final List<String> STORAGE_WORDS = new ArrayList<>(java.util.Arrays.asList(
            "chest", "crate", "barrel", "drawer", "storage", "shulker", "cabinet", "locker", "safe", "vault",
            "cupboard", "strongbox", "warehouse", "trunk", "coffer", "basket", "sack", "bin", "shelf", "cache",
            "box", "container", "depot", "stash", "wardrobe", "dresser", "closet", "pantry"));

    /** Keywords that mark a machine / logistics block; checked before {@link #STORAGE_WORDS}. */
    private static final List<String> MACHINE_WORDS = new ArrayList<>(java.util.Arrays.asList(
            "furnace", "smelter", "oven", "kiln", "forge", "machine", "generator", "engine", "crusher", "grinder",
            "macerator", "compressor", "extractor", "pulverizer", "centrifuge", "mixer", "assembler",
            "crafter", "autocraft", "workbench", "crafting", "hopper", "pipe", "duct", "conduit", "conveyor",
            "belt", "inserter", "importer", "exporter", "interface", "bus", "router", "transposer", "dropper",
            "dispenser", "brewing", "beacon", "charger", "battery", "energy", "reactor", "turbine", "solar",
            "terminal", "drive", "fabricator", "enchant", "anvil", "sorter", "filter", "recycler", "trash",
            "incinerator", "void", "mailbox", "jukebox", "turret", "spawner", "planter", "harvester", "fisher",
            "feeder", "trough", "incubator", "infuser", "altar", "pedestal", "display", "itemstand", "frame"));

    private static final List<Rule> RULES = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, Boolean> CACHE = new ConcurrentHashMap<>();

    private static final class Rule {
        final String pattern;
        final boolean storage;
        Rule(String pattern, boolean storage) { this.pattern = pattern; this.storage = storage; }
        boolean matches(String id) {
            if (pattern.endsWith(":*")) return id.startsWith(pattern.substring(0, pattern.length() - 1));
            if (pattern.endsWith("*")) return id.startsWith(pattern.substring(0, pattern.length() - 1));
            return id.equals(pattern);
        }
    }

    // --- configuration --------------------------------------------------------------------------------

    /**
     * Forces a classification for blocks whose registry name matches {@code pattern}
     * ({@code mod:block}, {@code mod:*} or a {@code mod:prefix*}). Later rules win over earlier ones.
     */
    public static void addRule(String pattern, boolean storage) {
        RULES.add(new Rule(pattern.trim().toLowerCase(Locale.ROOT), storage));
        CACHE.clear();
    }

    /** Removes all rules added with {@link #addRule} (for config reloads). */
    public static void clearRules() {
        RULES.clear();
        CACHE.clear();
    }

    /**
     * Parses one config line: {@code +mod:block} / {@code mod:block} = storage, {@code -mod:block} = never.
     * Blank lines and {@code #} comments are ignored. Returns false for an unparsable line.
     */
    public static boolean addRuleLine(String line) {
        String l = line.trim();
        int hash = l.indexOf('#');
        if (hash >= 0) l = l.substring(0, hash).trim();
        if (l.isEmpty()) return true;
        boolean storage = true;
        if (l.startsWith("-")) { storage = false; l = l.substring(1).trim(); }
        else if (l.startsWith("+")) l = l.substring(1).trim();
        if (!l.contains(":")) return false;
        addRule(l, storage);
        return true;
    }

    public static void addStorageKeyword(String word) {
        STORAGE_WORDS.add(word.toLowerCase(Locale.ROOT));
        CACHE.clear();
    }

    public static void addMachineKeyword(String word) {
        MACHINE_WORDS.add(word.toLowerCase(Locale.ROOT));
        CACHE.clear();
    }

    // --- lookup ---------------------------------------------------------------------------------------

    /** The item handler of the block at {@code pos}, or null when it has no inventory (or the chunk is not loaded). */
    public static IItemHandler handler(World world, BlockPos pos) {
        if (!world.isBlockLoaded(pos)) return null;
        return handler(world.getTileEntity(pos));
    }

    /** The item handler of a tile: capability (side-less, then any side), then IInventory wrappers. */
    public static IItemHandler handler(TileEntity te) {
        if (te == null || te.isInvalid()) return null;
        if (te instanceof TileEntityChest) return new InvWrapper((IInventory) te); // one half only
        try {
            if (te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
                IItemHandler h = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
                if (h != null && h.getSlots() > 0) return h;
            }
            for (EnumFacing f : EnumFacing.values()) {
                if (te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, f)) {
                    IItemHandler h = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, f);
                    if (h != null && h.getSlots() > 0) return h;
                }
            }
        } catch (RuntimeException ignored) {
            // a broken mod capability: fall back to the plain inventory below
        }
        if (te instanceof ISidedInventory) return new SidedInvWrapper((ISidedInventory) te, EnumFacing.UP);
        if (te instanceof IInventory && ((IInventory) te).getSizeInventory() > 0) return new InvWrapper((IInventory) te);
        return null;
    }

    /** Whether the block at {@code pos} is an item storage an NPC may put things into and take things from. */
    public static boolean isStorage(World world, BlockPos pos) {
        if (!world.isBlockLoaded(pos)) return false;
        return isStorage(world.getTileEntity(pos));
    }

    public static boolean isStorage(TileEntity te) {
        if (te == null || te.isInvalid()) return false;
        Block block = te.getBlockType();
        ResourceLocation rl = block == null ? null : block.getRegistryName();
        String id = rl == null ? "?" : rl.toString().toLowerCase(Locale.ROOT);
        String key = id + "|" + te.getClass().getName();
        Boolean cached = CACHE.get(key);
        if (cached != null) return cached;
        Boolean result = classify(te, block, id);
        if (result == null) return false; // undecidable right now (e.g. structural test on a full inventory): retry later
        CACHE.put(key, result);
        return result;
    }

    private static Boolean classify(TileEntity te, Block block, String id) {
        synchronized (RULES) {
            for (int i = RULES.size() - 1; i >= 0; i--) if (RULES.get(i).matches(id)) return RULES.get(i).storage;
        }
        if (te instanceof TileEntityChest || te instanceof TileEntityShulkerBox) return true;
        if (te instanceof TileEntityFurnace || te instanceof TileEntityHopper || te instanceof TileEntityBrewingStand
                || te instanceof TileEntityDispenser || te instanceof TileEntityBeacon || block == Blocks.ENDER_CHEST) {
            return false;
        }
        IItemHandler h = handler(te);
        if (h == null) return false;
        String path = id.substring(id.indexOf(':') + 1);
        String cls = te.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        for (String w : MACHINE_WORDS) if (path.contains(w) || cls.contains(w)) return false;
        for (String w : STORAGE_WORDS) if (path.contains(w) || cls.contains(w)) return true;
        return structural(h);
    }

    /**
     * No telling name: a storage is an inventory of 9+ slots that all take a full stack of cobblestone.
     * Machines have few slots, output slots refuse insertion, and many filter by recipe.
     */
    private static Boolean structural(IItemHandler h) {
        int slots = h.getSlots();
        if (slots < 9) return false;
        ItemStack probe = new ItemStack(Blocks.COBBLESTONE, 64);
        int tested = 0;
        for (int i = 0; i < slots; i++) {
            if (h.getSlotLimit(i) < 16) return false;
            if (!h.getStackInSlot(i).isEmpty()) continue;
            ItemStack rest = h.insertItem(i, probe.copy(), true);
            if (rest.getCount() == probe.getCount()) return false;
            tested++;
        }
        return tested > 0 ? Boolean.TRUE : null;
    }

    // --- item moving ----------------------------------------------------------------------------------

    /** Inserts into a handler (merging into partial stacks first, then empty slots). Returns what did not fit. */
    public static ItemStack insert(IItemHandler h, ItemStack stack, boolean simulate) {
        if (h == null || stack.isEmpty()) return stack;
        ItemStack rest = stack.copy();
        int slots = h.getSlots();
        for (int i = 0; i < slots && !rest.isEmpty(); i++) {
            ItemStack cur = h.getStackInSlot(i);
            if (!cur.isEmpty() && ItemStackMatch.canStack(cur, rest)) rest = h.insertItem(i, rest, simulate);
        }
        for (int i = 0; i < slots && !rest.isEmpty(); i++) {
            if (h.getStackInSlot(i).isEmpty()) rest = h.insertItem(i, rest, simulate);
        }
        return rest;
    }

    /** Whether at least one item of {@code stack} fits. */
    public static boolean canAccept(IItemHandler h, ItemStack stack) {
        if (h == null || stack.isEmpty()) return false;
        ItemStack one = stack.copy();
        one.setCount(1);
        return insert(h, one, true).isEmpty();
    }

    /** Free slots of a handler (slots that are empty). */
    public static int freeSlots(IItemHandler h) {
        int n = 0;
        if (h != null) for (int i = 0; i < h.getSlots(); i++) if (h.getStackInSlot(i).isEmpty()) n++;
        return n;
    }

    public static int count(IItemHandler h, Predicate<ItemStack> filter) {
        int n = 0;
        if (h == null) return 0;
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (!s.isEmpty() && filter.test(s)) n += s.getCount();
        }
        return n;
    }

    public static boolean contains(IItemHandler h, Predicate<ItemStack> filter) {
        if (h == null) return false;
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (!s.isEmpty() && filter.test(s)) return true;
        }
        return false;
    }

    /**
     * Removes up to {@code amount} matching items and returns them as one stack (the first match fixes the
     * exact type; only stackable equal items are added to it). Works with handlers that hand out at most a
     * stack per call (drawers, barrels with huge counts).
     */
    public static ItemStack extract(IItemHandler h, Predicate<ItemStack> filter, int amount) {
        ItemStack out = ItemStack.EMPTY;
        if (h == null || amount <= 0) return out;
        for (int i = 0; i < h.getSlots() && amount > 0; i++) {
            for (int guard = 0; guard < 64 && amount > 0; guard++) {
                ItemStack s = h.getStackInSlot(i);
                if (s.isEmpty() || !filter.test(s)) break;
                if (!out.isEmpty() && !ItemStackMatch.canStack(out, s)) break;
                int room = out.isEmpty() ? s.getMaxStackSize() : out.getMaxStackSize() - out.getCount();
                int take = Math.min(amount, Math.min(room, s.getCount()));
                if (take <= 0) return out;
                ItemStack part = h.extractItem(i, take, false);
                if (part.isEmpty()) break;
                if (out.isEmpty()) out = part;
                else out.grow(part.getCount());
                amount -= part.getCount();
                if (out.getCount() >= out.getMaxStackSize()) return out;
            }
        }
        return out;
    }

    /** Copies of all non-empty stacks (stock snapshots). */
    public static List<ItemStack> contents(IItemHandler h) {
        List<ItemStack> out = new ArrayList<>();
        if (h == null) return out;
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (!s.isEmpty()) out.add(s.copy());
        }
        return out;
    }

    /** Small stack-equality helpers kept here so the class works without the mod's own utils. */
    public static final class ItemStackMatch {
        private ItemStackMatch() {}

        public static boolean canStack(ItemStack a, ItemStack b) {
            return a.isStackable() && a.getItem() == b.getItem() && a.getMetadata() == b.getMetadata()
                    && ItemStack.areItemStackTagsEqual(a, b) && a.areCapsCompatible(b);
        }
    }
}
