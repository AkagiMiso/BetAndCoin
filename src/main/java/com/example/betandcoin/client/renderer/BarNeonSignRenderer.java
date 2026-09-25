package com.example.betandcoin.client.renderer;

import com.example.betandcoin.block.BarNeonSignBlock;
import com.example.betandcoin.client.ClientModEvents;
import com.example.betandcoin.tileentity.BarNeonSignTileEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector3f;

public class BarNeonSignRenderer
        extends TileEntityRenderer<BarNeonSignTileEntity> {

    private static final int FULL_BRIGHT = 15728880;

    public BarNeonSignRenderer(
            TileEntityRendererDispatcher dispatcher
    ) {
        super(dispatcher);
    }

    @Override
    public void render(
            BarNeonSignTileEntity tileEntity,
            float partialTicks,
            MatrixStack matrixStack,
            IRenderTypeBuffer buffer,
            int combinedLight,
            int combinedOverlay
    ) {
        BlockState state = tileEntity.getBlockState();

        if (!state.getValue(BarNeonSignBlock.LIT)) {
            return;
        }

        IBakedModel glowModel = Minecraft.getInstance()
                .getModelManager()
                .getModel(
                        ClientModEvents.BAR_NEON_SIGN_GLOW_MODEL
                );

        Direction facing =
                state.getValue(BarNeonSignBlock.FACING);

        matrixStack.pushPose();

        /*
         * 与方块 JSON 模型一样，围绕方块中心旋转。
         */
        matrixStack.translate(0.5D, 0.0D, 0.5D);
        matrixStack.mulPose(
                Vector3f.YP.rotationDegrees(
                        getRendererRotationY(facing)
                )
        );
        matrixStack.translate(-0.5D, 0.0D, -0.5D);

        IVertexBuilder vertexBuilder =
                buffer.getBuffer(RenderType.cutout());

        Minecraft.getInstance()
                .getBlockRenderer()
                .getModelRenderer()
                .renderModel(
                        matrixStack.last(),
                        vertexBuilder,
                        state,
                        glowModel,
                        1.0F,
                        1.0F,
                        1.0F,
                        FULL_BRIGHT,
                        combinedOverlay
                );

        matrixStack.popPose();
    }

    /*
     * 重要：
     *
     * blockstate JSON 的 y 旋转方向，与 MatrixStack 中
     * Vector3f.YP.rotationDegrees() 的视觉旋转方向相反。
     *
     * 当前 blockstate 使用：
     * north = 0
     * east  = 90
     * south = 180
     * west  = 270
     *
     * 因此 BER 必须使用其反向角：
     * north = 0
     * east  = 270
     * south = 180
     * west  = 90
     *
     * 这样既会让东西方向的三个字整体旋转 180°，
     * 也会让发光层重新与暗色基础模型重合。
     */
    private static float getRendererRotationY(Direction facing) {
        switch (facing) {
            case EAST:
                return 270.0F;
            case SOUTH:
                return 180.0F;
            case WEST:
                return 90.0F;
            case NORTH:
            default:
                return 0.0F;
        }
    }
}
