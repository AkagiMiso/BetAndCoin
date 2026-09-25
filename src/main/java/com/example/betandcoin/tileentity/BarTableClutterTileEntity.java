package com.example.betandcoin.tileentity;

import com.example.betandcoin.block.BarTableClutterBlock;
import com.example.betandcoin.registry.ModTileEntities;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;
import java.util.Random;

public class BarTableClutterTileEntity
        extends TileEntity
        implements ITickableTileEntity {

    public static final int VARIANT_COUNT = 8;

    /*
     * 与 BAR 霓虹灯相同的“上电稳定”闪烁节奏：
     * 3 tick 熄灭，5 tick 点亮，
     * 7 tick 再次熄灭，16 tick 恢复，
     * 30 tick 后正式结束。
     */
    private static final int QUICK_OFF_TICK = 3;
    private static final int QUICK_ON_TICK = 5;
    private static final int LONG_OFF_TICK = 7;
    private static final int RECOVER_TICK = 16;
    private static final int EFFECT_END_TICK = 30;

    private int variant = 0;

    /*
     * -1 表示当前没有运行放置闪烁。
     */
    private int effectTick = -1;

    public BarTableClutterTileEntity() {
        super(ModTileEntities.BAR_TABLE_CLUTTER.get());
    }

    public void initializePlaced(Random random) {
        if (level == null || level.isClientSide) {
            return;
        }

        variant = random.nextInt(VARIANT_COUNT);
        effectTick = 0;

        setLit(true);
        markChangedAndSync();
    }

    @Override
    public void tick() {
        if (level == null
                || level.isClientSide
                || effectTick < 0) {
            return;
        }

        effectTick++;

        switch (effectTick) {
            case QUICK_OFF_TICK:
                setLit(false);
                break;

            case QUICK_ON_TICK:
                setLit(true);
                break;

            case LONG_OFF_TICK:
                setLit(false);
                break;

            case RECOVER_TICK:
                setLit(true);
                break;

            default:
                break;
        }

        if (effectTick >= EFFECT_END_TICK) {
            effectTick = -1;
            setLit(true);
        }

        setChanged();
    }

    public int getVariant() {
        return clampVariant(variant);
    }

    public boolean isLightOn() {
        BlockState state = getBlockState();

        return state.hasProperty(BarTableClutterBlock.LIT)
                && state.getValue(BarTableClutterBlock.LIT);
    }

    private void setLit(boolean lit) {
        if (level == null) {
            return;
        }

        BlockState state = getBlockState();

        if (!state.hasProperty(BarTableClutterBlock.LIT)
                || state.getValue(BarTableClutterBlock.LIT) == lit) {
            return;
        }

        level.setBlock(
                worldPosition,
                state.setValue(BarTableClutterBlock.LIT, lit),
                3
        );

        level.getLightEngine().checkBlock(worldPosition);
    }

    private void markChangedAndSync() {
        setChanged();

        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();

            level.sendBlockUpdated(
                    worldPosition,
                    state,
                    state,
                    3
            );
        }
    }

    private static int clampVariant(int value) {
        return Math.max(
                0,
                Math.min(VARIANT_COUNT - 1, value)
        );
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);

        nbt.putInt("Variant", getVariant());
        nbt.putInt("EffectTick", effectTick);

        return nbt;
    }

    @Override
    public void load(
            BlockState state,
            CompoundNBT nbt
    ) {
        super.load(state, nbt);

        variant = clampVariant(
                nbt.contains("Variant")
                        ? nbt.getInt("Variant")
                        : 0
        );

        effectTick =
                nbt.contains("EffectTick")
                        ? nbt.getInt("EffectTick")
                        : -1;
    }

    @Override
    public CompoundNBT getUpdateTag() {
        return save(new CompoundNBT());
    }

    @Override
    public void handleUpdateTag(
            BlockState state,
            CompoundNBT nbt
    ) {
        load(state, nbt);
    }

    @Nullable
    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(
                worldPosition,
                0,
                getUpdateTag()
        );
    }

    @Override
    public void onDataPacket(
            NetworkManager networkManager,
            SUpdateTileEntityPacket packet
    ) {
        CompoundNBT nbt = packet.getTag();

        if (nbt != null) {
            handleUpdateTag(getBlockState(), nbt);
        }
    }
}
