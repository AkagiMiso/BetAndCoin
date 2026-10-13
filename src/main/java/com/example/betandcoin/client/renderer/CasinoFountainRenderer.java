package com.example.betandcoin.client.renderer;

import com.example.betandcoin.client.model.CasinoFountainBaseLayerModel;
import com.example.betandcoin.client.model.CasinoFountainModel;
import com.example.betandcoin.client.model.CasinoFountainWaterFallModel;
import com.example.betandcoin.client.model.CasinoFountainWaterLowerModel;
import com.example.betandcoin.client.model.CasinoFountainWaterUpperModel;
import com.example.betandcoin.tileentity.CasinoFountainTileEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

/**
 * 主体先正常渲染，再追加三个独立的透明水材质 pass。
 *
 * 水面、水帘使用从原始 geo 中拆出的独立几何文件，
 * 因此不会与主体贴图互相覆盖。
 */
public class CasinoFountainRenderer
        extends GeoBlockRenderer<CasinoFountainTileEntity> {

    private final GeoBlockRenderer<CasinoFountainTileEntity> middleBaseRenderer;
    private final GeoBlockRenderer<CasinoFountainTileEntity> upperBaseRenderer;
    private final CasinoFountainWaterRenderer lowerWaterRenderer;
    private final CasinoFountainWaterRenderer upperWaterRenderer;
    private final CasinoFountainWaterRenderer fallWaterRenderer;

    public CasinoFountainRenderer(
            TileEntityRendererDispatcher rendererDispatcher
    ) {
        super(
                rendererDispatcher,
                new CasinoFountainModel()
        );

        this.middleBaseRenderer = createBaseRenderer(rendererDispatcher, "middle");
        this.upperBaseRenderer = createBaseRenderer(rendererDispatcher, "upper");

        this.lowerWaterRenderer =
                new CasinoFountainWaterRenderer(
                        rendererDispatcher,
                        new CasinoFountainWaterLowerModel()
                );

        this.upperWaterRenderer =
                new CasinoFountainWaterRenderer(
                        rendererDispatcher,
                        new CasinoFountainWaterUpperModel()
                );

        this.fallWaterRenderer =
                new CasinoFountainWaterRenderer(
                        rendererDispatcher,
                        new CasinoFountainWaterFallModel()
                );
    }

    private static GeoBlockRenderer<CasinoFountainTileEntity> createBaseRenderer(
            TileEntityRendererDispatcher dispatcher, String layer) {
        return new GeoBlockRenderer<CasinoFountainTileEntity>(
                dispatcher, new CasinoFountainBaseLayerModel(layer)) {
            @Override
            public RenderType getRenderType(CasinoFountainTileEntity animatable,
                    float partialTicks, MatrixStack stack, IRenderTypeBuffer buffers,
                    IVertexBuilder builder, int packedLight, ResourceLocation texture) {
                return RenderType.entityCutoutNoCull(texture);
            }
        };
    }

    /**
     * 兼容当前项目 GeckoLib 3 / Forge 1.16.5 的六参数 TileEntity render。
     */
    @Override
    public void render(
            TileEntity tileEntity,
            float partialTicks,
            MatrixStack matrixStack,
            IRenderTypeBuffer buffer,
            int combinedLight,
            int combinedOverlay
    ) {
        super.render(
                tileEntity,
                partialTicks,
                matrixStack,
                buffer,
                combinedLight,
                combinedOverlay
        );

        if (!(tileEntity instanceof CasinoFountainTileEntity)) {
            return;
        }

        this.middleBaseRenderer.render(tileEntity, partialTicks, matrixStack,
                buffer, combinedLight, combinedOverlay);
        this.upperBaseRenderer.render(tileEntity, partialTicks, matrixStack,
                buffer, combinedLight, combinedOverlay);

        this.lowerWaterRenderer.render(
                tileEntity,
                partialTicks,
                matrixStack,
                buffer,
                combinedLight,
                combinedOverlay
        );

        this.upperWaterRenderer.render(
                tileEntity,
                partialTicks,
                matrixStack,
                buffer,
                combinedLight,
                combinedOverlay
        );

        this.fallWaterRenderer.render(
                tileEntity,
                partialTicks,
                matrixStack,
                buffer,
                combinedLight,
                combinedOverlay
        );
    }
}
