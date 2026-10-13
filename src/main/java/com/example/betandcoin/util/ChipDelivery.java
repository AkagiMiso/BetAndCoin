package com.example.betandcoin.util;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;

/** Shared online payout/refund delivery; never discard creative-mode overflow. */
public final class ChipDelivery {
    private ChipDelivery() {}

    public static void give(ServerPlayerEntity player, ItemStack amount) {
        ItemStack remaining = insert(player.inventory, amount);
        while (!remaining.isEmpty()) {
            player.drop(remaining.split(remaining.getMaxStackSize()), false);
        }
    }

    /** Uses vanilla slot selection and stack matching, without Inventory.add's creative discard. */
    public static ItemStack insert(PlayerInventory inventory, ItemStack amount) {
        ItemStack remaining = amount.copy();
        while (!remaining.isEmpty()) {
            int slot = inventory.getSlotWithRemainingSpace(remaining);
            if (slot < 0) slot = inventory.getFreeSlot();
            if (slot < 0) break;

            ItemStack existing = inventory.getItem(slot);
            int limit = Math.min(remaining.getMaxStackSize(), inventory.getMaxStackSize());
            int count = Math.min(remaining.getCount(), limit - existing.getCount());
            if (count <= 0) break;
            if (existing.isEmpty()) {
                existing = remaining.split(count);
                inventory.setItem(slot, existing);
            } else {
                existing.grow(count);
                remaining.shrink(count);
            }
            existing.setPopTime(5);
        }
        inventory.setChanged();
        return remaining;
    }
}
