package com.example.betandcoin.client.model;

import com.example.betandcoin.BetAndCoin;
import com.example.betandcoin.tileentity.CasinoFountainTileEntity;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

import javax.annotation.Nonnull;

public class CasinoFountainModel
        extends AnimatedGeoModel<CasinoFountainTileEntity> {

    @Nonnull
    @Override
    public ResourceLocation getModelLocation(
            @Nonnull CasinoFountainTileEntity object
    ) {
        return new ResourceLocation(
                BetAndCoin.MOD_ID,
                "geo/casino_fountain.geo.json"
        );
    }

    @Nonnull
    @Override
    public ResourceLocation getTextureLocation(
            @Nonnull CasinoFountainTileEntity object
    ) {
        return new ResourceLocation(
                BetAndCoin.MOD_ID,
                "textures/block/casino_fountain.png"
        );
    }

    @Nonnull
    @Override
    public ResourceLocation getAnimationFileLocation(
            @Nonnull CasinoFountainTileEntity animatable
    ) {
        return new ResourceLocation(
                BetAndCoin.MOD_ID,
                "animations/casino_fountain.animation.json"
        );
    }
}
