package com.example.betandcoin.block;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Bootstrap;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.server.ServerWorld;
import org.junit.Test;
import sun.misc.Unsafe;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.Assert.*;

/** Exercise actual placement entry points against the vanilla world-border implementation. */
public class FurniturePlacementBorderTest {
    static { Bootstrap.bootStrap(); }
    private static final Direction[] FACINGS = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    private static class TestWorld extends ServerWorld {
        WorldBorder border;
        Map<BlockPos, BlockState> occupied;
        private TestWorld() { super(null, null, null, null, null, null, null, null, false, 0L, null, false); }
        @Override public WorldBorder getWorldBorder() { return border; }
        @Override public BlockState getBlockState(BlockPos pos) {
            return occupied.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }
        @Override public boolean setBlock(BlockPos pos, BlockState state, int flags) {
            throw new AssertionError("Rejected placement must not create partial furniture");
        }
    }
    private static class Context extends BlockItemUseContext {
        final Direction facing;
        Context(TestWorld world, BlockPos pos, Direction facing) {
            super(world, null, Hand.MAIN_HAND, ItemStack.EMPTY,
                    new BlockRayTraceResult(new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
                            Direction.UP, pos, false));
            this.facing = facing;
        }
        @Override public Direction getHorizontalDirection() { return facing.getOpposite(); }
    }

    @Test public void allFourFacingsMatchFullFootprintAtEveryIntegerBorderEdge() throws Exception {
        TestWorld world = world(0, 0, 10);
        for (Block block : furniture()) for (Direction facing : FACINGS) {
            for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
                assertPlacement(world, block, new BlockPos(x, 64, z), facing);
            }
        }
    }

    @Test public void shiftedFractionalBorderFollowsSameRuleAsRoulette() throws Exception {
        TestWorld world = world(0.25, -0.25, 9.5);
        for (Block block : furniture()) for (Direction facing : FACINGS) {
            for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
                assertPlacement(world, block, new BlockPos(x, 64, z), facing);
            }
        }
    }

    @Test public void normalInteriorPlacementPreservesRequestedFacing() throws Exception {
        TestWorld world = world(0, 0, 100);
        for (Block block : furniture()) for (Direction facing : FACINGS) {
            BlockState placed = block.getStateForPlacement(new Context(world, new BlockPos(0, 64, 0), facing));
            assertNotNull(placed);
            assertSame(block, placed.getBlock());
            assertEquals(facing, placed.getValue(HorizontalBlock.FACING));
        }
    }

    @Test public void occupiedFootprintStillRefusesPlacementWithinBorder() throws Exception {
        TestWorld world = world(0, 0, 100);
        BlockPos main = new BlockPos(0, 64, 0);
        for (Block block : furniture()) for (Direction facing : FACINGS) {
            world.occupied.clear();
            world.occupied.put(main.relative(facing.getClockWise()), Blocks.STONE.defaultBlockState());
            assertNull(block.getStateForPlacement(new Context(world, main, facing)));
        }
    }

    private void assertPlacement(TestWorld world, Block block, BlockPos main, Direction facing) {
        boolean fits = true;
        for (BlockPos cell : footprint(block, main, facing)) fits &= world.border.isWithinBounds(cell);
        BlockState placed = block.getStateForPlacement(new Context(world, main, facing));
        assertEquals(block.getClass().getSimpleName() + " " + main + " " + facing, fits, placed != null);
        if (placed != null) assertEquals(facing, placed.getValue(HorizontalBlock.FACING));
    }

    private List<BlockPos> footprint(Block block, BlockPos main, Direction facing) {
        List<BlockPos> cells = new ArrayList<>();
        if (block instanceof BlackjackTableBlock) {
            for (int side = -1; side <= 1; side++) for (int front = 0; front <= 1; front++) {
                cells.add(main.relative(facing.getClockWise(), side).relative(facing, front));
            }
        } else {
            int radius = block instanceof CasinoFountainBlock ? 2 : 1;
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                cells.add(main.offset(x, 0, z));
            }
        }
        return cells;
    }

    private Block[] furniture() {
        return new Block[] {
                new BlackjackTableBlock(AbstractBlock.Properties.of(Material.WOOD)),
                new CasinoFarkleTableBlock(AbstractBlock.Properties.of(Material.WOOD)),
                new CasinoFountainBlock(AbstractBlock.Properties.of(Material.STONE)),
                new RouletteTableBlock(AbstractBlock.Properties.of(Material.WOOD))
        };
    }

    private TestWorld world(double centerX, double centerZ, double size) throws Exception {
        Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
        unsafe.setAccessible(true);
        TestWorld world = (TestWorld) ((Unsafe) unsafe.get(null)).allocateInstance(TestWorld.class);
        world.border = new WorldBorder();
        world.border.setCenter(centerX, centerZ);
        world.border.setSize(size);
        world.occupied = new HashMap<>();
        return world;
    }
}
