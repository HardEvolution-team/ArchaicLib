package com.electrifiedded.archaiclib.mixin.v189;

import com.electrifiedded.archaiclib.v189.LegacyItemTransforms189;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Function;

/** Bakes 1.8 (legacy) json models into a wrapper backed by the ported 1.8.9 model engine. */
@Mixin(targets = "net.minecraftforge.client.model.ModelLoader$VanillaModelWrapper", remap = false)
public class VanillaModelWrapperMixin {
    @Shadow @Final private ResourceLocation location;
    @Shadow @Final private ModelBlock model;

    @Inject(method = "bakeImpl", at = @At("RETURN"), cancellable = true)
    private void archaiclib$wrapLegacy(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> getter,
                                       CallbackInfoReturnable<IBakedModel> cir) {
        boolean identity;
        try {
            Optional<TRSRTransformation> t = state.apply(Optional.empty());
            identity = !t.isPresent() || t.get().isIdentity();
        } catch (Throwable e) {
            identity = false;
        }
        IBakedModel wrapped = LegacyItemTransforms189.wrap(model, cir.getReturnValue(), location, identity);
        if (wrapped != cir.getReturnValue()) cir.setReturnValue(wrapped);
    }
}
