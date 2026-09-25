package com.example.betandcoin.tileentity;

import com.example.betandcoin.block.BarNeonSignBlock;
import com.example.betandcoin.registry.ModSounds;
import com.example.betandcoin.registry.ModTileEntities;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;

public class BarNeonSignTileEntity
        extends TileEntity
        implements ITickableTileEntity {

    public static final int QUICK_OFF_TICK = 3;
    public static final int QUICK_ON_TICK = 5;
    public static final int LONG_OFF_TICK = 7;
    public static final int RECOVER_TICK = 16;
    public static final int EFFECT_END_TICK = 30;

    /*
     * 正式频率：每次效果结束后，随机等待 24000 至 48000 tick。
     * 约等于 20 至 40 分钟现实时间，平均约 30 分钟一次。
     */
    private static final int MIN_RANDOM_DELAY = 24000;
    private static final int MAX_RANDOM_DELAY = 48000;

    /*
     * -1 表示当前没有运行闪烁序列。
     */
    private int effectTick = -1;
    private int nextRandomEffectDelay = -1;

    public BarNeonSignTileEntity() {
        super(ModTileEntities.BAR_NEON_SIGN.get());
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }

        if (effectTick >= 0) {
            tickPowerStabilizeEffect();
            return;
        }

        if (nextRandomEffectDelay < 0) {
            scheduleNextRandomEffect();
        } else if (nextRandomEffectDelay > 0) {
            nextRandomEffectDelay--;
        } else {
            startPowerStabilizeEffect();
        }
    }

    public void startPowerStabilizeEffect() {
        if (level == null
                || level.isClientSide
                || effectTick >= 0) {
            return;
        }

        effectTick = 0;
        nextRandomEffectDelay = -1;

        setLit(true);

        level.playSound(
                null,
                worldPosition,
                ModSounds.BAR_NEON_POWER_STABILIZE.get(),
                SoundCategory.BLOCKS,
                0.75F,
                1.0F
        );

        setChanged();
    }

    private void tickPowerStabilizeEffect() {
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
            scheduleNextRandomEffect();
        }

        setChanged();
    }

    private void setLit(boolean lit) {
        if (level == null) {
            return;
        }

        BlockState state = getBlockState();

        if (!state.hasProperty(BarNeonSignBlock.LIT)
                || state.getValue(BarNeonSignBlock.LIT) == lit) {
            return;
        }

        level.setBlock(
                worldPosition,
                state.setValue(BarNeonSignBlock.LIT, lit),
                3
        );

        /*
         * 让实际方块光照立即重新计算。
         */
        level.getLightEngine().checkBlock(worldPosition);
    }

    private void scheduleNextRandomEffect() {
        if (level == null) {
            nextRandomEffectDelay = MIN_RANDOM_DELAY;
            return;
        }

        int range =
                MAX_RANDOM_DELAY - MIN_RANDOM_DELAY + 1;

        nextRandomEffectDelay =
                MIN_RANDOM_DELAY + level.random.nextInt(range);

        setChanged();
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);

        nbt.putInt("EffectTick", effectTick);
        nbt.putInt(
                "NextRandomEffectDelay",
                nextRandomEffectDelay
        );

        return nbt;
    }

    @Override
    public void load(
            BlockState state,
            CompoundNBT nbt
    ) {
        super.load(state, nbt);

        effectTick = nbt.contains("EffectTick")
                ? nbt.getInt("EffectTick")
                : -1;

        nextRandomEffectDelay =
                nbt.contains("NextRandomEffectDelay")
                        ? nbt.getInt("NextRandomEffectDelay")
                        : -1;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(
                worldPosition.offset(-2, -1, -2),
                worldPosition.offset(3, 2, 3)
        );
    }
}
