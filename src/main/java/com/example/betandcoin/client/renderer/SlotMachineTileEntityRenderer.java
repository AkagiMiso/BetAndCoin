package com.example.betandcoin.client.renderer;

import com.example.betandcoin.block.SlotMachineBlock;
import com.example.betandcoin.client.ClientModEvents;
import com.example.betandcoin.tileentity.SlotMachineTileEntity;
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

public class SlotMachineTileEntityRenderer
        extends TileEntityRenderer<SlotMachineTileEntity> {

    private static final double REEL_PIVOT_X = 8.0D / 16.0D;
    private static final double REEL_PIVOT_Y = 22.5D / 16.0D;
    private static final double REEL_PIVOT_Z = 8.5D / 16.0D;

    private static final double LEFT_X_OFFSET = -3.0D / 16.0D;
    private static final double MIDDLE_X_OFFSET = 0.0D;
    private static final double RIGHT_X_OFFSET = 3.0D / 16.0D;

    // Blockbench 中拉杆的旋转枢轴。
    private static final double LEVER_PIVOT_X = 2.5D / 16.0D;
    private static final double LEVER_PIVOT_Y = 22.25D / 16.0D;
    private static final double LEVER_PIVOT_Z = 8.5D / 16.0D;

    private static final float LEVER_REST_ANGLE = 0.0F;
    private static final float LEVER_PULLED_ANGLE = -67.5F;

    public SlotMachineTileEntityRenderer(
            TileEntityRendererDispatcher dispatcher
    ) {
        super(dispatcher);
    }

    @Override
    public void render(
            SlotMachineTileEntity tileEntity,
            float partialTicks,
            MatrixStack matrixStack,
            IRenderTypeBuffer buffer,
            int combinedLight,
            int combinedOverlay
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        IBakedModel signModel = minecraft
                .getModelManager()
                .getModel(
                        tileEntity.isSignLit()
                                ? ClientModEvents.SIGN_ON_MODEL
                                : ClientModEvents.SIGN_OFF_MODEL
                );

        IBakedModel reelModel = minecraft
                .getModelManager()
                .getModel(ClientModEvents.REEL_MODEL);

        IBakedModel leverModel = minecraft
                .getModelManager()
                .getModel(ClientModEvents.LEVER_MODEL);

        BlockState state = tileEntity.getBlockState();
        Direction facing = state.getValue(SlotMachineBlock.FACING);

        matrixStack.pushPose();

        matrixStack.translate(0.5D, 0.0D, 0.5D);
        matrixStack.mulPose(
                Vector3f.YP.rotationDegrees(getRotationY(facing))
        );
        matrixStack.translate(-0.5D, 0.0D, -0.5D);

        renderModel(
                matrixStack,
                buffer,
                state,
                signModel,
                combinedLight,
                combinedOverlay
        );

        float leftAngle = getAnimatedReelAngle(
                tileEntity.getStartLeftReel(),
                tileEntity.getLeftReel(),
                tileEntity.getSpinTicks(),
                partialTicks,
                SlotMachineTileEntity.LEFT_STOP_TICK,
                4,
                tileEntity.isSpinning()
        );

        float middleAngle = getAnimatedReelAngle(
                tileEntity.getStartMiddleReel(),
                tileEntity.getMiddleReel(),
                tileEntity.getSpinTicks(),
                partialTicks,
                SlotMachineTileEntity.MIDDLE_STOP_TICK,
                6,
                tileEntity.isSpinning()
        );

        float rightAngle = getAnimatedReelAngle(
                tileEntity.getStartRightReel(),
                tileEntity.getRightReel(),
                tileEntity.getSpinTicks(),
                partialTicks,
                SlotMachineTileEntity.RIGHT_STOP_TICK,
                8,
                tileEntity.isSpinning()
        );

        renderReel(
                matrixStack,
                buffer,
                state,
                reelModel,
                LEFT_X_OFFSET,
                leftAngle,
                combinedLight,
                combinedOverlay
        );

        renderReel(
                matrixStack,
                buffer,
                state,
                reelModel,
                MIDDLE_X_OFFSET,
                middleAngle,
                combinedLight,
                combinedOverlay
        );

        renderReel(
                matrixStack,
                buffer,
                state,
                reelModel,
                RIGHT_X_OFFSET,
                rightAngle,
                combinedLight,
                combinedOverlay
        );

        float leverAngle = getLeverAngle(
                tileEntity.getLeverTicks(),
                partialTicks
        );

        renderLever(
                matrixStack,
                buffer,
                state,
                leverModel,
                leverAngle,
                combinedLight,
                combinedOverlay
        );

        matrixStack.popPose();
    }

    private static void renderReel(
            MatrixStack matrixStack,
            IRenderTypeBuffer buffer,
            BlockState state,
            IBakedModel reelModel,
            double xOffset,
            float rotationX,
            int combinedLight,
            int combinedOverlay
    ) {
        matrixStack.pushPose();

        matrixStack.translate(xOffset, 0.0D, 0.0D);

        matrixStack.translate(
                REEL_PIVOT_X,
                REEL_PIVOT_Y,
                REEL_PIVOT_Z
        );

        matrixStack.mulPose(
                Vector3f.XP.rotationDegrees(rotationX)
        );

        matrixStack.translate(
                -REEL_PIVOT_X,
                -REEL_PIVOT_Y,
                -REEL_PIVOT_Z
        );

        renderModel(
                matrixStack,
                buffer,
                state,
                reelModel,
                combinedLight,
                combinedOverlay
        );

        matrixStack.popPose();
    }

    private static void renderLever(
            MatrixStack matrixStack,
            IRenderTypeBuffer buffer,
            BlockState state,
            IBakedModel leverModel,
            float rotationX,
            int combinedLight,
            int combinedOverlay
    ) {
        matrixStack.pushPose();

        matrixStack.translate(
                LEVER_PIVOT_X,
                LEVER_PIVOT_Y,
                LEVER_PIVOT_Z
        );

        matrixStack.mulPose(
                Vector3f.XP.rotationDegrees(rotationX)
        );

        matrixStack.translate(
                -LEVER_PIVOT_X,
                -LEVER_PIVOT_Y,
                -LEVER_PIVOT_Z
        );

        renderModel(
                matrixStack,
                buffer,
                state,
                leverModel,
                combinedLight,
                combinedOverlay
        );

        matrixStack.popPose();
    }

    private static void renderModel(
            MatrixStack matrixStack,
            IRenderTypeBuffer buffer,
            BlockState state,
            IBakedModel model,
            int combinedLight,
            int combinedOverlay
    ) {
        IVertexBuilder vertexBuilder =
                buffer.getBuffer(RenderType.cutout());

        Minecraft.getInstance()
                .getBlockRenderer()
                .getModelRenderer()
                .renderModel(
                        matrixStack.last(),
                        vertexBuilder,
                        state,
                        model,
                        1.0F,
                        1.0F,
                        1.0F,
                        combinedLight,
                        combinedOverlay
                );
    }

    private static float getLeverAngle(
            int leverTicks,
            float partialTicks
    ) {
        float elapsed = Math.min(
                leverTicks + partialTicks,
                SlotMachineTileEntity.LEVER_ANIMATION_TICKS
        );

        if (elapsed < SlotMachineTileEntity.LEVER_PULL_TICKS) {
            float progress =
                    elapsed / SlotMachineTileEntity.LEVER_PULL_TICKS;

            return lerp(
                    smoothStep(progress),
                    LEVER_REST_ANGLE,
                    LEVER_PULLED_ANGLE
            );
        }

        float returnStart =
                SlotMachineTileEntity.LEVER_PULL_TICKS
                        + SlotMachineTileEntity.LEVER_HOLD_TICKS;

        if (elapsed < returnStart) {
            return LEVER_PULLED_ANGLE;
        }

        if (elapsed < SlotMachineTileEntity.LEVER_ANIMATION_TICKS) {
            float progress =
                    (elapsed - returnStart)
                            / SlotMachineTileEntity.LEVER_RETURN_TICKS;

            return lerp(
                    smoothStep(progress),
                    LEVER_PULLED_ANGLE,
                    LEVER_REST_ANGLE
            );
        }

        return LEVER_REST_ANGLE;
    }

    private static float smoothStep(float value) {
        float clamped = Math.max(0.0F, Math.min(1.0F, value));
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static float lerp(float progress, float start, float end) {
        return start + (end - start) * progress;
    }

    private static float getAnimatedReelAngle(
            int startResult,
            int targetResult,
            int spinTicks,
            float partialTicks,
            int stopTick,
            int fullTurns,
            boolean spinning
    ) {
        float targetAngle = getResultAngle(targetResult);

        if (!spinning) {
            return targetAngle;
        }

        float elapsed = Math.min(
                spinTicks + partialTicks,
                stopTick
        );

        float progress = elapsed / (float) stopTick;
        float inverse = 1.0F - progress;
        float eased = 1.0F - inverse * inverse * inverse;

        float startAngle = getResultAngle(startResult);
        float resultDifference = targetAngle - startAngle;

        while (resultDifference > 0.0F) {
            resultDifference -= 360.0F;
        }

        while (resultDifference <= -360.0F) {
            resultDifference += 360.0F;
        }

        float totalRotation =
                resultDifference - fullTurns * 360.0F;

        return startAngle + totalRotation * eased;
    }

    private static float getResultAngle(int result) {
        switch (result) {
            case 1:
                return -90.0F;
            case 2:
                return -180.0F;
            case 3:
                return -270.0F;
            case 0:
            default:
                return 0.0F;
        }
    }

    private static float getRotationY(Direction facing) {
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
