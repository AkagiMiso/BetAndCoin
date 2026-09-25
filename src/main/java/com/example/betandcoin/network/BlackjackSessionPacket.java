package com.example.betandcoin.network;

import com.example.betandcoin.client.BlackjackClientSession;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务器向客户端同步当前 Blackjack 会话。
 *
 * active = true：
 * 保存牌桌主坐标与栏位编号。
 *
 * active = false：
 * 清除当前客户端会话。
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

    public static BlackjackSessionPacket clear() {
        return new BlackjackSessionPacket(
                false,
                BlockPos.ZERO,
                -1
        );
    }

    public static void encode(
            BlackjackSessionPacket packet,
            PacketBuffer buffer
    ) {
        buffer.writeBoolean(packet.active);

        if (packet.active) {
            buffer.writeBlockPos(packet.tablePos);
            buffer.writeInt(packet.slotIndex);
        }
    }

    public static BlackjackSessionPacket decode(
            PacketBuffer buffer
    ) {
        boolean active = buffer.readBoolean();

        if (!active) {
            return clear();
        }

        return open(
                buffer.readBlockPos(),
                buffer.readInt()
        );
    }

    public static void handle(
            BlackjackSessionPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context =
                contextSupplier.get();

        context.enqueueWork(() -> {
            if (packet.active) {
                BlackjackClientSession.open(
                        packet.tablePos,
                        packet.slotIndex
                );
            } else {
                BlackjackClientSession.clear();
            }
        });

        context.setPacketHandled(true);
    }
}
