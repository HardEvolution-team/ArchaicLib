package com.electrifiedded.archaiclib.v1710.api.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;

public abstract class AbstractPacket {

    public abstract void writeTo(ByteBuf buffer, Side side);

    public abstract void readFrom(ByteBuf buffer, Side side);

    public abstract void execute(Side side, EntityPlayer player);
}
