package com.example.betandcoin;

import com.example.betandcoin.network.ModNetwork;
import com.example.betandcoin.registry.ModBlocks;
import com.example.betandcoin.registry.ModEntityTypes;
import com.example.betandcoin.registry.ModItems;
import com.example.betandcoin.registry.ModSounds;
import com.example.betandcoin.registry.ModTileEntities;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import software.bernie.geckolib3.GeckoLib;

@Mod(BetAndCoin.MOD_ID)
public class BetAndCoin {

    public static final String MOD_ID = "bet_and_coin";

    public BetAndCoin() {
        GeckoLib.initialize();

        IEventBus modEventBus =
                FMLJavaModLoadingContext.get()
                        .getModEventBus();

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModSounds.register(modEventBus);
        ModTileEntities.register(modEventBus);
        ModEntityTypes.register(modEventBus);

        ModNetwork.register();
    }
}