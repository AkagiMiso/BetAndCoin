package com.example.betandcoin.block;

import net.minecraft.block.material.PushReaction;

import com.example.betandcoin.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

/** Invisible 3x3 footprint cell; its narrow shape follows the 1.5-block table. */
public class CasinoFarkleTablePartBlock extends Block {

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }
    public static final IntegerProperty OFFSET_X = IntegerProperty.create("offset_x", 0, 2);
    public static final IntegerProperty OFFSET_Z = IntegerProperty.create("offset_z", 0, 2);

    public CasinoFarkleTablePartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OFFSET_X, 0)
                .setValue(OFFSET_Z, 0));
    }

    public static BlockPos mainPos(BlockPos partPos, BlockState state) {
        return partPos.offset(1 - state.getValue(OFFSET_X), 0,
                1 - state.getValue(OFFSET_Z));
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(OFFSET_X, OFFSET_Z);
    }

    @Override
    public BlockRenderType getRenderShape(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos,
                               ISelectionContext context) {
        int dx = state.getValue(OFFSET_X) - 1;
        int dz = state.getValue(OFFSET_Z) - 1;
        return Block.box(Math.max(0, -4 - 16 * dx), 0, Math.max(0, -4 - 16 * dz),
                Math.min(16, 20 - 16 * dx), 20, Math.min(16, 20 - 16 * dz));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader world,
                                        BlockPos pos, ISelectionContext context) {
        int dx = state.getValue(OFFSET_X) - 1;
        int dz = state.getValue(OFFSET_Z) - 1;
        return Block.box(Math.max(0, -4 - 16 * dx), 0, Math.max(0, -4 - 16 * dz),
                Math.min(16, 20 - 16 * dx), 15, Math.min(16, 20 - 16 * dz));
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos,
                                PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        BlockPos main = mainPos(pos, state);
        BlockState mainState = world.getBlockState(main);
        if (!(mainState.getBlock() instanceof CasinoFarkleTableBlock)) return ActionResultType.PASS;
        return mainState.getBlock().use(mainState, world, main, player, hand, hit);
    }

    @Override
    public void playerWillDestroy(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        // Remove the main block before this cell so its callback cannot create creative drops.
        if (!world.isClientSide && player.isCreative()) {
            BlockPos main = mainPos(pos, state);
            if (world.getBlockState(main).is(ModBlocks.CASINO_FARKLE_TABLE.get())) {
                world.destroyBlock(main, false);
            }
        }
        super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, World world, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (!world.isClientSide && !state.is(newState.getBlock())) {
            BlockPos main = mainPos(pos, state);
            if (world.getBlockState(main).is(ModBlocks.CASINO_FARKLE_TABLE.get())) {
                world.destroyBlock(main, true);
            }
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }
}
