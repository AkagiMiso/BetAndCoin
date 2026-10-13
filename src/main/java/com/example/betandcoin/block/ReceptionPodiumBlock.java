package com.example.betandcoin.block;

import com.example.betandcoin.tileentity.ReceptionPodiumTileEntity;
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

public class ReceptionPodiumBlock extends HorizontalBlock {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    private static final VoxelShape NORTH_SHAPE = makeShape(Direction.NORTH);
    private static final VoxelShape EAST_SHAPE = makeShape(Direction.EAST);
    private static final VoxelShape SOUTH_SHAPE = makeShape(Direction.SOUTH);
    private static final VoxelShape WEST_SHAPE = makeShape(Direction.WEST);

    public ReceptionPodiumBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, true));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(LIT, true);
    }

    @Override
    public void setPlacedBy(World world, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (!world.isClientSide) {
            TileEntity tileEntity = world.getBlockEntity(pos);
            if (tileEntity instanceof ReceptionPodiumTileEntity) {
                ((ReceptionPodiumTileEntity) tileEntity).startPlacementFlicker();
            }
        }
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new ReceptionPodiumTileEntity();
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world,
                               BlockPos pos, ISelectionContext context) {
        switch (state.getValue(FACING)) {
            case EAST: return EAST_SHAPE;
            case SOUTH: return SOUTH_SHAPE;
            case WEST: return WEST_SHAPE;
            default: return NORTH_SHAPE;
        }
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader world,
                                        BlockPos pos, ISelectionContext context) {
        return getShape(state, world, pos, context);
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    private static VoxelShape makeShape(Direction facing) {
        // The narrow plinth, column, sloped writing surface and lamp follow
        // the supplied Blockbench model. The slope uses three shallow steps.
        return VoxelShapes.or(
                rotatedBox(facing, 3.5, 0, 3.5, 12.5, 0.5, 12.5),
                rotatedBox(facing, 7.25, 0.25, 7.25, 8.75, 15.25, 8.75),
                rotatedBox(facing, 2, 13.2, 3.7, 14, 14.7, 6.5),
                rotatedBox(facing, 2, 14.3, 6.5, 14, 15.9, 9.3),
                rotatedBox(facing, 2, 15.4, 9.3, 14, 17.1, 12.3),
                rotatedBox(facing, 9.25, 17.4, 9.3, 13.25, 18.75, 11.2)
        );
    }

    private static VoxelShape rotatedBox(Direction facing,
                                          double x1, double y1, double z1,
                                          double x2, double y2, double z2) {
        switch (facing) {
            case EAST: return Block.box(16 - z2, y1, x1, 16 - z1, y2, x2);
            case SOUTH: return Block.box(16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1);
            case WEST: return Block.box(z1, y1, 16 - x2, z2, y2, 16 - x1);
            default: return Block.box(x1, y1, z1, x2, y2, z2);
        }
    }
}
