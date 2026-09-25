package com.example.betandcoin.block;

import com.example.betandcoin.tileentity.BarTableClutterTileEntity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.BlockItemUseContext;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BarTableClutterBlock extends HorizontalBlock {

    public static final BooleanProperty LIT =
            BooleanProperty.create("lit");

    private static final VoxelShape SHAPE =
            Block.box(
                    1,
                    0,
                    1,
                    15,
                    8,
                    15
            );


    public BarTableClutterBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(LIT, true)
        );
    }


    @Override
    public BlockState getStateForPlacement(
            BlockItemUseContext context
    ) {
        return this.defaultBlockState()
                .setValue(
                        FACING,
                        context.getHorizontalDirection()
                                .getOpposite()
                )
                .setValue(
                        LIT,
                        true
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
        return new BarTableClutterTileEntity();
    }


    @Override
    public VoxelShape getShape(
            @Nonnull BlockState state,
            @Nonnull IBlockReader world,
            @Nonnull BlockPos pos,
            @Nonnull ISelectionContext context
    ) {
        return SHAPE;
    }


    @Override
    public VoxelShape getCollisionShape(
            @Nonnull BlockState state,
            @Nonnull IBlockReader world,
            @Nonnull BlockPos pos,
            @Nonnull ISelectionContext context
    ) {
        return VoxelShapes.empty();
    }


    @Override
    public void setPlacedBy(
            @Nonnull World world,
            @Nonnull BlockPos pos,
            @Nonnull BlockState state,
            @Nullable LivingEntity placer,
            @Nonnull ItemStack stack
    ) {
        super.setPlacedBy(world, pos, state, placer, stack);

        if (!world.isClientSide) {
            TileEntity tileEntity = world.getBlockEntity(pos);

            if (tileEntity instanceof BarTableClutterTileEntity) {
                ((BarTableClutterTileEntity) tileEntity)
                        .initializePlaced(world.random);
            }
        }
    }


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
        builder.add(FACING, LIT);
    }
}
