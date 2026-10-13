package com.example.betandcoin.network;

import com.example.betandcoin.tileentity.BlackjackTableTileEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端识别到 Blackjack 手势后发送给服务器。
 *
 * 点头 = 要牌
 * 摇头 = 停牌
 */
public class BlackjackGesturePacket {

    private final BlockPos tablePos;
    private final int slotIndex;
    private final Gesture gesture;

    public BlackjackGesturePacket(
            BlockPos tablePos,
            int slotIndex,
            Gesture gesture
    ) {
        this.tablePos = tablePos;
        this.slotIndex = slotIndex;
        this.gesture = gesture;
    }

    public static void encode(
            BlackjackGesturePacket packet,
            PacketBuffer buffer
    ) {
        buffer.writeBlockPos(packet.tablePos);
        buffer.writeInt(packet.slotIndex);
        buffer.writeEnum(packet.gesture);
    }

    public static BlackjackGesturePacket decode(
            PacketBuffer buffer
    ) {
        return new BlackjackGesturePacket(
                buffer.readBlockPos(),
                buffer.readInt(),
                buffer.readEnum(Gesture.class)
        );
    }

    public static void handle(
            BlackjackGesturePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context =
                contextSupplier.get();

        context.enqueueWork(() -> {
            ServerPlayerEntity player =
                    context.getSender();

            if (player == null) {
                return;
            }

            if (!player.level.hasChunkAt(packet.tablePos)) {
                return;
            }

            TileEntity tileEntity =
                    player.level.getBlockEntity(
                            packet.tablePos
                    );

            if (!(tileEntity
                    instanceof BlackjackTableTileEntity)) {
                return;
            }

            BlackjackTableTileEntity table =
                    (BlackjackTableTileEntity) tileEntity;

            if (!table.isSlotOwnedBy(
                    packet.slotIndex,
                    player
            )) {
                return;
            }

            Vector3d slotAnchor =
                    table.getSlotAnchor(
                            packet.slotIndex
                    );

            if (slotAnchor == null
                    || player.distanceToSqr(
                            slotAnchor.x,
                            slotAnchor.y,
                            slotAnchor.z
                    ) > BlackjackTableTileEntity
                            .MAX_SLOT_DISTANCE_SQUARED) {
                return;
            }

            table.handlePlayerGesture(
                    packet.slotIndex,
                    player,
                    packet.gesture == Gesture.NOD
            );
        });

        context.setPacketHandled(true);
    }


    public enum Gesture {
        NOD,
        SHAKE
    }
}
