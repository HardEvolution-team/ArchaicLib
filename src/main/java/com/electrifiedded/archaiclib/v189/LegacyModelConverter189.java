package com.electrifiedded.archaiclib.v189;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class LegacyModelConverter189 {

    public interface TextureLookup {
        boolean exists(String namespace, String path);
    }

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String[] FOLDERS = {"blocks/", "items/", "block/", "item/"};
    private static final String TEXTURES_PREFIX = "textures/";
    private static final String PNG = ".png";

    private LegacyModelConverter189() {}

    public static byte[] convert(String path, byte[] data) {
        return convert(path, data, null);
    }

    public static byte[] convert(String path, byte[] data, TextureLookup lookup) {
        if (data == null || data.length == 0) return data;
        if (isSounds(path)) return convertSounds(namespaceOf(path), data, null);
        if (!isModel(path)) return data;
        JsonElement parsed = parse(new String(data, StandardCharsets.UTF_8));
        if (parsed == null || !parsed.isJsonObject()) return data;
        JsonObject model = parsed.getAsJsonObject();
        if (!convert(model, namespaceOf(path), lookup)) return data;
        return GSON.toJson(model).getBytes(StandardCharsets.UTF_8);
    }

    public static boolean isModel(String path) {
        if (path == null) return false;
        String p = path.replace('\\', '/');
        return p.endsWith(".json") && p.contains("/models/");
    }

    public static String namespaceOf(String path) {
        String p = path == null ? "" : path.replace('\\', '/');
        int assets = p.indexOf("assets/");
        int start = assets < 0 ? 0 : assets + "assets/".length();
        int end = p.indexOf('/', start);
        return end > start ? p.substring(start, end) : "minecraft";
    }

    public static boolean convert(JsonObject model, String namespace, TextureLookup lookup) {
        JsonElement parentElement = model.get("parent");
        String parent = parentElement != null && parentElement.isJsonPrimitive() ? parentElement.getAsString() : null;
        JsonObject display = model.has("display") && model.get("display").isJsonObject()
                ? model.getAsJsonObject("display") : null;
        boolean legacyDisplay = LegacyDisplayTransforms189.isLegacy(display);
        boolean modernDisplay = display != null && hasModernKeys(display);
        LegacyDisplayTransforms189.Kind kind = kindOf(model, parent, display);
        boolean changed = false;
        if (!modernDisplay && !model.has(LegacyItemTransforms189.MARKER)) {
            // keep the untouched 1.8 numbers: the client renders them through the ported 1.8.9 pipeline
            JsonObject marker = new JsonObject();
            marker.add("raw", model.deepCopy());
            if (display != null) marker.add("display", display.deepCopy());
            model.add(LegacyItemTransforms189.MARKER, marker);
            changed = true;
        }

        if (parent != null) {
            String mapped = parent;
            String bare = parent.startsWith("minecraft:") ? parent.substring("minecraft:".length()) : parent;
            if ("builtin/generated".equals(bare) && !modernDisplay) {
                // 1.8 items were parented to builtin/generated; in 1.12 hand/ground/head defaults live in item/*.
                mapped = kind.isItem() ? kind.itemParent : "item/generated";
            } else if (parent.indexOf(':') < 0 || parent.startsWith("minecraft:")) {
                if (LegacyVanillaAssets189.isRemovedModel(bare)) mapped = LegacyVanillaAssets189.replacementModel(bare);
            }
            if (!mapped.equals(parent)) {
                model.addProperty("parent", mapped);
                changed = true;
            }
        }
        changed |= fixTextures(model, namespace, lookup);

        if (display != null && legacyDisplay) {
            changed |= LegacyDisplayTransforms189.convert(display, kind);
        }
        // A 1.8 root block model (elements, no parent) relied on hard-coded hand/GUI transforms.
        if (parent == null && model.has("elements") && !modernDisplay) {
            if (display == null) {
                display = new JsonObject();
                model.add("display", display);
            } else if (!legacyDisplay && display.has("gui")) {
                LegacyDisplayTransforms189.convert(display, kind);
            }
            LegacyDisplayTransforms189.addBlockDefaults(display);
            changed = true;
        }
        return changed;
    }

    private static boolean hasModernKeys(JsonObject display) {
        for (String k : new String[]{"thirdperson_righthand", "thirdperson_lefthand", "firstperson_righthand",
                "firstperson_lefthand"}) { // ground/fixed/head/gui already existed in 1.8.8+
            if (display.has(k)) return true;
        }
        return false;
    }

    private static LegacyDisplayTransforms189.Kind kindOf(JsonObject model, String parent, JsonObject display) {
        if (model.has("elements")) return LegacyDisplayTransforms189.Kind.BLOCK;
        String p = parent == null ? "" : parent.toLowerCase(java.util.Locale.ROOT);
        boolean item = p.endsWith("builtin/generated") || p.startsWith("item/") || p.contains(":item/");
        if (p.isEmpty()) {
            JsonElement tex = model.get("textures");
            item = tex != null && tex.isJsonObject() && tex.getAsJsonObject().has("layer0");
        }
        if (!item) return LegacyDisplayTransforms189.Kind.BLOCK;
        if (p.endsWith("item/handheld_rod")) return LegacyDisplayTransforms189.Kind.ROD;
        if (p.endsWith("item/handheld")) return LegacyDisplayTransforms189.Kind.HANDHELD;
        return LegacyDisplayTransforms189.guessItemKind(display);
    }

    /** True for an {@code assets/<ns>/sounds.json} path. */
    public static boolean isSounds(String path) {
        if (path == null) return false;
        String p = path.replace('\\', '/');
        if (!p.startsWith("assets/") || !p.endsWith("/sounds.json")) return false;
        return p.indexOf('/', "assets/".length()) == p.length() - "/sounds.json".length();
    }

    public interface SoundLookup {
        /** @param path sound path without "sounds/" and ".ogg" */
        boolean exists(String namespace, String path);
    }

    /**
     * 1.8 resolved an unqualified sound file name against the namespace of the sounds.json that
     * declared it; 1.9+ resolves it against "minecraft". Qualifies such names with the file's namespace.
     * With a lookup, a name is only qualified when the file really exists in that namespace.
     */
    public static byte[] convertSounds(String namespace, byte[] data, SoundLookup lookup) {
        if (data == null || data.length == 0 || namespace == null || "minecraft".equals(namespace)) return data;
        JsonElement parsed = parse(new String(data, StandardCharsets.UTF_8));
        if (parsed == null || !parsed.isJsonObject()) return data;
        boolean changed = false;
        for (Map.Entry<String, JsonElement> ev : parsed.getAsJsonObject().entrySet()) {
            if (!ev.getValue().isJsonObject()) continue;
            JsonElement list = ev.getValue().getAsJsonObject().get("sounds");
            if (list == null || !list.isJsonArray()) continue;
            JsonArray arr = list.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                JsonElement s = arr.get(i);
                if (s.isJsonPrimitive()) {
                    String q = qualify(namespace, s.getAsString(), lookup);
                    if (q != null) {
                        arr.set(i, new JsonPrimitive(q));
                        changed = true;
                    }
                } else if (s.isJsonObject()) {
                    JsonObject o = s.getAsJsonObject();
                    if (o.has("type") && "event".equals(o.get("type").getAsString())) continue;
                    if (!o.has("name") || !o.get("name").isJsonPrimitive()) continue;
                    String q = qualify(namespace, o.get("name").getAsString(), lookup);
                    if (q != null) {
                        o.addProperty("name", q);
                        changed = true;
                    }
                }
            }
        }
        return changed ? GSON.toJson(parsed).getBytes(StandardCharsets.UTF_8) : data;
    }

    private static String qualify(String namespace, String name, SoundLookup lookup) {
        if (name == null || name.isEmpty() || name.indexOf(':') >= 0) return null;
        if (lookup != null) {
            try {
                if (!lookup.exists(namespace, name)) return null;
            } catch (Throwable ignored) {
                return null;
            }
        }
        return namespace + ":" + name;
    }

    private static boolean fixTextures(JsonObject model, String namespace, TextureLookup lookup) {
        JsonElement texturesElement = model.get("textures");
        if (texturesElement == null || !texturesElement.isJsonObject()) return false;
        JsonObject textures = texturesElement.getAsJsonObject();
        boolean changed = false;
        for (Map.Entry<String, JsonElement> entry : new java.util.ArrayList<>(textures.entrySet())) {
            JsonElement value = entry.getValue();
            if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) continue;
            String reference = value.getAsString();
            String fixed = fixTexture(reference, namespace, lookup);
            if (!fixed.equals(reference)) {
                textures.addProperty(entry.getKey(), fixed);
                changed = true;
            }
        }
        return changed;
    }

    static String fixTexture(String reference, String modelNamespace, TextureLookup lookup) {
        String value = reference == null ? "" : reference.trim();
        if (value.isEmpty() || value.charAt(0) == '#') return reference;

        int colon = value.indexOf(':');
        String namespace = colon >= 0 ? value.substring(0, colon) : "minecraft";
        String path = colon >= 0 ? value.substring(colon + 1) : value;

        path = normalizePath(path);
        if (path.isEmpty()) return reference;
        // 1.8 resolved unqualified textures against "minecraft"; old packs that put the file in their
        // own namespace anyway are salvaged when the lookup can see it there.
        if (colon < 0 && lookup != null && !"minecraft".equals(modelNamespace)
                && exists(lookup, modelNamespace, path) && !exists(lookup, "minecraft", path)) {
            namespace = modelNamespace;
        }
        if ("minecraft".equals(namespace)) {
            String renamed = LegacyVanillaAssets189.renamedTexture(path);
            return renamed != null ? renamed : path;
        }
        return namespace + ":" + path;
    }

    private static String normalizePath(String raw) {
        String path = raw.replace('\\', '/');
        while (path.startsWith("/")) path = path.substring(1);
        if (path.startsWith(TEXTURES_PREFIX)) path = path.substring(TEXTURES_PREFIX.length());
        if (path.endsWith(PNG)) path = path.substring(0, path.length() - PNG.length());
        boolean collapsed = true;
        while (collapsed) {
            collapsed = false;
            for (String folder : FOLDERS) {
                if (path.startsWith(folder + folder)) {
                    path = path.substring(folder.length());
                    collapsed = true;
                }
            }
        }
        return path;
    }

    private static boolean exists(TextureLookup lookup, String namespace, String path) {
        try {
            return lookup.exists(namespace, path);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static JsonElement parse(String text) {
        String json = text;
        if (!json.isEmpty() && json.charAt(0) == '\uFEFF') json = json.substring(1);
        try {
            return new JsonParser().parse(json);
        } catch (Throwable first) {
            try {
                return new JsonParser().parse(stripCommentsAndTrailingCommas(json));
            } catch (Throwable second) {
                return null;
            }
        }
    }

    private static String stripCommentsAndTrailingCommas(String text) {
        StringBuilder out = new StringBuilder(text.length());
        boolean string = false;
        boolean escape = false;
        boolean lineComment = false;
        boolean blockComment = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (lineComment) {
                if (c == '\n') {
                    lineComment = false;
                    out.append(c);
                }
                continue;
            }
            if (blockComment) {
                if (c == '*' && i + 1 < text.length() && text.charAt(i + 1) == '/') {
                    blockComment = false;
                    i++;
                }
                continue;
            }
            if (string) {
                out.append(c);
                if (escape) escape = false;
                else if (c == '\\') escape = true;
                else if (c == '"') string = false;
                continue;
            }
            if (c == '"') {
                string = true;
                out.append(c);
                continue;
            }
            if (c == '/' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                if (next == '/') {
                    lineComment = true;
                    i++;
                    continue;
                }
                if (next == '*') {
                    blockComment = true;
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString().replaceAll(",\\s*([}\\]])", "$1");
    }
}
