package com.electrifiedded.archaiclib.jei;

import com.electrifiedded.archaiclib.ghost.GhostRecipeTransfer;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import net.minecraft.inventory.Container;

/** Only loaded by JEI/HEI. Hooks ghost-grid containers into the recipe "+" button. */
@JEIPlugin
public class ArchaicJeiPlugin implements IModPlugin {

    @Override
    public void register(IModRegistry registry) {
        for (Class<? extends Container> c : GhostRecipeTransfer.containers()) {
            registry.getRecipeTransferRegistry().addRecipeTransferHandler(
                    new GhostTransferHandler<>(c, registry.getJeiHelpers().recipeTransferHandlerHelper()),
                    VanillaRecipeCategoryUid.CRAFTING);
        }
    }
}
