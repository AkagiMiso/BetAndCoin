package com.example.betandcoin.game.farkle;

/**
 * Rotates the authored die so the requested pip count is on top.
 * Texture faces: up=1, east=2, north=3, south=4, west=5, down=6.
 */
public final class FarkleDieOrientation {
    private FarkleDieOrientation() {}

    public static int xDegrees(int value) {
        switch (value) {
            case 3: return 90;
            case 4: return -90;
            case 6: return 180;
            default: return 0;
        }
    }

    public static int zDegrees(int value) {
        switch (value) {
            case 2: return 90;
            case 5: return -90;
            default: return 0;
        }
    }
}
