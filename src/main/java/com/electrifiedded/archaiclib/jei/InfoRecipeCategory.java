package com.electrifiedded.archaiclib.jei;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Info page: name, structure layer (drawn by hand, cycled), entity preview, material/extra slots, wrapped text.
 * Only loaded by JEI.
 */
class InfoRecipeCategory implements IRecipeCategory<InfoRecipeCategory.Wrapper> {

    static final int CELL = 17, TITLE_H = 11, ENTITY_W = 58, MIN_TOP = 64;

    static final class Wrapper implements IRecipeWrapper {
        final ArchaicJei.InfoEntry e;
        final int width;
        final List<List<ItemStack>> materials;
        int gridW, gridH, topH, matY, extraY, textY, height;
        List<String> text = Collections.emptyList();
        private Entity cached;
        private World cachedWorld;
        private boolean broken;

        Wrapper(ArchaicJei.InfoEntry e, int width) {
            this.e = e;
            this.width = width;
            this.materials = e.materials();
            layout();
        }

        private static int rows(int n, int perRow) {
            return n == 0 ? 0 : (n + perRow - 1) / perRow;
        }

        int perRow() {
            return Math.max(1, width / 18);
        }

        void layout() {
            for (ArchaicJei.Layer l : e.layers) {
                gridW = Math.max(gridW, l.width());
                gridH = Math.max(gridH, l.height());
            }
            int gh = e.layers.isEmpty() ? 0 : gridH * CELL + 10;
            topH = Math.max(gh, e.entity != null ? MIN_TOP : (!e.icon.isEmpty() ? 34 : 0));
            int y = TITLE_H + topH + 2;
            matY = y + 9;
            if (!materials.isEmpty() || !e.output.isEmpty()) y += 9 + rows(materials.size() + (e.output.isEmpty() ? 0 : 1), perRow()) * 18;
            extraY = y + 9;
            if (!e.extras.isEmpty()) y += 9 + rows(e.extras.size(), perRow()) * 18;
            FontRenderer f = Minecraft.getMinecraft().fontRenderer;
            List<String> t = new ArrayList<>();
            for (String k : e.lines) {
                String[] parts = k.split("\\|");
                String s = I18n.hasKey(parts[0])
                        ? I18n.format(parts[0], (Object[]) java.util.Arrays.copyOfRange(parts, 1, parts.length)) : k;
                for (String part : s.split("\\\\n")) t.addAll(f.listFormattedStringToWidth(part, width));
            }
            text = t;
            textY = y + 2;
            height = textY + text.size() * 10;
        }

        Entity entity() {
            World w = Minecraft.getMinecraft().world;
            if (e.entity == null || broken || w == null) return null;
            if (cached == null || cachedWorld != w) {
                try {
                    cached = e.entity.apply(w);
                    cachedWorld = w;
                } catch (Throwable t) {
                    broken = true;
                    cached = null;
                }
            }
            return cached;
        }

        ArchaicJei.Layer currentLayer() {
            if (e.layers.isEmpty()) return null;
            return e.layers.get((int) ((System.currentTimeMillis() / 2000L) % e.layers.size()));
        }

        int layerIndex() {
            return e.layers.isEmpty() ? 0 : (int) ((System.currentTimeMillis() / 2000L) % e.layers.size());
        }

        static ItemStack pick(ItemStack[] alt) {
            if (alt == null || alt.length == 0) return ItemStack.EMPTY;
            ItemStack s = alt[(int) ((System.currentTimeMillis() / 1000L) % alt.length)];
            return s == null ? ItemStack.EMPTY : s;
        }

        int gridX() {
            return 0;
        }

        @Override
        public void getIngredients(IIngredients ing) {
            List<List<ItemStack>> in = new ArrayList<>(materials);
            for (ItemStack s : e.extras) in.add(Collections.singletonList(s));
            ing.setInputLists(VanillaTypes.ITEM, in);
            if (!e.output.isEmpty()) ing.setOutput(VanillaTypes.ITEM, e.output);
        }

        @Override
        public void drawInfo(Minecraft mc, int w, int h, int mouseX, int mouseY) {
            FontRenderer f = mc.fontRenderer;
            f.drawString(I18n.format(e.nameKey), 0, 1, 0x202020);
            ArchaicJei.Layer l = currentLayer();
            int ex0 = width - ENTITY_W;
            if (l != null) {
                String label = l.labelKey == null ? "" : I18n.format(l.labelKey);
                if (e.layers.size() > 1) label = (layerIndex() + 1) + "/" + e.layers.size() + " " + label;
                f.drawString(f.trimStringToWidth(label, Math.max(40, ex0 - 2)), 0, TITLE_H, 0x606060);
                int gy = TITLE_H + 10;
                RenderItem ri = mc.getRenderItem();
                for (int y = 0; y < l.height(); y++) for (int x = 0; x < l.width(); x++) {
                    int px = gridX() + x * CELL, py = gy + y * CELL;
                    ItemStack[] alt = l.at(x, y);
                    Gui.drawRect(px, py, px + 16, py + 16, alt == null ? 0x22000000 : 0x55000000);
                    ItemStack s = pick(alt);
                    if (!s.isEmpty()) {
                        RenderHelper.enableGUIStandardItemLighting();
                        GlStateManager.enableDepth();
                        ri.renderItemAndEffectIntoGUI(s, px, py);
                        RenderHelper.disableStandardItemLighting();
                    }
                }
                GlStateManager.disableLighting();
                GlStateManager.disableDepth();
            }
            Entity en = entity();
            int boxTop = TITLE_H, boxH = topH;
            if (en instanceof EntityLivingBase) {
                float eh = Math.max(0.5F, en.height), ew = Math.max(0.5F, en.width);
                int scale = (int) Math.max(4, Math.min((boxH - 6) / eh, (ENTITY_W - 4) / (ew * 1.4F)) * e.entityScale);
                int cx = ex0 + ENTITY_W / 2, cy = boxTop + boxH - 3;
                try {
                    GlStateManager.color(1F, 1F, 1F, 1F);
                    GlStateManager.enableDepth();
                    GuiInventory.drawEntityOnScreen(cx, cy, scale, cx - mouseX, cy - boxH / 2F - mouseY, (EntityLivingBase) en);
                } catch (Throwable t) {
                    broken = true;
                    cached = null;
                }
                GlStateManager.disableDepth();
                GlStateManager.disableLighting();
                GlStateManager.color(1F, 1F, 1F, 1F);
            } else if (!e.icon.isEmpty()) {
                GlStateManager.pushMatrix();
                int x = ex0 + ENTITY_W / 2 - 16, y = boxTop + boxH / 2 - 16;
                GlStateManager.translate(x, y, 0);
                GlStateManager.scale(2F, 2F, 1F);
                RenderHelper.enableGUIStandardItemLighting();
                GlStateManager.enableDepth();
                mc.getRenderItem().renderItemAndEffectIntoGUI(e.icon, 0, 0);
                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableDepth();
                GlStateManager.popMatrix();
            }
            if (!materials.isEmpty() || !e.output.isEmpty())
                f.drawString(I18n.format("archaiclib.jei.materials"), 0, matY - 9, 0x606060);
            if (!e.extras.isEmpty())
                f.drawString(e.extrasLabelKey == null ? "" : I18n.format(e.extrasLabelKey), 0, extraY - 9, 0x606060);
            for (int i = 0; i < text.size(); i++) f.drawString(text.get(i), 0, textY + i * 10, 0x303030);
            GL11.glColor4f(1F, 1F, 1F, 1F);
        }

        @Override
        public List<String> getTooltipStrings(int mouseX, int mouseY) {
            ArchaicJei.Layer l = currentLayer();
            if (l == null) return Collections.emptyList();
            int gy = TITLE_H + 10;
            if (mouseX < 0 || mouseY < gy) return Collections.emptyList();
            int x = mouseX / CELL, y = (mouseY - gy) / CELL;
            ItemStack s = pick(l.at(x, y));
            if (s.isEmpty() || mouseX % CELL >= 16 || (mouseY - gy) % CELL >= 16) return Collections.emptyList();
            Minecraft mc = Minecraft.getMinecraft();
            return s.getTooltip(mc.player, ITooltipFlag.TooltipFlags.NORMAL);
        }
    }

    private final ArchaicJei.InfoCategory cat;
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slot;

    InfoRecipeCategory(ArchaicJei.InfoCategory cat, int height, IGuiHelper gui) {
        this.cat = cat;
        this.background = gui.createBlankDrawable(cat.width, height);
        this.icon = cat.catalyst.isEmpty() ? null : gui.createDrawableIngredient(cat.catalyst);
        this.slot = gui.getSlotDrawable();
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

    @Override
    public void setRecipe(IRecipeLayout layout, Wrapper w, IIngredients ing) {
        IGuiItemStackGroup g = layout.getItemStacks();
        int per = w.perRow(), idx = 0;
        List<List<ItemStack>> in = ing.getInputs(VanillaTypes.ITEM);
        int nMat = w.materials.size();
        for (int i = 0; i < nMat; i++, idx++) {
            g.init(idx, true, (i % per) * 18, w.matY + (i / per) * 18);
            g.setBackground(idx, slot);
            g.set(idx, in.get(i));
        }
        if (!w.e.output.isEmpty()) {
            int i = nMat;
            g.init(idx, false, (i % per) * 18, w.matY + (i / per) * 18);
            g.setBackground(idx, slot);
            g.set(idx, w.e.output);
            idx++;
        }
        for (int i = 0; i < w.e.extras.size(); i++, idx++) {
            g.init(idx, true, (i % per) * 18, w.extraY + (i / per) * 18);
            g.setBackground(idx, slot);
            g.set(idx, in.get(nMat + i));
        }
    }
}
