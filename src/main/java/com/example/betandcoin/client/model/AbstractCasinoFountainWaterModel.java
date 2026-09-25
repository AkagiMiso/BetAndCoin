package com.example.betandcoin.client.model;

import com.example.betandcoin.BetAndCoin;
import com.example.betandcoin.tileentity.CasinoFountainTileEntity;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

import javax.annotation.Nonnull;

public abstract class AbstractCasinoFountainWaterModel
        extends AnimatedGeoModel<CasinoFountainTileEntity> {

    protected int getFrame(
            @Nonnull CasinoFountainTileEntity object
    ) {
        if (object.getLevel() == null) {
            return 0;
        }

        /*
         * 4 tick/帧，5 帧一轮：
         * 20 tick = 1 秒完整循环。
         */
        return (int) (
                (object.getLevel().getGameTime() / 4L)
                        % 5L
        );
    }

    @Nonnull
    protected ResourceLocation texture(
            @Nonnull String fileName
    ) {
        return new ResourceLocation(
                BetAndCoin.MOD_ID,
                "textures/block/" + fileName
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
