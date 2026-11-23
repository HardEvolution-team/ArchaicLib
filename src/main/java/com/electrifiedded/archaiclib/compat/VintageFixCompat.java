package com.electrifiedded.archaiclib.compat;

import com.electrifiedded.archaiclib.ExampleMod;
import com.electrifiedded.archaiclib.original.client.OriginalJarResourcePack;
import com.electrifiedded.archaiclib.v189.LegacyModelResourcePack189;
import net.minecraft.client.resources.IResourcePack;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * VintageFix (dynamic_resources / textures) stitches only textures it can LIST in the resource packs, and it only
 * knows how to list vanilla/Forge pack classes. Our own packs (original-jar assets, 1.8 model converter) were
 * "Cannot list resources" for it, so every texture they serve was "not discovered during texture pass" and the
 * items/blocks of those mods went purple-black. Here they get listing adapters - by reflection, VintageFix
 * stays an optional mod.
 */
public final class VintageFixCompat {
    private VintageFixCompat() {}

    private static boolean done;

    public static void install() {
        if (done) return;
        done = true;
        try {
            Class<?> helper = Class.forName("org.embeddedt.vintagefix.dynamicresources.ResourcePackHelper", false,
                    VintageFixCompat.class.getClassLoader());
            Class<?> adapter = Class.forName("org.embeddedt.vintagefix.dynamicresources.ResourcePackHelper$Adapter", false,
                    VintageFixCompat.class.getClassLoader());
            Method register = helper.getMethod("registerAdapter", Class.class, adapter);
            Method listOther = helper.getMethod("getAllPaths", IResourcePack.class, Predicate.class);
            register.invoke(null, OriginalJarResourcePack.class, proxy(adapter, (pack, filter) ->
                    ((OriginalJarResourcePack) pack).listPaths(filter)));
            register.invoke(null, com.electrifiedded.archaiclib.client.texture.GeneratedTexturePack.class, proxy(adapter, (pack, filter) ->
                    ((com.electrifiedded.archaiclib.client.texture.GeneratedTexturePack) pack).listPaths(filter)));
            register.invoke(null, LegacyModelResourcePack189.class, proxy(adapter, (pack, filter) -> {
                @SuppressWarnings("unchecked")
                Collection<String> c = (Collection<String>) listOther.invoke(null, ((LegacyModelResourcePack189) pack).getSource(), filter);
                return new ArrayList<>(c);
            }));
            ExampleMod.LOGGER.info("VintageFix found: resource listing adapters registered for ArchaicLib packs");
        } catch (ClassNotFoundException e) {
            // no VintageFix
        } catch (Throwable t) {
            ExampleMod.LOGGER.warn("Could not register VintageFix adapters", t);
        }
    }

    private interface Lister {
        Collection<String> list(Object pack, Predicate<String> filter) throws Exception;
    }

    @SuppressWarnings("unchecked")
    private static Object proxy(Class<?> adapter, Lister lister) {
        return Proxy.newProxyInstance(VintageFixCompat.class.getClassLoader(), new Class<?>[]{adapter}, (p, m, args) -> {
            switch (m.getName()) {
                case "getAllPaths": return lister.list(args[0], (Predicate<String>) args[1]).iterator();
                case "hashCode": return System.identityHashCode(p);
                case "equals": return p == args[0];
                case "toString": return "ArchaicLib VintageFix adapter";
                default: return null;
            }
        });
    }
}
