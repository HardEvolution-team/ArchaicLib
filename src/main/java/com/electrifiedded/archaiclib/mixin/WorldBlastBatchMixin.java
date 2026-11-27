package com.electrifiedded.archaiclib.mixin;

import com.electrifiedded.archaiclib.world.BlastBatch;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Routes setBlockState into {@link BlastBatch} while a batch is open on this world. */
@Mixin(World.class)
public abstract class WorldBlastBatchMixin {
    @Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z", at = @At("HEAD"), cancellable = true)
    private void archaiclib$blastBatch(BlockPos pos, IBlockState state, int flags, CallbackInfoReturnable<Boolean> cir) {
        World self = (World) (Object) this;
        if (BlastBatch.isActive(self)) cir.setReturnValue(BlastBatch.set(self, pos, state, flags));
    }
}
