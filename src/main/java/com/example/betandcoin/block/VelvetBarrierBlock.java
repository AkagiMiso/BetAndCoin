package com.example.betandcoin.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceBlock;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;

/** Uses vanilla fence placement, neighbor updates, waterlogging and transforms. */
public class VelvetBarrierBlock extends FenceBlock {
    private final VoxelShape[] outlineShapes = makeOutlineShapes();
    private final VoxelShape[] barrierCollisionShapes;

    public VelvetBarrierBlock(Properties properties) {
        super(properties);
        // Keep the vanilla 1.5-block fence collision height to prevent jumping over ropes.
        barrierCollisionShapes = makeShapes(1.5F, 0.125F, 24.0F, 0.0F, 24.0F);
    }

    @Override
    public boolean connectsTo(BlockState neighbor, boolean sturdyFace, Direction direction) {
        return neighbor.is(this);
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos,
                               ISelectionContext context) {
        return outlineShapes[getAABBIndex(state)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos,
                                        ISelectionContext context) {
        return barrierCollisionShapes[getAABBIndex(state)];
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, IBlockReader world, BlockPos pos) {
        return VoxelShapes.empty();
    }

    private static VoxelShape[] makeOutlineShapes() {
        VoxelShape post = VoxelShapes.or(
                Block.box(6.5, 0, 6.5, 9.5, 0.5, 9.5),
                Block.box(7.25, 0.25, 7.25, 8.75, 15.5, 8.75),
                Block.box(7.5, 15.5, 7.5, 8.5, 15.75, 8.5),
                Block.box(7.25, 15.75, 7.25, 8.75, 17.25, 8.75));
        VoxelShape[] ropes = new VoxelShape[4];
        ropes[Direction.NORTH.get2DDataValue()] = Block.box(7.875, 14, 0, 8.125, 15, 7.25);
        ropes[Direction.EAST.get2DDataValue()] = Block.box(8.75, 14, 7.875, 16, 15, 8.125);
        ropes[Direction.SOUTH.get2DDataValue()] = Block.box(7.875, 14, 8.75, 8.125, 15, 16);
        ropes[Direction.WEST.get2DDataValue()] = Block.box(0, 14, 7.875, 7.25, 15, 8.125);
        VoxelShape[] shapes = new VoxelShape[16];
        for (int mask = 0; mask < shapes.length; mask++) {
            VoxelShape shape = post;
            for (int direction = 0; direction < ropes.length; direction++) {
                if ((mask & (1 << direction)) != 0) {
                    shape = VoxelShapes.or(shape, ropes[direction]);
                }
            }
            shapes[mask] = shape;
        }
        return shapes;
    }
}
