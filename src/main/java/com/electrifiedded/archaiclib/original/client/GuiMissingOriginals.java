package com.electrifiedded.archaiclib.original.client;

import com.electrifiedded.archaiclib.original.OriginalDownloader;
import com.electrifiedded.archaiclib.original.OriginalModAssets;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.Sys;

import java.util.List;

/** Opened from the main-menu banner when a port's original jar is still missing from archaic_originals/. */
public class GuiMissingOriginals extends GuiScreen {

    private final GuiScreen parent;
    private List<OriginalModAssets.Requirement> missing;
    private volatile String status = "";
    private volatile boolean busy;

    public GuiMissingOriginals(GuiScreen parent) {
        this.parent = parent;
    }

    /** manifest page (config/archaiclib/originals/<port>.json) first, then the URL compiled into the port */
    static String linkFor(OriginalModAssets.Requirement r) {
        String p = OriginalDownloader.pageFor(r.portModId);
        return p != null ? p : r.downloadUrl;
    }

    @Override
    public void initGui() {
        missing = OriginalModAssets.missing();
        buttonList.clear();
        int y = Math.min(height - 28, 84 + missing.size() * 24 + 10);
        buttonList.add(new GuiButton(2, width / 2 - 154, y, 100, 20, I18n.format("archaiclib.originals.retry")));
        buttonList.add(new GuiButton(0, width / 2 - 50, y, 100, 20, I18n.format("archaiclib.originals.open_folder")));
        buttonList.add(new GuiButton(1, width / 2 + 54, y, 100, 20, I18n.format("archaiclib.originals.continue")));
        for (int i = 0; i < missing.size(); i++) {
            buttonList.add(new GuiButton(10 + i, width / 2 + 104, 76 + i * 24, 50, 20, I18n.format("archaiclib.originals.link")));
        }
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        if (b.id == 0) {
            Sys.openURL(OriginalModAssets.folder().toURI().toString());
        } else if (b.id == 1) {
            mc.displayGuiScreen(parent);
        } else if (b.id == 2) {
            retry(b);
        } else if (b.id >= 10 && b.id - 10 < missing.size()) {
            String url = linkFor(missing.get(b.id - 10));
            if (url != null) Sys.openURL(url.split(" ")[0]);
        }
    }

    private void retry(GuiButton b) {
        if (busy) return;
        List<OriginalDownloader.Entry> failed = OriginalDownloader.failed();
        if (failed.isEmpty()) { status = TextFormatting.GRAY + I18n.format("archaiclib.originals.retry_none"); return; }
        busy = true;
        b.enabled = false;
        Thread t = new Thread(() -> {
            int ok = 0;
            for (OriginalDownloader.Entry e : failed) {
                status = TextFormatting.YELLOW + I18n.format("archaiclib.originals.retry_progress", e.file);
                if (OriginalDownloader.downloadOne(e)) ok++;
            }
            status = ok == failed.size()
                    ? TextFormatting.GREEN + I18n.format("archaiclib.originals.retry_ok")
                    : TextFormatting.RED + I18n.format("archaiclib.originals.retry_fail", failed.size() - ok);
            busy = false;
            b.enabled = true;
        }, "ArchaicLib original download");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, TextFormatting.YELLOW + I18n.format("archaiclib.originals.title"), width / 2, 16, 0xFFFFFF);
        drawCenteredString(fontRenderer, I18n.format("archaiclib.originals.line1"), width / 2, 32, 0xDDDDDD);
        drawCenteredString(fontRenderer, I18n.format("archaiclib.originals.line2", OriginalModAssets.FOLDER), width / 2, 44, 0xDDDDDD);
        drawCenteredString(fontRenderer, I18n.format("archaiclib.originals.line3"), width / 2, 56, 0xAAAAAA);
        for (int i = 0; i < missing.size(); i++) {
            OriginalModAssets.Requirement r = missing.get(i);
            drawString(fontRenderer, r.displayName, width / 2 - 154, 78 + i * 24, 0xFFFFFF);
            drawString(fontRenderer, I18n.format("archaiclib.originals.needed_by", r.portModId), width / 2 - 154, 88 + i * 24, 0xAAAAAA);
        }
        if (!status.isEmpty()) drawCenteredString(fontRenderer, status, width / 2, height - 42, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
