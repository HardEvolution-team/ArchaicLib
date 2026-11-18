package com.electrifiedded.archaiclib.legacy.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.FMLCommonHandler;

/**
 * 1.7 {@code MinecraftServer.getServer().getConfigurationManager().sendChatMsg(new ChatComponentText(s))}.
 * Safe from any thread/side: does nothing when no server is running (e.g. when 1.7 code ran it on the client).
 */
public final class LegacyChat {
    private LegacyChat() {}

    public static MinecraftServer server() {
        return FMLCommonHandler.instance().getMinecraftServerInstance();
    }

    public static void broadcast(String text) {
        broadcast(new TextComponentString(text));
    }

    public static void broadcast(ITextComponent msg) {
        MinecraftServer s = server();
        if (s != null && s.getPlayerList() != null) s.getPlayerList().sendMessage(msg);
    }

    /** 1.7 {@code player.addChatMessage(new ChatComponentText(s))}. */
    public static void tell(EntityPlayer player, String text) {
        if (player != null) player.sendMessage(new TextComponentString(text));
    }
}
