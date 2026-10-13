package com.example.betandcoin.client;

import com.example.betandcoin.client.event.BlackjackGestureTestHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import org.junit.Test;
import static org.junit.Assert.*;

public class BlackjackDisconnectTest {
    @Test public void logoutClearsOldSessionEvenWithoutPlayerOrConnection() {
        BlackjackClientSession.open(new BlockPos(10, 64, 10), 1);
        BlackjackGestureTestHandler.onLogout(new ClientPlayerNetworkEvent.LoggedOutEvent(null, null, null));
        assertFalse(BlackjackClientSession.isActive());
        assertNull(BlackjackClientSession.getTablePos());
        assertEquals(-1, BlackjackClientSession.getSlotIndex());
    }

    @Test public void respawnOrDimensionChangeClearsOldSession() {
        BlackjackClientSession.open(new BlockPos(10, 64, 10), 0);
        BlackjackGestureTestHandler.onRespawn(new ClientPlayerNetworkEvent.RespawnEvent(null, null, null, null));
        assertFalse(BlackjackClientSession.isActive());
    }
}
