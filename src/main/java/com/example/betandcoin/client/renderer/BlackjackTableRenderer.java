package com.example.betandcoin.client.renderer;

import com.example.betandcoin.client.model.BlackjackTableModel;
import com.example.betandcoin.tileentity.BlackjackTableTileEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

public class BlackjackTableRenderer
        extends GeoBlockRenderer<BlackjackTableTileEntity> {

    private boolean renderLogged = false;

    public BlackjackTableRenderer(
            TileEntityRendererDispatcher rendererDispatcher
    ) {
        super(
                rendererDispatcher,
                new BlackjackTableModel()
        );

        System.out.println(
                "[BetAndCoin] BlackjackTableRenderer constructed"
        );
    }

    /**
     * GeckoLib 3.0.104 在 Forge 1.16.5 中实际覆盖的六参数方法，
     * 第一个参数是原始 TileEntity，而不是泛型类型。
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
        if (!this.renderLogged) {
            this.renderLogged = true;

            System.out.println(
                    "[BetAndCoin] BlackjackTableRenderer render called. "
                            + "Tile class: "
                            + tileEntity.getClass().getName()
                            + ", tile type: "
                            + tileEntity.getType().getRegistryName()
            );
        }

        super.render(
                tileEntity,
                partialTicks,
                matrixStack,
                buffer,
                combinedLight,
                combinedOverlay
        );
    }
}
