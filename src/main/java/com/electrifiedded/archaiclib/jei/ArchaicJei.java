package com.electrifiedded.archaiclib.jei;

import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/**
 * JEI/HEI recipe categories for custom crafting grids (any size), registered without touching the JEI API:
 * safe to call when JEI is not installed. The lib's JEI plugin shows them when it is.
 */
public final class ArchaicJei {

    private ArchaicJei() {}

    /** One recipe: {@code width x height} input slots (row by row, each slot a list of alternatives), one output. */
    public static final class GridRecipe {
        public final int width, height;
        public final List<List<ItemStack>> inputs;
        public final ItemStack output;
        public final boolean shapeless;

        public GridRecipe(int width, int height, List<List<ItemStack>> inputs, ItemStack output, boolean shapeless) {
            this.width = width;
            this.height = height;
            this.inputs = inputs;
            this.output = output;
            this.shapeless = shapeless;
        }

        /** Shaped recipe from a flat array of stacks (empty = no item). */
        public static GridRecipe shaped(int width, int height, ItemStack[] items, ItemStack output) {
            List<List<ItemStack>> in = new ArrayList<>();
            for (ItemStack s : items) in.add(s == null || s.isEmpty() ? Collections.emptyList() : Collections.singletonList(s.copy()));
            return new GridRecipe(width, height, in, output.copy(), false);
        }

        public static GridRecipe shapeless(List<ItemStack> items, ItemStack output) {
            List<List<ItemStack>> in = new ArrayList<>();
            for (ItemStack s : items) in.add(Collections.singletonList(s.copy()));
            return new GridRecipe(0, 0, in, output.copy(), true);
        }
    }

    /** A grid category: uid, title translation key, grid size, catalyst, recipe source and optional "+" transfer. */
    public static final class GridCategory {
        public final String uid;
        public final String modName;
        public final String titleKey;
        public final int width, height;
        public final ItemStack catalyst;
        public final Supplier<List<GridRecipe>> recipes;
        /** container for the "+" button (null: none), first grid slot index, first player inventory slot index */
        public Class<? extends Container> container;
        public int gridSlotStart, playerSlotStart;

        public GridCategory(String uid, String modName, String titleKey, int width, int height, ItemStack catalyst,
                            Supplier<List<GridRecipe>> recipes) {
            this.uid = uid;
            this.modName = modName;
            this.titleKey = titleKey;
            this.width = width;
            this.height = height;
            this.catalyst = catalyst;
            this.recipes = recipes;
        }

        public GridCategory transfer(Class<? extends Container> container, int gridSlotStart, int playerSlotStart) {
            this.container = container;
            this.gridSlotStart = gridSlotStart;
            this.playerSlotStart = playerSlotStart;
            return this;
        }
    }

    private static final List<GridCategory> CATEGORIES = new ArrayList<>();

    /** Call in pre-init, init or post-init (recipes are read when JEI starts). */
    public static void registerGridCategory(GridCategory c) {
        CATEGORIES.add(c);
    }

    static List<GridCategory> categories() {
        return CATEGORIES;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Info pages: "how to build / what it does" for mobs built from blocks, multiblock shrines, NPC jobs...
    // ---------------------------------------------------------------------------------------------------------

    /**
     * One 2D slice of a structure (front view, or one level seen from above) as rows of pattern chars.
     * ' ' = nothing; every other char maps to one or more alternative stacks (cycled once per second).
     */
    public static final class Layer {
        public final String labelKey;
        public final String[] rows;
        public final Map<Character, ItemStack[]> key = new LinkedHashMap<>();

        public Layer(String labelKey, String... rows) {
            this.labelKey = labelKey;
            this.rows = rows;
        }

        public Layer where(char c, ItemStack... stacks) {
            key.put(c, stacks);
            return this;
        }

        public int width() {
            int w = 0;
            for (String r : rows) w = Math.max(w, r.length());
            return w;
        }

        public int height() {
            return rows.length;
        }

        public ItemStack[] at(int x, int y) {
            if (y < 0 || y >= rows.length || x < 0 || x >= rows[y].length()) return null;
            char c = rows[y].charAt(x);
            return c == ' ' ? null : key.get(c);
        }
    }

    /**
     * One info page: a name, an optional rendered entity, optional structure layers (cycled every 2 s, each with
     * its label), the materials (counted from the layers), extra item slots (upgrades, tools, symbols...) and
     * description lines (translation keys; a translated text may contain a literal backslash-n for line breaks; "key|a|b" passes a and b as format args).
     * Every item shown is an input, so JEI "uses" (U) of a block finds the pages that need it.
     */
    public static final class InfoEntry {
        public final String nameKey;
        public Function<World, Entity> entity;
        public ItemStack icon = ItemStack.EMPTY;
        public final List<Layer> layers = new ArrayList<>();
        public final List<ItemStack> extras = new ArrayList<>();
        public String extrasLabelKey;
        public final List<String> lines = new ArrayList<>();
        public ItemStack output = ItemStack.EMPTY;
        /** entity render scale multiplier (1 = fit the box) */
        public float entityScale = 1F;

        public InfoEntry(String nameKey) {
            this.nameKey = nameKey;
        }

        /** The entity drawn on the page (client only; created lazily with the client world). */
        public InfoEntry entity(Function<World, Entity> factory) {
            this.entity = factory;
            return this;
        }

        public InfoEntry entityScale(float f) {
            this.entityScale = f;
            return this;
        }

        /** Shown instead of an entity when none is given or it cannot be created. */
        public InfoEntry icon(ItemStack icon) {
            this.icon = icon;
            return this;
        }

        public InfoEntry layer(Layer l) {
            layers.add(l);
            return this;
        }

        public InfoEntry extras(String labelKey, ItemStack... stacks) {
            this.extrasLabelKey = labelKey;
            for (ItemStack s : stacks) if (s != null && !s.isEmpty()) extras.add(s);
            return this;
        }

        public InfoEntry lines(String... keys) {
            Collections.addAll(lines, keys);
            return this;
        }

        /** Optional output slot: JEI recipe (R) lookup of that item finds the page. */
        public InfoEntry output(ItemStack s) {
            this.output = s == null ? ItemStack.EMPTY : s;
            return this;
        }

        /** Materials summed over all layers (first alternative decides equality; the slot cycles alternatives). */
        public List<List<ItemStack>> materials() {
            List<ItemStack[]> seen = new ArrayList<>();
            List<int[]> counts = new ArrayList<>();
            for (Layer l : layers) {
                for (String r : l.rows) for (char c : r.toCharArray()) {
                    if (c == ' ') continue;
                    ItemStack[] alt = l.key.get(c);
                    if (alt == null || alt.length == 0) continue;
                    int found = -1;
                    for (int i = 0; i < seen.size(); i++) if (same(seen.get(i), alt)) found = i;
                    if (found < 0) {
                        seen.add(alt);
                        counts.add(new int[]{1});
                    } else {
                        counts.get(found)[0]++;
                    }
                }
            }
            List<List<ItemStack>> out = new ArrayList<>();
            for (int i = 0; i < seen.size(); i++) {
                List<ItemStack> alt = new ArrayList<>();
                for (ItemStack s : seen.get(i)) {
                    if (s == null || s.isEmpty()) continue;
                    ItemStack c = s.copy();
                    c.setCount(Math.min(64, counts.get(i)[0]));
                    alt.add(c);
                }
                if (!alt.isEmpty()) out.add(alt);
            }
            return out;
        }

        private static boolean same(ItemStack[] a, ItemStack[] b) {
            if (a.length != b.length) return false;
            for (int i = 0; i < a.length; i++) if (!ItemStack.areItemStacksEqual(a[i], b[i])) return false;
            return true;
        }
    }

    /** A category of info pages. */
    public static final class InfoCategory {
        public final String uid;
        public final String modName;
        public final String titleKey;
        public final ItemStack catalyst;
        public final Supplier<List<InfoEntry>> entries;
        /** page width in pixels */
        public int width = 172;

        public InfoCategory(String uid, String modName, String titleKey, ItemStack catalyst, Supplier<List<InfoEntry>> entries) {
            this.uid = uid;
            this.modName = modName;
            this.titleKey = titleKey;
            this.catalyst = catalyst == null ? ItemStack.EMPTY : catalyst;
            this.entries = entries;
        }

        public InfoCategory width(int w) {
            this.width = w;
            return this;
        }
    }

    private static final List<InfoCategory> INFO = new ArrayList<>();

    /** Call in pre-init, init or post-init (pages are read when JEI starts). Safe without JEI installed. */
    public static void registerInfoCategory(InfoCategory c) {
        INFO.add(c);
    }

    static List<InfoCategory> infoCategories() {
        return INFO;
    }
}
