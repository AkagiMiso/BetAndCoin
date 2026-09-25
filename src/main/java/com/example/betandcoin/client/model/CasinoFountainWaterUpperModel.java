package com.example.betandcoin.client.model;

import com.example.betandcoin.BetAndCoin;
import com.example.betandcoin.tileentity.CasinoFountainTileEntity;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

public class CasinoFountainWaterUpperModel
        extends AbstractCasinoFountainWaterModel {

    @Nonnull
    @Override
    public ResourceLocation getModelLocation(
            @Nonnull CasinoFountainTileEntity object
    ) {
        return new ResourceLocation(
                BetAndCoin.MOD_ID,
                "geo/casino_fountain_water_upper.geo.json"
        );
    }

    @Nonnull
    @Override
    public ResourceLocation getTextureLocation(
            @Nonnull CasinoFountainTileEntity object
    ) {
        return texture(
                "casino_fountain_water_up_"
                        + getFrame(object)
                        + ".png"
        );
    }
}
