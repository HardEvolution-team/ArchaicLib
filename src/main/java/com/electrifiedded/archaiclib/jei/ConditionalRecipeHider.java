package com.electrifiedded.archaiclib.jei;

import com.electrifiedded.archaiclib.legacy.registry.LegacyRecipes;
import mezz.jei.api.IRecipeRegistry;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Old mods swapped recipes at runtime (config / per-world settings), so only one variant existed at a time.
 * Ports register every variant via {@link LegacyRecipes#addConditionalRecipe}; this keeps JEI showing only
 * the currently active ones (re-checked once a second, settings usually arrive on world join).
 */
final class ConditionalRecipeHider {
    private static ConditionalRecipeHider instance;
    private final IRecipeRegistry registry;
    private final Map<IRecipe, Boolean> shown = new HashMap<>();
    private int tick;

    private ConditionalRecipeHider(IRecipeRegistry registry) {
        this.registry = registry;
    }

    static void start(IRecipeRegistry registry) {
        if (instance != null) MinecraftForge.EVENT_BUS.unregister(instance);
        instance = new ConditionalRecipeHider(registry);
        instance.sync();
        MinecraftForge.EVENT_BUS.register(instance);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END && ++tick % 20 == 0) sync();
    }

    private void sync() {
        for (Map.Entry<IRecipe, BooleanSupplier> en : LegacyRecipes.CONDITIONAL.entrySet()) {
            boolean active;
            try {
                active = en.getValue().getAsBoolean();
            } catch (RuntimeException ex) {
                active = true;
            }
            Boolean prev = shown.get(en.getKey());
            if (prev != null && prev == active) continue;
            IRecipeWrapper w = registry.getRecipeWrapper(en.getKey(), VanillaRecipeCategoryUid.CRAFTING);
            if (w == null) continue;
            if (active) registry.unhideRecipe(w, VanillaRecipeCategoryUid.CRAFTING);
            else registry.hideRecipe(w, VanillaRecipeCategoryUid.CRAFTING);
            shown.put(en.getKey(), active);
        }
    }
}
