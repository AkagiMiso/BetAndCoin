package com.example.betandcoin.block;

import com.example.betandcoin.registry.ModItems;
import com.example.betandcoin.tileentity.SlotMachineTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class SlotMachineBlock extends HorizontalBlock {

    public static final EnumProperty<DoubleBlockHalf> HALF =
            BlockStateProperties.DOUBLE_BLOCK_HALF;

    public SlotMachineBlock(Properties properties) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(HALF, DoubleBlockHalf.LOWER)
        );
    }

    @Override
    public float getShadeBrightness(BlockState state, IBlockReader world, BlockPos pos) {
        // The full collision box must not make neighboring open cabinet models almost black.
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, IBlockReader world, BlockPos pos) {
        return true;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        BlockPos lowerPos = context.getClickedPos();
        BlockPos upperPos = lowerPos.above();

        // 上方没有空间时禁止放置。
        if (lowerPos.getY() >= 255
                || !context.getLevel()
                .getBlockState(upperPos)
                .canBeReplaced(context)) {
            return null;
        }

        return defaultBlockState()
                .setValue(
                        FACING,
                        context.getHorizontalDirection().getOpposite()
                )
                .setValue(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(
            World world,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        super.setPlacedBy(world, pos, state, placer, stack);

        if (!world.isClientSide) {
            BlockState upperState = state
                    .setValue(HALF, DoubleBlockHalf.UPPER);

            world.setBlock(pos.above(), upperState, 3);
        }
    }

    @Override
    public void onRemove(
            BlockState oldState,
            World world,
            BlockPos pos,
            BlockState newState,
            boolean isMoving
    ) {
        if (!oldState.is(newState.getBlock())) {
            DoubleBlockHalf half = oldState.getValue(HALF);

            // Both halves eventually remove the lower block; settle before its data is removed.
            if (!world.isClientSide && half == DoubleBlockHalf.LOWER) {
                TileEntity tile = world.getBlockEntity(pos);
                if (tile instanceof SlotMachineTileEntity) {
                    ((SlotMachineTileEntity) tile).settleOnRemoval(oldState);
                }
            }

            BlockPos otherPos = half == DoubleBlockHalf.LOWER
                    ? pos.above()
                    : pos.below();

            BlockState otherState = world.getBlockState(otherPos);

            if (otherState.is(this)
                    && otherState.getValue(HALF) != half) {
                world.setBlock(
                        otherPos,
                        Blocks.AIR.defaultBlockState(),
                        35
                );
            }
        }

        super.onRemove(oldState, world, pos, newState, isMoving);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(
            BlockState state,
            IBlockReader world
    ) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return new SlotMachineTileEntity();
        }

        return null;
    }

    @Override
    public ActionResultType use(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            Hand hand,
            BlockRayTraceResult hitResult
    ) {
        ItemStack heldStack = player.getItemInHand(hand);

        /*
         * 目前两种筹码都可用于老虎机。
         * 未持有有效筹码时，不触发机器。
         */
        boolean isValidChip =
                heldStack.getItem() == ModItems.CHIP_5.get()
                        || heldStack.getItem() == ModItems.CHIP_20.get();

        if (!isValidChip) {
            return ActionResultType.PASS;
        }

        /*
         * TileEntity 只存在于老虎机下半部。
         * 如果玩家点击的是上半部，就向下寻找真正的数据方块。
         */
        BlockPos lowerPos =
                state.getValue(HALF) == DoubleBlockHalf.UPPER
                        ? pos.below()
                        : pos;

        if (!world.isClientSide) {
            TileEntity tileEntity = world.getBlockEntity(lowerPos);

            if (tileEntity instanceof SlotMachineTileEntity) {
                SlotMachineTileEntity slotMachine =
                        (SlotMachineTileEntity) tileEntity;

                /*
                 * 旋转过程中不会再次启动，也不会重复扣除筹码。
                 */
                if (!slotMachine.isSpinning()) {
                    ItemStack betStack = heldStack.copy();
                    betStack.setCount(1);

                    slotMachine.startSpin(
                            world.random,
                            betStack
                    );

                    /*
                     * 创造模式不消耗物品；生存模式消耗一个筹码。
                     */
                    if (!player.abilities.instabuild) {
                        heldStack.shrink(1);
                    }
                }
            }
        }

        return ActionResultType.sidedSuccess(world.isClientSide);
    }

    @Override
    protected void createBlockStateDefinition(
            StateContainer.Builder<Block, BlockState> builder
    ) {
        builder.add(FACING, HALF);
    }
}
