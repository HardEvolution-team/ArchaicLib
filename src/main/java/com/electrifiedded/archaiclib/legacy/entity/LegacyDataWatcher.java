package com.electrifiedded.archaiclib.legacy.entity;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializer;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public final class LegacyDataWatcher {
    private static final Map<Entity, LegacyDataWatcher> WATCHERS = new com.google.common.collect.MapMaker().weakKeys().makeMap();
    private static Field entriesField;

    private final Entity entity;
    private final Map<Integer, DataParameter<?>> params = new HashMap<>();
    private int base = -1;

    private LegacyDataWatcher(Entity entity) { this.entity = entity; }

    public static synchronized LegacyDataWatcher of(Entity entity) {
        return WATCHERS.computeIfAbsent(entity, LegacyDataWatcher::new);
    }

    private EntityDataManager dm() { return entity.getDataManager(); }

    @SuppressWarnings("unchecked")
    private int computeBase() {
        try {
            if (entriesField == null) entriesField = ObfuscationReflectionHelper.findField(EntityDataManager.class, "field_187234_c");
            Map<Integer, ?> entries = (Map<Integer, ?>) entriesField.get(dm());
            int max = -1;
            for (Integer k : entries.keySet()) max = Math.max(max, k);
            return max + 1;
        } catch (Exception e) {
            throw new RuntimeException("LegacyDataWatcher: cannot read EntityDataManager entries", e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addObject(int id, Object value) {
        if (params.containsKey(id)) throw new IllegalArgumentException("Duplicate legacy data id " + id + " on " + entity.getClass());
        if (base < 0) base = computeBase();
        DataSerializer ser = serializerFor(value);
        int newId = base + id;
        if (newId > 254) throw new IllegalArgumentException("Legacy data id too large: " + id);
        DataParameter p = ser.createKey(newId);
        params.put(id, p);
        dm().register(p, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void updateObject(int id, Object value) {
        DataParameter p = param(id);
        Object cur = dm().get(p);
        if (value instanceof Number) {
            Number n = (Number) value;
            if (cur instanceof Integer) value = n.intValue();
            else if (cur instanceof Byte) value = n.byteValue();
            else if (cur instanceof Float) value = n.floatValue();
        }
        dm().set(p, value);
    }

    public void setObjectWatched(int id) { dm().setDirty(param(id)); }

    public boolean has(int id) { return params.containsKey(id); }

    public Object getWatchableObject(int id) { return dm().get(param(id)); }
    public int getWatchableObjectInt(int id) { return ((Number) getWatchableObject(id)).intValue(); }
    public byte getWatchableObjectByte(int id) { return ((Number) getWatchableObject(id)).byteValue(); }
    public short getWatchableObjectShort(int id) { return ((Number) getWatchableObject(id)).shortValue(); }
    public float getWatchableObjectFloat(int id) { return ((Number) getWatchableObject(id)).floatValue(); }
    public String getWatchableObjectString(int id) { return (String) getWatchableObject(id); }
    public ItemStack getWatchableObjectItemStack(int id) { return (ItemStack) getWatchableObject(id); }

    private DataParameter<?> param(int id) {
        DataParameter<?> p = params.get(id);
        if (p == null) throw new IllegalArgumentException("Legacy data id " + id + " not registered on " + entity.getClass());
        return p;
    }

    private static DataSerializer<?> serializerFor(Object v) {
        if (v instanceof Integer) return DataSerializers.VARINT;
        if (v instanceof Byte) return DataSerializers.BYTE;
        if (v instanceof Float) return DataSerializers.FLOAT;
        if (v instanceof String) return DataSerializers.STRING;
        if (v instanceof Boolean) return DataSerializers.BOOLEAN;
        if (v instanceof ItemStack) return DataSerializers.ITEM_STACK;
        if (v instanceof BlockPos) return DataSerializers.BLOCK_POS;
        throw new IllegalArgumentException("Unsupported legacy data type " + (v == null ? "null" : v.getClass()));
    }
}
