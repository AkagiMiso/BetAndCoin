package com.example.betandcoin.block;

import net.minecraft.block.material.PushReaction;

import com.example.betandcoin.registry.ModBlocks;
import com.example.betandcoin.tileentity.RouletteTableTileEntity;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.*;
import net.minecraft.world.*;

public class RouletteTablePartBlock extends HorizontalBlock {

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }
    public static final IntegerProperty OFFSET_X = IntegerProperty.create("offset_x", 0, 2);
    public static final IntegerProperty OFFSET_Z = IntegerProperty.create("offset_z", 0, 2);

    public RouletteTablePartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(OFFSET_X, 0).setValue(OFFSET_Z, 0));
    }

    public static BlockPos mainPos(BlockPos pos, BlockState state) {
        return pos.offset(1 - state.getValue(OFFSET_X), 0, 1 - state.getValue(OFFSET_Z));
    }

    @Override protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> b) {
        b.add(FACING, OFFSET_X, OFFSET_Z);
    }
    @Override public BlockRenderType getRenderShape(BlockState state) { return BlockRenderType.INVISIBLE; }
    @Override public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext ctx) {
        return RouletteTableBlock.cellShape(state.getValue(FACING), state.getValue(OFFSET_X) - 1,
                state.getValue(OFFSET_Z) - 1);
    }
    @Override public ActionResultType use(BlockState state, World world, BlockPos pos,
                                          PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        BlockPos main = mainPos(pos, state);
        BlockState mainState = world.getBlockState(main);
        if (!mainState.is(ModBlocks.ROULETTE_TABLE.get())) return ActionResultType.PASS;
        return mainState.getBlock().use(mainState, world, main, player, hand, hit);
    }
    @Override
    public void playerWillDestroy(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        // Remove the main block before this cell so its callback cannot create creative drops.
        if (!world.isClientSide && player.isCreative()) {
            BlockPos main = mainPos(pos, state);
            if (world.getBlockState(main).is(ModBlocks.ROULETTE_TABLE.get())) {
                world.destroyBlock(main, false);
            }
        }
        super.playerWillDestroy(world, pos, state, player);
    }

    @Override public void onRemove(BlockState state, World world, BlockPos pos, BlockState next, boolean moving) {
        if (!world.isClientSide && !state.is(next.getBlock())) {
            BlockPos main = mainPos(pos, state);
            TileEntity tile = world.getBlockEntity(main);
            if (world.getBlockState(main).is(ModBlocks.ROULETTE_TABLE.get())
                    && tile instanceof RouletteTableTileEntity && !((RouletteTableTileEntity) tile).removing)
                world.destroyBlock(main, true);
        }
        super.onRemove(state, world, pos, next, moving);
    }
}
