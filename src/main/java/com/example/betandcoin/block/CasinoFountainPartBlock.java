package com.example.betandcoin.block;

import com.example.betandcoin.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.Direction;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/**
 * 喷泉 5x5 多方块结构的不可见占位方块。
 *
 * 5x5 仍然作为“结构占地/放置检查区域”保留，
 * 但碰撞体不再把整个外圈都做成完整方块。
 *
 * 模型底座最外沿大约只伸入 local +/-2 的方块约 1/4 格，
 * 因此这里把最外圈碰撞缩成靠近喷泉中心的窄条，
 * 避免玩家在视觉模型之外被“空气墙”卡住。
 */
public class CasinoFountainPartBlock extends HorizontalBlock {

    public static final DirectionProperty FACING =
            HorizontalBlock.FACING;

    public static final EnumProperty<FountainPart> PART =
            EnumProperty.create("part", FountainPart.class);

    public CasinoFountainPartBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(PART, FountainPart.X_NEG2_Z_NEG2)
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
     * 实体碰撞直接使用当前 Part 真正覆盖到的喷泉模型几何。
     */
    @Nonnull
    @Override
    public VoxelShape getCollisionShape(
            @Nonnull BlockState state,
            @Nonnull IBlockReader world,
            @Nonnull BlockPos pos,
            @Nonnull ISelectionContext context
    ) {
        FountainPart part = state.getValue(PART);

        return CasinoFountainShapes.getShape(
                part.getLocalX(),
                part.getLocalZ(),
                state.getValue(FACING)
        );
    }

    /**
     * 准星选择框与碰撞形状保持一致。
     *
     * 因此准星只有真正指向池沿、底座、中柱或上层水池时才会选中，
     * 不会再选中整块不可见的 5x5 Part 空间。
     */
    @Nonnull
    @Override
    public VoxelShape getShape(
            @Nonnull BlockState state,
            @Nonnull IBlockReader world,
            @Nonnull BlockPos pos,
            @Nonnull ISelectionContext context
    ) {
        FountainPart part = state.getValue(PART);

        return CasinoFountainShapes.getShape(
                part.getLocalX(),
                part.getLocalZ(),
                state.getValue(FACING)
        );
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

            if (world.getBlockState(mainPos)
                    .is(ModBlocks.CASINO_FOUNTAIN.get())) {
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
        FountainPart part = partState.getValue(PART);

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

    /**
     * 5x5 中除中心 (0, 0) 之外的 24 个位置。
     */
    public enum FountainPart implements IStringSerializable {
        X_NEG2_Z_NEG2("x_neg2_z_neg2", -2, -2),
        X_NEG1_Z_NEG2("x_neg1_z_neg2", -1, -2),
        X_0_Z_NEG2("x_0_z_neg2", 0, -2),
        X_1_Z_NEG2("x_1_z_neg2", 1, -2),
        X_2_Z_NEG2("x_2_z_neg2", 2, -2),

        X_NEG2_Z_NEG1("x_neg2_z_neg1", -2, -1),
        X_NEG1_Z_NEG1("x_neg1_z_neg1", -1, -1),
        X_0_Z_NEG1("x_0_z_neg1", 0, -1),
        X_1_Z_NEG1("x_1_z_neg1", 1, -1),
        X_2_Z_NEG1("x_2_z_neg1", 2, -1),

        X_NEG2_Z_0("x_neg2_z_0", -2, 0),
        X_NEG1_Z_0("x_neg1_z_0", -1, 0),
        X_1_Z_0("x_1_z_0", 1, 0),
        X_2_Z_0("x_2_z_0", 2, 0),

        X_NEG2_Z_1("x_neg2_z_1", -2, 1),
        X_NEG1_Z_1("x_neg1_z_1", -1, 1),
        X_0_Z_1("x_0_z_1", 0, 1),
        X_1_Z_1("x_1_z_1", 1, 1),
        X_2_Z_1("x_2_z_1", 2, 1),

        X_NEG2_Z_2("x_neg2_z_2", -2, 2),
        X_NEG1_Z_2("x_neg1_z_2", -1, 2),
        X_0_Z_2("x_0_z_2", 0, 2),
        X_1_Z_2("x_1_z_2", 1, 2),
        X_2_Z_2("x_2_z_2", 2, 2);

        private final String serializedName;
        private final int localX;
        private final int localZ;

        FountainPart(
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
