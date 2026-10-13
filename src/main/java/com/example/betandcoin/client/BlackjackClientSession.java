package com.example.betandcoin.client;

import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

/**
 * 保存本地玩家当前绑定的 Blackjack 牌桌会话。
 *
 * 这里只保存客户端需要发送手势请求时使用的目标信息：
 * - 牌桌主方块坐标
 * - 玩家栏位编号
 */
public final class BlackjackClientSession {

    @Nullable
    private static BlockPos tablePos;

    private static int slotIndex = -1;

    private BlackjackClientSession() {
    }

    public static void open(
            BlockPos newTablePos,
            int newSlotIndex
    ) {
        tablePos = newTablePos.immutable();
        slotIndex = newSlotIndex;
    }

    public static void clear() {
        tablePos = null;
        slotIndex = -1;
    }

    public static void clear(BlockPos sourceTable) {
        if (sourceTable.equals(tablePos)) clear();
    }

    public static boolean isActive() {
        return tablePos != null
                && slotIndex >= 0;
    }

    @Nullable
    public static BlockPos getTablePos() {
        return tablePos;
    }

    public static int getSlotIndex() {
        return slotIndex;
    }
}
