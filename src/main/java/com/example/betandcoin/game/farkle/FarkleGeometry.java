package com.example.betandcoin.game.farkle;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;

/** Shared table-local positions (in Blockbench pixels) and precise interaction ray. */
public final class FarkleGeometry {
    public static final float[] DIE_X = {-7, -3, 1, -7, -3, 1};
    public static final float[] DIE_Z = {-4, -4, -4, 4, 4, 4};
    public static final float DIE_Y = 15.7f;
    // Inside the white scoring frame: two columns, three rows.
    public static final float[] SCORED_X = {8.2f, 10.2f, 8.2f, 10.2f, 8.2f, 10.2f};
    public static final float[] SCORED_Z = {-5, -5, 0, 0, 5, 5};

    private FarkleGeometry() {}

    public static Hit raycast(PlayerEntity player, BlockPos pos, Direction facing,
                              BlockRayTraceResult blockHit) {
        Vector3d eye = player.getEyePosition(1.0f);
        // The packet's block hit reflects the client's actual crosshair position.
        // Server look direction can be a tick behind, especially for tiny dice.
        Vector3d clickDirection = blockHit.getLocation().subtract(eye);
        if (clickDirection.lengthSqr() < 1.0e-8) return null;
        Vector3d end = eye.add(clickDirection.normalize().scale(6.0));
        double[] start = local(eye, pos, facing);
        double[] finish = local(end, pos, facing);
        Hit closest = null;
        for (int i = 0; i < 6; i++) {
            double t = intersect(start, finish,
                    DIE_X[i] - 1.6, 14.2, DIE_Z[i] - 1.6,
                    DIE_X[i] + 1.6, 17.2, DIE_Z[i] + 1.6);
            if (t >= 0 && (closest == null || t < closest.distance))
                closest = new Hit(0, i, -1, t);
        }
        // The three authored buttons are only one model pixel wide.
        // Project the click ray onto their top plane, then classify by nearest
        // button zone so small viewpoint changes cannot swap green and yellow.
        double deltaY = finish[1] - start[1];
        if (Math.abs(deltaY) > 1.0e-8) {
            double t = (16.25 - start[1]) / deltaY;
            if (t >= 0 && t <= 1 && (closest == null || t < closest.distance)) {
                double x = start[0] + t * (finish[0] - start[0]);
                double z = start[2] + t * (finish[2] - start[2]);
                if (Math.abs(x) <= 3.25) {
                    for (int side = 0; side < 2; side++) {
                        double buttonZ = side == 0 ? -12 : 12;
                        if (Math.abs(z - buttonZ) <= 2.0) {
                            int action = x >= 1.0 ? 1 : x <= -1.0 ? 3 : 2;
                            closest = new Hit(action, -1, side, t);
                        }
                    }
                }
            }
        }
        return closest;
    }

    private static double[] local(Vector3d point, BlockPos pos, Direction facing) {
        double x = (point.x - pos.getX() - 0.5) * 16.0;
        double y = (point.y - pos.getY() - 0.01) * 16.0;
        double z = (point.z - pos.getZ() - 0.5) * 16.0;
        switch (facing) {
            case SOUTH: return new double[] {-x, y, -z};
            case WEST: return new double[] {-z, y, x};
            case EAST: return new double[] {z, y, -x};
            default: return new double[] {x, y, z};
        }
    }

    private static double intersect(double[] start, double[] end,
                                    double minX, double minY, double minZ,
                                    double maxX, double maxY, double maxZ) {
        double[] min = {minX, minY, minZ};
        double[] max = {maxX, maxY, maxZ};
        double lo = 0.0;
        double hi = 1.0;
        for (int axis = 0; axis < 3; axis++) {
            double delta = end[axis] - start[axis];
            if (Math.abs(delta) < 1.0e-8) {
                if (start[axis] < min[axis] || start[axis] > max[axis]) return -1;
            } else {
                double first = (min[axis] - start[axis]) / delta;
                double second = (max[axis] - start[axis]) / delta;
                if (first > second) {
                    double swap = first;
                    first = second;
                    second = swap;
                }
                lo = Math.max(lo, first);
                hi = Math.min(hi, second);
                if (lo > hi) return -1;
            }
        }
        return lo;
    }

    public static final class Hit {
        public final int action;
        public final int index;
        public final int side;
        public final double distance;

        private Hit(int action, int index, int side, double distance) {
            this.action = action;
            this.index = index;
            this.side = side;
            this.distance = distance;
        }
    }
}
