package com.example.betandcoin.client.model;

import com.example.betandcoin.BetAndCoin;
import com.example.betandcoin.item.CasinoFarkleTableItem;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

/** The inventory and held item use the same main geometry and texture as the table. */
public class CasinoFarkleTableItemModel extends AnimatedGeoModel<CasinoFarkleTableItem> {
    private static final ResourceLocation MODEL = new ResourceLocation(
            BetAndCoin.MOD_ID, "geo/casino_farkle_table_main.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            BetAndCoin.MOD_ID, "textures/block/casino_farkle_table_main.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            BetAndCoin.MOD_ID, "animations/casino_farkle_table.animation.json");

    @Override
    public ResourceLocation getModelLocation(CasinoFarkleTableItem object) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(CasinoFarkleTableItem object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(CasinoFarkleTableItem object) {
        return ANIMATION;
    }
}
