package com.electrifiedded.archaiclib.legacy.entity;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "archaiclib")
public final class ExtendedEntityProperties {
    private static final ResourceLocation KEY = new ResourceLocation("archaiclib", "extended_properties");
    private static final Map<Entity, Map<String, IExtendedEntityProperties>> PROPS = new com.google.common.collect.MapMaker().weakKeys().makeMap();

    private ExtendedEntityProperties() {}

    public static synchronized String register(Entity entity, String name, IExtendedEntityProperties props) {
        Map<String, IExtendedEntityProperties> map = PROPS.computeIfAbsent(entity, e -> new LinkedHashMap<>());
        String id = name;
        int i = 1;
        while (map.containsKey(id)) id = name + i++;
        map.put(id, props);
        props.init(entity, entity.world);
        return id;
    }

    @Nullable
    public static synchronized IExtendedEntityProperties get(Entity entity, String name) {
        Map<String, IExtendedEntityProperties> map = PROPS.get(entity);
        return map == null ? null : map.get(name);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T extends IExtendedEntityProperties> T getAs(Entity entity, String name) {
        return (T) get(entity, name);
    }

    static synchronized Map<String, IExtendedEntityProperties> all(Entity entity) {
        Map<String, IExtendedEntityProperties> map = PROPS.get(entity);
        return map == null ? Collections.emptyMap() : new LinkedHashMap<>(map);
    }

    @SubscribeEvent
    public static void onAttach(AttachCapabilitiesEvent<Entity> event) {
        event.addCapability(KEY, new Provider(event.getObject()));
    }

    private static final class Provider implements ICapabilitySerializable<NBTTagCompound> {
        private final Entity entity;

        Provider(Entity entity) { this.entity = entity; }

        @Override
        public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) { return false; }

        @Nullable
        @Override
        public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) { return null; }

        @Override
        public NBTTagCompound serializeNBT() {
            NBTTagCompound tag = new NBTTagCompound();
            for (IExtendedEntityProperties p : all(entity).values()) p.saveNBTData(tag);
            return tag;
        }

        @Override
        public void deserializeNBT(NBTTagCompound tag) {
            for (IExtendedEntityProperties p : all(entity).values()) p.loadNBTData(tag);
        }
    }
}
