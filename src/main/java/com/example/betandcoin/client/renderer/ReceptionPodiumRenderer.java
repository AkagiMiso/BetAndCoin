package com.example.betandcoin.client.renderer;

import com.example.betandcoin.block.ReceptionPodiumBlock;
import com.example.betandcoin.client.ClientModEvents;
import com.example.betandcoin.tileentity.ReceptionPodiumTileEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector3f;

public class ReceptionPodiumRenderer extends TileEntityRenderer<ReceptionPodiumTileEntity> {
    private static final int FULL_BRIGHT = 15728880;

    public ReceptionPodiumRenderer(TileEntityRendererDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public void render(ReceptionPodiumTileEntity tileEntity, float partialTicks,
                       MatrixStack matrixStack, IRenderTypeBuffer buffer,
                       int combinedLight, int combinedOverlay) {
        BlockState state = tileEntity.getBlockState();
        if (!state.getValue(ReceptionPodiumBlock.LIT)) {
            return;
        }

        IBakedModel glowModel = Minecraft.getInstance().getModelManager()
                .getModel(ClientModEvents.RECEPTION_PODIUM_GLOW_MODEL);
        Direction facing = state.getValue(ReceptionPodiumBlock.FACING);

        matrixStack.pushPose();
        matrixStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case EAST: matrixStack.mulPose(Vector3f.YP.rotationDegrees(-90)); break;
            case SOUTH: matrixStack.mulPose(Vector3f.YP.rotationDegrees(180)); break;
            case WEST: matrixStack.mulPose(Vector3f.YP.rotationDegrees(90)); break;
            default: break;
        }
        matrixStack.translate(-0.5D, 0.0D, -0.5D);

        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                matrixStack.last(), buffer.getBuffer(RenderType.cutout()), state,
                glowModel, 1.0F, 1.0F, 1.0F, FULL_BRIGHT, combinedOverlay);
        matrixStack.popPose();
    }
}
