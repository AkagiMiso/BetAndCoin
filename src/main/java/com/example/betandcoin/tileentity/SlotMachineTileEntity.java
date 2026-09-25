package com.example.betandcoin.tileentity;

import com.example.betandcoin.block.SlotMachineBlock;
import com.example.betandcoin.registry.ModSounds;
import com.example.betandcoin.registry.ModTileEntities;
import net.minecraft.block.BlockState;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;

import javax.annotation.Nullable;
import java.util.Random;

public class SlotMachineTileEntity
        extends TileEntity
        implements ITickableTileEntity {

    // 0 = 苹果
    // 1 = 金锭
    // 2 = 绿宝石
    // 3 = 钻石

    public static final int LEFT_STOP_TICK = 40;
    public static final int MIDDLE_STOP_TICK = 55;
    public static final int RIGHT_STOP_TICK = 70;

    // 拉杆动画：下压 5 tick，停留 2 tick，回弹 7 tick。
    public static final int LEVER_PULL_TICKS = 5;
    public static final int LEVER_HOLD_TICKS = 2;
    public static final int LEVER_RETURN_TICKS = 7;
    public static final int LEVER_ANIMATION_TICKS =
            LEVER_PULL_TICKS + LEVER_HOLD_TICKS + LEVER_RETURN_TICKS;

    private int leftReel = 0;
    private int middleReel = 0;
    private int rightReel = 0;

    private int startLeftReel = 0;
    private int startMiddleReel = 0;
    private int startRightReel = 0;

    private boolean signLit = false;
    private boolean spinning = false;
    private int spinTicks = 0;
    private int leverTicks = LEVER_ANIMATION_TICKS;

    /*
     * 本次投入的筹码。
     * 开始旋转时保存 1 个，开奖后无论输赢都会清空。
     */
    private ItemStack currentBet = ItemStack.EMPTY;

    public SlotMachineTileEntity() {
        super(ModTileEntities.SLOT_MACHINE.get());
    }

    public int getLeftReel() {
        return leftReel;
    }

    public int getMiddleReel() {
        return middleReel;
    }

    public int getRightReel() {
        return rightReel;
    }

    public int getStartLeftReel() {
        return startLeftReel;
    }

    public int getStartMiddleReel() {
        return startMiddleReel;
    }

    public int getStartRightReel() {
        return startRightReel;
    }

    public int getSpinTicks() {
        return spinTicks;
    }

    public int getLeverTicks() {
        return leverTicks;
    }

    public boolean isSignLit() {
        return signLit;
    }

    public boolean isSpinning() {
        return spinning;
    }

    public ItemStack getCurrentBet() {
        return currentBet.copy();
    }

    /**
     * 启动一次旋转，并记录本次投入的筹码种类。
     */
    public void startSpin(Random random, ItemStack betStack) {
        if (spinning || betStack.isEmpty()) {
            return;
        }

        currentBet = betStack.copy();
        currentBet.setCount(1);

        startLeftReel = leftReel;
        startMiddleReel = middleReel;
        startRightReel = rightReel;

        leftReel = random.nextInt(4);
        middleReel = random.nextInt(4);
        rightReel = random.nextInt(4);

        spinTicks = 0;
        leverTicks = 0;
        spinning = true;
        signLit = false;

        if (level != null && !level.isClientSide) {
            level.playSound(
                    null,
                    worldPosition,
                    ModSounds.LEVER_PULL.get(),
                    SoundCategory.BLOCKS,
                    1.0F,
                    1.0F
            );

            level.playSound(
                    null,
                    worldPosition,
                    ModSounds.REEL_SPIN.get(),
                    SoundCategory.BLOCKS,
                    1.0F,
                    1.0F
            );
        }

        markChangedAndSync();
    }

    @Override
    public void tick() {
        if (!spinning) {
            return;
        }

        if (spinTicks < RIGHT_STOP_TICK) {
            spinTicks++;
        }

        if (leverTicks < LEVER_ANIMATION_TICKS) {
            leverTicks++;
        }

        if (level != null
                && !level.isClientSide
                && spinTicks >= RIGHT_STOP_TICK) {

            spinning = false;
            signLit = leftReel == middleReel
                    && middleReel == rightReel;

            if (signLit) {
                level.playSound(
                        null,
                        worldPosition,
                        ModSounds.WIN.get(),
                        SoundCategory.BLOCKS,
                        1.0F,
                        1.0F
                );

                dispenseReward();
            }

            // 输赢结算后都清除本次下注。
            currentBet = ItemStack.EMPTY;

            markChangedAndSync();
        }
    }

    /**
     * 中奖时从老虎机正面吐出两个同类型筹码。
     */
    private void dispenseReward() {
        if (level == null || currentBet.isEmpty()) {
            return;
        }

        ItemStack reward = currentBet.copy();
        reward.setCount(2);

        BlockState state = getBlockState();
        Direction facing = state.getValue(SlotMachineBlock.FACING);

        double spawnX =
                worldPosition.getX() + 0.5D
                        + facing.getStepX() * 0.8D;
        double spawnY = worldPosition.getY() + 0.8D;
        double spawnZ =
                worldPosition.getZ() + 0.5D
                        + facing.getStepZ() * 0.8D;

        ItemEntity itemEntity = new ItemEntity(
                level,
                spawnX,
                spawnY,
                spawnZ,
                reward
        );

        itemEntity.setDeltaMovement(
                facing.getStepX() * 0.12D,
                0.16D,
                facing.getStepZ() * 0.12D
        );

        level.addFreshEntity(itemEntity);
    }

    public void setReelResults(int left, int middle, int right) {
        leftReel = clampResult(left);
        middleReel = clampResult(middle);
        rightReel = clampResult(right);
        markChangedAndSync();
    }

    public void setSignLit(boolean signLit) {
        this.signLit = signLit;
        markChangedAndSync();
    }

    public void setSpinning(boolean spinning) {
        this.spinning = spinning;
        markChangedAndSync();
    }

    private int clampResult(int result) {
        return Math.max(0, Math.min(3, result));
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

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);

        nbt.putInt("LeftReel", leftReel);
        nbt.putInt("MiddleReel", middleReel);
        nbt.putInt("RightReel", rightReel);

        nbt.putInt("StartLeftReel", startLeftReel);
        nbt.putInt("StartMiddleReel", startMiddleReel);
        nbt.putInt("StartRightReel", startRightReel);

        nbt.putInt("SpinTicks", spinTicks);
        nbt.putInt("LeverTicks", leverTicks);
        nbt.putBoolean("SignLit", signLit);
        nbt.putBoolean("Spinning", spinning);

        if (!currentBet.isEmpty()) {
            nbt.put(
                    "CurrentBet",
                    currentBet.save(new CompoundNBT())
            );
        }

        return nbt;
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);

        leftReel = clampResult(nbt.getInt("LeftReel"));
        middleReel = clampResult(nbt.getInt("MiddleReel"));
        rightReel = clampResult(nbt.getInt("RightReel"));

        startLeftReel = clampResult(nbt.getInt("StartLeftReel"));
        startMiddleReel = clampResult(nbt.getInt("StartMiddleReel"));
        startRightReel = clampResult(nbt.getInt("StartRightReel"));

        spinTicks = Math.max(
                0,
                Math.min(
                        RIGHT_STOP_TICK,
                        nbt.getInt("SpinTicks")
                )
        );

        leverTicks = nbt.contains("LeverTicks")
                ? Math.max(
                        0,
                        Math.min(
                                LEVER_ANIMATION_TICKS,
                                nbt.getInt("LeverTicks")
                        )
                )
                : LEVER_ANIMATION_TICKS;

        signLit = nbt.getBoolean("SignLit");
        spinning = nbt.getBoolean("Spinning");

        currentBet = nbt.contains("CurrentBet")
                ? ItemStack.of(nbt.getCompound("CurrentBet"))
                : ItemStack.EMPTY;
    }

    @Override
    public CompoundNBT getUpdateTag() {
        return save(new CompoundNBT());
    }

    @Override
    public void handleUpdateTag(BlockState state, CompoundNBT nbt) {
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

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(
                worldPosition,
                worldPosition.offset(1, 2, 1)
        );
    }
}
