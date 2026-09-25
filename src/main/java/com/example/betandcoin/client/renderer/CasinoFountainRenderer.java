package com.example.betandcoin.client.renderer;

import com.example.betandcoin.client.model.CasinoFountainModel;
import com.example.betandcoin.client.model.CasinoFountainWaterFallModel;
import com.example.betandcoin.client.model.CasinoFountainWaterLowerModel;
import com.example.betandcoin.client.model.CasinoFountainWaterUpperModel;
import com.example.betandcoin.tileentity.CasinoFountainTileEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

/**
 * 主体先正常渲染，再追加三个独立的透明水材质 pass。
 *
 * 水面、水帘使用从原始 geo 中拆出的独立几何文件，
 * 因此不会与主体贴图互相覆盖。
 */
public class CasinoFountainRenderer
        extends GeoBlockRenderer<CasinoFountainTileEntity> {

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
