package com.example.betandcoin.registry;

import com.example.betandcoin.BetAndCoin;
import com.example.betandcoin.block.BarNeonSignBlock;
import com.example.betandcoin.block.BarTableClutterBlock;
import com.example.betandcoin.block.BlackjackTableBlock;
import com.example.betandcoin.block.BlackjackTablePartBlock;
import com.example.betandcoin.block.CardDeckBlock;
import com.example.betandcoin.block.CasinoFarkleTableBlock;
import com.example.betandcoin.block.CasinoFountainBlock;
import com.example.betandcoin.block.CasinoFountainPartBlock;
import com.example.betandcoin.block.SlotMachineBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(
                    ForgeRegistries.BLOCKS,
                    BetAndCoin.MOD_ID
            );

    public static final RegistryObject<Block> SLOT_MACHINE =
            BLOCKS.register("slot_machine",
                    () -> new SlotMachineBlock(AbstractBlock.Properties.of(Material.METAL)
                            .strength(3.5F, 6.0F).sound(SoundType.METAL).noOcclusion()));

    public static final RegistryObject<Block> CARD_DECK =
            BLOCKS.register("card_deck",
                    () -> new CardDeckBlock(AbstractBlock.Properties.of(Material.WOOD)
                            .strength(0.8F).sound(SoundType.WOOD).noOcclusion()));

    public static final RegistryObject<Block> BLACKJACK_TABLE =
            BLOCKS.register("blackjack_table",
                    () -> new BlackjackTableBlock(AbstractBlock.Properties.of(Material.WOOD)
                            .strength(2.5F, 6.0F).sound(SoundType.WOOD).noOcclusion()));

    public static final RegistryObject<Block> BLACKJACK_TABLE_PART =
            BLOCKS.register("blackjack_table_part",
                    () -> new BlackjackTablePartBlock(AbstractBlock.Properties.of(Material.WOOD)
                            .strength(2.5F, 6.0F).sound(SoundType.WOOD).noOcclusion().noDrops()));

    public static final RegistryObject<Block> CASINO_FOUNTAIN =
            BLOCKS.register("casino_fountain",
                    () -> new CasinoFountainBlock(AbstractBlock.Properties.of(Material.STONE)
                            .strength(3.0F, 6.0F).sound(SoundType.STONE).noOcclusion()));

    public static final RegistryObject<Block> CASINO_FOUNTAIN_PART =
            BLOCKS.register("casino_fountain_part",
                    () -> new CasinoFountainPartBlock(AbstractBlock.Properties.of(Material.STONE)
                            .strength(3.0F, 6.0F).sound(SoundType.STONE).noOcclusion().noDrops()));

    public static final RegistryObject<Block> BAR_NEON_SIGN =
            BLOCKS.register("bar_neon_sign",
                    () -> new BarNeonSignBlock(AbstractBlock.Properties.of(Material.METAL)
                            .strength(1.5F, 3.0F).sound(SoundType.METAL).noOcclusion()
                            .lightLevel(state -> state.getValue(BarNeonSignBlock.LIT) ? 12 : 0)));

    public static final RegistryObject<Block> BAR_TABLE_CLUTTER =
            BLOCKS.register("bar_table_clutter",
                    () -> new BarTableClutterBlock(AbstractBlock.Properties.of(Material.WOOD)
                            .strength(1.0F).sound(SoundType.WOOD).noOcclusion()
                            .lightLevel(state -> state.getValue(BarTableClutterBlock.LIT) ? 10 : 0)));

    // Farkle table
    public static final RegistryObject<Block> CASINO_FARKLE_TABLE =
            BLOCKS.register("casino_farkle_table",
                    () -> new CasinoFarkleTableBlock(AbstractBlock.Properties.of(Material.WOOD)
                            .strength(2.5F, 6.0F).sound(SoundType.WOOD).noOcclusion()));

    private ModBlocks() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
