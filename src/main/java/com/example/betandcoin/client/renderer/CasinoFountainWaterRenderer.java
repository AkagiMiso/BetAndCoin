package com.example.betandcoin.client.renderer;

import com.example.betandcoin.tileentity.CasinoFountainTileEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

/**
 * 只负责水面/水帘的额外半透明 GeckoLib pass。
 */
public class CasinoFountainWaterRenderer
        extends GeoBlockRenderer<CasinoFountainTileEntity> {

    public CasinoFountainWaterRenderer(
            TileEntityRendererDispatcher rendererDispatcher,
            AnimatedGeoModel<CasinoFountainTileEntity> model
    ) {
        super(rendererDispatcher, model);
    }

    @Override
    public RenderType getRenderType(
            CasinoFountainTileEntity animatable,
            float partialTicks,
            MatrixStack stack,
            IRenderTypeBuffer renderTypeBuffer,
            IVertexBuilder vertexBuilder,
            int packedLightIn,
            ResourceLocation textureLocation
    ) {
        return RenderType.entityTranslucent(textureLocation);
    }
}
