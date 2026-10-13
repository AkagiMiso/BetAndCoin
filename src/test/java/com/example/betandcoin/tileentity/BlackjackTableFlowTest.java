package com.example.betandcoin.tileentity;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.*;

/** Exercise the real table state machine without a running world or renderer. */
public class BlackjackTableFlowTest {
    private static class Table extends BlackjackTableTileEntity {
        Table() {
            super(null);
        }

        @Override
        public void playAnimation(String animation, boolean loop) {
            // World/animation notifications are irrelevant to server turn state.
        }
    }

    @Test
    public void playerLeavingDuringPrepareIsRemovedBeforeFirstDeal() throws Exception {
        Table table = new Table();
        occupy(table, 0, true);
        occupy(table, 1, true);
        set(table, "phase", BlackjackTableTileEntity.Phase.DEAL_PREPARE);
        invoke(table, "forfeitSlot", new Class<?>[] {int.class}, 0);

        invoke(table, "startRound");

        assertFalse(flags(table, "roundParticipants")[0]);
        assertTrue(flags(table, "roundParticipants")[1]);
        assertEquals(1, get(table, "dealingStep"));
        invoke(table, "beginPlayerTurns");
        assertEquals(1, table.getCurrentTurnSlot());
    }

    @Test
    public void firstRoundDoesNotPromotePrepaidCandidate() throws Exception {
        Table table = new Table();
        occupy(table, 0, true);
        occupy(table, 1, false);
        invoke(table, "startRound");
        assertFalse(flags(table, "roundParticipants")[1]);
        assertEquals(0, get(table, "dealingStep"));
    }

    @Test
    public void noRemainingParticipantsSkipsDepartedSeatsAndPreservesCandidateWager() throws Exception {
        Table table = new Table();
        occupy(table, 0, true);
        occupy(table, 1, false);
        invoke(table, "forfeitSlot", new Class<?>[] {int.class}, 0);
        invoke(table, "startRound");
        assertFalse(flags(table, "roundParticipants")[0]);
        assertFalse(flags(table, "roundParticipants")[1]);
        assertEquals(3, get(table, "dealingStep"));
        invoke(table, "beginPlayerTurns");
        assertEquals(BlackjackTableTileEntity.Phase.DEALER_TURN, table.getPhase());
        invoke(table, "finishClearing");
        assertEquals(BlackjackTableTileEntity.Phase.ROUND_END_IDLE, table.getPhase());
        assertTrue(flags(table, "roundParticipants")[1]);
        assertEquals(BlackjackTableTileEntity.WagerState.CURRENT_ROUND,
                ((BlackjackTableTileEntity.WagerState[]) get(table, "wagerStates"))[1]);
    }

    @Test
    public void departedSeatReceivesNoCardsAtInitialLandingFrame() throws Exception {
        Table table = initialDeal();
        invoke(table, "forfeitSlot", new Class<?>[] {int.class}, 0);
        invoke(table, "tickDealing");
        assertNoDraw(table);
    }

    @Test
    public void replacementCandidateReceivesNoCardsFromPendingAnimation() throws Exception {
        Table table = initialDeal();
        invoke(table, "forfeitSlot", new Class<?>[] {int.class}, 0);
        // State established by tryActivateSlotWithWager for a mid-round join.
        occupy(table, 0, false);
        flags(table, "playerForfeited")[0] = false;
        flags(table, "playerStood")[0] = false;
        invoke(table, "tickDealing");
        assertNoDraw(table);
    }

    @Test
    public void remainingPlayerStillReceivesBothInitialCards() throws Exception {
        Table table = initialDeal();
        invoke(table, "tickDealing");
        assertEquals(2, hands(table)[0].size());
        assertEquals(50, ((List<?>) get(table, "remainingCards")).size());
    }

    @Test
    public void dealerWaitsUntilAllThreePlayersFinish() throws Exception {
        Table table = new Table();
        for (int seat = 0; seat < 3; seat++) {
            occupy(table, seat, true);
        }
        invoke(table, "beginPlayerTurns");
        for (int seat = 0; seat < 3; seat++) {
            assertEquals(BlackjackTableTileEntity.Phase.PLAYER_TURNS, table.getPhase());
            assertEquals(seat, table.getCurrentTurnSlot());
            flags(table, "playerStood")[seat] = true;
            invoke(table, "advanceTurn");
        }
        assertEquals(BlackjackTableTileEntity.Phase.DEALER_TURN, table.getPhase());
    }

    private static Table initialDeal() throws Exception {
        Table table = new Table();
        occupy(table, 0, true);
        occupy(table, 1, true);
        invoke(table, "startRound");
        set(table, "dealingTicker", 9);
        return table;
    }

    private static void assertNoDraw(Table table) throws Exception {
        assertTrue(hands(table)[0].isEmpty());
        assertEquals(52, ((List<?>) get(table, "remainingCards")).size());
        assertTrue((Boolean) get(table, "dealingCardsSpawned"));
        // The skipped animation must complete and continue to the other player.
        set(table, "dealingTicker", 19);
        invoke(table, "tickDealing");
        assertEquals(1, get(table, "dealingStep"));
    }

    private static void occupy(Table table, int seat, boolean participant) throws Exception {
        flags(table, "activeSlots")[seat] = true;
        flags(table, "roundParticipants")[seat] = participant;
        ((BlackjackTableTileEntity.WagerState[]) get(table, "wagerStates"))[seat] = participant
                ? BlackjackTableTileEntity.WagerState.CURRENT_ROUND
                : BlackjackTableTileEntity.WagerState.NEXT_ROUND_PREPAID;
    }

    private static boolean[] flags(Table table, String name) throws Exception {
        return (boolean[]) get(table, name);
    }

    private static List<?>[] hands(Table table) throws Exception {
        return (List<?>[]) get(table, "playerHands");
    }

    private static Field field(String name) throws Exception {
        Field field = BlackjackTableTileEntity.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Object get(Table table, String name) throws Exception {
        return field(name).get(table);
    }

    private static void set(Table table, String name, Object value) throws Exception {
        field(name).set(table, value);
    }

    private static void invoke(Table table, String name) throws Exception {
        invoke(table, name, new Class<?>[0]);
    }

    private static void invoke(Table table, String name, Class<?>[] types, Object... args)
            throws Exception {
        Method method = BlackjackTableTileEntity.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        method.invoke(table, args);
    }
}
