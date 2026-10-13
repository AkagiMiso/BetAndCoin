package com.example.betandcoin.network;

import com.example.betandcoin.BetAndCoin;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public final class ModNetwork {

    private static final String PROTOCOL_VERSION = "2";

    public static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    new ResourceLocation(
                            BetAndCoin.MOD_ID,
                            "main"
                    ),
                    () -> PROTOCOL_VERSION,
                    PROTOCOL_VERSION::equals,
                    PROTOCOL_VERSION::equals
            );

    private static int nextPacketId = 0;

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(
                nextPacketId++,
                BlackjackGesturePacket.class,
                BlackjackGesturePacket::encode,
                BlackjackGesturePacket::decode,
                BlackjackGesturePacket::handle
        );

        CHANNEL.registerMessage(
                nextPacketId++,
                BlackjackSessionPacket.class,
                BlackjackSessionPacket::encode,
                BlackjackSessionPacket::decode,
                BlackjackSessionPacket::handle
        );
    }
}
