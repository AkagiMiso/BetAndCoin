package com.example.betandcoin.game.farkle;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Checks the authored texture faces against the renderer's top-face rotations. */
public class FarkleDieOrientationTest {
    private static final int[][] FACE_NORMALS = {
            {0, 0, 0},
            {0, 1, 0},   // 1: up
            {1, 0, 0},   // 2: east
            {0, 0, -1},  // 3: north
            {0, 0, 1},   // 4: south
            {-1, 0, 0},  // 5: west
            {0, -1, 0}   // 6: down
    };

    @Test
    public void everyStoredValueShowsTheSameNumberOnTop() {
        for (int value = 1; value <= 6; value++) {
            int visible = topFace(
                    FarkleDieOrientation.xDegrees(value),
                    FarkleDieOrientation.zDegrees(value));
            assertEquals("stored value " + value, value, visible);
        }
    }

    private static int topFace(int xDegrees, int zDegrees) {
        double xAngle = Math.toRadians(xDegrees);
        double zAngle = Math.toRadians(zDegrees);
        double highestY = -2;
        int result = 0;
        for (int value = 1; value <= 6; value++) {
            int[] normal = FACE_NORMALS[value];
            double yAfterX = normal[1] * Math.cos(xAngle)
                    - normal[2] * Math.sin(xAngle);
            double yAfterZ = normal[0] * Math.sin(zAngle)
                    + yAfterX * Math.cos(zAngle);
            if (yAfterZ > highestY) {
                highestY = yAfterZ;
                result = value;
            }
        }
        return result;
    }
}
