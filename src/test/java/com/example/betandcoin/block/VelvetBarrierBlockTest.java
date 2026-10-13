package com.example.betandcoin.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.state.BooleanProperty;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.world.IWorld;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.*;

public class VelvetBarrierBlockTest {
    static {
        net.minecraft.util.registry.Bootstrap.bootStrap();
    }

    private static final Direction[] DIRECTIONS = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };
    private static final BooleanProperty[] CONNECTIONS = {
            VelvetBarrierBlock.NORTH, VelvetBarrierBlock.EAST,
            VelvetBarrierBlock.SOUTH, VelvetBarrierBlock.WEST
    };

    private VelvetBarrierBlock barrier() {
        return new VelvetBarrierBlock(AbstractBlock.Properties.of(Material.METAL).noOcclusion());
    }

    @Test
    public void allLayoutsUpdateAndDisconnectWithoutChangingOtherRopes() {
        VelvetBarrierBlock block = barrier();
        IWorld world = (IWorld) Proxy.newProxyInstance(IWorld.class.getClassLoader(),
                new Class<?>[] {IWorld.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getBlockState")) return Blocks.AIR.defaultBlockState();
                    throw new UnsupportedOperationException(method.getName());
                });
        for (int mask = 0; mask < 16; mask++) {
            BlockState state = block.defaultBlockState();
            for (int i = 0; i < 4; i++) {
                boolean connected = (mask & (1 << i)) != 0;
                state = block.updateShape(state, DIRECTIONS[i],
                        connected ? block.defaultBlockState() : Blocks.AIR.defaultBlockState(),
                        world, BlockPos.ZERO, BlockPos.ZERO.relative(DIRECTIONS[i]));
            }
            for (int i = 0; i < 4; i++) {
                assertEquals((mask & (1 << i)) != 0, state.getValue(CONNECTIONS[i]));
                BlockState removed = block.updateShape(state, DIRECTIONS[i],
                        Blocks.AIR.defaultBlockState(), world,
                        BlockPos.ZERO, BlockPos.ZERO.relative(DIRECTIONS[i]));
                for (int j = 0; j < 4; j++) {
                    assertEquals(j != i && (mask & (1 << j)) != 0,
                            removed.getValue(CONNECTIONS[j]));
                }
            }
        }
    }

    @Test
    public void solidWallsAndOtherFencesDoNotCreateRopes() {
        VelvetBarrierBlock block = barrier();
        assertTrue(block.connectsTo(block.defaultBlockState(), false, Direction.NORTH));
        assertFalse(block.connectsTo(Blocks.STONE.defaultBlockState(), true, Direction.NORTH));
        assertFalse(block.connectsTo(Blocks.OAK_FENCE.defaultBlockState(), false, Direction.NORTH));
    }

    @Test
    public void selectionContainsOnlyConnectedRopesAndCollisionHasFenceHeight() {
        VelvetBarrierBlock block = barrier();
        BlockState isolated = block.defaultBlockState();
        assertEquals(6.5 / 16.0, block.getShape(isolated, null, BlockPos.ZERO,
                ISelectionContext.empty()).min(Direction.Axis.Z), 0.00001);
        BlockState north = isolated.setValue(VelvetBarrierBlock.NORTH, true);
        assertEquals(0, block.getShape(north, null, BlockPos.ZERO,
                ISelectionContext.empty()).min(Direction.Axis.Z), 0.00001);
        assertEquals(9.5 / 16.0, block.getShape(north, null, BlockPos.ZERO,
                ISelectionContext.empty()).max(Direction.Axis.Z), 0.00001);
        assertEquals(1.5, block.getCollisionShape(north, null, BlockPos.ZERO,
                ISelectionContext.empty()).max(Direction.Axis.Y), 0.00001);
    }

    @Test
    public void structureTransformsRetainWaterAndRotateConnections() {
        VelvetBarrierBlock block = barrier();
        BlockState north = block.defaultBlockState().setValue(VelvetBarrierBlock.NORTH, true)
                .setValue(VelvetBarrierBlock.WATERLOGGED, true);
        BlockState rotated = block.rotate(north, Rotation.CLOCKWISE_90);
        assertTrue(rotated.getValue(VelvetBarrierBlock.EAST));
        assertFalse(rotated.getValue(VelvetBarrierBlock.NORTH));
        assertTrue(rotated.getValue(VelvetBarrierBlock.WATERLOGGED));
        BlockState mirrored = block.mirror(north, Mirror.LEFT_RIGHT);
        assertTrue(mirrored.getValue(VelvetBarrierBlock.SOUTH));
        assertFalse(mirrored.getValue(VelvetBarrierBlock.NORTH));
    }
}
