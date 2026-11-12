package com.electrifiedded.archaiclib.mixin.v189;

import com.electrifiedded.archaiclib.v189.ILegacyModelBlock189;
import com.google.gson.JsonObject;
import net.minecraft.client.renderer.block.model.ModelBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ModelBlock.class)
public class ModelBlockMixin implements ILegacyModelBlock189 {
    @Unique
    private JsonObject archaiclib$legacyDisplay;

    @Override
    public JsonObject archaiclib$getLegacyDisplay() {
        return archaiclib$legacyDisplay;
    }

    @Override
    public void archaiclib$setLegacyDisplay(JsonObject display) {
        archaiclib$legacyDisplay = display;
    }
}
