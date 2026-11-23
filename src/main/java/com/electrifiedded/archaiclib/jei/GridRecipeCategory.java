package com.electrifiedded.archaiclib.jei;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

import java.util.List;

/** A W x H grid, an arrow and the output slot. Only loaded by JEI. */
class GridRecipeCategory implements IRecipeCategory<GridRecipeCategory.Wrapper> {

    static final class Wrapper implements IRecipeWrapper {
        final ArchaicJei.GridRecipe recipe;

        Wrapper(ArchaicJei.GridRecipe recipe) {
            this.recipe = recipe;
        }

        @Override
        public void getIngredients(IIngredients ingredients) {
            ingredients.setInputLists(VanillaTypes.ITEM, recipe.inputs);
            ingredients.setOutput(VanillaTypes.ITEM, recipe.output);
        }
    }

    private final ArchaicJei.GridCategory cat;
    private final IDrawable background;
    private final IDrawableStatic slot;
    private final IDrawable icon;

    GridRecipeCategory(ArchaicJei.GridCategory cat, IGuiHelper gui) {
        this.cat = cat;
        this.background = gui.createBlankDrawable(cat.width * 18 + 60, Math.max(cat.height * 18, 26));
        this.slot = gui.getSlotDrawable();
        this.icon = cat.catalyst.isEmpty() ? null : gui.createDrawableIngredient(cat.catalyst);
    }

    @Override
    public String getUid() {
        return cat.uid;
    }

    @Override
    public String getTitle() {
        return I18n.format(cat.titleKey);
    }

    @Override
    public String getModName() {
        return cat.modName;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    private int outX() {
        return cat.width * 18 + 38;
    }

    private int outY() {
        return cat.height * 18 / 2 - 9;
    }

    @Override
    public void drawExtras(Minecraft mc) {
        for (int y = 0; y < cat.height; y++) for (int x = 0; x < cat.width; x++) slot.draw(mc, x * 18, y * 18);
        slot.draw(mc, outX(), outY());
        mc.fontRenderer.drawString("->", cat.width * 18 + 15, outY() + 5, 0x404040);
    }

    @Override
    public void setRecipe(IRecipeLayout layout, Wrapper w, IIngredients ingredients) {
        IGuiItemStackGroup g = layout.getItemStacks();
        int n = cat.width * cat.height;
        for (int i = 0; i < n; i++) g.init(i, true, (i % cat.width) * 18, (i / cat.width) * 18);
        g.init(n, false, outX(), outY());
        List<List<ItemStack>> in = ingredients.getInputs(VanillaTypes.ITEM);
        ArchaicJei.GridRecipe r = w.recipe;
        for (int i = 0; i < in.size(); i++) {
            int slotIndex;
            if (r.shapeless || r.width <= 0) {
                slotIndex = i;
            } else {
                int x = i % r.width, y = i / r.width;
                if (x >= cat.width || y >= cat.height) continue;
                slotIndex = y * cat.width + x;
            }
            if (slotIndex < n && !in.get(i).isEmpty()) g.set(slotIndex, in.get(i));
        }
        g.set(n, ingredients.getOutputs(VanillaTypes.ITEM).get(0));
        if (r.shapeless) layout.setShapeless();
    }
}
