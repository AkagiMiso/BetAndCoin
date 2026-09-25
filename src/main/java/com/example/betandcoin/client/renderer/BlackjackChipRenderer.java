package com.example.betandcoin.client.renderer;

import com.example.betandcoin.entity.BlackjackChipEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

import javax.annotation.Nonnull;

public class BlackjackChipRenderer
        extends EntityRenderer<BlackjackChipEntity> {

    public BlackjackChipRenderer(
            EntityRendererManager renderManager
    ) {
        super(renderManager);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(
            @Nonnull BlackjackChipEntity entity,
            float entityYaw,
            float partialTicks,
            @Nonnull MatrixStack matrixStack,
            @Nonnull IRenderTypeBuffer buffer,
            int packedLight
    ) {
        ItemStack stack = entity.getChipStack();

        if (stack.isEmpty()) {
            return;
        }

        matrixStack.pushPose();

        matrixStack.mulPose(
                Vector3f.YP.rotationDegrees(-entityYaw)
        );

        /*
         * 在牌桌朝向的基础上再绕 Y 轴旋转 180°，
         * 让筹码正面的朝向与玩家视角保持一致。
         */
        matrixStack.mulPose(
                Vector3f.YP.rotationDegrees(180.0F)
        );

        /*
         * 使用物品的 GROUND 变换平放在桌面上。
         * 缩放后约为普通掉落物的一半大小，
         * 保留现有 CHIP_5 / CHIP_20 的物品模型与贴图。
         */
        matrixStack.scale(
                0.58F,
                0.58F,
                0.58F
        );

        Minecraft.getInstance()
                .getItemRenderer()
                .renderStatic(
                        stack,
                        ItemCameraTransforms.TransformType.GROUND,
                        packedLight,
                        OverlayTexture.NO_OVERLAY,
                        matrixStack,
                        buffer
                );

        matrixStack.popPose();

        super.render(
                entity,
                entityYaw,
                partialTicks,
                matrixStack,
                buffer,
                packedLight
        );
    }

    @Nonnull
    @Override
    public ResourceLocation getTextureLocation(
            @Nonnull BlackjackChipEntity entity
    ) {
        return AtlasTexture.LOCATION_BLOCKS;
    }
}
