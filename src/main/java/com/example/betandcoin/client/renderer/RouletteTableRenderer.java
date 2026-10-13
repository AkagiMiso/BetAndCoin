package com.example.betandcoin.client.renderer;

import com.example.betandcoin.BetAndCoin;
import com.example.betandcoin.block.RouletteTableBlock;
import com.example.betandcoin.game.roulette.*;
import com.example.betandcoin.tileentity.RouletteTableTileEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

public class RouletteTableRenderer extends TileEntityRenderer<RouletteTableTileEntity> {
    public static final ResourceLocation MAIN = model("roulette_table_main");
    public static final ResourceLocation FRAME_45 = model("roulette_table_main_45");
    public static final ResourceLocation[] GLOW = {
            model("roulette_table_glow_red"), model("roulette_table_glow_black"), model("roulette_table_glow_green")
    };
    private static final int FULL_BRIGHT = 15728880;

    private static ResourceLocation model(String name) { return new ResourceLocation(BetAndCoin.MOD_ID, "block/" + name); }
    public RouletteTableRenderer(TileEntityRendererDispatcher dispatcher) { super(dispatcher); }

    @Override public void render(RouletteTableTileEntity table, float partialTicks, MatrixStack pose,
                                  IRenderTypeBuffer buffer, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        Direction facing = table.getBlockState().getValue(RouletteTableBlock.FACING);
        switch (facing) {
            case EAST: pose.mulPose(Vector3f.YP.rotationDegrees(-90)); break;
            case SOUTH: pose.mulPose(Vector3f.YP.rotationDegrees(180)); break;
            case WEST: pose.mulPose(Vector3f.YP.rotationDegrees(90)); break;
            default: break;
        }
        pose.translate(-0.5, 0, -0.5);
        renderModel(table, table.centerFrame45() ? FRAME_45 : MAIN, pose, buffer, light, overlay);
        if (table.showGlow()) {
            RouletteLayout.Segment segment = RouletteLayout.segment(table.currentSegment(partialTicks));
            pose.pushPose();
            // The exported single glow starts at model (12,13.51,18).
            pose.translate((segment.x - 12) / 16.0, RouletteGeometry.GLOW_Y_OFFSET / 16.0,
                    (segment.z - 18) / 16.0);
            renderModel(table, GLOW[segment.color.ordinal()], pose, buffer, FULL_BRIGHT, overlay);
            pose.popPose();
        }
        if (table.getPhase() != RouletteRules.Phase.SETTLED_RESULT) renderChips(table, pose, buffer, light, overlay);
        pose.popPose();
    }

    private void renderChips(RouletteTableTileEntity table, MatrixStack pose, IRenderTypeBuffer buffer,
                             int light, int overlay) {
        int[] visible = new int[3];
        for (RouletteTableTileEntity.Bet bet : table.getVisibleChips()) {
            int color = bet.color.ordinal();
            for (int i = 0; i < bet.chips.getCount() && visible[color] < RouletteRules.MAX_VISIBLE_CHIPS; i++) {
                int slot = visible[color]++;
                RouletteGeometry.BetZone zone = RouletteGeometry.zone(bet.color);
                pose.pushPose();
                pose.translate(zone.centerX() / 16.0,
                        (RouletteGeometry.CHIP_Y + slot * RouletteGeometry.CHIP_LAYER_HEIGHT) / 16.0,
                        zone.centerZ() / 16.0);
                pose.mulPose(Vector3f.YP.rotationDegrees(slot * 17));
                pose.scale(RouletteGeometry.CHIP_SCALE, RouletteGeometry.CHIP_SCALE, RouletteGeometry.CHIP_SCALE);
                ItemStack chip = bet.chips.copy(); chip.setCount(1);
                Minecraft.getInstance().getItemRenderer().renderStatic(chip,
                        ItemCameraTransforms.TransformType.GROUND, light, overlay, pose, buffer);
                pose.popPose();
            }
        }
    }

    private void renderModel(RouletteTableTileEntity table, ResourceLocation id, MatrixStack pose,
                             IRenderTypeBuffer buffer, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffer.getBuffer(RenderType.cutout()),
                table.getBlockState(), mc.getModelManager().getModel(id), 1, 1, 1, light, overlay);
    }
}
