package com.example.betandcoin.client.model;

import com.example.betandcoin.BetAndCoin;
import com.example.betandcoin.tileentity.CasinoFarkleTableTileEntity;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.model.AnimatedGeoModel;

/** Independent 2048x2048 seven-segment glow geometry. */
public class CasinoFarkleTableSegmentModel
        extends AnimatedGeoModel<CasinoFarkleTableTileEntity> {
    @Override
    public ResourceLocation getModelLocation(CasinoFarkleTableTileEntity object) {
        return new ResourceLocation(BetAndCoin.MOD_ID,
                "geo/casino_farkle_table_segment.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(CasinoFarkleTableTileEntity object) {
        return new ResourceLocation(BetAndCoin.MOD_ID,
                "textures/block/casino_farkle_table_segment.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(CasinoFarkleTableTileEntity object) {
        return new ResourceLocation(BetAndCoin.MOD_ID,
                "animations/casino_farkle_table.animation.json");
    }

    @Override
    public void setCustomAnimations(CasinoFarkleTableTileEntity object,
                                    int instanceId, AnimationEvent animationEvent) {
        // Bone visibility and parent angle are controlled in the segment pass.
    }
}
