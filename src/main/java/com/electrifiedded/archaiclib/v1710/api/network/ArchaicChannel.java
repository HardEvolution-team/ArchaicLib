package com.electrifiedded.archaiclib.v1710.api.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public final class ArchaicChannel {

    private static final Logger LOGGER = LogManager.getLogger("ArchaicLib/Network");

    private final String name;
    private final FMLEventChannel channel;
    private final List<Class<? extends AbstractPacket>> packets;

    @SafeVarargs
    public ArchaicChannel(String name, Class<? extends AbstractPacket>... packetClasses) {
        if (packetClasses.length > 256) throw new IllegalArgumentException("Too many packets for channel " + name);
        this.name = name;
        this.packets = List.of(packetClasses);
        this.channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(name);
        this.channel.register(this);
    }

    public String getName() {
        return name;
    }


    public void sendToServer(AbstractPacket packet) {
        channel.sendToServer(build(packet, Side.SERVER));
    }

    public void sendToAll(AbstractPacket packet) {
        channel.sendToAll(build(packet, Side.CLIENT));
    }

    public void sendToPlayer(AbstractPacket packet, EntityPlayerMP player) {
        channel.sendTo(build(packet, Side.CLIENT), player);
    }

    public void sendToDimension(AbstractPacket packet, int dimension) {
        channel.sendToDimension(build(packet, Side.CLIENT), dimension);
    }

    public void sendToAllAround(AbstractPacket packet, NetworkRegistry.TargetPoint point) {
        channel.sendToAllAround(build(packet, Side.CLIENT), point);
    }

    private FMLProxyPacket build(AbstractPacket packet, Side target) {
        int id = packets.indexOf(packet.getClass());
        if (id < 0) throw new IllegalArgumentException(packet.getClass().getName() + " is not registered in channel " + name);
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(id);
        packet.writeTo(buf, target);
        return new FMLProxyPacket(new PacketBuffer(buf), name);
    }


    private AbstractPacket decode(ByteBuf buf, Side side) {
        int id = buf.readUnsignedByte();
        if (id >= packets.size()) {
            LOGGER.warn("Unknown packet id {} on channel {}", id, name);
            return null;
        }
        try {
            AbstractPacket packet = packets.get(id).getDeclaredConstructor().newInstance();
            packet.readFrom(buf, side);
            return packet;
        } catch (ReflectiveOperationException e) {
            LOGGER.error("Could not create packet {} on channel {}", packets.get(id).getName(), name, e);
            return null;
        }
    }

    @SubscribeEvent
    public void onServerPacket(FMLNetworkEvent.ServerCustomPacketEvent event) {
        AbstractPacket packet = decode(event.getPacket().payload(), Side.SERVER);
        if (packet == null) return;
        EntityPlayerMP player = ((NetHandlerPlayServer) event.getHandler()).player;
        player.getServerWorld().addScheduledTask(() -> packet.execute(Side.SERVER, player));
    }

    @SubscribeEvent
    public void onClientPacket(FMLNetworkEvent.ClientCustomPacketEvent event) {
        AbstractPacket packet = decode(event.getPacket().payload(), Side.CLIENT);
        if (packet != null) ClientHooks.schedule(packet);
    }

    @SideOnly(Side.CLIENT)
    private static final class ClientHooks {
        static void schedule(AbstractPacket packet) {
            Minecraft mc = Minecraft.getMinecraft();
            mc.addScheduledTask(() -> packet.execute(Side.CLIENT, mc.player));
        }
    }
}
