package com.electrifiedded.archaiclib.v189;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Vanilla 1.8.9 assets that 1.9-1.12 removed or renamed. Old resource packs often parent their models
 * to such vanilla models (block/fence_n, block/wall_ns, block/pane_*, ...). ArchaicLib ships 1.12-ready
 * copies of every removed 1.8.9 vanilla model under {@code archaiclib:legacy189/<path>}.
 */
public final class LegacyVanillaAssets189 {

    public static final String MODEL_PREFIX = "archaiclib:legacy189/";
    private static final String LIST = "/assets/archaiclib/legacy189/removed_models.txt";

    private static volatile Set<String> removed;

    private static final Map<String, String> TEXTURES = new HashMap<>();

    static {
        TEXTURES.put("blocks/command_block", "blocks/command_block_back");
        TEXTURES.put("blocks/redstone_dust_cross", "blocks/redstone_dust_dot");
        TEXTURES.put("blocks/redstone_dust_line", "blocks/redstone_dust_line0");
        TEXTURES.put("blocks/redstone_dust_cross_overlay", "blocks/redstone_dust_overlay");
        TEXTURES.put("blocks/redstone_dust_line_overlay", "blocks/redstone_dust_overlay");
        TEXTURES.put("blocks/bed_feet_end", "blocks/planks_oak");
        TEXTURES.put("blocks/bed_feet_side", "blocks/planks_oak");
        TEXTURES.put("blocks/bed_head_end", "blocks/planks_oak");
        TEXTURES.put("blocks/bed_head_side", "blocks/planks_oak");
        TEXTURES.put("blocks/bed_feet_top", "blocks/wool_colored_red");
        TEXTURES.put("blocks/bed_head_top", "blocks/wool_colored_red");
        TEXTURES.put("items/clock", "items/clock_00");
        TEXTURES.put("items/compass", "items/compass_00");
        TEXTURES.put("items/boat", "items/oak_boat");
        TEXTURES.put("items/bed", "blocks/wool_colored_red");
        TEXTURES.put("items/banner_base", "blocks/wool_colored_white");
        TEXTURES.put("items/quiver", "items/arrow");
        TEXTURES.put("entity/boat", "entity/boat/boat_oak");
    }

    private LegacyVanillaAssets189() {}

    /** @param path vanilla model path without namespace, e.g. "block/fence_n" */
    public static boolean isRemovedModel(String path) {
        return path != null && removed().contains(path);
    }

    public static String replacementModel(String path) {
        return MODEL_PREFIX + path;
    }

    /** 1.12 name of a removed vanilla texture ("blocks/..."), or null if unchanged. */
    public static String renamedTexture(String path) {
        return TEXTURES.get(path);
    }

    public static Set<String> removedModels() {
        return removed();
    }

    private static Set<String> removed() {
        Set<String> r = removed;
        if (r != null) return r;
        synchronized (LegacyVanillaAssets189.class) {
            if (removed != null) return removed;
            Set<String> set = new HashSet<>();
            try (InputStream in = LegacyVanillaAssets189.class.getResourceAsStream(LIST)) {
                if (in != null) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && line.charAt(0) != '#') set.add(line);
                    }
                }
            } catch (Throwable ignored) {
            }
            removed = Collections.unmodifiableSet(set);
            return removed;
        }
    }
}
