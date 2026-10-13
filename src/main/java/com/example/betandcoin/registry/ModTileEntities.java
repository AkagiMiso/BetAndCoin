package com.example.betandcoin.registry;

import com.example.betandcoin.BetAndCoin;
import com.example.betandcoin.tileentity.RouletteTableTileEntity;
import com.example.betandcoin.tileentity.BarNeonSignTileEntity;
import com.example.betandcoin.tileentity.BarTableClutterTileEntity;
import com.example.betandcoin.tileentity.BlackjackTableTileEntity;
import com.example.betandcoin.tileentity.CardDeckTileEntity;
import com.example.betandcoin.tileentity.CasinoFarkleTableTileEntity;
import com.example.betandcoin.tileentity.CasinoFountainTileEntity;
import com.example.betandcoin.tileentity.SlotMachineTileEntity;
import com.example.betandcoin.tileentity.ReceptionPodiumTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModTileEntities {

    public static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES =
            DeferredRegister.create(
                    ForgeRegistries.TILE_ENTITIES,
                    BetAndCoin.MOD_ID
            );

    public static final RegistryObject<TileEntityType<SlotMachineTileEntity>> SLOT_MACHINE =
            TILE_ENTITIES.register("slot_machine",
                    () -> TileEntityType.Builder.of(
                            SlotMachineTileEntity::new,
                            ModBlocks.SLOT_MACHINE.get()
                    ).build(null));

    public static final RegistryObject<TileEntityType<CardDeckTileEntity>> CARD_DECK =
            TILE_ENTITIES.register("card_deck",
                    () -> TileEntityType.Builder.of(
                            CardDeckTileEntity::new,
                            ModBlocks.CARD_DECK.get()
                    ).build(null));

    public static final RegistryObject<TileEntityType<BlackjackTableTileEntity>> BLACKJACK_TABLE =
            TILE_ENTITIES.register("blackjack_table",
                    () -> TileEntityType.Builder.of(
                            () -> new BlackjackTableTileEntity(
                                    ModTileEntities.BLACKJACK_TABLE.get()
                            ),
                            ModBlocks.BLACKJACK_TABLE.get()
                    ).build(null));

    public static final RegistryObject<TileEntityType<CasinoFountainTileEntity>> CASINO_FOUNTAIN =
            TILE_ENTITIES.register("casino_fountain",
                    () -> TileEntityType.Builder.of(
                            () -> new CasinoFountainTileEntity(
                                    ModTileEntities.CASINO_FOUNTAIN.get()
                            ),
                            ModBlocks.CASINO_FOUNTAIN.get()
                    ).build(null));

    public static final RegistryObject<TileEntityType<BarNeonSignTileEntity>> BAR_NEON_SIGN =
            TILE_ENTITIES.register("bar_neon_sign",
                    () -> TileEntityType.Builder.of(
                            BarNeonSignTileEntity::new,
                            ModBlocks.BAR_NEON_SIGN.get()
                    ).build(null));

    public static final RegistryObject<TileEntityType<ReceptionPodiumTileEntity>> RECEPTION_PODIUM =
            TILE_ENTITIES.register("reception_podium",
                    () -> TileEntityType.Builder.of(ReceptionPodiumTileEntity::new,
                            ModBlocks.RECEPTION_PODIUM.get()).build(null));

    public static final RegistryObject<TileEntityType<BarTableClutterTileEntity>> BAR_TABLE_CLUTTER =
            TILE_ENTITIES.register("bar_table_clutter",
                    () -> TileEntityType.Builder.of(
                            BarTableClutterTileEntity::new,
                            ModBlocks.BAR_TABLE_CLUTTER.get()
                    ).build(null));

    // Farkle table
    public static final RegistryObject<TileEntityType<CasinoFarkleTableTileEntity>> CASINO_FARKLE_TABLE =
            TILE_ENTITIES.register("casino_farkle_table",
                    () -> TileEntityType.Builder.of(
                            CasinoFarkleTableTileEntity::new,
                            ModBlocks.CASINO_FARKLE_TABLE.get()
                    ).build(null));

    public static final RegistryObject<TileEntityType<RouletteTableTileEntity>> ROULETTE_TABLE =
            TILE_ENTITIES.register("roulette_table", () -> TileEntityType.Builder.of(
                    RouletteTableTileEntity::new, ModBlocks.ROULETTE_TABLE.get()).build(null));

    private ModTileEntities() {
    }

    public static void register(IEventBus eventBus) {
        TILE_ENTITIES.register(eventBus);
    }
}
