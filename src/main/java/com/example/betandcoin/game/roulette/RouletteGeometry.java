package com.example.betandcoin.game.roulette;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

/** Blockbench pixels, shared by hit detection, footprint and rendering. */
public final class RouletteGeometry {
    // Authored texture interiors: red pixels [8,16)x[8,13), black
    // [17,25)x[8,13), green [12,21)x[14,19). Table UP UV [0,0,2,2]
    // maps each texture pixel to 0.5 model pixels, starting at model Z=-8.
    private static final BetZone[] BET_ZONES = {
            new BetZone(4, -4, 8, -1.5),
            new BetZone(8.5, -4, 12.5, -1.5),
            new BetZone(6, -1, 10.5, 1.5)
    };
    public static final double CHIP_Y = 15.25;
    public static final double CHIP_LAYER_HEIGHT = 0.35;
    public static final float CHIP_SCALE = 0.48f;
    public static final double GLOW_Y_OFFSET = 0.005;

    private RouletteGeometry() {}

    public static double[] local(Vector3d point, BlockPos pos, Direction facing) {
        double x = (point.x - pos.getX() - 0.5) * 16;
        double z = (point.z - pos.getZ() - 0.5) * 16;
        double y = (point.y - pos.getY()) * 16;
        switch (facing) {
            case SOUTH: return new double[] {8 - x, y, 8 - z};
            case WEST: return new double[] {8 - z, y, 8 + x};
            case EAST: return new double[] {8 + z, y, 8 - x};
            default: return new double[] {8 + x, y, 8 + z};
        }
    }

    public static RouletteRules.Color bettingZone(double x, double z) {
        for (int i = 0; i < BET_ZONES.length; i++) {
            BetZone zone = BET_ZONES[i];
            if (x >= zone.minX && x < zone.maxX && z >= zone.minZ && z < zone.maxZ)
                return RouletteRules.Color.values()[i];
        }
        return null;
    }

    public static BetZone zone(RouletteRules.Color color) { return BET_ZONES[color.ordinal()]; }

    public static final class BetZone {
        public final double minX, minZ, maxX, maxZ;
        private BetZone(double minX, double minZ, double maxX, double maxZ) {
            this.minX = minX; this.minZ = minZ; this.maxX = maxX; this.maxZ = maxZ;
        }
        public double centerX() { return (minX + maxX) / 2; }
        public double centerZ() { return (minZ + maxZ) / 2; }
    }

    /** Returns the table's rotated AABB in pixels relative to the main cell. */
    public static double[] bounds(Direction facing) {
        switch (facing) {
            case SOUTH: return new double[] {-1, -7, 17, 25};
            case EAST: return new double[] {-7, -1, 25, 17};
            case WEST: return new double[] {-9, -1, 23, 17};
            default: return new double[] {-1, -9, 17, 23};
        }
    }
}
