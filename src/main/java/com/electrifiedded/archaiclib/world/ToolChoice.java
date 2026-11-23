package com.electrifiedded.archaiclib.world;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.function.Predicate;

/**
 * Picking the right tool for a block, the way a player switches between pickaxe and shovel: for NPCs that
 * carry several tools in a bag (miners with a pickaxe and a shovel, builders...).
 */
public final class ToolChoice {

    private ToolChoice() {}

    /** Effective dig speed of {@code tool} on {@code state} (1 = bare hand), efficiency included. */
    public static float speed(ItemStack tool, IBlockState state) {
        if (tool.isEmpty()) return 1.0F;
        float s = Math.max(1.0F, tool.getDestroySpeed(state));
        int eff = EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, tool);
        if (eff > 0 && s > 1.0F) s += eff * eff + 1;
        return s;
    }

    /**
     * Shovel blocks: dirt, grass, sand, gravel, clay, snow, soul sand, farmland, mycelium - by harvest tool,
     * then by material for modded blocks that declare none.
     */
    public static boolean isShovelBlock(IBlockState state) {
        String t = state.getBlock().getHarvestTool(state);
        if (t != null) return "shovel".equals(t);
        Material m = state.getMaterial();
        return m == Material.GROUND || m == Material.GRASS || m == Material.SAND || m == Material.CLAY
                || m == Material.SNOW || m == Material.CRAFTED_SNOW;
    }

    /**
     * Index of the best stack in {@code candidates} for breaking {@code state}: tools that get drops beat
     * those that do not, then the faster one wins. Only stacks passing {@code allowed} are looked at; -1
     * when none is allowed. Ties keep the earlier index (put the held tool first to avoid needless swaps).
     */
    public static int best(List<ItemStack> candidates, IBlockState state, Predicate<ItemStack> allowed) {
        int best = -1;
        float bestScore = -1;
        for (int i = 0; i < candidates.size(); i++) {
            ItemStack s = candidates.get(i);
            if (s.isEmpty() || !allowed.test(s)) continue;
            float score = speed(s, state) + (DigSafety.canHarvest(state, s) ? 1000F : 0F);
            if (score > bestScore + 1e-3F) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }
}
