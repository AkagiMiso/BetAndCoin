package com.example.betandcoin.block;

import net.minecraft.block.Block;
import net.minecraft.util.Direction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;

import javax.annotation.Nonnull;

/**
 * Casino Fountain 的精细静态碰撞/准星选择形状。
 *
 * 重要：
 * 这里不是按 5x5 格子粗略估计，而是直接使用当前
 * casino_fountain_v2.geo.json 中静态实体结构的 105 个 cube。
 *
 * 参与：
 * - 底座下
 * - 底座中
 * - 底座上
 * - 中层水池
 * - 中柱
 *
 * 不参与：
 * - 水面 / 水帘
 * - 纸牌鸟及翅膀
 * - 顶端悬浮方块
 * - 不需要阻挡玩家的细小装饰
 *
 * 坐标映射同时考虑：
 * 1. 模型以主方块中心为 (0, 0)；
 * 2. GeoBlockRenderer 的方向旋转；
 * 3. 5x5 PartBlock 随 FACING 旋转后的实际世界位置；
 * 4. 每个 Part 内部的 X/Z 朝向也必须随 FACING 旋转。
 */
public final class CasinoFountainShapes {

    private static final VoxelShape[][][] SHAPES =
            new VoxelShape[4][5][5];

    /*
     * Blockbench 模型空间中的旋转后 AABB：
     * {minX, minY, minZ, maxX, maxY, maxZ}
     *
     * 结构内的 cube rotation 只有 Y 轴 +/-90°，
     * 因此已严格按各自 pivot 旋转后再取 AABB。
     */
    private static final double[][] MODEL_BOXES = new double[][] {
            {-20.0D, 0.0D, -20.0D, 20.0D, 1.0D, 20.0D},
            {-24.0D, 0.0D, -12.0D, -20.0D, 1.0D, 12.0D},
            {20.0D, 0.0D, -12.0D, 24.0D, 1.0D, 12.0D},
            {-11.0D, 0.0D, 20.0D, 11.0D, 1.0D, 24.0D},
            {-11.0D, 0.0D, -24.0D, 11.0D, 1.0D, -20.0D},
            {-20.0D, 0.0D, -24.25D, -13.25D, 2.0D, -20.0D},
            {-13.25D, 0.0D, -28.25D, 13.25D, 2.0D, -24.0D},
            {13.25D, 0.0D, -24.25D, 20.0D, 2.0D, -20.0D},
            {20.0D, 0.0D, -24.25D, 22.0D, 2.0D, -22.0D},
            {-22.0D, 0.0D, -24.25D, -20.0D, 2.0D, -22.0D},
            {11.0D, 0.0D, -24.0D, 13.25D, 2.0D, -20.0D},
            {-13.25D, 0.0D, -24.0D, -11.0D, 2.0D, -20.0D},
            {20.0D, 0.0D, -22.0D, 24.25D, 2.0D, -13.25D},
            {20.0D, 0.0D, -13.25D, 24.0D, 2.0D, -12.0D},
            {24.0D, 0.0D, -13.25D, 28.25D, 2.0D, 13.25D},
            {20.0D, 0.0D, 13.25D, 24.25D, 2.0D, 22.0D},
            {20.0D, 0.0D, 12.0D, 24.0D, 2.0D, 13.25D},
            {13.25D, 0.0D, 20.0D, 20.0D, 2.0D, 24.25D},
            {20.0D, 0.0D, 22.0D, 22.0D, 2.0D, 24.25D},
            {11.0D, 0.0D, 20.0D, 13.25D, 2.0D, 24.0D},
            {-13.25D, 0.0D, 24.0D, 13.25D, 2.0D, 28.25D},
            {-20.0D, 0.0D, 20.0D, -13.25D, 2.0D, 24.25D},
            {-22.0D, 0.0D, 22.0D, -20.0D, 2.0D, 24.25D},
            {-13.25D, 0.0D, 20.0D, -11.0D, 2.0D, 24.0D},
            {-24.25D, 0.0D, 13.25D, -20.0D, 2.0D, 22.0D},
            {-28.25D, 0.0D, -13.25D, -24.0D, 2.0D, 13.25D},
            {-24.25D, 0.0D, -22.0D, -20.0D, 2.0D, -13.25D},
            {-24.0D, 0.0D, -13.25D, -20.0D, 2.0D, -12.0D},
            {-24.0D, 0.0D, 12.0D, -20.0D, 2.0D, 13.25D},
            {-27.0D, 0.0D, -27.0D, -22.0D, 2.0D, -22.0D},
            {22.0D, 0.0D, -27.0D, 27.0D, 2.0D, -22.0D},
            {22.0D, 0.0D, 22.0D, 27.0D, 2.0D, 27.0D},
            {-27.0D, 0.0D, 22.0D, -22.0D, 2.0D, 27.0D},
            {-22.0D, 2.0D, -23.75D, -11.0D, 8.0D, -20.0D},
            {-12.5D, 2.0D, -27.5D, 12.5D, 8.0D, -23.5D},
            {11.0D, 2.0D, -23.75D, 22.0D, 8.0D, -20.0D},
            {20.0D, 2.0D, -22.0D, 23.75D, 8.0D, -12.0D},
            {23.5D, 2.0D, -12.5D, 27.5D, 8.0D, 12.5D},
            {20.0D, 2.0D, 12.0D, 23.75D, 8.0D, 22.0D},
            {11.0D, 2.0D, 20.0D, 22.0D, 8.0D, 23.75D},
            {-12.5D, 2.0D, 23.5D, 12.5D, 8.0D, 27.5D},
            {-22.0D, 2.0D, 20.0D, -11.0D, 8.0D, 23.75D},
            {-23.75D, 2.0D, 12.0D, -20.0D, 8.0D, 22.0D},
            {-27.5D, 2.0D, -12.5D, -23.5D, 8.0D, 12.5D},
            {-23.75D, 2.0D, -22.0D, -20.0D, 8.0D, -12.0D},
            {-27.0D, 2.0D, -27.0D, -22.0D, 8.0D, -22.0D},
            {22.0D, 2.0D, -27.0D, 27.0D, 8.0D, -22.0D},
            {22.0D, 2.0D, 22.0D, 27.0D, 8.0D, 27.0D},
            {-27.0D, 2.0D, 22.0D, -22.0D, 8.0D, 27.0D},
            {-20.0D, 8.0D, -24.0D, -11.0D, 10.0D, -20.0D},
            {-13.0D, 8.0D, -28.0D, 13.0D, 10.0D, -24.0D},
            {11.0D, 8.0D, -24.0D, 20.0D, 10.0D, -20.0D},
            {20.0D, 8.0D, -24.0D, 22.0D, 10.0D, -22.0D},
            {-22.0D, 8.0D, -24.0D, -20.0D, 10.0D, -22.0D},
            {-22.0D, 8.0D, 22.0D, -20.0D, 10.0D, 24.0D},
            {20.0D, 8.0D, 22.0D, 22.0D, 10.0D, 24.0D},
            {20.0D, 8.0D, -22.0D, 24.0D, 10.0D, -12.0D},
            {24.0D, 8.0D, -13.0D, 28.0D, 10.0D, 13.0D},
            {20.0D, 8.0D, 12.0D, 24.0D, 10.0D, 22.0D},
            {11.0D, 8.0D, 20.0D, 20.0D, 10.0D, 24.0D},
            {-13.0D, 8.0D, 24.0D, 13.0D, 10.0D, 28.0D},
            {-11.0D, 8.0D, 23.5D, 11.0D, 10.0D, 24.0D},
            {-11.0D, 8.0D, -24.0D, 11.0D, 10.0D, -23.5D},
            {-20.0D, 8.0D, 20.0D, -11.0D, 10.0D, 24.0D},
            {-24.0D, 8.0D, 12.0D, -20.0D, 10.0D, 22.0D},
            {-28.0D, 8.0D, -13.0D, -24.0D, 10.0D, 13.0D},
            {-24.0D, 8.0D, -12.0D, -23.5D, 10.0D, 12.0D},
            {23.5D, 8.0D, -12.0D, 24.0D, 10.0D, 12.0D},
            {-24.0D, 8.0D, -22.0D, -20.0D, 10.0D, -12.0D},
            {-27.0D, 8.0D, -27.0D, -22.0D, 10.0D, -22.0D},
            {22.0D, 8.0D, -27.0D, 27.0D, 10.0D, -22.0D},
            {22.0D, 8.0D, 22.0D, 27.0D, 10.0D, 27.0D},
            {23.0D, 10.0D, 23.0D, 26.0D, 11.0D, 26.0D},
            {23.0D, 10.0D, -26.0D, 26.0D, 11.0D, -23.0D},
            {-26.0D, 10.0D, -26.0D, -23.0D, 11.0D, -23.0D},
            {-26.0D, 10.0D, 23.0D, -23.0D, 11.0D, 26.0D},
            {-27.0D, 8.0D, 22.0D, -22.0D, 10.0D, 27.0D},
            {-12.0D, 24.0D, -12.0D, 12.0D, 28.0D, -11.0D},
            {-12.0D, 24.0D, 11.0D, 12.0D, 28.0D, 12.0D},
            {-12.0D, 24.0D, -11.0D, -11.0D, 28.0D, 11.0D},
            {11.0D, 24.0D, -11.0D, 12.0D, 28.0D, 11.0D},
            {-12.0D, 23.0D, -12.0D, 12.0D, 24.0D, 12.0D},
            {-12.5D, 28.0D, -12.5D, -3.0D, 29.0D, -11.0D},
            {3.0D, 28.0D, -12.5D, 12.5D, 29.0D, -11.0D},
            {3.0D, 28.0D, 11.0D, 12.5D, 29.0D, 12.5D},
            {-12.5D, 28.0D, 11.0D, -3.0D, 29.0D, 12.5D},
            {-12.5D, 28.0D, 3.0D, -11.0D, 29.0D, 11.0D},
            {-12.5D, 28.0D, -11.0D, -11.0D, 29.0D, -3.0D},
            {11.0D, 28.0D, -11.0D, 12.5D, 29.0D, -3.0D},
            {11.0D, 28.0D, 3.0D, 12.5D, 29.0D, 11.0D},
            {-10.0D, 22.0D, -10.0D, 10.0D, 23.0D, 10.0D},
            {-7.0D, 9.0D, -7.0D, 7.0D, 23.0D, 7.0D},
            {-11.0D, 1.0D, -11.0D, 11.0D, 9.0D, 11.0D},
            {-4.0D, 25.0D, -4.0D, 4.0D, 38.0D, 4.0D},
            {-5.0D, 38.0D, -5.0D, 5.0D, 39.0D, 5.0D},
            {-2.0D, 39.0D, -2.0D, 2.0D, 40.0D, 2.0D},
            {-5.5D, 39.0D, -5.5D, -2.0D, 40.0D, -2.0D},
            {2.0D, 39.0D, -5.5D, 5.5D, 40.0D, -2.0D},
            {2.0D, 39.0D, 2.0D, 5.5D, 40.0D, 5.5D},
            {-5.5D, 39.0D, 2.0D, -2.0D, 40.0D, 5.5D},
            {-6.0D, 40.0D, -6.0D, 6.0D, 41.0D, 6.0D},
            {-5.0D, 41.0D, -5.0D, 5.0D, 42.0D, 5.0D},
            {-3.0D, 45.0D, -3.0D, 3.0D, 46.0D, 3.0D},
            {-3.5D, 44.0D, -3.5D, 3.5D, 45.0D, 3.5D},
            {-3.0D, 42.0D, -3.0D, 3.0D, 44.0D, 3.0D}
    };

    static {
        Direction[] directions = new Direction[] {
                Direction.NORTH,
                Direction.EAST,
                Direction.SOUTH,
                Direction.WEST
        };

        for (int directionIndex = 0;
             directionIndex < directions.length;
             directionIndex++) {

            Direction facing = directions[directionIndex];

            for (int localZ = -2; localZ <= 2; localZ++) {
                for (int localX = -2; localX <= 2; localX++) {
                    SHAPES[directionIndex][localZ + 2][localX + 2] =
                            buildShape(
                                    localX,
                                    localZ,
                                    facing
                            );
                }
            }
        }
    }

    private CasinoFountainShapes() {
    }

    @Nonnull
    public static VoxelShape getShape(
            int localX,
            int localZ,
            @Nonnull Direction facing
    ) {
        if (localX < -2 || localX > 2
                || localZ < -2 || localZ > 2) {
            return VoxelShapes.empty();
        }

        int directionIndex = getDirectionIndex(facing);

        return SHAPES[directionIndex]
                [localZ + 2]
                [localX + 2];
    }

    private static int getDirectionIndex(
            @Nonnull Direction facing
    ) {
        switch (facing) {
            case EAST:
                return 1;

            case SOUTH:
                return 2;

            case WEST:
                return 3;

            case NORTH:
            default:
                return 0;
        }
    }

    /**
     * 依据模型实际世界朝向构建某一个 Part 的形状。
     *
     * 这里直接在“主方块中心为原点”的世界相对坐标中完成：
     *
     * NORTH: model (x,z) -> world ( x,-z)
     * EAST : model (x,z) -> world ( z, x)
     * SOUTH: model (x,z) -> world (-x, z)
     * WEST : model (x,z) -> world (-z,-x)
     *
     * 这与 CasinoFountainBlock.getPartBlockPos 使用的局部坐标旋转保持一致。
     */
    @Nonnull
    private static VoxelShape buildShape(
            int localX,
            int localZ,
            @Nonnull Direction facing
    ) {
        int partOffsetX =
                getPartOffsetX(
                        localX,
                        localZ,
                        facing
                );

        int partOffsetZ =
                getPartOffsetZ(
                        localX,
                        localZ,
                        facing
                );

        /*
         * 当前 Part 方块在“主方块中心原点”的模型像素坐标范围。
         * 一格 = 16，方块中心相距 16，因此边界是 center +/- 8。
         */
        double cellMinX =
                partOffsetX * 16.0D - 8.0D;
        double cellMaxX =
                cellMinX + 16.0D;

        double cellMinZ =
                partOffsetZ * 16.0D - 8.0D;
        double cellMaxZ =
                cellMinZ + 16.0D;

        VoxelShape result = VoxelShapes.empty();

        for (double[] modelBox : MODEL_BOXES) {
            double[] worldBox =
                    rotateModelBoxToFacing(
                            modelBox,
                            facing
                    );

            double minX =
                    Math.max(
                            worldBox[0],
                            cellMinX
                    );

            double minY = worldBox[1];

            double minZ =
                    Math.max(
                            worldBox[2],
                            cellMinZ
                    );

            double maxX =
                    Math.min(
                            worldBox[3],
                            cellMaxX
                    );

            double maxY = worldBox[4];

            double maxZ =
                    Math.min(
                            worldBox[5],
                            cellMaxZ
                    );

            if (maxX <= minX
                    || maxY <= minY
                    || maxZ <= minZ) {
                continue;
            }

            /*
             * 从“主方块中心世界相对坐标”转换成当前 Part 自身的 0..16 坐标。
             */
            VoxelShape piece =
                    Block.box(
                            minX - cellMinX,
                            minY,
                            minZ - cellMinZ,
                            maxX - cellMinX,
                            maxY,
                            maxZ - cellMinZ
                    );

            result =
                    VoxelShapes.or(
                            result,
                            piece
                    );
        }

        return result.optimize();
    }

    /**
     * 把 Blockbench 模型空间的 AABB 转成该 FACING 下实际渲染方向的 AABB。
     *
     * 对 NORTH 来说，模型 Z 正方向对应世界 Z 负方向，
     * 因此不能像上一版那样直接把 model Z 当成方块局部 Z。
     */
    @Nonnull
    private static double[] rotateModelBoxToFacing(
            @Nonnull double[] box,
            @Nonnull Direction facing
    ) {
        double minX = box[0];
        double minY = box[1];
        double minZ = box[2];
        double maxX = box[3];
        double maxY = box[4];
        double maxZ = box[5];

        switch (facing) {
            case EAST:
                return new double[] {
                        minZ,
                        minY,
                        minX,
                        maxZ,
                        maxY,
                        maxX
                };

            case SOUTH:
                return new double[] {
                        -maxX,
                        minY,
                        minZ,
                        -minX,
                        maxY,
                        maxZ
                };

            case WEST:
                return new double[] {
                        -maxZ,
                        minY,
                        -maxX,
                        -minZ,
                        maxY,
                        -minX
                };

            case NORTH:
            default:
                return new double[] {
                        minX,
                        minY,
                        -maxZ,
                        maxX,
                        maxY,
                        -minZ
                };
        }
    }

    private static int getPartOffsetX(
            int localX,
            int localZ,
            @Nonnull Direction facing
    ) {
        Direction right = facing.getClockWise();

        return right.getStepX() * localX
                + facing.getStepX() * localZ;
    }

    private static int getPartOffsetZ(
            int localX,
            int localZ,
            @Nonnull Direction facing
    ) {
        Direction right = facing.getClockWise();

        return right.getStepZ() * localX
                + facing.getStepZ() * localZ;
    }
}
