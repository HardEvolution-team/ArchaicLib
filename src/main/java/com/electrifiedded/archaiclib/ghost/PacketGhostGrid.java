package com.electrifiedded.archaiclib.ghost;

import com.electrifiedded.archaiclib.v1710.api.network.AbstractPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.List;

/** Client -> server: fill the ghost grid of the open container (JEI/HEI recipe transfer). */
public class PacketGhostGrid extends AbstractPacket {

    private int windowId;
    private final List<ItemStack> cells = new ArrayList<>();

    public PacketGhostGrid() {}

    public PacketGhostGrid(int windowId, List<ItemStack> cells) {
        this.windowId = windowId;
        this.cells.addAll(cells);
    }

    @Override
    public void writeTo(ByteBuf buf, Side side) {
        int n = Math.min(cells.size(), 9);
        buf.writeInt(windowId);
        buf.writeByte(n);
        for (int i = 0; i < n; i++) {
            ItemStack s = cells.get(i) == null ? ItemStack.EMPTY : cells.get(i).copy();
            if (!s.isEmpty()) s.setCount(1);
            ByteBufUtils.writeItemStack(buf, s);
        }
    }

    @Override
    public void readFrom(ByteBuf buf, Side side) {
        windowId = buf.readInt();
        int n = Math.min(buf.readUnsignedByte(), 9);
        for (int i = 0; i < n; i++) cells.add(ByteBufUtils.readItemStack(buf));
    }

    @Override
    public void execute(Side side, EntityPlayer player) {
        if (side != Side.SERVER || player.openContainer == null || player.openContainer.windowId != windowId) return;
        if (player.openContainer instanceof IGhostGridContainer) {
            ((IGhostGridContainer) player.openContainer).setGhostGrid(player, cells);
        }
    }
}
