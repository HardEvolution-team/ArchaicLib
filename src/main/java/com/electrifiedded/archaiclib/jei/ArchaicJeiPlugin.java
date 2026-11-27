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
        INFO_WRAPPERS.clear();
        for (ArchaicJei.InfoCategory c : ArchaicJei.infoCategories()) {
            List<InfoRecipeCategory.Wrapper> list = new ArrayList<>();
            int h = 20;
            for (ArchaicJei.InfoEntry e : c.entries.get()) {
                InfoRecipeCategory.Wrapper w = new InfoRecipeCategory.Wrapper(e, c.width);
                list.add(w);
                h = Math.max(h, w.height);
            }
            INFO_WRAPPERS.put(c.uid, list);
            registry.addRecipeCategories(new InfoRecipeCategory(c, h, registry.getJeiHelpers().getGuiHelper()));
        }
    }

    private static final java.util.Map<String, List<InfoRecipeCategory.Wrapper>> INFO_WRAPPERS = new java.util.HashMap<>();

    @Override
    public void register(IModRegistry registry) {
        for (ArchaicJei.InfoCategory c : ArchaicJei.infoCategories()) {
            List<InfoRecipeCategory.Wrapper> list = INFO_WRAPPERS.get(c.uid);
            if (list == null) continue;
            registry.addRecipes(list, c.uid);
            if (!c.catalyst.isEmpty()) registry.addRecipeCatalyst(c.catalyst, c.uid);
        }
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
