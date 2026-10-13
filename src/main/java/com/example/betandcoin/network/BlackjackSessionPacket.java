package com.example.betandcoin.network;

import com.example.betandcoin.client.BlackjackClientSession;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkDirection;

import java.util.function.Supplier;

/**
 * 服务器向客户端同步当前 Blackjack 会话。
 *
 * active = true：
 * 保存牌桌主坐标与栏位编号。
 *
 * active = false：
 * 仅清除与发送方牌桌匹配的客户端会话。
 */
public class BlackjackSessionPacket {

    private final boolean active;
    private final BlockPos tablePos;
    private final int slotIndex;

    private BlackjackSessionPacket(
            boolean active,
            BlockPos tablePos,
            int slotIndex
    ) {
        this.active = active;
        this.tablePos = tablePos;
        this.slotIndex = slotIndex;
    }

    public static BlackjackSessionPacket open(
            BlockPos tablePos,
            int slotIndex
    ) {
        return new BlackjackSessionPacket(
                true,
                tablePos,
                slotIndex
        );
    }

    public static BlackjackSessionPacket clear(BlockPos tablePos) {
        return new BlackjackSessionPacket(
                false,
                tablePos,
                -1
        );
    }

    public static void encode(
            BlackjackSessionPacket packet,
            PacketBuffer buffer
    ) {
        buffer.writeBoolean(packet.active);
        buffer.writeBlockPos(packet.tablePos);

        if (packet.active) {
            buffer.writeInt(packet.slotIndex);
        }
    }

    public static BlackjackSessionPacket decode(
            PacketBuffer buffer
    ) {
        boolean active = buffer.readBoolean();
        BlockPos tablePos = buffer.readBlockPos();

        if (!active) {
            return clear(tablePos);
        }

        return open(
                tablePos,
                buffer.readInt()
        );
    }

    public static void handle(
            BlackjackSessionPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context =
                contextSupplier.get();

        if (context.getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> {
            if (packet.active) {
                BlackjackClientSession.open(
                        packet.tablePos,
                        packet.slotIndex
                );
            } else {
                BlackjackClientSession.clear(packet.tablePos);
            }
        });

        context.setPacketHandled(true);
    }
}
