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
        for (net.minecraft.block.Block b : com.electrifiedded.archaiclib.legacy.fluid.LegacyFluids.pendingLevelIgnored()) ignoreAll(b);
        com.electrifiedded.archaiclib.legacy.fluid.LegacyFluids.pendingLevelIgnored().clear();
        com.electrifiedded.archaiclib.legacy.fluid.LegacyFluids.lateStateMapperHook = LegacyModelRegistrar::ignoreAll;
        LegacyGameRegistry.lateModelHook = p -> Minecraft.getMinecraft().getRenderItem().getItemModelMesher()
                .register(p.item, p.meta, new ModelResourceLocation(p.location, p.variant));
    }

    /** every state of the block -> registryName#normal (1.7 fluid blocks: LEVEL is not a model property) */
    private static void ignoreAll(net.minecraft.block.Block b) {
        final ModelResourceLocation loc = new ModelResourceLocation(b.getRegistryName(), "normal");
        ModelLoader.setCustomStateMapper(b, new net.minecraft.client.renderer.block.statemap.StateMapperBase() {
            @Override
            protected ModelResourceLocation getModelResourceLocation(net.minecraft.block.state.IBlockState state) {
                return loc;
            }
        });
    }
}
