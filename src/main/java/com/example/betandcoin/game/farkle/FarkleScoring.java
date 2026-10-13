package com.example.betandcoin.game.farkle;

import java.util.Arrays;

/** Scores one selection from one roll batch. Every selected die must score. */
public final class FarkleScoring {
    private FarkleScoring() {}

    /** Returns -1 for an invalid selection; zero is reserved for an empty selection. */
    public static int score(int[] dice) {
        if (dice.length == 0 || dice.length > 6) return -1;
        for (int die : dice) if (die < 1 || die > 6) return -1;
        int[] memo = new int[1 << dice.length];
        Arrays.fill(memo, Integer.MIN_VALUE);
        memo[0] = 0;
        return best(dice, (1 << dice.length) - 1, memo);
    }

    /**
     * Scores the currently highlighted dice from this roll.
     * Returns zero for no selection and -1 if any selected die is unavailable
     * or the whole selection cannot be scored.
     */
    public static int scoreSelection(int[] dice, boolean[] available, boolean[] selected) {
        if (dice.length != available.length || dice.length != selected.length
                || dice.length > 6) return -1;
        int count = 0;
        for (int i = 0; i < dice.length; i++) {
            if (!selected[i]) continue;
            if (!available[i]) return -1;
            count++;
        }
        if (count == 0) return 0;
        int[] chosen = new int[count];
        int next = 0;
        for (int i = 0; i < dice.length; i++) {
            if (selected[i]) chosen[next++] = dice[i];
        }
        return score(chosen);
    }

    public static boolean hasScoringMove(int[] dice) {
        for (int mask = 1; mask < (1 << dice.length); mask++) {
            int[] subset = new int[Integer.bitCount(mask)];
            int next = 0;
            for (int i = 0; i < dice.length; i++) {
                if ((mask & (1 << i)) != 0) subset[next++] = dice[i];
            }
            if (score(subset) > 0) return true;
        }
        return false;
    }

    private static int best(int[] dice, int mask, int[] memo) {
        if (memo[mask] != Integer.MIN_VALUE) return memo[mask];
        int[] counts = new int[7];
        int size = Integer.bitCount(mask);
        for (int i = 0; i < dice.length; i++) {
            if ((mask & (1 << i)) != 0) counts[dice[i]]++;
        }
        int result = -1;
        if (size == 6) {
            boolean straight = true;
            int pairs = 0;
            int triples = 0;
            boolean fourAndPair = false;
            for (int face = 1; face <= 6; face++) {
                if (counts[face] != 1) straight = false;
                if (counts[face] == 2) pairs++;
                if (counts[face] == 3) triples++;
                if (counts[face] == 4) {
                    for (int other = 1; other <= 6; other++) {
                        if (counts[other] == 2) fourAndPair = true;
                    }
                }
            }
            if (straight || pairs == 3 || fourAndPair) result = 1500;
            if (triples == 2) result = 2500;
            for (int face = 1; face <= 6; face++) {
                if (counts[face] == 6) result = 3000;
            }
        }
        for (int face = 1; face <= 6; face++) {
            for (int count = 1; count <= counts[face]; count++) {
                int points = pointsForGroup(face, count);
                if (points < 0) continue;
                int groupMask = 0;
                int chosen = 0;
                for (int i = 0; i < dice.length && chosen < count; i++) {
                    if ((mask & (1 << i)) != 0 && dice[i] == face) {
                        groupMask |= 1 << i;
                        chosen++;
                    }
                }
                int remainder = best(dice, mask & ~groupMask, memo);
                if (remainder >= 0) result = Math.max(result, points + remainder);
            }
        }
        memo[mask] = result;
        return result;
    }

    private static int pointsForGroup(int face, int count) {
        if (count == 1) return face == 1 ? 100 : face == 5 ? 50 : -1;
        if (count == 3) return face == 1 ? 300 : face * 100;
        if (count == 4) return 1000;
        if (count == 5) return 2000;
        if (count == 6) return 3000;
        return -1;
    }
}
