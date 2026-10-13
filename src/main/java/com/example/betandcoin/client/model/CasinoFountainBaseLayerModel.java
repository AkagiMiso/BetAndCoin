package com.example.betandcoin.client.model;

import com.example.betandcoin.BetAndCoin;
import com.example.betandcoin.tileentity.CasinoFountainTileEntity;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

/** Static base layers keep the authored cubes and UVs while rendering separately. */
public class CasinoFountainBaseLayerModel extends AnimatedGeoModel<CasinoFountainTileEntity> {
    private final ResourceLocation modelLocation;
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            BetAndCoin.MOD_ID, "textures/block/casino_fountain.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            BetAndCoin.MOD_ID, "animations/casino_fountain.animation.json");

    public CasinoFountainBaseLayerModel(String layer) {
        modelLocation = new ResourceLocation(BetAndCoin.MOD_ID,
                "geo/casino_fountain_base_" + layer + ".geo.json");
    }

    @Override
    public ResourceLocation getModelLocation(CasinoFountainTileEntity object) {
        return modelLocation;
    }

    @Override
    public ResourceLocation getTextureLocation(CasinoFountainTileEntity object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(CasinoFountainTileEntity object) {
        return ANIMATION;
    }
}
