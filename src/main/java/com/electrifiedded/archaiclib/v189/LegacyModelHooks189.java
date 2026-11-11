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
            packs.set(i, new LegacyModelResourcePack189(pack));
            wrapped++;
        }
        return wrapped;
    }

    public static IResourcePack wrap(IResourcePack pack) {
        if (pack == null || pack instanceof LegacyModelResourcePack189) return pack;
        return new LegacyModelResourcePack189(pack);
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
