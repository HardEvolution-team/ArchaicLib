package com.electrifiedded.archaiclib.v189;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class LegacyModelConverter189 {

    public interface TextureLookup {
        boolean exists(String namespace, String path);
    }

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String[] DISPLAY_SIDES = {"thirdperson", "firstperson"};
    private static final String[] FOLDERS = {"blocks/", "items/", "block/", "item/"};
    private static final String TEXTURES_PREFIX = "textures/";
    private static final String PNG = ".png";

    private LegacyModelConverter189() {}

    public static byte[] convert(String path, byte[] data) {
        return convert(path, data, null);
    }

    public static byte[] convert(String path, byte[] data, TextureLookup lookup) {
        if (data == null || data.length == 0 || !isModel(path)) return data;
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
        boolean changed = mapParent(model);
        changed |= fixTextures(model, namespace, lookup);
        changed |= mapDisplay(model);
        return changed;
    }

    private static boolean mapParent(JsonObject model) {
        JsonElement parentElement = model.get("parent");
        if (parentElement == null || !parentElement.isJsonPrimitive()) return false;
        String parent = parentElement.getAsString();
        String mapped = "builtin/generated".equals(parent) ? "item/generated" : parent;
        if (mapped.equals(parent)) return false;
        model.addProperty("parent", mapped);
        return true;
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
        String namespace = colon >= 0 ? value.substring(0, colon) : modelNamespace;
        String path = colon >= 0 ? value.substring(colon + 1) : value;

        path = normalizePath(path);
        if (path.isEmpty()) return reference;

        if (colon < 0 && lookup != null) {
            boolean own = exists(lookup, modelNamespace, path);
            boolean vanilla = exists(lookup, "minecraft", path);
            if (!own && vanilla) return path;
            namespace = modelNamespace;
        }

        if ("minecraft".equals(namespace) && colon < 0) return path;
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

    private static boolean mapDisplay(JsonObject model) {
        JsonElement displayElement = model.get("display");
        if (displayElement == null || !displayElement.isJsonObject()) return false;
        JsonObject display = displayElement.getAsJsonObject();
        boolean changed = false;
        for (String side : DISPLAY_SIDES) {
            JsonElement old = display.get(side);
            if (old == null) continue;
            String right = side + "_righthand";
            String left = side + "_lefthand";
            if (!display.has(right)) {
                display.add(right, old.deepCopy());
                changed = true;
            }
            if (!display.has(left)) {
                display.add(left, old.deepCopy());
                changed = true;
            }
            display.remove(side);
            changed = true;
        }
        return changed;
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
