package com.example.betandcoin.client;

import com.example.betandcoin.network.BlackjackSessionPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;
import java.lang.reflect.Field;
import static org.junit.Assert.*;

public class BlackjackSessionOwnershipTest {
    @Test public void oldTablesClearDoesNotEraseNewSession() {
        BlockPos oldTable = new BlockPos(0, 64, 0);
        BlockPos newTable = new BlockPos(3, 64, 0);
        BlackjackClientSession.open(newTable, 2);
        try {
            BlackjackClientSession.clear(oldTable);
            assertTrue(BlackjackClientSession.isActive());
            assertEquals(newTable, BlackjackClientSession.getTablePos());
            assertEquals(2, BlackjackClientSession.getSlotIndex());
        } finally { BlackjackClientSession.clear(); }
    }

    @Test public void ownTablesClearAndRepeatedClearCloseSession() {
        BlockPos table = new BlockPos(0, 64, 0);
        BlackjackClientSession.open(table, 1);
        BlackjackClientSession.clear(table);
        BlackjackClientSession.clear(table);
        assertFalse(BlackjackClientSession.isActive());
        assertEquals(-1, BlackjackClientSession.getSlotIndex());
    }

    @Test public void openAndClearPacketsRetainTargetThroughNetworkEncoding() throws Exception {
        BlockPos table = new BlockPos(-20, 70, 30);
        for (boolean active : new boolean[] {true, false}) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            try {
                BlackjackSessionPacket.encode(active ? BlackjackSessionPacket.open(table, 2)
                        : BlackjackSessionPacket.clear(table), buffer);
                BlackjackSessionPacket decoded = BlackjackSessionPacket.decode(buffer);
                assertEquals(table, field(decoded, "tablePos"));
                assertEquals(active, field(decoded, "active"));
                assertEquals(active ? 2 : -1, field(decoded, "slotIndex"));
                assertEquals(0, buffer.readableBytes());
            } finally { buffer.release(); }
        }
    }

    private Object field(Object packet, String name) throws Exception {
        Field field = BlackjackSessionPacket.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(packet);
    }
}
