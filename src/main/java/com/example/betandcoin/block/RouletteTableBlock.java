package com.example.betandcoin.block;

import net.minecraft.block.material.PushReaction;

import com.example.betandcoin.game.roulette.RouletteGeometry;
import com.example.betandcoin.game.roulette.RouletteRules;
import com.example.betandcoin.registry.ModBlocks;
import com.example.betandcoin.tileentity.RouletteTableTileEntity;
import net.minecraft.block.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.*;
import net.minecraft.world.*;

import javax.annotation.Nullable;

/** One authoritative table, with clipped invisible footprint cells like Farkle. */
public class RouletteTableBlock extends HorizontalBlock {

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }
    public RouletteTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            BlockPos cell = context.getClickedPos().offset(x, 0, z);
            if (!context.getLevel().getBlockState(cell).getMaterial().isReplaceable()
                    || !context.getLevel().getWorldBorder().isWithinBounds(cell)) return null;
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(World world, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (world.isClientSide) return;
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (x == 0 && z == 0) continue;
            world.setBlock(pos.offset(x, 0, z), ModBlocks.ROULETTE_TABLE_PART.get().defaultBlockState()
                    .setValue(RouletteTablePartBlock.OFFSET_X, x + 1)
                    .setValue(RouletteTablePartBlock.OFFSET_Z, z + 1)
                    .setValue(FACING, state.getValue(FACING)), 3);
        }
    }

    public static VoxelShape cellShape(Direction facing, int dx, int dz) {
        double[] b = RouletteGeometry.bounds(facing);
        return Block.box(Math.max(0, b[0] - 16 * dx), 0, Math.max(0, b[1] - 16 * dz),
                Math.min(16, b[2] - 16 * dx), 15, Math.min(16, b[3] - 16 * dz));
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext ctx) {
        return cellShape(state.getValue(FACING), 0, 0);
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos,
                                PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        if (hit.getDirection() != Direction.UP) return ActionResultType.PASS;
        double[] local = RouletteGeometry.local(hit.getLocation(), pos, state.getValue(FACING));
        RouletteRules.Color color = RouletteGeometry.bettingZone(local[0], local[2]);
        if (color == null || !RouletteTableTileEntity.isChip(player.getItemInHand(hand)))
            return ActionResultType.PASS;
        if (world.isClientSide) return ActionResultType.SUCCESS;
        TileEntity tile = world.getBlockEntity(pos);
        if (tile instanceof RouletteTableTileEntity)
            ((RouletteTableTileEntity) tile).placeBet(player, color, player.getItemInHand(hand));
        return ActionResultType.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, World world, BlockPos pos, BlockState next, boolean moving) {
        if (!world.isClientSide && !state.is(next.getBlock())) {
            TileEntity tile = world.getBlockEntity(pos);
            if (tile instanceof RouletteTableTileEntity) {
                RouletteTableTileEntity table = (RouletteTableTileEntity) tile;
                table.removing = true;
                table.releaseOnBreak();
            }
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                BlockPos cell = pos.offset(x, 0, z);
                BlockState part = world.getBlockState(cell);
                if (part.is(ModBlocks.ROULETTE_TABLE_PART.get())
                        && RouletteTablePartBlock.mainPos(cell, part).equals(pos)) world.removeBlock(cell, false);
            }
        }
        super.onRemove(state, world, pos, next, moving);
    }

    @Override protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override public boolean hasTileEntity(BlockState state) { return true; }
    @Override public TileEntity createTileEntity(BlockState state, IBlockReader world) { return new RouletteTableTileEntity(); }
    @Override public BlockRenderType getRenderShape(BlockState state) { return BlockRenderType.ENTITYBLOCK_ANIMATED; }
}
