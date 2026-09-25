package com.example.betandcoin.block;

import com.example.betandcoin.network.BlackjackSessionPacket;
import com.example.betandcoin.network.ModNetwork;
import com.example.betandcoin.registry.ModBlocks;
import com.example.betandcoin.tileentity.BlackjackTableTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlackjackTablePartBlock extends HorizontalBlock {

    public static final DirectionProperty FACING =
            HorizontalBlock.FACING;

    public static final EnumProperty<TablePart> PART =
            EnumProperty.create("part", TablePart.class);

    public BlackjackTablePartBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(PART, TablePart.BACK_LEFT)
        );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(
            @Nonnull BlockItemUseContext context
    ) {
        return this.defaultBlockState()
                .setValue(
                        FACING,
                        context.getHorizontalDirection().getOpposite()
                );
    }

    @Override
    protected void createBlockStateDefinition(
            @Nonnull StateContainer.Builder<Block, BlockState> builder
    ) {
        builder.add(FACING, PART);
    }

    @Nonnull
    @Override
    public BlockRenderType getRenderShape(
            @Nonnull BlockState state
    ) {
        return BlockRenderType.INVISIBLE;
    }

    /**
     * 手持 5 或 20 面额筹码右键前排空栏位加入牌桌。
     *
     * 当前回合尚未锁定时立即参加；
     * 当前回合已经开始时，以预付下注的候补状态等待下一回合。
     */
    @Nonnull
    @Override
    public ActionResultType use(
            @Nonnull BlockState state,
            @Nonnull World world,
            @Nonnull BlockPos pos,
            @Nonnull PlayerEntity player,
            @Nonnull Hand hand,
            @Nonnull BlockRayTraceResult hit
    ) {
        int slotIndex =
                getSlotIndex(state.getValue(PART));

        if (slotIndex < 0) {
            return ActionResultType.PASS;
        }

        if (world.isClientSide) {
            return ActionResultType.SUCCESS;
        }

        if (!(player instanceof ServerPlayerEntity)) {
            return ActionResultType.CONSUME;
        }

        BlockPos mainPos =
                getMainBlockPos(pos, state);

        TileEntity tileEntity =
                world.getBlockEntity(mainPos);

        if (!(tileEntity instanceof BlackjackTableTileEntity)) {
            return ActionResultType.FAIL;
        }

        ServerPlayerEntity serverPlayer =
                (ServerPlayerEntity) player;

        BlackjackTableTileEntity table =
                (BlackjackTableTileEntity) tileEntity;

        boolean activated =
                table.tryActivateSlotWithWager(
                        slotIndex,
                        serverPlayer,
                        player.getItemInHand(hand)
                );

        if (!activated) {
            return ActionResultType.CONSUME;
        }

        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(
                        () -> serverPlayer
                ),
                BlackjackSessionPacket.open(
                        mainPos,
                        slotIndex
                )
        );

        return ActionResultType.CONSUME;
    }

    private static int getSlotIndex(
            @Nonnull TablePart part
    ) {
        switch (part) {
            case FRONT_LEFT:
                return 0;

            case FRONT_CENTER:
                return 1;

            case FRONT_RIGHT:
                return 2;

            default:
                return -1;
        }
    }



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

            BlockPos mainPos =
                    getMainBlockPos(pos, state);

            BlockState mainState =
                    world.getBlockState(mainPos);

            if (mainState.is(
                    ModBlocks.BLACKJACK_TABLE.get()
            )) {
                world.destroyBlock(mainPos, true);
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

    @Nonnull
    public static BlockPos getMainBlockPos(
            @Nonnull BlockPos partPos,
            @Nonnull BlockState partState
    ) {
        Direction facing = partState.getValue(FACING);
        Direction right = facing.getClockWise();
        TablePart part = partState.getValue(PART);

        int worldOffsetX =
                right.getStepX() * part.getLocalX()
                        + facing.getStepX() * part.getLocalZ();

        int worldOffsetZ =
                right.getStepZ() * part.getLocalX()
                        + facing.getStepZ() * part.getLocalZ();

        return partPos.offset(
                -worldOffsetX,
                0,
                -worldOffsetZ
        );
    }

    public enum TablePart implements IStringSerializable {

        BACK_LEFT(
                "back_left",
                -1,
                0
        ),

        BACK_RIGHT(
                "back_right",
                1,
                0
        ),

        FRONT_LEFT(
                "front_left",
                -1,
                1
        ),

        FRONT_CENTER(
                "front_center",
                0,
                1
        ),

        FRONT_RIGHT(
                "front_right",
                1,
                1
        );

        private final String serializedName;
        private final int localX;
        private final int localZ;

        TablePart(
                String serializedName,
                int localX,
                int localZ
        ) {
            this.serializedName = serializedName;
            this.localX = localX;
            this.localZ = localZ;
        }

        @Nonnull
        @Override
        public String getSerializedName() {
            return this.serializedName;
        }

        public int getLocalX() {
            return this.localX;
        }

        public int getLocalZ() {
            return this.localZ;
        }
    }
}
