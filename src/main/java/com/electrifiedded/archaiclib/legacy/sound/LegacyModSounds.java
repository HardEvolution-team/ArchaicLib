package com.electrifiedded.archaiclib.legacy.sound;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 1.7.10/1.8 mods only shipped a sounds.json and played sounds by string ("modid:mob.living").
 * In 1.12 every event must be a registered {@link SoundEvent}. Call {@link #autoRegister(String)}
 * in the mod constructor / preInit: every key of assets/<modid>/sounds.json becomes a SoundEvent
 * and is resolvable through {@link LegacySounds#get(String)} and {@link #get(String, String)}.
 */
public final class LegacyModSounds {
    private static final Map<String, Map<String, SoundEvent>> BY_MOD = new LinkedHashMap<>();

    private LegacyModSounds() {}

    public static synchronized Map<String, SoundEvent> autoRegister(String modid) {
        String domain = modid.toLowerCase(Locale.ROOT);
        if (BY_MOD.containsKey(domain)) return BY_MOD.get(domain);
        Map<String, SoundEvent> events = new LinkedHashMap<>();
        String path = "/assets/" + domain + "/sounds.json";
        try (InputStream in = LegacyModSounds.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("LegacyModSounds: missing " + path);
            JsonObject root = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Entry<String, ?> e : root.entrySet()) {
                ResourceLocation rl = new ResourceLocation(domain, e.getKey());
                SoundEvent ev = new SoundEvent(rl).setRegistryName(rl);
                events.put(rl.getPath(), ev);
                LegacySounds.map(rl.toString(), ev);
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("LegacyModSounds: cannot read " + path, e);
        }
        BY_MOD.put(domain, events);
        MinecraftForge.EVENT_BUS.register(new Registrar(events));
        return events;
    }

    /** Resolve "Domain:key" (any case) or a bare key in the given mod domain; falls back to {@link LegacySounds#get}. */
    public static SoundEvent get(String modid, String name) {
        if (name == null) return null;
        String n = name.toLowerCase(Locale.ROOT);
        int c = n.indexOf(':');
        String domain = c >= 0 ? n.substring(0, c) : modid.toLowerCase(Locale.ROOT);
        Map<String, SoundEvent> m = BY_MOD.get(domain);
        SoundEvent ev = m == null ? null : m.get(c >= 0 ? n.substring(c + 1) : n);
        return ev != null ? ev : LegacySounds.get(n);
    }

    public static final class Registrar {
        private final Map<String, SoundEvent> events;

        Registrar(Map<String, SoundEvent> events) { this.events = events; }

        @SubscribeEvent
        public void onSounds(RegistryEvent.Register<SoundEvent> event) {
            for (SoundEvent ev : events.values()) event.getRegistry().register(ev);
        }
    }
}
