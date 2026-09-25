package com.example.betandcoin.client.event;

import com.example.betandcoin.BetAndCoin;
import com.example.betandcoin.client.renderer.BlackjackChipRenderer;
import com.example.betandcoin.client.renderer.PlayingCardRenderer;
import com.example.betandcoin.registry.ModEntityTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = BetAndCoin.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class ClientEventHandler {

    private ClientEventHandler() {
    }

    @SubscribeEvent
    public static void registerEntityRenderers(
            ModelRegistryEvent event
    ) {
        RenderingRegistry.registerEntityRenderingHandler(
                ModEntityTypes.PLAYING_CARD.get(),
                PlayingCardRenderer::new
        );

        RenderingRegistry.registerEntityRenderingHandler(
                ModEntityTypes.BLACKJACK_CHIP.get(),
                BlackjackChipRenderer::new
        );
    }
}
