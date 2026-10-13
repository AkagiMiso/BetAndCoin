package com.example.betandcoin.game.farkle;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FarkleScoringTest {
    @Test
    public void singlesAndOrdinaryGroups() {
        assertEquals(100, FarkleScoring.score(new int[] {1}));
        assertEquals(50, FarkleScoring.score(new int[] {5}));
        for (int face = 1; face <= 6; face++) {
            assertEquals(face == 1 ? 300 : face * 100,
                    FarkleScoring.score(new int[] {face, face, face}));
            assertEquals(1000, FarkleScoring.score(new int[] {face, face, face, face}));
            assertEquals(2000, FarkleScoring.score(new int[] {face, face, face, face, face}));
            assertEquals(3000, FarkleScoring.score(new int[] {face, face, face, face, face, face}));
        }
        assertEquals(350, FarkleScoring.score(new int[] {1, 1, 1, 5}));
    }

    @Test
    public void sixDieSpecialsAreOnlyFromOneSelection() {
        assertEquals(1500, FarkleScoring.score(new int[] {1, 2, 3, 4, 5, 6}));
        assertEquals(1500, FarkleScoring.score(new int[] {2, 2, 3, 3, 4, 4}));
        assertEquals(1500, FarkleScoring.score(new int[] {2, 2, 2, 2, 5, 5}));
        assertEquals(2500, FarkleScoring.score(new int[] {2, 2, 2, 3, 3, 3}));
        assertEquals(3000, FarkleScoring.score(new int[] {6, 6, 6, 6, 6, 6}));
        // Scoring two separate triples across different rolls must not create
        // a two-triples bonus. The caller scores each roll batch separately.
        assertEquals(200, FarkleScoring.score(new int[] {2, 2, 2}));
        assertEquals(300, FarkleScoring.score(new int[] {3, 3, 3}));
    }

    @Test
    public void selectedDiceAreScoredAsOneCurrentRollBatch() {
        int[] roll = {1, 2, 5, 2, 2, 2};
        boolean[] available = {true, true, true, true, true, true};
        boolean[] selected = {false, false, false, false, false, false};
        assertEquals(0, FarkleScoring.scoreSelection(roll, available, selected));

        selected[0] = true;
        selected[2] = true;
        assertEquals(150, FarkleScoring.scoreSelection(roll, available, selected));

        selected[1] = true;
        assertEquals(-1, FarkleScoring.scoreSelection(roll, available, selected));
        selected[1] = false;

        available[0] = false;
        assertEquals(-1, FarkleScoring.scoreSelection(roll, available, selected));
        selected[0] = false;
        assertEquals(50, FarkleScoring.scoreSelection(roll, available, selected));
    }

    @Test
    public void specialRollsAndFarklesAreRecognized() {
        assertTrue(FarkleScoring.hasScoringMove(new int[] {2, 2, 3, 3, 4, 4}));
        assertTrue(FarkleScoring.hasScoringMove(new int[] {2, 3, 4, 5, 6, 1}));
        assertFalse(FarkleScoring.hasScoringMove(new int[] {2, 2, 3, 3, 4}));
        assertFalse(FarkleScoring.hasScoringMove(new int[] {2, 3, 4, 6}));
    }

    @Test
    public void everySelectedDieMustScore() {
        assertEquals(-1, FarkleScoring.score(new int[] {2}));
        assertEquals(-1, FarkleScoring.score(new int[] {1, 2}));
        assertEquals(-1, FarkleScoring.score(new int[] {2, 2, 2, 3}));
        assertFalse(FarkleScoring.hasScoringMove(new int[] {2, 3, 4, 6}));
        assertTrue(FarkleScoring.hasScoringMove(new int[] {2, 3, 4, 5}));
        assertTrue(FarkleScoring.hasScoringMove(new int[] {4, 4, 4}));
    }
}
