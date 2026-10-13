package com.example.betandcoin.network;

import com.example.betandcoin.client.BlackjackClientSession;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.*;
import org.junit.Test;
import java.lang.reflect.Constructor;
import static org.junit.Assert.*;

public class PacketDirectionTest {
    private NetworkEvent.Context context(NetworkDirection direction) throws Exception {
        Constructor<NetworkEvent.Context> ctor = NetworkEvent.Context.class.getDeclaredConstructor(
                NetworkManager.class, NetworkDirection.class, int.class);
        ctor.setAccessible(true);
        return ctor.newInstance(null, direction, 0);
    }

    @Test public void clientSessionPacketSentToServerDoesNotScheduleClientCode() throws Exception {
        BlackjackClientSession.open(new BlockPos(1, 2, 3), 1);
        try {
            NetworkEvent.Context context = context(NetworkDirection.PLAY_TO_SERVER);
            // A null network manager also proves no enqueueWork / client access is attempted.
            BlackjackSessionPacket.handle(BlackjackSessionPacket.clear(new BlockPos(1, 2, 3)), () -> context);
            assertTrue(context.getPacketHandled());
            assertTrue(BlackjackClientSession.isActive());
        } finally { BlackjackClientSession.clear(); }
    }

    @Test public void gesturePacketSentToClientDoesNotScheduleServerCode() throws Exception {
        NetworkEvent.Context context = context(NetworkDirection.PLAY_TO_CLIENT);
        BlackjackGesturePacket.handle(new BlackjackGesturePacket(BlockPos.ZERO, 0,
                BlackjackGesturePacket.Gesture.NOD), () -> context);
        assertTrue(context.getPacketHandled());
    }
}
