package com.electrifiedded.archaiclib.mixin.v189;

import com.electrifiedded.archaiclib.v189.engine.ItemCameraTransforms;
import com.electrifiedded.archaiclib.v189.engine.LegacyItemRenderer189;
import com.electrifiedded.archaiclib.v189.engine.LegacyModelEngine189;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Routes items with 1.8 models to the ported 1.8.9 RenderItem code (GUI slots, frames, heads, ground). */
@Mixin(RenderItem.class)
public class RenderItemMixin {
    @Shadow public float zLevel;

    @Inject(method = "renderItemModelIntoGUI", at = @At("HEAD"), cancellable = true)
    private void archaiclib$legacyGui(ItemStack stack, int x, int y, IBakedModel model, CallbackInfo ci) {
        LegacyModelEngine189.Model m = LegacyItemRenderer189.modelFor(model);
        if (m == null) return;
        LegacyItemRenderer189.renderItemIntoGUI(stack, x, y, zLevel, m);
        ci.cancel();
    }

    @Inject(method = "renderItemModel", at = @At("HEAD"), cancellable = true)
    private void archaiclib$legacyModel(ItemStack stack, IBakedModel model,
                                        net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType type,
                                        boolean leftHanded, CallbackInfo ci) {
        if (stack.isEmpty()) return;
        LegacyModelEngine189.Model m = LegacyItemRenderer189.modelFor(model);
        if (m == null) return;
        ItemCameraTransforms.TransformType t;
        switch (type) {
            case GROUND: t = ItemCameraTransforms.TransformType.GROUND; break;
            case FIXED: t = ItemCameraTransforms.TransformType.FIXED; break;
            case HEAD: t = ItemCameraTransforms.TransformType.HEAD; break;
            case GUI: t = ItemCameraTransforms.TransformType.GUI; break;
            case NONE: t = ItemCameraTransforms.TransformType.NONE; break;
            default: return; // hands: ItemRenderer / LayerHeldItem ports
        }
        LegacyItemRenderer189.renderItemModelTransform(stack, m, t);
        ci.cancel();
    }
}
