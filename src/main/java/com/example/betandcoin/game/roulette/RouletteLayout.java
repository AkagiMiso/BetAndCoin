package com.example.betandcoin.game.roulette;

import com.example.betandcoin.game.roulette.RouletteRules.Color;

/** Clockwise from the northwest corner. Sampled from the supplied 256px texture:
 * wheel UP UV [6,0,7.5,1.5], pixels (99,3)..(116,20), one pixel = 0.5 model pixels.
 * This asset has 33 red, 33 black and TWO green cells (confirmed by the user). */
public final class RouletteLayout {
    public static final int COUNT = 68;
    private static final Segment[] SEGMENTS = {
        new Segment(0, Color.BLACK, 3.5f, 9.5f),
        new Segment(1, Color.RED, 4f, 9.5f),
        new Segment(2, Color.BLACK, 4.5f, 9.5f),
        new Segment(3, Color.RED, 5f, 9.5f),
        new Segment(4, Color.BLACK, 5.5f, 9.5f),
        new Segment(5, Color.RED, 6f, 9.5f),
        new Segment(6, Color.BLACK, 6.5f, 9.5f),
        new Segment(7, Color.RED, 7f, 9.5f),
        new Segment(8, Color.BLACK, 7.5f, 9.5f),
        new Segment(9, Color.RED, 8f, 9.5f),
        new Segment(10, Color.BLACK, 8.5f, 9.5f),
        new Segment(11, Color.RED, 9f, 9.5f),
        new Segment(12, Color.BLACK, 9.5f, 9.5f),
        new Segment(13, Color.RED, 10f, 9.5f),
        new Segment(14, Color.GREEN, 10.5f, 9.5f),
        new Segment(15, Color.RED, 11f, 9.5f),
        new Segment(16, Color.BLACK, 11.5f, 9.5f),
        new Segment(17, Color.RED, 12f, 9.5f),
        new Segment(18, Color.BLACK, 12f, 10f),
        new Segment(19, Color.RED, 12f, 10.5f),
        new Segment(20, Color.BLACK, 12f, 11f),
        new Segment(21, Color.RED, 12f, 11.5f),
        new Segment(22, Color.BLACK, 12f, 12f),
        new Segment(23, Color.RED, 12f, 12.5f),
        new Segment(24, Color.BLACK, 12f, 13f),
        new Segment(25, Color.RED, 12f, 13.5f),
        new Segment(26, Color.BLACK, 12f, 14f),
        new Segment(27, Color.RED, 12f, 14.5f),
        new Segment(28, Color.BLACK, 12f, 15f),
        new Segment(29, Color.RED, 12f, 15.5f),
        new Segment(30, Color.BLACK, 12f, 16f),
        new Segment(31, Color.RED, 12f, 16.5f),
        new Segment(32, Color.BLACK, 12f, 17f),
        new Segment(33, Color.RED, 12f, 17.5f),
        new Segment(34, Color.BLACK, 12f, 18f),
        new Segment(35, Color.RED, 11.5f, 18f),
        new Segment(36, Color.BLACK, 11f, 18f),
        new Segment(37, Color.RED, 10.5f, 18f),
        new Segment(38, Color.BLACK, 10f, 18f),
        new Segment(39, Color.RED, 9.5f, 18f),
        new Segment(40, Color.BLACK, 9f, 18f),
        new Segment(41, Color.RED, 8.5f, 18f),
        new Segment(42, Color.BLACK, 8f, 18f),
        new Segment(43, Color.RED, 7.5f, 18f),
        new Segment(44, Color.BLACK, 7f, 18f),
        new Segment(45, Color.RED, 6.5f, 18f),
        new Segment(46, Color.BLACK, 6f, 18f),
        new Segment(47, Color.RED, 5.5f, 18f),
        new Segment(48, Color.BLACK, 5f, 18f),
        new Segment(49, Color.GREEN, 4.5f, 18f),
        new Segment(50, Color.BLACK, 4f, 18f),
        new Segment(51, Color.RED, 3.5f, 18f),
        new Segment(52, Color.BLACK, 3.5f, 17.5f),
        new Segment(53, Color.RED, 3.5f, 17f),
        new Segment(54, Color.BLACK, 3.5f, 16.5f),
        new Segment(55, Color.RED, 3.5f, 16f),
        new Segment(56, Color.BLACK, 3.5f, 15.5f),
        new Segment(57, Color.RED, 3.5f, 15f),
        new Segment(58, Color.BLACK, 3.5f, 14.5f),
        new Segment(59, Color.RED, 3.5f, 14f),
        new Segment(60, Color.BLACK, 3.5f, 13.5f),
        new Segment(61, Color.RED, 3.5f, 13f),
        new Segment(62, Color.BLACK, 3.5f, 12.5f),
        new Segment(63, Color.RED, 3.5f, 12f),
        new Segment(64, Color.BLACK, 3.5f, 11.5f),
        new Segment(65, Color.RED, 3.5f, 11f),
        new Segment(66, Color.BLACK, 3.5f, 10.5f),
        new Segment(67, Color.RED, 3.5f, 10f),
    };
    private RouletteLayout() {}
    public static Segment segment(int index) { return SEGMENTS[Math.floorMod(index, COUNT)]; }
    public static final class Segment {
        public final int index;
        public final Color color;
        public final float x, z;
        public final float y = 13.51f;
        public final float rotationY = 0; // square glow faces need no corner rotation
        private Segment(int index, Color color, float x, float z) {
            this.index = index; this.color = color; this.x = x; this.z = z;
        }
    }
}