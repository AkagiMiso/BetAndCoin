package com.example.betandcoin.client.model;

import com.example.betandcoin.BetAndCoin;
import com.example.betandcoin.tileentity.CasinoFarkleTableTileEntity;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.model.AnimatedGeoModel;

/** One 64x64 die model is drawn six times by the table renderer. */
public class CasinoFarkleDieModel extends AnimatedGeoModel<CasinoFarkleTableTileEntity> {
    @Override
    public ResourceLocation getModelLocation(CasinoFarkleTableTileEntity object) {
        return new ResourceLocation(BetAndCoin.MOD_ID, "geo/casino_farkle_die.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(CasinoFarkleTableTileEntity object) {
        return new ResourceLocation(BetAndCoin.MOD_ID,
                "textures/block/casino_farkle_die.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(CasinoFarkleTableTileEntity object) {
        return new ResourceLocation(BetAndCoin.MOD_ID,
                "animations/casino_farkle_table.animation.json");
    }

    @Override
    public void setCustomAnimations(CasinoFarkleTableTileEntity object,
                                    int instanceId, AnimationEvent animationEvent) {
        // Dice orientation is set for each render, not by the table arm controller.
    }
}
