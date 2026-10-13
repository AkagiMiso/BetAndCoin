package com.example.betandcoin.client.model;

import com.example.betandcoin.BetAndCoin;
import com.example.betandcoin.tileentity.CasinoFarkleTableTileEntity;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

/** The mechanical assembly has its own 2048x2048 UV space. */
public class CasinoFarkleTablePartModel
        extends AnimatedGeoModel<CasinoFarkleTableTileEntity> {

    @Override
    public ResourceLocation getModelLocation(CasinoFarkleTableTileEntity object) {
        return new ResourceLocation(BetAndCoin.MOD_ID,
                "geo/casino_farkle_table_part.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(CasinoFarkleTableTileEntity object) {
        return new ResourceLocation(BetAndCoin.MOD_ID,
                "textures/block/casino_farkle_table_part.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(CasinoFarkleTableTileEntity object) {
        return new ResourceLocation(BetAndCoin.MOD_ID,
                "animations/casino_farkle_table.animation.json");
    }
}
