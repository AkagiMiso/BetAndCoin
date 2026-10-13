package com.example.betandcoin.event;

import com.example.betandcoin.tileentity.BlackjackTableTileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.registry.Bootstrap;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.UUID;
import static com.example.betandcoin.tileentity.BlackjackTableTileEntity.*;
import static org.junit.Assert.*;

public class BlackjackDepartureHandlerTest {
    static { Bootstrap.bootStrap(); }
    private final UUID departing = new UUID(1, 1);
    private final UUID staying = new UUID(2, 2);

    @Test public void logoutImmediatelyForfeitsOnlyDepartingPlayerAcrossTables() throws Exception {
        BlackjackTableTileEntity first = table(Phase.DEALING, false);
        BlackjackTableTileEntity second = table(Phase.PLAYER_HIT, false);
        BlackjackDepartureHandler.releaseSeats(Arrays.asList(first, second), departing);
        for (BlackjackTableTileEntity table : Arrays.asList(first, second)) {
            assertFalse(table.isSlotActive(0));
            assertNull(table.getSlotPlayer(0));
            assertEquals(RoundResult.FORFEIT, ((RoundResult[]) field(table, "roundResults"))[0]);
            assertEquals(WagerState.NONE, ((WagerState[]) field(table, "wagerStates"))[0]);
            assertTrue(((ItemStack[]) field(table, "sessionBetStacks"))[0].isEmpty());
            assertTrue(table.isSlotActive(1));
            assertEquals(staying, table.getSlotPlayer(1));
            assertEquals(1, ((ItemStack[]) field(table, "sessionBetStacks"))[1].getCount());
        }
    }

    @Test public void repeatedLogoutAndReconnectCannotRecoverForfeitedSeat() throws Exception {
        BlackjackTableTileEntity table = table(Phase.DEALER_TURN, false);
        BlackjackDepartureHandler.releaseSeats(Arrays.asList(table), departing);
        BlackjackDepartureHandler.releaseSeats(Arrays.asList(table), departing);
        assertFalse(table.isSlotActive(0));
        assertNull(table.getSlotPlayer(0));
        assertEquals(RoundResult.FORFEIT, ((RoundResult[]) field(table, "roundResults"))[0]);
        assertTrue(table.isSlotActive(1));
    }

    @Test public void settledDepartureDoesNotBecomeAnotherForfeit() throws Exception {
        BlackjackTableTileEntity table = table(Phase.WINNER_ANIMATION, true);
        BlackjackDepartureHandler.releaseSeats(Arrays.asList(table), departing);
        assertFalse(table.isSlotActive(0));
        assertNull(table.getSlotPlayer(0));
        assertFalse(((boolean[]) field(table, "playerForfeited"))[0]);
        assertTrue(table.isSlotActive(1));
    }

    @Test public void unrelatedLogoutLeavesHealthyGameUntouched() throws Exception {
        BlackjackTableTileEntity table = table(Phase.DEALING, false);
        BlackjackDepartureHandler.releaseSeats(Arrays.asList(table), new UUID(3, 3));
        assertTrue(table.isSlotActive(0));
        assertTrue(table.isSlotActive(1));
        assertEquals(Phase.DEALING, table.getPhase());
    }

    private BlackjackTableTileEntity table(Phase phase, boolean settled) throws Exception {
        BlackjackTableTileEntity table = new BlackjackTableTileEntity(TileEntityType.CHEST);
        set(table, "phase", phase);
        set(table, "roundSettlementApplied", settled);
        for (int slot = 0; slot < 2; slot++) {
            ((boolean[]) field(table, "activeSlots"))[slot] = true;
            ((boolean[]) field(table, "roundParticipants"))[slot] = true;
            ((UUID[]) field(table, "slotPlayers"))[slot] = slot == 0 ? departing : staying;
            ((WagerState[]) field(table, "wagerStates"))[slot] = WagerState.CURRENT_ROUND;
            ((ItemStack[]) field(table, "sessionBetStacks"))[slot] = new ItemStack(Items.EMERALD);
        }
        return table;
    }

    private Object field(BlackjackTableTileEntity table, String name) throws Exception {
        Field field = BlackjackTableTileEntity.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(table);
    }

    private void set(BlackjackTableTileEntity table, String name, Object value) throws Exception {
        Field field = BlackjackTableTileEntity.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(table, value);
    }
}
