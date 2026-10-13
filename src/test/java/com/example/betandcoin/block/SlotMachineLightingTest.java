package com.example.betandcoin.block;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.LightType;
import org.junit.Test;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import static org.junit.Assert.*;

/** Exercise vanilla neighbor AO sampling and preserve collision/gameplay and environment lighting. */
public class SlotMachineLightingTest {
    static { net.minecraft.util.registry.Bootstrap.bootStrap(); }

    @Test public void neighboringMachinesNoLongerDarkenMiddleMoreThanIsolatedMachine() throws Exception {
        SlotMachineBlock oldBehavior = new SlotMachineBlock(properties()) {
            @Override public float getShadeBrightness(BlockState state, IBlockReader reader, BlockPos pos) {
                return state.isCollisionShapeFullBlock(reader, pos) ? 0.2F : 1.0F;
            }
            @Override public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos) {
                return false;
            }
        };
        float oldSingle = average((float[]) sample(oldBehavior, 0, 15, "brightness"));
        float oldMiddle = average((float[]) sample(oldBehavior, 1, 15, "brightness"));
        assertTrue("Reproduce neighbor-dependent excessive shadow", oldMiddle < oldSingle);
        SlotMachineBlock fixed = new SlotMachineBlock(properties());
        float single = average((float[]) sample(fixed, 0, 15, "brightness"));
        float middle = average((float[]) sample(fixed, 1, 15, "brightness"));
        assertEquals(single, middle, 0.00001F);
        assertTrue(middle > oldMiddle);
    }

    @Test public void bothHalvesKeepCollisionAndTilePlacementWithoutBecomingLightSources() {
        SlotMachineBlock block = new SlotMachineBlock(properties());
        IBlockReader reader = net.minecraft.world.EmptyBlockReader.INSTANCE;
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            assertTrue(state.isCollisionShapeFullBlock(reader, BlockPos.ZERO));
            assertEquals(state.getValue(SlotMachineBlock.HALF) == DoubleBlockHalf.LOWER, state.hasTileEntity());
            assertEquals(0, state.getLightEmission());
            assertEquals(0, state.getLightBlock(reader, BlockPos.ZERO));
            assertTrue(state.propagatesSkylightDown(reader, BlockPos.ZERO));
        }
    }

    @Test public void environmentLightStillChangesAndModelRetainsDirectionalShading() throws Exception {
        SlotMachineBlock block = new SlotMachineBlock(properties());
        int[] daylight = (int[]) sample(block, 1, 15, "lightmap");
        int[] darkness = (int[]) sample(block, 1, 0, "lightmap");
        for (int i = 0; i < 4; i++) {
            assertTrue(daylight[i] > darkness[i]);
            assertEquals(0, darkness[i]);
        }
        try (Reader reader = new InputStreamReader(getClass().getResourceAsStream(
                "/assets/bet_and_coin/models/block/slot_machine_static.json"), StandardCharsets.UTF_8)) {
            JsonObject model = new JsonParser().parse(reader).getAsJsonObject();
            assertFalse(model.get("ambientocclusion").getAsBoolean());
            for (com.google.gson.JsonElement element : model.getAsJsonArray("elements")) {
                JsonObject cube = element.getAsJsonObject();
                assertTrue(!cube.has("shade") || cube.get("shade").getAsBoolean());
            }
        }
    }

    private AbstractBlock.Properties properties() {
        return AbstractBlock.Properties.of(Material.METAL).strength(3.5F, 6.0F).noOcclusion();
    }

    private Object sample(SlotMachineBlock block, int rowRadius, int skyLight, String result) throws Exception {
        BlockState state = block.defaultBlockState();
        IBlockDisplayReader reader = (IBlockDisplayReader) Proxy.newProxyInstance(
                IBlockDisplayReader.class.getClassLoader(), new Class<?>[] {IBlockDisplayReader.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getBlockState")) {
                        BlockPos pos = (BlockPos) args[0];
                        return Math.abs(pos.getX()) <= rowRadius && pos.getZ() == 0
                                && pos.getY() >= 0 && pos.getY() <= 1 ? state : Blocks.AIR.defaultBlockState();
                    }
                    if (method.getName().equals("getBrightness")) return args[0] == LightType.SKY ? skyLight : 0;
                    if (method.getName().equals("getShade")) return (boolean) args[1] ? 0.8F : 1.0F;
                    throw new UnsupportedOperationException(method.getName());
                });
        Class<?> faceType = Class.forName("net.minecraft.client.renderer.BlockModelRenderer$AmbientOcclusionFace");
        Constructor<?> ctor = faceType.getDeclaredConstructor(BlockModelRenderer.class);
        ctor.setAccessible(true);
        Object face = ctor.newInstance(new BlockModelRenderer(null));
        Method calculate = faceType.getDeclaredMethod("calculate", IBlockDisplayReader.class, BlockState.class,
                BlockPos.class, Direction.class, float[].class, BitSet.class, boolean.class);
        calculate.setAccessible(true);
        calculate.invoke(face, reader, state, BlockPos.ZERO, Direction.NORTH, new float[12], new BitSet(3), true);
        Field values = faceType.getDeclaredField(result);
        values.setAccessible(true);
        return values.get(face);
    }

    private float average(float[] values) {
        float sum = 0;
        for (float value : values) sum += value;
        return sum / values.length;
    }
}
