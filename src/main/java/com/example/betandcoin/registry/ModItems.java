package com.example.betandcoin.registry;

import com.example.betandcoin.BetAndCoin;
import com.example.betandcoin.item.CasinoFarkleTableItem;
import com.example.betandcoin.item.PlayingCardTestItem;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;


public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(
                    ForgeRegistries.ITEMS,
                    BetAndCoin.MOD_ID
            );


    public static final RegistryObject<Item> SLOT_MACHINE =
            ITEMS.register(
                    "slot_machine",
                    () -> new BlockItem(
                            ModBlocks.SLOT_MACHINE.get(),
                            new Item.Properties()
                                    .tab(ModItemGroups.BET_AND_COIN)
                    )
            );


    public static final RegistryObject<Item> CARD_DECK =
            ITEMS.register(
                    "card_deck",
                    () -> new BlockItem(
                            ModBlocks.CARD_DECK.get(),
                            new Item.Properties()
                                    .stacksTo(64)
                    )
            );


    public static final RegistryObject<Item> BLACKJACK_TABLE =
            ITEMS.register(
                    "blackjack_table",
                    () -> new BlockItem(
                            ModBlocks.BLACKJACK_TABLE.get(),
                            new Item.Properties()
                                    .tab(ModItemGroups.BET_AND_COIN)
                                    .stacksTo(1)
                    )
            );


    public static final RegistryObject<Item> CASINO_FOUNTAIN =
            ITEMS.register(
                    "casino_fountain",
                    () -> new BlockItem(
                            ModBlocks.CASINO_FOUNTAIN.get(),
                            new Item.Properties()
                                    .tab(ModItemGroups.BET_AND_COIN)
                                    .stacksTo(1)
                    )
            );


    public static final RegistryObject<Item> BAR_NEON_SIGN =
            ITEMS.register(
                    "bar_neon_sign",
                    () -> new BlockItem(
                            ModBlocks.BAR_NEON_SIGN.get(),
                            new Item.Properties()
                                    .tab(ModItemGroups.BET_AND_COIN)
                                    .stacksTo(16)
                    )
            );

    public static final RegistryObject<Item> RECEPTION_PODIUM =
            ITEMS.register("reception_podium",
                    () -> new BlockItem(ModBlocks.RECEPTION_PODIUM.get(),
                            new Item.Properties().tab(ModItemGroups.BET_AND_COIN).stacksTo(16)));


    public static final RegistryObject<Item> CHIP_5 =
            ITEMS.register(
                    "chip_5",
                    () -> new Item(
                            new Item.Properties()
                                    .tab(ModItemGroups.BET_AND_COIN)
                                    .stacksTo(64)
                    )
            );


    public static final RegistryObject<Item> CHIP_20 =
            ITEMS.register(
                    "chip_20",
                    () -> new Item(
                            new Item.Properties()
                                    .tab(ModItemGroups.BET_AND_COIN)
                                    .stacksTo(64)
                    )
            );


    public static final RegistryObject<Item> PLAYING_CARD_TEST =
            ITEMS.register(
                    "playing_card_test",
                    () -> new PlayingCardTestItem(
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );


    public static final RegistryObject<Item> BAR_TABLE_CLUTTER =
            ITEMS.register(
                    "bar_table_clutter",
                    () -> new BlockItem(
                            ModBlocks.BAR_TABLE_CLUTTER.get(),
                            new Item.Properties()
                                    .tab(ModItemGroups.BET_AND_COIN)
                                    .stacksTo(1)
                    )
            );


    public static final RegistryObject<Item> CASINO_FARKLE_TABLE =
            ITEMS.register(
                    "casino_farkle_table",
                    () -> new CasinoFarkleTableItem(
                            ModBlocks.CASINO_FARKLE_TABLE.get(),
                            new Item.Properties()
                                    .tab(ModItemGroups.BET_AND_COIN)
                                    .stacksTo(1)
                    )
            );


    public static final RegistryObject<Item> VELVET_BARRIER =
            ITEMS.register("velvet_barrier",
                    () -> new BlockItem(ModBlocks.VELVET_BARRIER.get(),
                            new Item.Properties().tab(ModItemGroups.BET_AND_COIN).stacksTo(64)));

    public static final RegistryObject<Item> ROULETTE_TABLE =
            ITEMS.register("roulette_table", () -> new BlockItem(ModBlocks.ROULETTE_TABLE.get(),
                    new Item.Properties().tab(ModItemGroups.BET_AND_COIN).stacksTo(1)));

    private ModItems() {
    }


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
