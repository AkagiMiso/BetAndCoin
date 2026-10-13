package com.example.betandcoin.event;

import com.example.betandcoin.BetAndCoin;
import com.example.betandcoin.tileentity.BlackjackTableTileEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.UUID;

/** Release seats before Forge saves the departing player's inventory. */
@Mod.EventBusSubscriber(modid = BetAndCoin.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BlackjackDepartureHandler {
    private BlackjackDepartureHandler() {}

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayerEntity)) return;
        MinecraftServer server = event.getPlayer().getServer();
        if (server == null) return;
        for (ServerWorld world : server.getAllLevels()) {
            releaseSeats(new ArrayList<>(world.blockEntityList), event.getPlayer().getUUID());
        }
    }

    static void releaseSeats(Iterable<TileEntity> tiles, UUID playerId) {
        for (TileEntity tile : tiles) {
            if (!(tile instanceof BlackjackTableTileEntity) || tile.isRemoved()) continue;
            BlackjackTableTileEntity table = (BlackjackTableTileEntity) tile;
            for (int slot = 0; slot < BlackjackTableTileEntity.SLOT_COUNT; slot++) {
                if (playerId.equals(table.getSlotPlayer(slot))) table.releaseSlot(slot);
            }
        }
    }
}
