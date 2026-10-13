package com.example.betandcoin.util;

import com.example.betandcoin.block.BlackjackTableBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import javax.annotation.Nullable;

/** Clean up orphaned displays without loading chunks or consulting pending tile entities. */
public final class BlackjackDisplayCleanup {
    private BlackjackDisplayCleanup() {}

    public static void removeIfSourceMissing(Entity display, @Nullable BlockPos sourceTable) {
        if (sourceTable == null || display.removed || display.level == null
                || display.level.isClientSide || display.tickCount % 20 != 0) return;
        if (!display.level.hasChunkAt(sourceTable)) return;
        // Block data is authoritative even while the table's tile entity is still being loaded.
        if (!(display.level.getBlockState(sourceTable).getBlock() instanceof BlackjackTableBlock)) {
            display.remove();
        }
    }
}
