package com.electrifiedded.archaiclib.original.client;

import com.electrifiedded.archaiclib.ExampleMod;
import com.electrifiedded.archaiclib.original.OriginalAssetsHooks;
import com.electrifiedded.archaiclib.original.OriginalModAssets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.resources.IResourcePack;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.util.List;

/** Client half: installs resource packs for resolved originals and warns once about missing ones. */
public final class OriginalAssetsClient {

    private static boolean warned;

    private OriginalAssetsClient() {}

    private static final java.util.Set<java.io.File> ADDED = new java.util.HashSet<>();

    /** Called at mod construction, before the first resource reload. */
    public static void install() {
        for (java.util.Map.Entry<java.io.File, List<String>> e : OriginalModAssets.discoverAll().entrySet()) {
            addPack(e.getKey(), e.getValue());
        }
        OriginalAssetsHooks.setListener(r -> addPack(r.getJar(), r.domains));
        MinecraftForge.EVENT_BUS.register(new OriginalAssetsClient());
    }

    @SuppressWarnings("unchecked")
    private static void addPack(java.io.File jar, List<String> domains) {
        if (jar == null || !ADDED.add(jar.getAbsoluteFile())) return;
        try {
            List<IResourcePack> packs = (List<IResourcePack>) ReflectionHelper.getPrivateValue(
                    Minecraft.class, Minecraft.getMinecraft(), "defaultResourcePacks", "field_110449_ao");
            packs.add(new OriginalJarResourcePack(jar, domains));
            ExampleMod.LOGGER.info("Original assets pack {} serves {}", jar.getName(), domains);
        } catch (Throwable t) {
            ExampleMod.LOGGER.error("Could not register original assets from {}", jar, t);
        }
    }

    @SubscribeEvent
    public void onGui(GuiOpenEvent event) {
        if (warned || !(event.getGui() instanceof GuiMainMenu)) return;
        warned = true;
        if (!OriginalModAssets.missing().isEmpty()) {
            event.setGui(new GuiMissingOriginals(event.getGui()));
        }
    }
}
