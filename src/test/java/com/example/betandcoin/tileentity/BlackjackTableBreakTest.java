package com.example.betandcoin.tileentity;

import com.example.betandcoin.registry.ModItems;
import net.minecraft.block.Blocks;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.registry.Bootstrap;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.RegistryObject;
import org.junit.Test;

import java.lang.reflect.*;
import java.util.UUID;
import static com.example.betandcoin.tileentity.BlackjackTableTileEntity.*;
import static org.junit.Assert.*;

/** Exercise the actual removal refund ledger without running a world or networking. */
public class BlackjackTableBreakTest {
    static { Bootstrap.bootStrap(); }

    @Test public void everyUnsettledPhaseRefundsCurrentAndPrepaidWagers() throws Exception {
        for (Phase phase : Phase.values()) {
            if (phase == Phase.WINNER_ANIMATION || phase == Phase.CLEARING) continue;
            BlackjackTableTileEntity table = table(phase, false);
            reserve(table, 0, WagerState.CURRENT_ROUND, Items.EMERALD);
            reserve(table, 1, WagerState.NEXT_ROUND_PREPAID, Items.DIAMOND);
            ItemStack[] refunds = take(table);
            assertEquals(phase.name(), 1, refunds[0].getCount());
            assertEquals(phase.name(), 1, refunds[1].getCount());
            assertTrue(refunds[2].isEmpty());
        }
    }

    @Test public void settledWinsPushesAndLossesAreNotPaidAgainButCandidatesAreRefunded() throws Exception {
        for (Phase phase : new Phase[] {Phase.WINNER_ANIMATION, Phase.CLEARING}) {
            for (RoundResult result : new RoundResult[] {RoundResult.WIN, RoundResult.PUSH, RoundResult.LOSS}) {
                BlackjackTableTileEntity table = table(phase, true);
                reserve(table, 0, WagerState.CURRENT_ROUND, Items.EMERALD);
                reserve(table, 1, WagerState.NEXT_ROUND_PREPAID, Items.DIAMOND);
                ((RoundResult[]) get(table, "roundResults"))[0] = result;
                ItemStack[] refunds = take(table);
                assertTrue(refunds[0].isEmpty());
                assertEquals(1, refunds[1].getCount());
                assertSame(Items.DIAMOND, refunds[1].getItem());
            }
        }
    }

    @Test public void nextRoundAlreadyChargedDuringIdleIsRefundedEvenWithOldWinResult() throws Exception {
        BlackjackTableTileEntity table = table(Phase.ROUND_END_IDLE, false);
        reserve(table, 0, WagerState.CURRENT_ROUND, Items.EMERALD);
        ((RoundResult[]) get(table, "roundResults"))[0] = RoundResult.WIN;
        assertEquals(1, take(table)[0].getCount());
    }

    @Test public void departedForfeitedSeatHasNoRefundableWager() throws Exception {
        BlackjackTableTileEntity table = table(Phase.PLAYER_TURNS, false);
        reserve(table, 0, WagerState.CURRENT_ROUND, Items.EMERALD);
        Method forfeit = BlackjackTableTileEntity.class.getDeclaredMethod("forfeitSlot", int.class);
        forfeit.setAccessible(true);
        forfeit.invoke(table, 0);
        assertTrue(take(table)[0].isEmpty());
    }

    @Test public void repeatedRemovalCannotDuplicateRefundsAndPreservesDenominationAndTags() throws Exception {
        BlackjackTableTileEntity table = table(Phase.PLAYER_HIT, false);
        reserve(table, 0, WagerState.CURRENT_ROUND, Items.EMERALD);
        ItemStack original = ((ItemStack[]) get(table, "sessionBetStacks"))[0];
        original.setHoverName(new StringTextComponent("Named chip"));
        ItemStack refund = take(table)[0];
        assertEquals(1, refund.getCount());
        assertSame(Items.EMERALD, refund.getItem());
        assertTrue(ItemStack.tagMatches(original, refund));
        assertEquals(1, original.getCount());
        for (ItemStack stack : take(table)) assertTrue(stack.isEmpty());
        for (WagerState state : (WagerState[]) get(table, "wagerStates")) assertEquals(WagerState.NONE, state);
    }

    @Test public void savedSettlementAndConsumedRefundLedgerRetainTheirMeaningAfterLoad() throws Exception {
        Field registryValue = RegistryObject.class.getDeclaredField("value");
        registryValue.setAccessible(true);
        Object old5 = registryValue.get(ModItems.CHIP_5), old20 = registryValue.get(ModItems.CHIP_20);
        try {
            registryValue.set(ModItems.CHIP_5, Items.EMERALD);
            registryValue.set(ModItems.CHIP_20, Items.DIAMOND);
            BlackjackTableTileEntity table = table(Phase.CLEARING, true);
            reserve(table, 0, WagerState.CURRENT_ROUND, Items.EMERALD);
            reserve(table, 1, WagerState.NEXT_ROUND_PREPAID, Items.DIAMOND);
            BlackjackTableTileEntity restored = table(Phase.WAITING, false);
            restored.load(Blocks.AIR.defaultBlockState(), table.save(new CompoundNBT()));
            ItemStack[] refunds = take(restored);
            assertTrue(refunds[0].isEmpty());
            assertEquals(1, refunds[1].getCount());
            BlackjackTableTileEntity again = table(Phase.WAITING, false);
            again.load(Blocks.AIR.defaultBlockState(), restored.save(new CompoundNBT()));
            for (ItemStack stack : take(again)) assertTrue(stack.isEmpty());
        } finally {
            registryValue.set(ModItems.CHIP_5, old5);
            registryValue.set(ModItems.CHIP_20, old20);
        }
    }

    private BlackjackTableTileEntity table(Phase phase, boolean settled) throws Exception {
        BlackjackTableTileEntity table = new BlackjackTableTileEntity(TileEntityType.CHEST);
        set(table, "phase", phase);
        set(table, "roundSettlementApplied", settled);
        return table;
    }

    private void reserve(BlackjackTableTileEntity table, int slot, WagerState state, Item item) throws Exception {
        ((boolean[]) get(table, "activeSlots"))[slot] = true;
        ((boolean[]) get(table, "roundParticipants"))[slot] = state == WagerState.CURRENT_ROUND;
        ((UUID[]) get(table, "slotPlayers"))[slot] = new UUID(0, slot + 1);
        ((WagerState[]) get(table, "wagerStates"))[slot] = state;
        ((ItemStack[]) get(table, "sessionBetStacks"))[slot] = new ItemStack(item);
    }

    private ItemStack[] take(BlackjackTableTileEntity table) throws Exception {
        Method method = BlackjackTableTileEntity.class.getDeclaredMethod("takeBreakRefunds");
        method.setAccessible(true);
        return (ItemStack[]) method.invoke(table);
    }

    private Object get(BlackjackTableTileEntity table, String name) throws Exception {
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
