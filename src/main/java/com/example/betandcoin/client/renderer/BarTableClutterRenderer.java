package com.example.betandcoin.client.renderer;

import com.example.betandcoin.client.ClientModEvents;
import com.example.betandcoin.tileentity.BarTableClutterTileEntity;
import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector3f;

public class BarTableClutterRenderer
        extends TileEntityRenderer<BarTableClutterTileEntity> {

    private static final int FULL_BRIGHT_LIGHT = 15728880;

    public BarTableClutterRenderer(
            TileEntityRendererDispatcher dispatcher
    ) {
        super(dispatcher);
    }

    @Override
    public void render(
            BarTableClutterTileEntity tileEntity,
            float partialTicks,
            MatrixStack matrixStack,
            IRenderTypeBuffer buffer,
            int combinedLight,
            int combinedOverlay
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        BlockState state = tileEntity.getBlockState();
        int variant = tileEntity.getVariant();
        boolean lightOn = tileEntity.isLightOn();

        matrixStack.pushPose();

        matrixStack.translate(0.5D, 0.0D, 0.5D);
        rotateByFacing(state, matrixStack);
        matrixStack.translate(-0.5D, 0.0D, -0.5D);

        IBakedModel baseModel =
                minecraft.getModelManager()
                        .getModel(
                                lightOn
                                        ? ClientModEvents.BAR_TABLE_CLUTTER_MODELS[variant]
                                        : ClientModEvents.BAR_TABLE_CLUTTER_OFF_MODELS[variant]
                        );

        renderModel(
                matrixStack,
                buffer,
                state,
                baseModel,
                RenderType.cutout(),
                combinedLight,
                combinedOverlay
        );

        if (lightOn) {
            IBakedModel glowModel =
                    minecraft.getModelManager()
                            .getModel(
                                    ClientModEvents.BAR_TABLE_CLUTTER_GLOW_MODEL
                            );

            renderModel(
                    matrixStack,
                    buffer,
                    state,
                    glowModel,
                    RenderType.cutout(),
                    FULL_BRIGHT_LIGHT,
                    combinedOverlay
            );
        }

        if (ClientModEvents.BAR_TABLE_CLUTTER_GLASS_MODELS[variant] != null) {
            IBakedModel glassModel =
                    minecraft.getModelManager()
                            .getModel(
                                    ClientModEvents.BAR_TABLE_CLUTTER_GLASS_MODELS[variant]
                            );

            renderModel(
                    matrixStack,
                    buffer,
                    state,
                    glassModel,
                    RenderType.translucent(),
                    combinedLight,
                    combinedOverlay
            );
        }

        matrixStack.popPose();
    }

    private void rotateByFacing(
            BlockState state,
            MatrixStack matrixStack
    ) {
        Direction facing =
                state.getValue(HorizontalBlock.FACING);

        switch (facing) {
            case SOUTH:
                matrixStack.mulPose(Vector3f.YP.rotationDegrees(180));
                break;

            case WEST:
                matrixStack.mulPose(Vector3f.YP.rotationDegrees(90));
                break;

            case EAST:
                matrixStack.mulPose(Vector3f.YP.rotationDegrees(-90));
                break;

            default:
                break;
        }
    }

    private static void renderModel(
            MatrixStack matrixStack,
            IRenderTypeBuffer buffer,
            BlockState state,
            IBakedModel model,
            RenderType renderType,
            int light,
            int overlay
    ) {
        Minecraft.getInstance()
                .getBlockRenderer()
                .getModelRenderer()
                .renderModel(
                        matrixStack.last(),
                        buffer.getBuffer(renderType),
                        state,
                        model,
                        1.0F,
                        1.0F,
                        1.0F,
                        light,
                        overlay
                );
    }
}
