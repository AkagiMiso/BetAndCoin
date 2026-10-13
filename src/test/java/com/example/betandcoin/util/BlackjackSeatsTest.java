package com.example.betandcoin.util;

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

public class BlackjackSeatsTest {
    static { Bootstrap.bootStrap(); }
    private final UUID player = new UUID(1, 1);

    @Test public void currentAndPrepaidSeatsBothPreventJoiningAnotherTable() throws Exception {
        for (WagerState wager : new WagerState[] {WagerState.CURRENT_ROUND, WagerState.NEXT_ROUND_PREPAID}) {
            BlackjackTableTileEntity occupied = table(player, wager);
            BlackjackTableTileEntity empty = new BlackjackTableTileEntity(TileEntityType.CHEST);
            assertTrue(BlackjackSeats.hasSeat(Arrays.asList(empty, occupied), player));
            assertEquals(wager, ((WagerState[]) get(occupied, "wagerStates"))[0]);
            assertEquals(1, ((ItemStack[]) get(occupied, "sessionBetStacks"))[0].getCount());
            assertFalse(empty.isSlotActive(0));
        }
    }

    @Test public void otherPlayersSeatsDoNotPreventJoining() throws Exception {
        assertFalse(BlackjackSeats.hasSeat(Arrays.asList(table(new UUID(2, 2), WagerState.CURRENT_ROUND)), player));
    }

    @Test public void departedSeatAllowsSwitchingWhileOtherPlayerRemainsSeated() throws Exception {
        BlackjackTableTileEntity table = table(player, WagerState.CURRENT_ROUND);
        ((boolean[]) get(table, "activeSlots"))[1] = true;
        ((UUID[]) get(table, "slotPlayers"))[1] = new UUID(2, 2);
        Field phase = BlackjackTableTileEntity.class.getDeclaredField("phase");
        phase.setAccessible(true);
        phase.set(table, Phase.DEALING);
        assertTrue(BlackjackSeats.hasSeat(Arrays.asList(table), player));
        assertTrue(table.releaseSlot(0));
        assertFalse(BlackjackSeats.hasSeat(Arrays.asList(table), player));
        assertTrue(table.isSlotActive(1));
    }

    @Test public void removedTableDoesNotKeepPlayerLocked() throws Exception {
        BlackjackTableTileEntity table = table(player, WagerState.CURRENT_ROUND);
        table.setRemoved();
        assertFalse(BlackjackSeats.hasSeat(Arrays.asList(table), player));
    }

    @Test public void inactiveOldUuidDoesNotKeepPlayerLocked() throws Exception {
        BlackjackTableTileEntity table = table(player, WagerState.CURRENT_ROUND);
        ((boolean[]) get(table, "activeSlots"))[0] = false;
        assertFalse(BlackjackSeats.hasSeat(Arrays.asList(table), player));
    }

    private BlackjackTableTileEntity table(UUID owner, WagerState wager) throws Exception {
        BlackjackTableTileEntity table = new BlackjackTableTileEntity(TileEntityType.CHEST);
        ((boolean[]) get(table, "activeSlots"))[0] = true;
        ((boolean[]) get(table, "roundParticipants"))[0] = wager == WagerState.CURRENT_ROUND;
        ((UUID[]) get(table, "slotPlayers"))[0] = owner;
        ((WagerState[]) get(table, "wagerStates"))[0] = wager;
        ((ItemStack[]) get(table, "sessionBetStacks"))[0] = new ItemStack(Items.EMERALD);
        return table;
    }

    private Object get(BlackjackTableTileEntity table, String name) throws Exception {
        Field field = BlackjackTableTileEntity.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(table);
    }
}
