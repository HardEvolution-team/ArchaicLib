package com.electrifiedded.archaiclib.jei;

import com.electrifiedded.archaiclib.ghost.GhostRecipeTransfer;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import net.minecraft.inventory.Container;

import java.util.ArrayList;
import java.util.List;

/** Only loaded by JEI/HEI. Ghost-grid "+" transfer and the {@link ArchaicJei} grid categories. */
@JEIPlugin
public class ArchaicJeiPlugin implements IModPlugin {

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        for (ArchaicJei.GridCategory c : ArchaicJei.categories()) {
            registry.addRecipeCategories(new GridRecipeCategory(c, registry.getJeiHelpers().getGuiHelper()));
        }
    }

    @Override
    public void register(IModRegistry registry) {
        for (Class<? extends Container> c : GhostRecipeTransfer.containers()) {
            registry.getRecipeTransferRegistry().addRecipeTransferHandler(
                    new GhostTransferHandler<>(c, registry.getJeiHelpers().recipeTransferHandlerHelper()),
                    VanillaRecipeCategoryUid.CRAFTING);
        }
        for (ArchaicJei.GridCategory c : ArchaicJei.categories()) {
            List<GridRecipeCategory.Wrapper> list = new ArrayList<>();
            for (ArchaicJei.GridRecipe r : c.recipes.get()) list.add(new GridRecipeCategory.Wrapper(r));
            registry.addRecipes(list, c.uid);
            if (!c.catalyst.isEmpty()) registry.addRecipeCatalyst(c.catalyst, c.uid);
            if (c.container != null) {
                registry.getRecipeTransferRegistry().addRecipeTransferHandler(c.container, c.uid,
                        c.gridSlotStart, c.width * c.height, c.playerSlotStart, 36);
            }
        }
    }

    @Override
    public void onRuntimeAvailable(mezz.jei.api.IJeiRuntime runtime) {
        ConditionalRecipeHider.start(runtime.getRecipeRegistry());
    }
}
