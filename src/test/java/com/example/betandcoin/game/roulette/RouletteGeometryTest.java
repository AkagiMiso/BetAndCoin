package com.example.betandcoin.game.roulette;

import com.example.betandcoin.block.RouletteTableBlock;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import org.junit.Test;
import static org.junit.Assert.*;

public class RouletteGeometryTest {
    @Test public void allFacingsAndForwardedCellsResolveSameBettingZones() {
        BlockPos main = new BlockPos(103, 64, -57);
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            for (int color = 0; color < 3; color++) {
                RouletteGeometry.BetZone zone = RouletteGeometry.zone(RouletteRules.Color.values()[color]);
                double x = zone.centerX(), z = zone.centerZ();
                double dx = x - 8, dz = z - 8;
                double wx = dx, wz = dz;
                switch (facing) {
                    case SOUTH: wx = -dx; wz = -dz; break;
                    case EAST: wx = -dz; wz = dx; break;
                    case WEST: wx = dz; wz = -dx; break;
                    default: break;
                }
                Vector3d hit = new Vector3d(main.getX() + 0.5 + wx / 16,
                        main.getY() + 15 / 16.0, main.getZ() + 0.5 + wz / 16);
                double[] local = RouletteGeometry.local(hit, main, facing);
                assertEquals(x, local[0], 0.0001); assertEquals(z, local[2], 0.0001);
                assertEquals(RouletteRules.Color.values()[color], RouletteGeometry.bettingZone(local[0], local[2]));
            }
            double area = 0;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                net.minecraft.util.math.shapes.VoxelShape shape = RouletteTableBlock.cellShape(facing, dx, dz);
                area += (shape.max(Direction.Axis.X) - shape.min(Direction.Axis.X))
                        * (shape.max(Direction.Axis.Z) - shape.min(Direction.Axis.Z));
            }
            assertEquals(18 * 32 / 256.0, area, 0.00001);
        }
        assertNull(RouletteGeometry.bettingZone(8.25, -3));
        assertNull(RouletteGeometry.bettingZone(8, 14));
        assertNull(RouletteGeometry.bettingZone(0, 3));
    }
}
