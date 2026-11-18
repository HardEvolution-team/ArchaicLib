package com.electrifiedded.archaiclib.storage;

import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Lets an NPC "open" a storage for a moment, the way a player does: the lid of a chest / shulker box /
 * Iron Chest opens with its sound and closes again after a few ticks, trapped chests pulse redstone.
 * Uses the inventory's own {@code openInventory}/{@code closeInventory} with a fake player, so any mod
 * storage that animates for players animates for NPCs too. Opening an already open storage only extends it.
 */
@Mod.EventBusSubscriber(modid = "archaiclib")
public final class StorageAnimator {

    private StorageAnimator() {}

    private static final class Open {
        final World world;
        final BlockPos pos;
        final TileEntity te;
        final FakePlayer player;
        int ticks;
        Open(World world, BlockPos pos, TileEntity te, FakePlayer player, int ticks) {
            this.world = world; this.pos = pos; this.te = te; this.player = player; this.ticks = ticks;
        }
    }

    private static final List<Open> OPEN = new ArrayList<>();

    /** Opens the storage at {@code pos} for {@code ticks} ticks (server side only; no-op for non-IInventory tiles). */
    public static void open(World world, BlockPos pos, int ticks) {
        if (world.isRemote || !(world instanceof WorldServer) || !world.isBlockLoaded(pos)) return;
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof IInventory)) return;
        for (Open o : OPEN) {
            if (o.te == te) { o.ticks = Math.max(o.ticks, ticks); return; }
        }
        FakePlayer fp = FakePlayerFactory.getMinecraft((WorldServer) world);
        try {
            ((IInventory) te).openInventory(fp);
        } catch (RuntimeException e) {
            return;
        }
        OPEN.add(new Open(world, pos.toImmutable(), te, fp, ticks));
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || OPEN.isEmpty()) return;
        for (Iterator<Open> it = OPEN.iterator(); it.hasNext(); ) {
            Open o = it.next();
            if (--o.ticks > 0) continue;
            it.remove();
            close(o);
        }
    }

    @SubscribeEvent
    public static void onUnload(WorldEvent.Unload e) {
        for (Iterator<Open> it = OPEN.iterator(); it.hasNext(); ) {
            Open o = it.next();
            if (o.world != e.getWorld()) continue;
            it.remove();
            close(o);
        }
    }

    private static void close(Open o) {
        if (o.te.isInvalid() || !o.world.isBlockLoaded(o.pos) || o.world.getTileEntity(o.pos) != o.te) return;
        try {
            ((IInventory) o.te).closeInventory(o.player);
        } catch (RuntimeException ignored) {
            // nothing to undo
        }
    }
}
