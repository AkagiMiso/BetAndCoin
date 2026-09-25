package com.example.betandcoin.client.model;

import com.example.betandcoin.BetAndCoin;
import com.example.betandcoin.tileentity.BlackjackTableTileEntity;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

import javax.annotation.Nonnull;

public class BlackjackTableModel
        extends AnimatedGeoModel<BlackjackTableTileEntity> {

    @Nonnull
    @Override
    public ResourceLocation getModelLocation(
            @Nonnull BlackjackTableTileEntity object
    ) {
        return new ResourceLocation(
                BetAndCoin.MOD_ID,
                "geo/blackjack_table.geo.json"
        );
    }

    @Nonnull
    @Override
    public ResourceLocation getTextureLocation(
            @Nonnull BlackjackTableTileEntity object
    ) {
        return new ResourceLocation(
                BetAndCoin.MOD_ID,
                "textures/block/blackjack_table.png"
        );
    }

    @Nonnull
    @Override
    public ResourceLocation getAnimationFileLocation(
            @Nonnull BlackjackTableTileEntity animatable
    ) {
        /*
         * 当前牌桌没有动画，但 GeckoLib 3 的模型类仍需要返回一个路径。
         * 稍后我们会创建一个最小的空动画文件。
         */
        return new ResourceLocation(
                BetAndCoin.MOD_ID,
                "animations/blackjack_table.animation.json"
        );
    }
}