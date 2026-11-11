package com.electrifiedded.archaiclib.legacy.client;

import com.electrifiedded.archaiclib.Reference;
import com.electrifiedded.archaiclib.legacy.registry.LegacyGameRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Side.CLIENT)
public final class LegacyModelRegistrar {
    private LegacyModelRegistrar() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onModelRegistry(ModelRegistryEvent event) {
        for (LegacyGameRegistry.PendingModel p : LegacyGameRegistry.pendingModels()) {
            if (p.item != null) ModelLoader.setCustomModelResourceLocation(p.item, p.meta, new ModelResourceLocation(p.location, p.variant));
        }
        LegacyGameRegistry.pendingModels().clear();
        LegacyGameRegistry.lateModelHook = p -> Minecraft.getMinecraft().getRenderItem().getItemModelMesher()
                .register(p.item, p.meta, new ModelResourceLocation(p.location, p.variant));
    }
}
