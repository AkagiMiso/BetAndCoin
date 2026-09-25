package com.example.betandcoin.client;

import com.example.betandcoin.BetAndCoin;
import com.example.betandcoin.client.renderer.BarNeonSignRenderer;
import com.example.betandcoin.client.renderer.BarTableClutterRenderer;
import com.example.betandcoin.client.renderer.BlackjackTableRenderer;
import com.example.betandcoin.client.renderer.CasinoFarkleTableRenderer;
import com.example.betandcoin.client.renderer.CasinoFountainRenderer;
import com.example.betandcoin.client.renderer.SlotMachineTileEntityRenderer;
import com.example.betandcoin.registry.ModBlocks;
import com.example.betandcoin.registry.ModTileEntities;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;


@Mod.EventBusSubscriber(
        modid = BetAndCoin.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class ClientModEvents {


    public static final ResourceLocation SIGN_OFF_MODEL =
            new ResourceLocation(
                    BetAndCoin.MOD_ID,
                    "block/slot_machine_sign_off"
            );


    public static final ResourceLocation SIGN_ON_MODEL =
            new ResourceLocation(
                    BetAndCoin.MOD_ID,
                    "block/slot_machine_sign_on"
            );


    public static final ResourceLocation REEL_MODEL =
            new ResourceLocation(
                    BetAndCoin.MOD_ID,
                    "block/slot_machine_reel"
            );


    public static final ResourceLocation LEVER_MODEL =
            new ResourceLocation(
                    BetAndCoin.MOD_ID,
                    "block/slot_machine_lever"
            );


    public static final ResourceLocation BAR_NEON_SIGN_GLOW_MODEL =
            new ResourceLocation(
                    BetAndCoin.MOD_ID,
                    "block/bar_neon_sign_glow"
            );


    public static final ResourceLocation[] BAR_TABLE_CLUTTER_MODELS =
            new ResourceLocation[8];


    public static final ResourceLocation[] BAR_TABLE_CLUTTER_OFF_MODELS =
            new ResourceLocation[8];


    public static final ResourceLocation[] BAR_TABLE_CLUTTER_GLASS_MODELS =
            new ResourceLocation[8];


    public static final ResourceLocation BAR_TABLE_CLUTTER_GLOW_MODEL =
            new ResourceLocation(
                    BetAndCoin.MOD_ID,
                    "block/bar_table_clutter_glow"
            );


    private ClientModEvents() {
    }


    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {

        ModelLoader.addSpecialModel(SIGN_OFF_MODEL);
        ModelLoader.addSpecialModel(SIGN_ON_MODEL);
        ModelLoader.addSpecialModel(REEL_MODEL);
        ModelLoader.addSpecialModel(LEVER_MODEL);

        ModelLoader.addSpecialModel(
                BAR_NEON_SIGN_GLOW_MODEL
        );


        for (int i = 0; i < 8; i++) {

            BAR_TABLE_CLUTTER_MODELS[i] =
                    new ResourceLocation(
                            BetAndCoin.MOD_ID,
                            "block/bar_table_clutter_" + i
                    );


            BAR_TABLE_CLUTTER_OFF_MODELS[i] =
                    new ResourceLocation(
                            BetAndCoin.MOD_ID,
                            "block/bar_table_clutter_" + i + "_off"
                    );


            ModelLoader.addSpecialModel(
                    BAR_TABLE_CLUTTER_MODELS[i]
            );

            ModelLoader.addSpecialModel(
                    BAR_TABLE_CLUTTER_OFF_MODELS[i]
            );
        }


        registerBarTableGlassModel(2);
        registerBarTableGlassModel(3);
        registerBarTableGlassModel(4);
        registerBarTableGlassModel(5);
        registerBarTableGlassModel(7);


        ModelLoader.addSpecialModel(
                BAR_TABLE_CLUTTER_GLOW_MODEL
        );
    }


    private static void registerBarTableGlassModel(int variant) {

        ResourceLocation model =
                new ResourceLocation(
                        BetAndCoin.MOD_ID,
                        "block/bar_table_clutter_"
                                + variant
                                + "_glass"
                );


        BAR_TABLE_CLUTTER_GLASS_MODELS[variant] = model;

        ModelLoader.addSpecialModel(model);
    }



    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {


        event.enqueueWork(() ->
                RenderTypeLookup.setRenderLayer(
                        ModBlocks.BAR_NEON_SIGN.get(),
                        RenderType.cutout()
                )
        );


        event.enqueueWork(() ->
                RenderTypeLookup.setRenderLayer(
                        ModBlocks.BAR_TABLE_CLUTTER.get(),
                        RenderType.cutout()
                )
        );


        ClientRegistry.bindTileEntityRenderer(
                ModTileEntities.SLOT_MACHINE.get(),
                SlotMachineTileEntityRenderer::new
        );


        ClientRegistry.bindTileEntityRenderer(
                ModTileEntities.BLACKJACK_TABLE.get(),
                BlackjackTableRenderer::new
        );


        ClientRegistry.bindTileEntityRenderer(
                ModTileEntities.CASINO_FOUNTAIN.get(),
                CasinoFountainRenderer::new
        );


        ClientRegistry.bindTileEntityRenderer(
                ModTileEntities.BAR_NEON_SIGN.get(),
                BarNeonSignRenderer::new
        );


        ClientRegistry.bindTileEntityRenderer(
                ModTileEntities.BAR_TABLE_CLUTTER.get(),
                BarTableClutterRenderer::new
        );


        /*
         * Farkle table renderer
         *
         * 只新增这一项，不影响已有 renderer。
         */
        ClientRegistry.bindTileEntityRenderer(
                ModTileEntities.CASINO_FARKLE_TABLE.get(),
                CasinoFarkleTableRenderer::new
        );
    }
}
