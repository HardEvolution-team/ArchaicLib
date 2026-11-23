package com.electrifiedded.archaiclib.v189;

import net.minecraft.client.resources.FolderResourcePack;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.LegacyV2Adapter;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

@SideOnly(Side.CLIENT)
public final class LegacyModelHooks189 {

    private static final org.apache.logging.log4j.Logger LOG = org.apache.logging.log4j.LogManager.getLogger("ArchaicLib");

    private LegacyModelHooks189() {}

    public static int install() {
        List<IResourcePack> packs = resourcePacks();
        if (packs == null) return 0;
        int wrapped = 0;
        for (int i = 0; i < packs.size(); i++) {
            IResourcePack pack = packs.get(i);
            if (pack instanceof LegacyModelResourcePack189) continue;
            if (!(pack instanceof LegacyV2Adapter) && !(pack instanceof net.minecraft.client.resources.FileResourcePack)
                    && !(pack instanceof FolderResourcePack)) {
                continue;
            }
            // Packs of installed (modern) mods are left alone; only loose / legacy packs are translated.
            IResourcePack inner = unwrap(pack);
            if (inner instanceof net.minecraftforge.fml.client.FMLFileResourcePack
                    || inner instanceof net.minecraftforge.fml.client.FMLFolderResourcePack) {
                continue;
            }
            packs.set(i, new LegacyModelResourcePack189(pack));
            wrapped++;
            LOG.info("[ArchaicLib] 1.8 asset translation enabled for resource pack '{}'", pack.getPackName());
        }
        return wrapped;
    }

    public static IResourcePack wrap(IResourcePack pack) {
        if (pack == null || pack instanceof LegacyModelResourcePack189) return pack;
        return new LegacyModelResourcePack189(pack);
    }

    /** Mod jars without pack.mcmeta are wrapped in LegacyV2Adapter: look at the real pack inside. */
    private static IResourcePack unwrap(IResourcePack pack) {
        for (int guard = 0; pack instanceof LegacyV2Adapter && guard < 4; guard++) {
            IResourcePack inner = null;
            for (Field f : LegacyV2Adapter.class.getDeclaredFields()) {
                if (!IResourcePack.class.isAssignableFrom(f.getType())) continue;
                try {
                    f.setAccessible(true);
                    inner = (IResourcePack) f.get(pack);
                } catch (Throwable ignored) {}
                break;
            }
            if (inner == null || inner == pack) break;
            pack = inner;
        }
        return pack;
    }

    @SuppressWarnings("unchecked")
    private static List<IResourcePack> resourcePacks() {
        try {
            Field field = FMLClientHandler.class.getDeclaredField("resourcePackList");
            field.setAccessible(true);
            Object value = field.get(FMLClientHandler.instance());
            return value instanceof List ? (List<IResourcePack>) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
