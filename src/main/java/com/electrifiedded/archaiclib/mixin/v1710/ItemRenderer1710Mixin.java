package com.electrifiedded.archaiclib.mixin.v1710;

import com.electrifiedded.archaiclib.v1710.api.client.IItemRenderer;
import com.electrifiedded.archaiclib.v1710.client.renderer.ArchaicTEISR;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.7 IItemRenderer items: first person drawn by the 1.7.10 code path; remembers the holder for third person. */
@Mixin(ItemRenderer.class)
public class ItemRenderer1710Mixin {
    @Inject(method = "renderItemInFirstPerson(Lnet/minecraft/client/entity/AbstractClientPlayer;FFLnet/minecraft/util/EnumHand;FLnet/minecraft/item/ItemStack;F)V",
            at = @At("HEAD"), cancellable = true)
    private void archaiclib$firstPerson1710(AbstractClientPlayer player, float partialTicks, float pitch, EnumHand hand,
                                            float swingProgress, ItemStack stack, float equipProgress, CallbackInfo ci) {
        IItemRenderer r = ArchaicTEISR.rendererFor(stack, IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON);
        if (r == null) return;
        EnumHandSide side = hand == EnumHand.MAIN_HAND ? player.getPrimaryHand() : player.getPrimaryHand().opposite();
        ArchaicTEISR.renderFirstPerson(player, hand, side == EnumHandSide.LEFT, swingProgress, stack, equipProgress, r);
        ci.cancel();
    }

    @Inject(method = "renderItemSide", at = @At("HEAD"))
    private void archaiclib$holder(EntityLivingBase entity, ItemStack stack, ItemCameraTransforms.TransformType type,
                                   boolean leftHanded, CallbackInfo ci) {
        ArchaicTEISR.currentEntity = entity;
    }
}
