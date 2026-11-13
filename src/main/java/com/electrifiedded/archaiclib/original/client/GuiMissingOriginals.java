package com.electrifiedded.archaiclib.original.client;

import com.electrifiedded.archaiclib.original.OriginalModAssets;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.Sys;

import java.util.List;

/** Shown once on the main menu when a port's original jar is missing from archaic_originals/. */
public class GuiMissingOriginals extends GuiScreen {

    private final GuiScreen parent;
    private List<OriginalModAssets.Requirement> missing;

    public GuiMissingOriginals(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        missing = OriginalModAssets.missing();
        buttonList.clear();
        int y = Math.min(height - 28, 70 + missing.size() * 24 + 10);
        buttonList.add(new GuiButton(0, width / 2 - 154, y, 150, 20, I18n.format("archaiclib.originals.open_folder")));
        buttonList.add(new GuiButton(1, width / 2 + 4, y, 150, 20, I18n.format("archaiclib.originals.continue")));
        for (int i = 0; i < missing.size(); i++) {
            buttonList.add(new GuiButton(10 + i, width / 2 + 104, 62 + i * 24, 50, 20, I18n.format("archaiclib.originals.link")));
        }
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        if (b.id == 0) {
            Sys.openURL(OriginalModAssets.folder().toURI().toString());
        } else if (b.id == 1) {
            mc.displayGuiScreen(parent);
        } else if (b.id >= 10 && b.id - 10 < missing.size()) {
            Sys.openURL(missing.get(b.id - 10).downloadUrl);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, TextFormatting.YELLOW + I18n.format("archaiclib.originals.title"), width / 2, 16, 0xFFFFFF);
        drawCenteredString(fontRenderer, I18n.format("archaiclib.originals.line1"), width / 2, 32, 0xDDDDDD);
        drawCenteredString(fontRenderer, I18n.format("archaiclib.originals.line2", OriginalModAssets.FOLDER), width / 2, 44, 0xDDDDDD);
        for (int i = 0; i < missing.size(); i++) {
            OriginalModAssets.Requirement r = missing.get(i);
            drawString(fontRenderer, r.displayName, width / 2 - 154, 64 + i * 24, 0xFFFFFF);
            drawString(fontRenderer, I18n.format("archaiclib.originals.needed_by", r.portModId), width / 2 - 154, 74 + i * 24, 0xAAAAAA);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
