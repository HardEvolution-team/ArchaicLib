package com.electrifiedded.archaiclib.mixin.v189;

import com.electrifiedded.archaiclib.v189.engine.LegacyItemRenderer189;
import com.electrifiedded.archaiclib.v189.engine.LegacyModelEngine189;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Third person: items with 1.8 models are drawn by the ported 1.8.9 LayerHeldItem code. */
@Mixin(LayerHeldItem.class)
public abstract class LayerHeldItemMixin {
    @Shadow protected abstract void translateToHand(EnumHandSide side);

    @Inject(method = "renderHeldItem", at = @At("HEAD"), cancellable = true)
    private void archaiclib$legacyThirdPerson(EntityLivingBase entity, ItemStack stack, ItemCameraTransforms.TransformType type,
                                              EnumHandSide side, CallbackInfo ci) {
        if (stack.isEmpty()) return;
        LegacyModelEngine189.Model m = LegacyItemRenderer189.modelFor(stack, entity);
        if (m == null) return;
        GlStateManager.pushMatrix();
        translateToHand(side);
        LegacyItemRenderer189.renderThirdPerson(entity, stack, side == EnumHandSide.LEFT, m);
        GlStateManager.popMatrix();
        ci.cancel();
    }
}
