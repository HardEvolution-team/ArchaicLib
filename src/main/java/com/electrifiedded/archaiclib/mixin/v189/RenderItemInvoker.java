package com.electrifiedded.archaiclib.mixin.v189;

import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderItem.class)
public interface RenderItemInvoker {
    @Invoker("renderModel")
    void archaiclib$renderModel(IBakedModel model, ItemStack stack);

    @Invoker("renderEffect")
    void archaiclib$renderEffect(IBakedModel model);
}
