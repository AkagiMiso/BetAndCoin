package com.example.betandcoin.client.renderer;

import com.example.betandcoin.block.CasinoFarkleTableBlock;
import com.example.betandcoin.client.model.CasinoFarkleTableModel;
import com.example.betandcoin.client.model.CasinoFarkleTablePartModel;
import com.example.betandcoin.tileentity.CasinoFarkleTableTileEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.vector.Vector3f;
import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

/** Each model uses its own texture pass and native UV pixel density. */
public class CasinoFarkleTableRenderer
        extends GeoBlockRenderer<CasinoFarkleTableTileEntity> {
    private static final float STAKE_SCALE = 0.48f;
    // Both chip models span Y=0..1 and use GROUND translation Y=2, scale=0.3.
    // ItemRenderer centers the model at -0.5 before applying that transform.
    // Compensate its bottom offset so the first chip rests on the Y=15 felt.
    private static final double STAKE_BASE_Y =
            15.0 / 16.0 - STAKE_SCALE * (2.0 / 16.0 - 0.5 * 0.3);
    private static final double STAKE_LAYER_HEIGHT = STAKE_SCALE * 0.3 / 16.0;
    private final GeoBlockRenderer<CasinoFarkleTableTileEntity> mainRenderer;
    private final CasinoFarkleSegmentRenderer segmentRenderer;
    private final CasinoFarkleDieRenderer dieRenderer;

    public CasinoFarkleTableRenderer(TileEntityRendererDispatcher dispatcher) {
        // The arm controller must resolve against part, which owns its animated bones.
        super(dispatcher, new CasinoFarkleTablePartModel());
        mainRenderer = new GeoBlockRenderer<CasinoFarkleTableTileEntity>(
                dispatcher, new CasinoFarkleTableModel()) {};
        segmentRenderer = new CasinoFarkleSegmentRenderer(dispatcher);
        dieRenderer = new CasinoFarkleDieRenderer(dispatcher);
    }

    @Override
    public void render(TileEntity tileEntity, float partialTicks,
                       MatrixStack matrixStack, IRenderTypeBuffer buffer,
                       int combinedLight, int combinedOverlay) {
        if (!(tileEntity instanceof CasinoFarkleTableTileEntity)) return;
        CasinoFarkleTableTileEntity table = (CasinoFarkleTableTileEntity) tileEntity;
        mainRenderer.render(tileEntity, partialTicks, matrixStack, buffer,
                combinedLight, combinedOverlay);
        super.render(tileEntity, partialTicks, matrixStack, buffer,
                combinedLight, combinedOverlay);
        segmentRenderer.render(tileEntity, partialTicks, matrixStack, buffer,
                combinedLight, combinedOverlay);
        dieRenderer.renderDice(table, partialTicks, matrixStack, buffer,
                combinedLight, combinedOverlay);
        renderStakes(table, matrixStack, buffer, combinedLight, combinedOverlay);
    }

    private void renderStakes(CasinoFarkleTableTileEntity table, MatrixStack stack,
                              IRenderTypeBuffer buffer, int light, int overlay) {
        stack.pushPose();
        stack.translate(0.5, 0.01, 0.5);
        rotateBlock(table.getBlockState().getValue(CasinoFarkleTableBlock.FACING), stack);
        for (int side = 0; side < 2; side++) {
            ItemStack wager = table.getStake(side);
            if (wager.isEmpty()) continue;
            ItemStack oneChip = wager.copy();
            oneChip.setCount(1);
            for (int layer = 0; layer < Math.min(wager.getCount(), 5); layer++) {
                stack.pushPose();
                // Keep wagers clear of the white scoring frame on the left.
                stack.translate(-9.0 / 16.0, STAKE_BASE_Y + layer * STAKE_LAYER_HEIGHT,
                        (side == 0 ? -8.0 : 8.0) / 16.0);
                stack.mulPose(Vector3f.YP.rotationDegrees(
                        (side == 0 ? 0 : 180) + layer * 17));
                stack.scale(STAKE_SCALE, STAKE_SCALE, STAKE_SCALE);
                Minecraft.getInstance().getItemRenderer().renderStatic(oneChip,
                        ItemCameraTransforms.TransformType.GROUND,
                        light, overlay, stack, buffer);
                stack.popPose();
            }
        }
        stack.popPose();
    }
}
