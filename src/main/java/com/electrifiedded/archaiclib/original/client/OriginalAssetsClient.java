package com.electrifiedded.archaiclib.original.client;

import com.electrifiedded.archaiclib.ExampleMod;
import com.electrifiedded.archaiclib.original.OriginalAssetsHooks;
import com.electrifiedded.archaiclib.original.OriginalDownloader;
import com.electrifiedded.archaiclib.original.OriginalModAssets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.List;

/**
 * Client half: installs resource packs for resolved originals; when some are still missing (download failed
 * or disabled) shows a banner on the main menu and one chat message after joining a world. Clicking the
 * banner opens {@link GuiMissingOriginals} with links, a folder button and a retry button.
 */
public final class OriginalAssetsClient {

    private static boolean chatWarned;
    private static int bannerX, bannerY, bannerW, bannerH;

    private OriginalAssetsClient() {}

    private static final java.util.Set<java.io.File> ADDED = new java.util.HashSet<>();

    /** Called at mod construction, before the first resource reload. */
    public static void install() {
        for (java.util.Map.Entry<java.io.File, List<String>> e : OriginalModAssets.discoverAll().entrySet()) {
            addPack(e.getKey(), e.getValue());
        }
        OriginalAssetsHooks.setListener(r -> addPack(r.getJar(), r.domains));
        MinecraftForge.EVENT_BUS.register(new OriginalAssetsClient());
    }

    @SuppressWarnings("unchecked")
    private static void addPack(java.io.File jar, List<String> domains) {
        if (jar == null || !ADDED.add(jar.getAbsoluteFile())) return;
        try {
            com.electrifiedded.archaiclib.client.resources.ArchaicPackInstaller.addDefaultPack(new OriginalJarResourcePack(jar, domains));
            ExampleMod.LOGGER.info("Original assets pack {} serves {}", jar.getName(), domains);
        } catch (Throwable t) {
            ExampleMod.LOGGER.error("Could not register original assets from {}", jar, t);
        }
    }

    static int missingCount() {
        return OriginalModAssets.missing().size();
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof GuiMainMenu)) return;
        int n = missingCount();
        if (n == 0) { bannerW = 0; return; }
        Minecraft mc = Minecraft.getMinecraft();
        String text = TextFormatting.YELLOW + "⚠ " + TextFormatting.WHITE + I18n.format("archaiclib.originals.banner", n)
                + "  " + TextFormatting.AQUA + TextFormatting.UNDERLINE + I18n.format("archaiclib.originals.banner_click");
        int w = mc.fontRenderer.getStringWidth(text) + 12;
        bannerW = w; bannerH = 14;
        bannerX = (event.getGui().width - w) / 2; bannerY = 2;
        Gui.drawRect(bannerX, bannerY, bannerX + w, bannerY + bannerH, 0xC0801010);
        Gui.drawRect(bannerX, bannerY + bannerH - 1, bannerX + w, bannerY + bannerH, 0xFFFF5555);
        mc.fontRenderer.drawStringWithShadow(text, bannerX + 6, bannerY + 3, 0xFFFFFF);
    }

    @SubscribeEvent
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.getGui() instanceof GuiMainMenu) || bannerW == 0 || !Mouse.getEventButtonState() || Mouse.getEventButton() != 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        int mx = Mouse.getEventX() * sr.getScaledWidth() / mc.displayWidth;
        int my = sr.getScaledHeight() - Mouse.getEventY() * sr.getScaledHeight() / mc.displayHeight - 1;
        if (mx >= bannerX && mx < bannerX + bannerW && my >= bannerY && my < bannerY + bannerH) {
            mc.displayGuiScreen(new GuiMissingOriginals(event.getGui()));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (chatWarned || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;
        chatWarned = true;
        int n = missingCount();
        if (n == 0) return;
        mc.player.sendMessage(new TextComponentTranslation("archaiclib.originals.chat", n)
                .setStyle(new Style().setColor(TextFormatting.GOLD)));
        for (OriginalModAssets.Requirement r : OriginalModAssets.missing()) {
            String url = GuiMissingOriginals.linkFor(r);
            TextComponentString line = new TextComponentString(" - " + r.displayName + " ");
            line.getStyle().setColor(TextFormatting.GRAY);
            if (url != null && url.startsWith("http")) {
                TextComponentString link = new TextComponentString("[" + I18n.format("archaiclib.originals.link") + "]");
                link.getStyle().setColor(TextFormatting.AQUA).setUnderlined(true)
                        .setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url.split(" ")[0]));
                line.appendSibling(link);
            }
            mc.player.sendMessage(line);
        }
        TextComponentString folder = new TextComponentString(I18n.format("archaiclib.originals.chat_folder", OriginalModAssets.FOLDER));
        folder.getStyle().setColor(TextFormatting.YELLOW).setUnderlined(true)
                .setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, OriginalModAssets.folder().getAbsolutePath()));
        mc.player.sendMessage(folder);
        if (!OriginalDownloader.failed().isEmpty()) {
            mc.player.sendMessage(new TextComponentTranslation("archaiclib.originals.chat_cfg").setStyle(new Style().setColor(TextFormatting.GRAY)));
        }
    }
}
