package com.example.betandcoin.client.renderer;

import com.example.betandcoin.client.model.CasinoFarkleTableSegmentModel;
import com.example.betandcoin.tileentity.CasinoFarkleTableTileEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

/** Adds only lit segments; the part texture already contains the dark ones. */
public class CasinoFarkleSegmentRenderer
        extends GeoBlockRenderer<CasinoFarkleTableTileEntity> {
    private static final int[] DIGIT_MASKS = {
            63, 6, 91, 79, 102, 109, 125, 7, 127, 111
    };
    private static final char[] SEGMENTS = {'a', 'b', 'c', 'd', 'e', 'f', 'g'};
    private final CasinoFarkleTableSegmentModel segmentModel;

    public CasinoFarkleSegmentRenderer(TileEntityRendererDispatcher dispatcher) {
        this(dispatcher, new CasinoFarkleTableSegmentModel());
    }

    private CasinoFarkleSegmentRenderer(TileEntityRendererDispatcher dispatcher,
                                        CasinoFarkleTableSegmentModel model) {
        super(dispatcher, model);
        this.segmentModel = model;
    }

    @Override
    public void render(TileEntity tileEntity, float partialTicks,
                       MatrixStack matrixStack, IRenderTypeBuffer buffer,
                       int combinedLight, int combinedOverlay) {
        if (!(tileEntity instanceof CasinoFarkleTableTileEntity)) return;
        CasinoFarkleTableTileEntity table = (CasinoFarkleTableTileEntity) tileEntity;
        GeoModel model = segmentModel.getModel(segmentModel.getModelLocation(table));
        GeoBone arm = model.getBone("arm_rotation_segment").orElse(null);
        // GeckoLib converts authored X rotations to the opposite runtime sign.
        // The B arm already has a +10 degree runtime rest angle.
        if (arm != null) arm.setRotationX((float) Math.toRadians(-table.getArmAngle(partialTicks)));
        for (int side = 0; side < 2; side++) {
            int value = table.getDisplayValue(side);
            for (int digit = 1; digit <= 5; digit++) {
                int number = value < 0 ? -1 : value / powerOfTen(5 - digit) % 10;
                int mask = number < 0 ? 0 : DIGIT_MASKS[number];
                for (int segment = 0; segment < 7; segment++) {
                    String boneName = "player_" + (side == 0 ? "a" : "b")
                            + "_digit_" + digit + "_" + SEGMENTS[segment];
                    GeoBone bone = model.getBone(boneName).orElse(null);
                    if (bone != null) bone.setHidden((mask & (1 << segment)) == 0);
                }
            }
        }
        super.render(tileEntity, partialTicks, matrixStack, buffer,
                combinedLight, combinedOverlay);
    }

    private static int powerOfTen(int power) {
        int value = 1;
        for (int i = 0; i < power; i++) value *= 10;
        return value;
    }

    @Override
    public RenderType getRenderType(CasinoFarkleTableTileEntity animatable,
                                    float partialTicks, MatrixStack stack,
                                    IRenderTypeBuffer buffers, IVertexBuilder builder,
                                    int packedLight, ResourceLocation texture) {
        return RenderType.eyes(texture);
    }
}
