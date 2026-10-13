package com.example.betandcoin.game.roulette;

/** Shared, deterministic timing. Clients animate the server's chosen segment. */
public final class RouletteRules {
    public static final int BETTING_TICKS = 100;
    public static final int FULL_LOOPS = 3;
    public static final int SPIN_TICKS = 160;
    public static final double FAST_STEP_TICKS = 0.25;
    public static final int SLOWDOWN_POWER = 16;
    public static final int MAX_VISIBLE_CHIPS = 5;
    public static final int RESULT_FLASH_TICKS = 8;
    public static final int RESULT_PAUSE_TICKS = 40;
    public static final double LEAVE_DISTANCE = 6.0;
    public static final int MAX_BETS = 256;
    public static final int MAX_CHIPS_PER_BET = 4096;

    private RouletteRules() {}

    public enum Color {
        RED(2), BLACK(2), GREEN(10);
        public final int totalReturn;
        Color(int totalReturn) { this.totalReturn = totalReturn; }
    }

    public enum Phase { IDLE, BETTING, SPINNING, RESULT_FLASH, SETTLED_RESULT }

    public static int totalSteps(int start, int target) {
        return FULL_LOOPS * RouletteLayout.COUNT
                + Math.floorMod(target - start, RouletteLayout.COUNT);
    }

    private static double stepTime(int completed, int total) {
        // Fast early laps, with progressively longer pauses on the final lap.
        // Fractional ticks let render frames show movement between server ticks.
        return FAST_STEP_TICKS * completed
                + (SPIN_TICKS - FAST_STEP_TICKS * total)
                * Math.pow(completed / (double) total, SLOWDOWN_POWER);
    }

    public static double stepInterval(int step, int total) {
        return stepTime(step + 1, total) - stepTime(step, total);
    }

    public static int elapsedSteps(double elapsed, int total) {
        int low = 0, high = total;
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if (stepTime(mid, total) <= elapsed) low = mid;
            else high = mid - 1;
        }
        return low;
    }

    public static int duration(int total) {
        return SPIN_TICKS;
    }
}
