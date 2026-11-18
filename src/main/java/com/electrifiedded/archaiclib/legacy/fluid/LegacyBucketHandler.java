package com.electrifiedded.archaiclib.legacy.fluid;

import java.util.HashMap;
import java.util.Map;

import com.electrifiedded.archaiclib.Reference;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * The classic 1.7 "BucketHandler" tutorial class, shared: {@code LegacyBucketHandler.register(fluidBlock, filledBucket)}
 * and an empty vanilla bucket right-clicked on a SOURCE block of that fluid picks it up as the given item.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class LegacyBucketHandler {
    private static final Map<Block, Item> BUCKETS = new HashMap<>();

    private LegacyBucketHandler() {}

    public static void register(Block fluidBlock, Item filledBucket) {
        BUCKETS.put(fluidBlock, filledBucket);
    }

    public static Item bucketFor(Block fluidBlock) {
        return BUCKETS.get(fluidBlock);
    }

    @SubscribeEvent
    public static void onFillBucket(FillBucketEvent event) {
        if (event.getResult() != Event.Result.DEFAULT || event.isCanceled()) return;
        ItemStack empty = event.getEmptyBucket();
        if (empty.isEmpty() || empty.getItem() != Items.BUCKET) return;
        RayTraceResult t = event.getTarget();
        if (t == null || t.typeOfHit != RayTraceResult.Type.BLOCK) return;
        World world = event.getWorld();
        BlockPos pos = t.getBlockPos();
        IBlockState s = world.getBlockState(pos);
        Item bucket = BUCKETS.get(s.getBlock());
        if (bucket == null) return;
        if (s.getPropertyKeys().contains(BlockFluidBase.LEVEL) && s.getValue(BlockFluidBase.LEVEL) != 0) return;
        if (!world.isRemote) world.setBlockToAir(pos);
        event.setFilledBucket(new ItemStack(bucket));
        event.setResult(Event.Result.ALLOW);
    }
}
