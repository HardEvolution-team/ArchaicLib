package com.electrifiedded.archaiclib.legacy.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class LegacyEntityList {
    private LegacyEntityList() {}

    @Nullable
    public static EntityEntry getEntryByName(String name) {
        if (name == null || name.isEmpty()) return null;
        for (EntityEntry entry : ForgeRegistries.ENTITIES.getValuesCollection()) {
            if (name.equals(entry.getName())) return entry;
        }
        if (name.indexOf(':') > 0) {
            return ForgeRegistries.ENTITIES.getValue(new ResourceLocation(name));
        }
        int dot = name.indexOf('.');
        String ns = dot > 0 ? name.substring(0, dot) : null;
        String bare = dot > 0 ? name.substring(dot + 1) : name;
        String path = bare.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        for (EntityEntry entry : ForgeRegistries.ENTITIES.getValuesCollection()) {
            ResourceLocation id = entry.getRegistryName();
            if (id == null || (ns != null && !ns.equalsIgnoreCase(id.getNamespace()))) continue;
            if (bare.equalsIgnoreCase(entry.getName()) || name.equalsIgnoreCase(entry.getName())
                    || path.equals(id.getPath())) {
                return entry;
            }
        }
        return null;
    }

    @Nullable
    public static Entity createEntityByName(String name, World world) {
        EntityEntry entry = getEntryByName(name);
        return entry == null ? null : entry.newInstance(world);
    }

    @Nullable
    public static String getEntityString(Entity entity) {
        return EntityList.getEntityString(entity);
    }

    public static List<String> getEntityNameList() {
        List<String> names = new ArrayList<>();
        for (EntityEntry entry : ForgeRegistries.ENTITIES.getValuesCollection()) {
            names.add(entry.getName());
        }
        return names;
    }

    @Nullable
    public static Class<? extends Entity> getClassFromName(String name) {
        EntityEntry entry = getEntryByName(name);
        return entry == null ? null : entry.getEntityClass();
    }
}
