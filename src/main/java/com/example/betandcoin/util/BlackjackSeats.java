package com.example.betandcoin.util;

import com.example.betandcoin.tileentity.BlackjackTableTileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.server.ServerWorld;
import java.util.UUID;

/** Read the live seat ledger rather than retaining a separate global registry. */
public final class BlackjackSeats {
    private BlackjackSeats() {}

    public static boolean hasSeat(MinecraftServer server, UUID playerId) {
        if (server == null) return false;
        for (ServerWorld world : server.getAllLevels()) {
            if (hasSeat(world.blockEntityList, playerId)) return true;
        }
        return false;
    }

    static boolean hasSeat(Iterable<TileEntity> tiles, UUID playerId) {
        for (TileEntity tile : tiles) {
            if (!(tile instanceof BlackjackTableTileEntity) || tile.isRemoved()) continue;
            BlackjackTableTileEntity table = (BlackjackTableTileEntity) tile;
            for (int slot = 0; slot < BlackjackTableTileEntity.SLOT_COUNT; slot++) {
                if (table.isSlotActive(slot) && playerId.equals(table.getSlotPlayer(slot))) return true;
            }
        }
        return false;
    }
}
