package com.electrifiedded.archaiclib.ghost;

import com.electrifiedded.archaiclib.v1710.api.network.ArchaicChannel;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Registry of ghost-grid containers for JEI/HEI recipe transfer. Mods call {@link #register} during
 * preInit/init; ArchaicLib's JEI plugin adds a crafting-category transfer handler for each class.
 * Works without JEI too (the plugin is simply never loaded).
 */
public final class GhostRecipeTransfer {

    private static final Set<Class<? extends Container>> CONTAINERS = new LinkedHashSet<>();
    private static ArchaicChannel channel;

    private GhostRecipeTransfer() {}

    public static synchronized <C extends Container & IGhostGridContainer> void register(Class<C> containerClass) {
        CONTAINERS.add(containerClass);
    }

    public static synchronized List<Class<? extends Container>> containers() {
        return Collections.unmodifiableList(new ArrayList<>(CONTAINERS));
    }

    /** Called by ArchaicLib in preInit on both sides. */
    public static synchronized void initNetwork() {
        if (channel == null) channel = new ArchaicChannel("archaiclib_ghost", PacketGhostGrid.class);
    }

    /** Client: ask the server to fill the open ghost grid. */
    public static void sendToServer(int windowId, List<ItemStack> cells) {
        if (channel != null) channel.sendToServer(new PacketGhostGrid(windowId, cells));
    }
}
