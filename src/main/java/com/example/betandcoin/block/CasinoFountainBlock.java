package com.example.betandcoin.block;

import com.example.betandcoin.block.CasinoFountainPartBlock.FountainPart;
import com.example.betandcoin.registry.ModBlocks;
import com.example.betandcoin.registry.ModTileEntities;
import com.example.betandcoin.tileentity.CasinoFountainTileEntity;
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
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 赌场大厅喷泉主方块。
 *
 * 主方块位于 5x5 占地区域正中心，其余 24 格由
 * CasinoFountainPartBlock 占位。
 */
public class CasinoFountainBlock extends HorizontalBlock {

    public static final DirectionProperty FACING =
            HorizontalBlock.FACING;

    public CasinoFountainBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
        );
    }

    /**
     * 放置前检查完整 5x5 地面占地区域。
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

        for (FountainPart part : FountainPart.values()) {
            BlockPos partPos =
                    getPartBlockPos(mainPos, facing, part);

            if (!world.getBlockState(partPos)
                    .getMaterial()
                    .isReplaceable()) {
                return null;
            }
        }

        return this.defaultBlockState()
                .setValue(FACING, facing);
    }

    /**
     * 主方块放下后生成其余 24 个占位方块。
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

        for (FountainPart part : FountainPart.values()) {
            BlockPos partPos =
                    getPartBlockPos(pos, facing, part);

            BlockState partState =
                    ModBlocks.CASINO_FOUNTAIN_PART.get()
                            .defaultBlockState()
                            .setValue(
                                    CasinoFountainPartBlock.FACING,
                                    facing
                            )
                            .setValue(
                                    CasinoFountainPartBlock.PART,
                                    part
                            );

            world.setBlock(partPos, partState, 3);
        }
    }

    /**
     * 主方块被移除时清理全部占位方块。
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

            Direction facing = state.getValue(FACING);

            for (FountainPart part : FountainPart.values()) {
                BlockPos partPos =
                        getPartBlockPos(pos, facing, part);

                BlockState partState =
                        world.getBlockState(partPos);

                if (partState.is(
                        ModBlocks.CASINO_FOUNTAIN_PART.get()
                )) {
                    BlockPos linkedMainPos =
                            CasinoFountainPartBlock
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
     * 局部坐标跟随喷泉朝向旋转。
     */
    @Nonnull
    public static BlockPos getPartBlockPos(
            @Nonnull BlockPos mainPos,
            @Nonnull Direction facing,
            @Nonnull FountainPart part
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
     * 主方块中心格也使用真实模型几何，不再沿用默认整格立方体。
     */
    @Nonnull
    @Override
    public VoxelShape getShape(
            @Nonnull BlockState state,
            @Nonnull IBlockReader world,
            @Nonnull BlockPos pos,
            @Nonnull ISelectionContext context
    ) {
        return CasinoFountainShapes.getShape(
                0,
                0,
                state.getValue(FACING)
        );
    }

    @Nonnull
    @Override
    public VoxelShape getCollisionShape(
            @Nonnull BlockState state,
            @Nonnull IBlockReader world,
            @Nonnull BlockPos pos,
            @Nonnull ISelectionContext context
    ) {
        return CasinoFountainShapes.getShape(
                0,
                0,
                state.getValue(FACING)
        );
    }

    @Override
    public boolean hasTileEntity(@Nonnull BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(
            @Nonnull BlockState state,
            @Nonnull IBlockReader world
    ) {
        return new CasinoFountainTileEntity(
                ModTileEntities.CASINO_FOUNTAIN.get()
        );
    }

    @Nonnull
    @Override
    public BlockRenderType getRenderShape(
            @Nonnull BlockState state
    ) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected void createBlockStateDefinition(
            @Nonnull StateContainer.Builder<Block, BlockState> builder
    ) {
        builder.add(FACING);
    }
}
