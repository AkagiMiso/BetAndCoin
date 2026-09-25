package com.example.betandcoin.registry;

import com.example.betandcoin.BetAndCoin;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(
                    ForgeRegistries.SOUND_EVENTS,
                    BetAndCoin.MOD_ID
            );

    public static final RegistryObject<SoundEvent> LEVER_PULL =
            SOUNDS.register(
                    "lever_pull",
                    () -> new SoundEvent(
                            new ResourceLocation(
                                    BetAndCoin.MOD_ID,
                                    "lever_pull"
                            )
                    )
            );

    public static final RegistryObject<SoundEvent> REEL_SPIN =
            SOUNDS.register(
                    "reel_spin",
                    () -> new SoundEvent(
                            new ResourceLocation(
                                    BetAndCoin.MOD_ID,
                                    "reel_spin"
                            )
                    )
            );

    public static final RegistryObject<SoundEvent> WIN =
            SOUNDS.register(
                    "win",
                    () -> new SoundEvent(
                            new ResourceLocation(
                                    BetAndCoin.MOD_ID,
                                    "win"
                            )
                    )
            );

    public static final RegistryObject<SoundEvent>
            BAR_NEON_POWER_STABILIZE =
            SOUNDS.register(
                    "bar_neon_power_stabilize",
                    () -> new SoundEvent(
                            new ResourceLocation(
                                    BetAndCoin.MOD_ID,
                                    "bar_neon_power_stabilize"
                            )
                    )
            );

    private ModSounds() {
    }

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}
