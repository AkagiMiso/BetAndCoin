package com.example.betandcoin.util;

import com.mojang.authlib.GameProfile;
import com.example.betandcoin.tileentity.BlackjackTableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import net.minecraft.util.registry.Bootstrap;
import net.minecraft.util.text.StringTextComponent;
import org.junit.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import static org.junit.Assert.*;

/** Exercise real vanilla inventory slots; capture drops without starting a server. */
public class ChipDeliveryTest {
    static { Bootstrap.bootStrap(); }

    private static class Player extends ServerPlayerEntity {
        List<ItemStack> drops;
        private Player() { super(null, null, new GameProfile(UUID.randomUUID(), "audit"), null); }
        @Override public ItemEntity drop(ItemStack stack, boolean random) {
            drops.add(stack.copy());
            return null;
        }
    }

    private Player player(boolean creative, boolean full) throws Exception {
        // Skip the server constructor; only inventory and the captured drop method are used.
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Player player = (Player) ((Unsafe) unsafeField.get(null)).allocateInstance(Player.class);
        PlayerAbilities abilities = new PlayerAbilities();
        abilities.instabuild = creative;
        set(player, "abilities", abilities);
        set(player, "inventory", new PlayerInventory(player));
        player.drops = new ArrayList<>();
        if (full) for (int i = 0; i < 36; i++) player.inventory.items.set(i, new ItemStack(Items.STONE, 64));
        return player;
    }

    private void set(Player player, String name, Object value) throws Exception {
        Field field = PlayerEntity.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(player, value);
    }

    @Test public void fullCreativeInventoryDropsTheEntireReward() throws Exception {
        Player player = player(true, true);
        ChipDelivery.give(player, new ItemStack(Items.EMERALD, 2));
        assertEquals(1, player.drops.size());
        assertEquals(2, player.drops.get(0).getCount());
        assertEquals(0, count(player.inventory, Items.EMERALD));
    }

    @Test public void fullSurvivalInventoryDropsTheRefund() throws Exception {
        Player player = player(false, true);
        ChipDelivery.give(player, new ItemStack(Items.EMERALD, 1));
        assertEquals(1, player.drops.get(0).getCount());
        assertEquals(0, count(player.inventory, Items.EMERALD));
    }

    @Test public void partialStackReceivesOnlyWhatFitsAndDropsTheRest() throws Exception {
        Player player = player(true, true);
        player.inventory.items.set(5, new ItemStack(Items.EMERALD, 63));
        ChipDelivery.give(player, new ItemStack(Items.EMERALD, 5));
        assertEquals(64, player.inventory.getItem(5).getCount());
        assertEquals(4, player.drops.get(0).getCount());
    }

    @Test public void emptySlotsReceiveLargePaymentWithoutOverstackingOrChangingLedger() throws Exception {
        Player player = player(true, false);
        ItemStack ledger = new ItemStack(Items.EMERALD, 130);
        ChipDelivery.give(player, ledger);
        assertEquals(130, ledger.getCount());
        assertEquals(130, count(player.inventory, Items.EMERALD));
        assertEquals(64, player.inventory.getItem(0).getCount());
        assertEquals(64, player.inventory.getItem(1).getCount());
        assertEquals(2, player.inventory.getItem(2).getCount());
        assertTrue(player.drops.isEmpty());
    }

    @Test public void largeOverflowSplitsIntoLegalDroppedStacks() throws Exception {
        Player player = player(true, true);
        ChipDelivery.give(player, new ItemStack(Items.EMERALD, 130));
        assertEquals(3, player.drops.size());
        assertEquals(64, player.drops.get(0).getCount());
        assertEquals(64, player.drops.get(1).getCount());
        assertEquals(2, player.drops.get(2).getCount());
    }

    @Test public void differentNamesDoNotMergeAndDroppedMetadataIsPreserved() throws Exception {
        Player player = player(true, true);
        ItemStack existing = new ItemStack(Items.EMERALD, 63);
        existing.setHoverName(new StringTextComponent("A"));
        player.inventory.items.set(0, existing);
        ItemStack payment = new ItemStack(Items.EMERALD, 3);
        payment.setHoverName(new StringTextComponent("B"));
        ChipDelivery.give(player, payment);
        assertEquals(63, existing.getCount());
        assertEquals(3, player.drops.get(0).getCount());
        assertTrue(ItemStack.tagMatches(payment, player.drops.get(0)));
    }

    @Test public void matchingOffhandStackIsFilledBeforeDroppingOverflow() throws Exception {
        Player player = player(true, true);
        player.inventory.offhand.set(0, new ItemStack(Items.EMERALD, 62));
        ChipDelivery.give(player, new ItemStack(Items.EMERALD, 5));
        assertEquals(64, player.inventory.offhand.get(0).getCount());
        assertEquals(3, player.drops.get(0).getCount());
    }

    private int count(PlayerInventory inventory, Item item) {
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() == item) count += stack.getCount();
        }
        return count;
    }

    @Test public void logoutRefundsUnstartedAndPrepaidWagersExactlyOnce() throws Exception {
        for (BlackjackTableTileEntity.WagerState wager : new BlackjackTableTileEntity.WagerState[] {
                BlackjackTableTileEntity.WagerState.CURRENT_ROUND,
                BlackjackTableTileEntity.WagerState.NEXT_ROUND_PREPAID}) {
            Player player = player(true, false);
            BlackjackTableTileEntity table = new BlackjackTableTileEntity(TileEntityType.CHEST);
            Field active = BlackjackTableTileEntity.class.getDeclaredField("activeSlots");
            Field stacks = BlackjackTableTileEntity.class.getDeclaredField("sessionBetStacks");
            Field states = BlackjackTableTileEntity.class.getDeclaredField("wagerStates");
            Field phase = BlackjackTableTileEntity.class.getDeclaredField("phase");
            for (Field field : new Field[] {active, stacks, states, phase}) field.setAccessible(true);
            ((boolean[]) active.get(table))[0] = true;
            ((ItemStack[]) stacks.get(table))[0] = new ItemStack(Items.EMERALD);
            ((BlackjackTableTileEntity.WagerState[]) states.get(table))[0] = wager;
            phase.set(table, wager == BlackjackTableTileEntity.WagerState.CURRENT_ROUND
                    ? BlackjackTableTileEntity.Phase.FANCY_INTRO : BlackjackTableTileEntity.Phase.PLAYER_HIT);
            Method leave = BlackjackTableTileEntity.class.getDeclaredMethod("handleSlotDeparture", int.class, ServerPlayerEntity.class);
            leave.setAccessible(true);
            leave.invoke(table, 0, player);
            leave.invoke(table, 0, player);
            assertEquals(1, count(player.inventory, Items.EMERALD));
            assertTrue(player.drops.isEmpty());
            assertFalse(table.isSlotActive(0));
            assertNull(table.getSlotPlayer(0));
            assertTrue(((ItemStack[]) stacks.get(table))[0].isEmpty());
            assertEquals(BlackjackTableTileEntity.WagerState.NONE,
                    ((BlackjackTableTileEntity.WagerState[]) states.get(table))[0]);
        }
    }
}
