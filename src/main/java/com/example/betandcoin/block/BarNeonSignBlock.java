package com.example.betandcoin.block;

import com.example.betandcoin.tileentity.BarNeonSignTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BarNeonSignBlock extends HorizontalBlock {

    public static final BooleanProperty LIT =
            BooleanProperty.create("lit");

    private static final VoxelShape NORTH_SHAPE =
            VoxelShapes.box(
                    -10.5D / 16.0D,
                    0.0D,
                    8.5D / 16.0D,
                    26.5D / 16.0D,
                    15.5D / 16.0D,
                    9.0D / 16.0D
            );

    private static final VoxelShape SOUTH_SHAPE =
            VoxelShapes.box(
                    -10.5D / 16.0D,
                    0.0D,
                    7.0D / 16.0D,
                    26.5D / 16.0D,
                    15.5D / 16.0D,
                    7.5D / 16.0D
            );

    /*
     * 东西模型整体旋转 180° 后，薄片所在的 X 侧也随之交换。
     */
    private static final VoxelShape EAST_SHAPE =
            VoxelShapes.box(
                    7.0D / 16.0D,
                    0.0D,
                    -10.5D / 16.0D,
                    7.5D / 16.0D,
                    15.5D / 16.0D,
                    26.5D / 16.0D
            );

    private static final VoxelShape WEST_SHAPE =
            VoxelShapes.box(
                    8.5D / 16.0D,
                    0.0D,
                    -10.5D / 16.0D,
                    9.0D / 16.0D,
                    15.5D / 16.0D,
                    26.5D / 16.0D
            );

    public BarNeonSignBlock(Properties properties) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(LIT, true)
        );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(
            BlockItemUseContext context
    ) {
        return defaultBlockState()
                .setValue(
                        FACING,
                        context.getHorizontalDirection().getOpposite()
                )
                .setValue(LIT, true);
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
            TileEntity tileEntity = world.getBlockEntity(pos);

            if (tileEntity instanceof BarNeonSignTileEntity) {
                ((BarNeonSignTileEntity) tileEntity)
                        .startPowerStabilizeEffect();
            }
        }
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(
            BlockState state,
            IBlockReader world
    ) {
        return new BarNeonSignTileEntity();
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            IBlockReader world,
            BlockPos pos,
            ISelectionContext context
    ) {
        switch (state.getValue(FACING)) {
            case SOUTH:
                return SOUTH_SHAPE;
            case EAST:
                return EAST_SHAPE;
            case WEST:
                return WEST_SHAPE;
            case NORTH:
            default:
                return NORTH_SHAPE;
        }
    }

    @Override
    protected void createBlockStateDefinition(
            StateContainer.Builder<Block, BlockState> builder
    ) {
        builder.add(FACING, LIT);
    }
}
