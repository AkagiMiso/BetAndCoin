package com.example.betandcoin.registry;

import com.example.betandcoin.BetAndCoin;
import com.example.betandcoin.entity.BlackjackChipEntity;
import com.example.betandcoin.entity.PlayingCardEntity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(
                    ForgeRegistries.ENTITIES,
                    BetAndCoin.MOD_ID
            );

    public static final RegistryObject<EntityType<PlayingCardEntity>>
            PLAYING_CARD =
            ENTITY_TYPES.register(
                    "playing_card",
                    () -> EntityType.Builder
                            .<PlayingCardEntity>of(
                                    PlayingCardEntity::new,
                                    EntityClassification.MISC
                            )
                            .sized(0.75F, 0.05F)
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build(
                                    BetAndCoin.MOD_ID + ":playing_card"
                            )
            );

    public static final RegistryObject<EntityType<BlackjackChipEntity>>
            BLACKJACK_CHIP =
            ENTITY_TYPES.register(
                    "blackjack_chip",
                    () -> EntityType.Builder
                            .<BlackjackChipEntity>of(
                                    BlackjackChipEntity::new,
                                    EntityClassification.MISC
                            )
                            .sized(0.5F, 0.125F)
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build(
                                    BetAndCoin.MOD_ID
                                            + ":blackjack_chip"
                            )
            );

    private ModEntityTypes() {
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}