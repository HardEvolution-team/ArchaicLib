package com.electrifiedded.archaiclib.client.resources;

import com.electrifiedded.archaiclib.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.LegacyV2Adapter;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.versioning.ArtifactVersion;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

/**
 * Resource pack plumbing shared by ArchaicLib's virtual packs.
 *
 * <p><b>Late packs.</b> In 1.12 FML runs the one and only startup resource reload BETWEEN mod construction and
 * preInit. A pack put into {@code Minecraft.defaultResourcePacks} during preInit/init is therefore not in any
 * FallbackResourceManager until the user presses F3+T: every lookup is "FileNotFound" and VintageFix (which lists the
 * packs of the live domain managers) never discovers its textures. {@link #addDefaultPack} also pushes the pack into
 * the live resource manager when the reload already happened.
 *
 * <p><b>Mod jars without pack.mcmeta.</b> FMLFileResourcePack then reports a dummy {@code pack_format 2} and FML
 * wraps the jar in {@link LegacyV2Adapter}, which rewrites {@code lang/en_us.lang} to {@code lang/en_US.lang}: the
 * port's lower-case 1.12 lang files are never read (raw keys in game). {@link #fixModPackFormats} unwraps such
 * packs for mods that depend on ArchaicLib and have no real pack.mcmeta.
 */
@SideOnly(Side.CLIENT)
public final class ArchaicPackInstaller {
    private ArchaicPackInstaller() {}

    /** Adds the pack to the default packs (survives F3+T) and to the live resource manager if it is already loaded. */
    @SuppressWarnings("unchecked")
    public static void addDefaultPack(IResourcePack pack) {
        Minecraft mc = Minecraft.getMinecraft();
        List<IResourcePack> packs = ObfuscationReflectionHelper.getPrivateValue(
                Minecraft.class, mc, "defaultResourcePacks", "field_110449_ao");
        if (!packs.contains(pack)) packs.add(pack);
        IResourceManager rm = mc.getResourceManager();
        if (rm instanceof SimpleReloadableResourceManager && !rm.getResourceDomains().isEmpty()) {
            ((SimpleReloadableResourceManager) rm).reloadResourcePack(pack);
            ExampleMod.LOGGER.info("Resource pack {} added after the startup reload - injected into the live manager", pack.getPackName());
        }
    }

    /** Called at ArchaicLib construction (mod packs exist, first reload not yet done). Returns the number unwrapped. */
    @SuppressWarnings("unchecked")
    public static int fixModPackFormats() {
        int fixed = 0;
        try {
            FMLClientHandler h = FMLClientHandler.instance();
            Field listF = FMLClientHandler.class.getDeclaredField("resourcePackList");
            Field mapF = FMLClientHandler.class.getDeclaredField("resourcePackMap");
            listF.setAccessible(true);
            mapF.setAccessible(true);
            List<IResourcePack> list = (List<IResourcePack>) listF.get(h);
            Map<String, IResourcePack> map = (Map<String, IResourcePack>) mapF.get(h);
            if (list == null || map == null) return 0;
            for (Map.Entry<String, IResourcePack> e : map.entrySet()) {
                IResourcePack pack = e.getValue();
                if (!(pack instanceof LegacyV2Adapter)) continue;
                ModContainer mod = Loader.instance().getIndexedModList().get(e.getKey());
                if (mod == null || !dependsOnArchaic(mod) || hasRealPackMeta(mod.getSource())) continue;
                IResourcePack inner = inner((LegacyV2Adapter) pack);
                if (inner == null) continue;
                int i = list.indexOf(pack);
                if (i >= 0) list.set(i, inner);
                e.setValue(inner);
                fixed++;
                ExampleMod.LOGGER.info("Mod {} has no pack.mcmeta: unwrapped LegacyV2Adapter so its lang/xx_xx.lang files load", e.getKey());
            }
        } catch (Throwable t) {
            ExampleMod.LOGGER.warn("Could not fix pack formats of mod resource packs", t);
        }
        return fixed;
    }

    private static boolean dependsOnArchaic(ModContainer mod) {
        if ("archaiclib".equals(mod.getModId())) return false;
        for (ArtifactVersion v : mod.getRequirements()) if ("archaiclib".equals(v.getLabel())) return true;
        for (ArtifactVersion v : mod.getDependencies()) if ("archaiclib".equals(v.getLabel())) return true;
        return false;
    }

    private static boolean hasRealPackMeta(File src) {
        if (src == null) return true;
        if (src.isDirectory()) return new File(src, "pack.mcmeta").isFile();
        try (ZipFile z = new ZipFile(src)) {
            return z.getEntry("pack.mcmeta") != null;
        } catch (Exception e) {
            return true;
        }
    }

    private static IResourcePack inner(LegacyV2Adapter a) {
        for (Field f : LegacyV2Adapter.class.getDeclaredFields()) {
            if (!IResourcePack.class.isAssignableFrom(f.getType())) continue;
            try {
                f.setAccessible(true);
                return (IResourcePack) f.get(a);
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    }
}
