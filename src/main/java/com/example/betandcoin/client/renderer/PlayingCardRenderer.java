package com.example.betandcoin.client.renderer;

import com.example.betandcoin.entity.PlayingCardEntity;
import com.example.betandcoin.game.blackjack.CardRank;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;

import javax.annotation.Nonnull;
import java.util.Locale;

public class PlayingCardRenderer extends EntityRenderer<PlayingCardEntity> {

    private static final float HALF_WIDTH = 2.0F / 16.0F;
    private static final float HEIGHT = 6.0F / 16.0F;
    private static final float HALF_THICKNESS = 0.05F / 16.0F;

    public PlayingCardRenderer(EntityRendererManager renderManager) {
        super(renderManager);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(
            @Nonnull PlayingCardEntity entity,
            float entityYaw,
            float partialTicks,
            @Nonnull MatrixStack matrixStack,
            @Nonnull IRenderTypeBuffer buffer,
            int packedLight
    ) {
        matrixStack.pushPose();

        matrixStack.mulPose(
                Vector3f.YP.rotationDegrees(-entityYaw)
        );

        if (entity.isBlackjackCard()) {
            /*
             * 原始模型位于 X-Y 平面。
             * 绕 X 轴旋转 90° 后平放到桌面的 X-Z 平面，
             * 再绕 Z 轴旋转 180° 调整牌面朝向。
             */
            matrixStack.mulPose(
                    Vector3f.XP.rotationDegrees(90.0F)
            );

            matrixStack.mulPose(
                    Vector3f.ZP.rotationDegrees(180.0F)
            );
        }

        if (!entity.isFaceUp()) {
            matrixStack.mulPose(Vector3f.YP.rotationDegrees(180.0F));
        }

        IVertexBuilder vertexBuilder = buffer.getBuffer(
                RenderType.entityCutout(getTextureLocation(entity))
        );

        MatrixStack.Entry entry = matrixStack.last();
        Matrix4f pose = entry.pose();
        Matrix3f normal = entry.normal();

        float x0 = -HALF_WIDTH;
        float x1 = HALF_WIDTH;
        float y0 = 0.0F;
        float y1 = HEIGHT;
        float z0 = -HALF_THICKNESS;
        float z1 = HALF_THICKNESS;

        // north：牌面，JSON UV [0, 0, 4, 6]
        addQuad(
                vertexBuilder, pose, normal, packedLight,
                x1, y0, z0,  x0, y0, z0,
                x0, y1, z0,  x1, y1, z0,
                0.0F, 0.0F, 4.0F, 6.0F,
                0.0F, 0.0F, -1.0F
        );

        // south：牌背，JSON UV [4, 0, 8, 6]
        addQuad(
                vertexBuilder, pose, normal, packedLight,
                x0, y0, z1,  x1, y0, z1,
                x1, y1, z1,  x0, y1, z1,
                4.0F, 0.0F, 8.0F, 6.0F,
                0.0F, 0.0F, 1.0F
        );

        // east：JSON UV [0, 6, 0.125, 12]
        addQuad(
                vertexBuilder, pose, normal, packedLight,
                x1, y0, z1,  x1, y0, z0,
                x1, y1, z0,  x1, y1, z1,
                0.0F, 6.0F, 0.125F, 12.0F,
                1.0F, 0.0F, 0.0F
        );

        // west：JSON UV [1, 6, 1.125, 12]
        addQuad(
                vertexBuilder, pose, normal, packedLight,
                x0, y0, z0,  x0, y0, z1,
                x0, y1, z1,  x0, y1, z0,
                1.0F, 6.0F, 1.125F, 12.0F,
                -1.0F, 0.0F, 0.0F
        );

        // up：JSON UV [6, 6.125, 2, 6]
        addQuad(
                vertexBuilder, pose, normal, packedLight,
                x0, y1, z1,  x1, y1, z1,
                x1, y1, z0,  x0, y1, z0,
                6.0F, 6.125F, 2.0F, 6.0F,
                0.0F, 1.0F, 0.0F
        );

        // down：JSON UV [10, 6, 6, 6.125]
        addQuad(
                vertexBuilder, pose, normal, packedLight,
                x0, y0, z0,  x1, y0, z0,
                x1, y0, z1,  x0, y0, z1,
                10.0F, 6.0F, 6.0F, 6.125F,
                0.0F, -1.0F, 0.0F
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

    private static void addQuad(
            IVertexBuilder builder,
            Matrix4f pose,
            Matrix3f normal,
            int packedLight,

            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,

            float u0, float v0,
            float u1, float v1,

            float normalX,
            float normalY,
            float normalZ
    ) {
        float minU = u0 / 16.0F;
        float minV = v0 / 16.0F;
        float maxU = u1 / 16.0F;
        float maxV = v1 / 16.0F;

        addVertex(
                builder, pose, normal, packedLight,
                x0, y0, z0,
                minU, maxV,
                normalX, normalY, normalZ
        );

        addVertex(
                builder, pose, normal, packedLight,
                x1, y1, z1,
                maxU, maxV,
                normalX, normalY, normalZ
        );

        addVertex(
                builder, pose, normal, packedLight,
                x2, y2, z2,
                maxU, minV,
                normalX, normalY, normalZ
        );

        addVertex(
                builder, pose, normal, packedLight,
                x3, y3, z3,
                minU, minV,
                normalX, normalY, normalZ
        );
    }

    private static void addVertex(
            IVertexBuilder builder,
            Matrix4f pose,
            Matrix3f normal,
            int packedLight,
            float x,
            float y,
            float z,
            float u,
            float v,
            float normalX,
            float normalY,
            float normalZ
    ) {
        builder.vertex(pose, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal, normalX, normalY, normalZ)
                .endVertex();
    }

    @Nonnull
    @Override
    public ResourceLocation getTextureLocation(
            @Nonnull PlayingCardEntity entity
    ) {
        String suitName = entity.getSuit()
                .name()
                .toLowerCase(Locale.ROOT);

        String rankName = getRankTextureName(entity.getRank());

        return new ResourceLocation(
                "bet_and_coin",
                "textures/entity/cards/card_"
                        + suitName
                        + "_"
                        + rankName
                        + ".png"
        );
    }

    private String getRankTextureName(@Nonnull CardRank rank) {
        switch (rank.name()) {
            case "ACE":
                return "ace";
            case "TWO":
                return "02";
            case "THREE":
                return "03";
            case "FOUR":
                return "04";
            case "FIVE":
                return "05";
            case "SIX":
                return "06";
            case "SEVEN":
                return "07";
            case "EIGHT":
                return "08";
            case "NINE":
                return "09";
            case "TEN":
                return "10";
            case "JACK":
                return "jack";
            case "QUEEN":
                return "queen";
            case "KING":
                return "king";
            default:
                throw new IllegalArgumentException(
                        "Unknown card rank: " + rank
                );
        }
    }
}
