package com.example.betandcoin.game.roulette;

import org.junit.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

public class RouletteRulesTest {
    @Test public void everyStartAndTargetStopsExactlyAndNeverReverses() {
        for (int start = 0; start < 68; start++) for (int target = 0; target < 68; target++) {
            int total = RouletteRules.totalSteps(start, target);
            assertTrue(total >= 3 * 68);
            assertEquals(target, (start + total) % 68);
            int duration = RouletteRules.duration(total);
            int oldDuration = 0;
            for (int step = 0; step < total; step++) {
                double progress = step / (double) total;
                oldDuration += 1 + (int) (7 * progress * progress * progress);
            }
            assertTrue("at least twice as fast", duration * 2 <= oldDuration);
            assertEquals(total - 1, RouletteRules.elapsedSteps(duration - 1, total));
            assertEquals(total, RouletteRules.elapsedSteps(duration, total));
            assertEquals(total, RouletteRules.elapsedSteps(duration + 1000, total));
            assertEquals(0, RouletteRules.elapsedSteps(-1, total));
            double previousInterval = 0;
            for (int step = 0; step < total; step++) {
                double interval = RouletteRules.stepInterval(step, total);
                assertTrue(interval + 1.0e-9 >= previousInterval);
                previousInterval = interval;
            }
            assertTrue("clearly slower final cells", previousInterval > 16 * RouletteRules.stepInterval(0, total));
            int previousSteps = 0;
            for (int tick = 0; tick <= duration; tick++) {
                int completed = RouletteRules.elapsedSteps(tick, total);
                assertTrue(completed >= previousSteps);
                previousSteps = completed;
            }
        }
    }

    @Test public void authoredPathHas68UniqueAdjacentCellsAndConfirmedProbabilities() {
        Set<String> locations = new HashSet<>();
        int[] counts = new int[3];
        for (int i = 0; i < 68; i++) {
            RouletteLayout.Segment s = RouletteLayout.segment(i);
            RouletteLayout.Segment next = RouletteLayout.segment(i + 1);
            assertEquals(i, s.index);
            assertTrue(locations.add(s.x + "," + s.z));
            assertEquals(0.5, Math.abs(s.x - next.x) + Math.abs(s.z - next.z), 0.00001);
            counts[s.color.ordinal()]++;
        }
        assertArrayEquals(new int[] {33, 33, 2}, counts);
        assertEquals(2, RouletteRules.Color.RED.totalReturn);
        assertEquals(2, RouletteRules.Color.BLACK.totalReturn);
        assertEquals(10, RouletteRules.Color.GREEN.totalReturn);
    }
}
