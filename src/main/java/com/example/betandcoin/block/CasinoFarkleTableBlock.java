package com.example.betandcoin.block;

import net.minecraft.block.material.PushReaction;

import com.example.betandcoin.game.farkle.FarkleGeometry;
import com.example.betandcoin.registry.ModBlocks;
import com.example.betandcoin.tileentity.CasinoFarkleTableTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class CasinoFarkleTableBlock extends HorizontalBlock {

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }
    public static final DirectionProperty FACING = HorizontalBlock.FACING;
    private static final VoxelShape OUTLINE = Block.box(-4, 0, -4, 20, 20, 20);
    private static final VoxelShape COLLISION = Block.box(0, 0, 0, 16, 15, 16);

    public CasinoFarkleTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        BlockPos main = context.getClickedPos();
        World world = context.getLevel();
        if (!world.getWorldBorder().isWithinBounds(main)) return null;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos partPos = main.offset(dx, 0, dz);
                if (!world.getWorldBorder().isWithinBounds(partPos)
                        || !world.getBlockState(partPos)
                        .getMaterial().isReplaceable()) return null;
            }
        }
        return defaultBlockState().setValue(FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(World world, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (world.isClientSide) return;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockState part = ModBlocks.CASINO_FARKLE_TABLE_PART.get()
                        .defaultBlockState()
                        .setValue(CasinoFarkleTablePartBlock.OFFSET_X, dx + 1)
                        .setValue(CasinoFarkleTablePartBlock.OFFSET_Z, dz + 1);
                world.setBlock(pos.offset(dx, 0, dz), part, 3);
            }
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos,
                               ISelectionContext context) {
        return OUTLINE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader world,
                                        BlockPos pos, ISelectionContext context) {
        return COLLISION;
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos,
                                PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        if (hand != Hand.MAIN_HAND) return ActionResultType.PASS;
        TileEntity tile = world.getBlockEntity(pos);
        if (!(tile instanceof CasinoFarkleTableTileEntity)) return ActionResultType.PASS;
        if (world.isClientSide) return ActionResultType.SUCCESS;
        if (!(player instanceof ServerPlayerEntity)) return ActionResultType.PASS;
        CasinoFarkleTableTileEntity table = (CasinoFarkleTableTileEntity) tile;
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        ItemStack held = player.getItemInHand(hand);
        Direction facing = state.getValue(FACING);
        double x = player.getX() - (pos.getX() + 0.5);
        double z = player.getZ() - (pos.getZ() + 0.5);
        int side = x * facing.getStepX() + z * facing.getStepZ() >= 0 ? 0 : 1;
        if (table.wager(side, serverPlayer, held)) return ActionResultType.CONSUME;
        FarkleGeometry.Hit target = FarkleGeometry.raycast(player, pos, facing, hit);
        if (target == null) {
            return ActionResultType.CONSUME;
        }
        if (target.side >= 0 && target.side != table.getCurrentSide()) {
            return ActionResultType.CONSUME;
        }
        table.handleAction(serverPlayer, target.action, target.index);
        if (target.action >= 1 && target.action <= 3)
            table.pressButton(target.side, target.action);
        return ActionResultType.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, World world, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (!world.isClientSide && !state.is(newState.getBlock())) {
            TileEntity tile = world.getBlockEntity(pos);
            if (tile instanceof CasinoFarkleTableTileEntity) {
                ((CasinoFarkleTableTileEntity) tile).releaseStakesOnBreak();
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    BlockPos partPos = pos.offset(dx, 0, dz);
                    BlockState part = world.getBlockState(partPos);
                    if (part.is(ModBlocks.CASINO_FARKLE_TABLE_PART.get())
                            && CasinoFarkleTablePartBlock.mainPos(partPos, part).equals(pos)) {
                        world.removeBlock(partPos, false);
                    }
                }
            }
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockRenderType getRenderShape(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new CasinoFarkleTableTileEntity();
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }
}
