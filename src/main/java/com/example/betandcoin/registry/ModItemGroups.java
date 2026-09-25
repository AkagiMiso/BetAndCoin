package com.example.betandcoin.registry;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;

public final class ModItemGroups {

    public static final ItemGroup BET_AND_COIN =
            new ItemGroup("bet_and_coin") {

                @Override
                public ItemStack makeIcon() {
                    /*
                     * 使用 5 面额筹码作为标签页图标。
                     */
                    return new ItemStack(ModItems.CHIP_5.get());
                }
            };

    private ModItemGroups() {
    }
}