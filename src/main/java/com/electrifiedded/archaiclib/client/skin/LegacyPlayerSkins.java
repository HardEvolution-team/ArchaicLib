package com.electrifiedded.archaiclib.client.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Skin of any Minecraft account by player name, for NPCs that "are" a player.
 * <p>
 * Old mods downloaded {@code http://skins.minecraft.net/MinecraftSkins/<name>.png} (or s3.amazonaws.com/MinecraftSkins),
 * which no longer exists. This resolves the name to a UUID ({@code api.mojang.com}, falling back to
 * {@code api.minecraftservices.com}), fills the profile textures through the game's session service and lets the
 * vanilla {@link SkinManager} download and cache the skin (64x64 skins, slim arms, same disk cache as player heads).
 * <p>
 * Until a skin is loaded, and for names without an account or when offline, the vanilla default skin of the name's
 * offline UUID is returned (Steve or Alex), exactly what an offline-mode player of that name would wear.
 * Lookups run on one background thread, are cached for the session and retried after 5 minutes on network errors.
 */
@SideOnly(Side.CLIENT)
public final class LegacyPlayerSkins {
    private static final Logger LOG = LogManager.getLogger("ArchaicLib/PlayerSkins");
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final long RETRY_MS = 5 * 60 * 1000L;
    private static final Map<String, Entry> CACHE = new ConcurrentHashMap<>();
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ArchaicLib skin lookup");
        t.setDaemon(true);
        return t;
    });

    private LegacyPlayerSkins() {}

    private static final class Entry {
        final String name;
        volatile ResourceLocation skin;
        volatile boolean slim;
        volatile boolean loading;
        volatile long retryAt;

        Entry(String name) {
            this.name = name;
            UUID offline = EntityPlayer.getOfflineUUID(name);
            this.slim = "slim".equals(DefaultPlayerSkin.getSkinType(offline));
        }
    }

    /** Skin texture for the player name; never null (default skin while loading / unknown). */
    public static ResourceLocation getSkin(String name) {
        Entry e = entry(name);
        ResourceLocation s = e == null ? null : e.skin;
        return s != null ? s : DefaultPlayerSkin.getDefaultSkin(EntityPlayer.getOfflineUUID(name == null ? "" : name));
    }

    /** True if the skin uses the slim (Alex) arm model. */
    public static boolean isSlim(String name) {
        Entry e = entry(name);
        return e != null ? e.slim : "slim".equals(DefaultPlayerSkin.getSkinType(EntityPlayer.getOfflineUUID(name == null ? "" : name)));
    }

    /** True once the real skin of that account is available. */
    public static boolean isLoaded(String name) {
        Entry e = entry(name);
        return e != null && e.skin != null;
    }

    @Nullable
    private static Entry entry(@Nullable String name) {
        if (name == null || !VALID_NAME.matcher(name).matches()) return null;
        Entry e = CACHE.computeIfAbsent(name.toLowerCase(Locale.ROOT), k -> new Entry(name));
        if (e.skin == null && !e.loading && System.currentTimeMillis() >= e.retryAt) {
            e.loading = true;
            WORKER.submit(() -> lookup(e));
        }
        return e;
    }

    private static void lookup(Entry e) {
        try {
            UUID id = resolveUuid(e.name);
            if (id == null) {
                // no such account: keep the default skin for the rest of the session
                e.retryAt = Long.MAX_VALUE;
                e.loading = false;
                return;
            }
            Minecraft mc = Minecraft.getMinecraft();
            GameProfile profile = mc.getSessionService().fillProfileProperties(new GameProfile(id, e.name), false);
            if (!profile.getProperties().containsKey("textures")) {
                fail(e, "no textures in profile");
                return;
            }
            mc.addScheduledTask(() -> mc.getSkinManager().loadProfileTextures(profile, (type, location, texture) -> {
                if (type != MinecraftProfileTexture.Type.SKIN) return;
                String model = texture.getMetadata("model");
                e.slim = "slim".equals(model);
                e.skin = location;
                e.loading = false;
            }, false));
            // if the profile has no SKIN texture the callback never comes: allow a retry later
            e.retryAt = System.currentTimeMillis() + RETRY_MS;
            e.loading = false;
        } catch (Exception ex) {
            fail(e, ex.toString());
        }
    }

    private static void fail(Entry e, String why) {
        LOG.debug("Skin of {} not available ({}), using the default skin", e.name, why);
        e.retryAt = System.currentTimeMillis() + RETRY_MS;
        e.loading = false;
    }

    @Nullable
    private static UUID resolveUuid(String name) throws Exception {
        Exception last = null;
        for (String base : new String[]{"https://api.mojang.com/users/profiles/minecraft/", "https://api.minecraftservices.com/minecraft/profile/lookup/name/"}) {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(base + name).openConnection();
                c.setConnectTimeout(10000);
                c.setReadTimeout(10000);
                c.setRequestProperty("User-Agent", "ArchaicLib");
                int code = c.getResponseCode();
                if (code == 204 || code == 404) return null;
                if (code != 200) throw new java.io.IOException("HTTP " + code);
                try (Reader r = new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject o = new JsonParser().parse(r).getAsJsonObject();
                    String raw = o.get("id").getAsString();
                    return UUID.fromString(raw.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                }
            } catch (Exception ex) {
                last = ex;
            }
        }
        throw last;
    }

    private static final Map<ResourceLocation, Boolean> TALL = new ConcurrentHashMap<>();

    /**
     * True if a skin texture uses the 1.8+ 64x64 layout (height == width), false for old 64x32 skins - choose
     * ModelPlayer or a 64x32 ModelBiped accordingly. Downloaded skins are always 64x64 (vanilla converts them).
     * Reads only the PNG header of resource-pack textures, cached.
     */
    public static boolean isModernLayout(ResourceLocation texture) {
        return TALL.computeIfAbsent(texture, t -> {
            try (net.minecraft.client.resources.IResource r = Minecraft.getMinecraft().getResourceManager().getResource(t);
                 java.io.DataInputStream in = new java.io.DataInputStream(r.getInputStream())) {
                byte[] head = new byte[24];
                in.readFully(head);
                int w = ((head[16] & 255) << 24) | ((head[17] & 255) << 16) | ((head[18] & 255) << 8) | (head[19] & 255);
                int h = ((head[20] & 255) << 24) | ((head[21] & 255) << 16) | ((head[22] & 255) << 8) | (head[23] & 255);
                return w > 0 && h == w;
            } catch (Exception e) {
                return true; // dynamic (downloaded / default) textures are not in the resource manager and are 64x64
            }
        });
    }

    /** Forget cached layouts (resource packs reloaded). */
    public static void clearLayoutCache() {
        TALL.clear();
    }
}
