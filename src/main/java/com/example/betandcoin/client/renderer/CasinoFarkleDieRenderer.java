package com.example.betandcoin.client.renderer;

import com.example.betandcoin.client.model.CasinoFarkleDieModel;
import com.example.betandcoin.game.farkle.FarkleGeometry;
import com.example.betandcoin.game.farkle.FarkleDieOrientation;
import com.example.betandcoin.game.farkle.FarklePhase;
import com.example.betandcoin.tileentity.CasinoFarkleTableTileEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector3d;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

/** Draws six logical dice using one 64x64 GeckoLib model and texture. */
public class CasinoFarkleDieRenderer
        extends GeoBlockRenderer<CasinoFarkleTableTileEntity> {
    private final CasinoFarkleDieModel dieModel;

    public CasinoFarkleDieRenderer(TileEntityRendererDispatcher dispatcher) {
        this(dispatcher, new CasinoFarkleDieModel());
    }

    private CasinoFarkleDieRenderer(TileEntityRendererDispatcher dispatcher,
                                    CasinoFarkleDieModel model) {
        super(dispatcher, model);
        dieModel = model;
    }

    public void renderDice(CasinoFarkleTableTileEntity table, float partialTicks,
                           MatrixStack stack, IRenderTypeBuffer buffer,
                           int light, int overlay) {
        GeoModel model = dieModel.getModel(dieModel.getModelLocation(table));
        GeoBone root = model.getBone("dice_root").orElse(null);
        if (root == null) return;
        // Keep the 2.5-pixel authored geometry and UVs; scale it around its center.
        // At 45%, each die is 1.125 model pixels wide.
        root.setScale(0.45f, 0.45f, 0.45f);
        Direction facing = table.getBlockState().getValue(
                com.example.betandcoin.block.CasinoFarkleTableBlock.FACING);
        for (int i = 0; i < 6; i++) {
            int value = table.getDieValue(i);
            float xAngle = FarkleDieOrientation.xDegrees(value);
            float zAngle = FarkleDieOrientation.zDegrees(value);
            boolean rolling = table.getPhase() == FarklePhase.ROLLING
                    && table.isDieAvailable(i);
            float spin = rolling ? (table.getPhaseTicks() + partialTicks) * 72.0f : 0;
            root.setRotationX((float) Math.toRadians(xAngle + spin));
            root.setRotationY((float) Math.toRadians(spin));
            root.setRotationZ((float) Math.toRadians(zAngle));
            float lift = table.isDieSelected(i) ? 1.5f : 0;
            if (rolling) lift += (float) Math.sin(Math.toRadians(spin)) * 1.5f;
            float x = FarkleGeometry.DIE_X[i];
            float z = FarkleGeometry.DIE_Z[i];
            int scoringSlot = table.getScoringSlot(i);
            if (!table.isDieAvailable(i) && scoringSlot >= 0 && scoringSlot < 6) {
                float progress = table.getDieTransferProgress(i, partialTicks);
                progress = progress * progress * (3.0f - 2.0f * progress);
                x += (FarkleGeometry.SCORED_X[scoringSlot] - x) * progress;
                z += (FarkleGeometry.SCORED_Z[scoringSlot] - z) * progress;
                lift += 0.8f * (float) Math.sin(Math.PI * progress);
            }
            Vector3d offset = worldOffset(x / 16.0, z / 16.0, facing);
            stack.pushPose();
            stack.translate(offset.x, (FarkleGeometry.DIE_Y + lift) / 16.0, offset.z);
            super.render(table, partialTicks, stack, buffer, light, overlay);
            stack.popPose();
        }
    }

    private static Vector3d worldOffset(double x, double z, Direction facing) {
        switch (facing) {
            case SOUTH: return new Vector3d(-x, 0, -z);
            case WEST: return new Vector3d(z, 0, -x);
            case EAST: return new Vector3d(-z, 0, x);
            default: return new Vector3d(x, 0, z);
        }
    }
}
