package com.electrifiedded.archaiclib.mixin.v189;

import com.electrifiedded.archaiclib.v189.engine.LegacyItemRenderer189;
import com.electrifiedded.archaiclib.v189.engine.LegacyModelEngine189;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** First person: items with 1.8 models are drawn by the ported 1.8.9 ItemRenderer code. */
@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
    @Inject(method = "renderItemInFirstPerson(Lnet/minecraft/client/entity/AbstractClientPlayer;FFLnet/minecraft/util/EnumHand;FLnet/minecraft/item/ItemStack;F)V",
            at = @At("HEAD"), cancellable = true)
    private void archaiclib$legacyFirstPerson(AbstractClientPlayer player, float partialTicks, float pitch, EnumHand hand,
                                              float swingProgress, ItemStack stack, float equipProgress, CallbackInfo ci) {
        if (stack.isEmpty() || stack.getItem() == Items.FILLED_MAP) return;
        LegacyModelEngine189.Model m = LegacyItemRenderer189.modelFor(stack, player);
        if (m == null) return;
        EnumHandSide side = hand == EnumHand.MAIN_HAND ? player.getPrimaryHand() : player.getPrimaryHand().opposite();
        LegacyItemRenderer189.renderFirstPerson(player, partialTicks, hand, side == EnumHandSide.LEFT, swingProgress, stack, equipProgress, m);
        ci.cancel();
    }
}
