package com.example.betandcoin.network;

import com.example.betandcoin.tileentity.BlackjackTableTileEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nonnull;
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
                reject(
                        player,
                        "目标牌桌区域未加载"
                );
                return;
            }

            TileEntity tileEntity =
                    player.level.getBlockEntity(
                            packet.tablePos
                    );

            if (!(tileEntity
                    instanceof BlackjackTableTileEntity)) {
                reject(
                        player,
                        "目标不是有效的 Blackjack 牌桌"
                );
                return;
            }

            BlackjackTableTileEntity table =
                    (BlackjackTableTileEntity) tileEntity;

            if (!table.isSlotOwnedBy(
                    packet.slotIndex,
                    player
            )) {
                reject(
                        player,
                        "该栏位不属于你"
                );
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
                reject(
                        player,
                        "你已离开有效栏位范围"
                );
                return;
            }

            BlackjackTableTileEntity.GestureResult result =
                    table.handlePlayerGesture(
                            packet.slotIndex,
                            player,
                            packet.gesture
                                    == Gesture.NOD
                    );

            switch (result) {
                case SUCCESS:
                    /*
                     * 成功信息由 TileEntity 根据实际动作显示。
                     */
                    break;

                case WRONG_PHASE:
                    reject(
                            player,
                            "当前牌局阶段不能执行该动作"
                    );
                    break;

                case NOT_OWNER:
                    reject(
                            player,
                            "该栏位不属于你"
                    );
                    break;

                case NOT_YOUR_TURN:
                    reject(
                            player,
                            "现在还没有轮到你"
                    );
                    break;

                case ALREADY_FINISHED:
                    reject(
                            player,
                            "你的本回合已经结束"
                    );
                    break;

                default:
                    reject(
                            player,
                            "未知的手势处理结果"
                    );
                    break;
            }
        });

        context.setPacketHandled(true);
    }

    private static void reject(
            @Nonnull ServerPlayerEntity player,
            @Nonnull String reason
    ) {
        player.displayClientMessage(
                new StringTextComponent(
                        "手势请求无效："
                                + reason
                ),
                true
        );
    }

    public enum Gesture {
        NOD,
        SHAKE
    }
}
