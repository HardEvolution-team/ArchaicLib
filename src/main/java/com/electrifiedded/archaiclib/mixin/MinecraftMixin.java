package com.electrifiedded.archaiclib.mixin;

import com.electrifiedded.archaiclib.ExampleMod;
import com.electrifiedded.archaiclib.v189.LegacyModelHooks189;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "createDisplay", at = @At("HEAD"))
    public void inject(CallbackInfo ci){
        ExampleMod.LOGGER.info("Mixin succeed!");
    }

    @Inject(method = "refreshResources", at = @At("HEAD"))
    public void archaiclib$installLegacyModels(CallbackInfo ci) {
        LegacyModelHooks189.install();
    }
}
