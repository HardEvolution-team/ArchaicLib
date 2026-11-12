package com.electrifiedded.archaiclib.mixin.v189;

import com.electrifiedded.archaiclib.v189.ILegacyModelBlock189;
import com.electrifiedded.archaiclib.v189.LegacyItemTransforms189;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import net.minecraft.client.renderer.block.model.ModelBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Type;

@Mixin(targets = "net.minecraft.client.renderer.block.model.ModelBlock$Deserializer")
public class ModelBlockDeserializerMixin {
    @Inject(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/client/renderer/block/model/ModelBlock;",
            at = @At("RETURN"))
    private void archaiclib$keepLegacyDisplay(JsonElement json, Type type, JsonDeserializationContext ctx,
                                              CallbackInfoReturnable<ModelBlock> cir) {
        if (cir.getReturnValue() == null || json == null || !json.isJsonObject()) return;
        JsonElement marker = json.getAsJsonObject().get(LegacyItemTransforms189.MARKER);
        if (marker != null && marker.isJsonObject()) {
            ((ILegacyModelBlock189) cir.getReturnValue()).archaiclib$setLegacyDisplay(marker.getAsJsonObject());
        }
    }
}
