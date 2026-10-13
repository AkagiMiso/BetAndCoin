package com.example.betandcoin.block;

import net.minecraft.block.material.PushReaction;

import com.example.betandcoin.block.BlackjackTablePartBlock.TablePart;
import com.example.betandcoin.registry.ModBlocks;
import com.example.betandcoin.registry.ModTileEntities;
import com.example.betandcoin.tileentity.BlackjackTableTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlackjackTableBlock extends HorizontalBlock {

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    public static final DirectionProperty FACING =
            HorizontalBlock.FACING;

    public BlackjackTableBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
        );
    }

    /**
     * 放置牌桌时，使正面朝向玩家。
     *
     * 同时检查完整的 3×2 区域是否都有足够空间。
     * 如果任意一个占位位置被其他方块占用，则取消放置。
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(
            @Nonnull BlockItemUseContext context
    ) {
        Direction facing =
                context.getHorizontalDirection().getOpposite();

        BlockPos mainPos = context.getClickedPos();
        World world = context.getLevel();

        if (!world.getWorldBorder().isWithinBounds(mainPos)) return null;

        for (TablePart part : TablePart.values()) {
            BlockPos partPos =
                    getPartBlockPos(mainPos, facing, part);

            if (!world.getWorldBorder().isWithinBounds(partPos)
                    || !world.getBlockState(partPos)
                    .getMaterial()
                    .isReplaceable()) {
                return null;
            }
        }

        return this.defaultBlockState()
                .setValue(FACING, facing);
    }

    /**
     * 主方块成功放置后，自动生成其余五个占位方块。
     */
    @Override
    public void setPlacedBy(
            @Nonnull World world,
            @Nonnull BlockPos pos,
            @Nonnull BlockState state,
            @Nullable LivingEntity placer,
            @Nonnull ItemStack stack
    ) {
        super.setPlacedBy(world, pos, state, placer, stack);

        if (world.isClientSide) {
            return;
        }

        Direction facing = state.getValue(FACING);

        for (TablePart part : TablePart.values()) {
            BlockPos partPos =
                    getPartBlockPos(pos, facing, part);

            BlockState partState =
                    ModBlocks.BLACKJACK_TABLE_PART.get()
                            .defaultBlockState()
                            .setValue(
                                    BlackjackTablePartBlock.FACING,
                                    facing
                            )
                            .setValue(
                                    BlackjackTablePartBlock.PART,
                                    part
                            );

            world.setBlock(partPos, partState, 3);
        }
    }

    /**
     * 主方块被拆除时，删除属于它的五个占位方块。
     */
    @Override
    public void onRemove(
            @Nonnull BlockState state,
            @Nonnull World world,
            @Nonnull BlockPos pos,
            @Nonnull BlockState newState,
            boolean isMoving
    ) {
        if (!state.is(newState.getBlock())
                && !world.isClientSide) {

            TileEntity tileEntity =
                    world.getBlockEntity(pos);

            if (tileEntity
                    instanceof BlackjackTableTileEntity) {
                BlackjackTableTileEntity blackjackTable =
                        (BlackjackTableTileEntity) tileEntity;

                blackjackTable.releaseStakesOnBreak();
            }

            Direction facing = state.getValue(FACING);

            for (TablePart part : TablePart.values()) {
                BlockPos partPos =
                        getPartBlockPos(pos, facing, part);

                BlockState partState =
                        world.getBlockState(partPos);

                if (partState.is(
                        ModBlocks.BLACKJACK_TABLE_PART.get()
                )) {
                    BlockPos linkedMainPos =
                            BlackjackTablePartBlock
                                    .getMainBlockPos(
                                            partPos,
                                            partState
                                    );

                    if (linkedMainPos.equals(pos)) {
                        world.removeBlock(partPos, false);
                    }
                }
            }
        }

        super.onRemove(
                state,
                world,
                pos,
                newState,
                isMoving
        );
    }

    /**
     * 根据主方块位置、朝向和局部部件位置，
     * 计算占位方块在世界中的坐标。
     */
    @Nonnull
    public static BlockPos getPartBlockPos(
            @Nonnull BlockPos mainPos,
            @Nonnull Direction facing,
            @Nonnull TablePart part
    ) {
        Direction right = facing.getClockWise();

        int offsetX =
                right.getStepX() * part.getLocalX()
                        + facing.getStepX()
                        * part.getLocalZ();

        int offsetZ =
                right.getStepZ() * part.getLocalX()
                        + facing.getStepZ()
                        * part.getLocalZ();

        return mainPos.offset(offsetX, 0, offsetZ);
    }

    /**
     * 声明牌桌方块拥有 TileEntity。
     */
    @Override
    public boolean hasTileEntity(@Nonnull BlockState state) {
        return true;
    }

    /**
     * 创建牌桌对应的 TileEntity。
     */
    @Nullable
    @Override
    public TileEntity createTileEntity(
            @Nonnull BlockState state,
            @Nonnull IBlockReader world
    ) {
        return new BlackjackTableTileEntity(
                ModTileEntities.BLACKJACK_TABLE.get()
        );
    }

    /**
     * 让牌桌主方块交由 TileEntity Renderer 渲染。
     */
    @Nonnull
    @Override
    public BlockRenderType getRenderShape(
            @Nonnull BlockState state
    ) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    /**
     * 将 facing 注册为方块状态属性。
     */
    @Override
    protected void createBlockStateDefinition(
            @Nonnull StateContainer.Builder<Block, BlockState> builder
    ) {
        builder.add(FACING);
    }
}
