package com.example.betandcoin.block;

import com.example.betandcoin.registry.ModBlocks;
import com.example.betandcoin.tileentity.RouletteTableTileEntity;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.*;
import net.minecraft.fluid.FluidState;
import net.minecraft.tileentity.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Bootstrap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.RegistryObject;
import org.junit.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.*;
import static org.junit.Assert.*;

/** Run the real player-removal and recursive onRemove callbacks for every footprint cell. */
public class FurnitureRemovalTest {
    static { Bootstrap.bootStrap(); }
    private static final BlockPos MAIN = new BlockPos(10, 64, 10);
    private static final Direction[] FACINGS = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    private static class Player extends ServerPlayerEntity {
        boolean creative;
        private Player() { super(null, null, new GameProfile(UUID.randomUUID(), "audit"), null); }
        @Override public boolean isCreative() { return creative; }
    }
    private static class TestWorld extends ServerWorld {
        Map<BlockPos, BlockState> states;
        List<Boolean> mainDropRequests;
        RouletteTableTileEntity roulette;
        private TestWorld() { super(null, null, null, null, null, null, null, null, false, 0L, null, false); }
        @Override public BlockState getBlockState(BlockPos pos) { return states.getOrDefault(pos, Blocks.AIR.defaultBlockState()); }
        @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
        @Override public TileEntity getBlockEntity(BlockPos pos) { return pos.equals(MAIN) ? roulette : null; }
        @Override public void removeBlockEntity(BlockPos pos) { if (pos.equals(MAIN)) roulette = null; }
        @Override public void levelEvent(PlayerEntity player, int id, BlockPos pos, int data) {}
        @Override public void blockEntityChanged(BlockPos pos, TileEntity tile) {}
        @Override public boolean setBlock(BlockPos pos, BlockState state, int flags) {
            BlockState old = getBlockState(pos);
            if (old == state) return false;
            if (state.isAir()) states.remove(pos); else states.put(pos.immutable(), state);
            // Real worlds install the new state before dispatching the old block's callback.
            old.onRemove(this, pos, state, false);
            return true;
        }
        @Override public boolean destroyBlock(BlockPos pos, boolean drops, Entity entity, int recursion) {
            if (getBlockState(pos).isAir()) return false;
            assertEquals("Only the authoritative main block may request a furniture drop", MAIN, pos);
            mainDropRequests.add(drops);
            return setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @Test public void creativeBreakingEveryFootprintCellRemovesWholeFurnitureWithoutDropRequests() throws Exception {
        exerciseParts(true);
    }
    @Test public void survivalBreakingEveryFootprintCellRequestsExactlyOneMainDrop() throws Exception {
        exerciseParts(false);
    }
    @Test public void breakingMainDoesNotRequestAnExtraDropFromAnyLinkedCell() throws Exception {
        withBindings(() -> {
            for (int type = 0; type < 4; type++) for (Direction facing : FACINGS) for (boolean creative : new boolean[]{true, false}) {
                TestWorld world = world(type, facing);
                Player player = player(creative);
                BlockState main = world.getBlockState(MAIN);
                assertTrue(main.removedByPlayer(world, MAIN, player, !creative, main.getFluidState()));
                assertTrue(world.states.isEmpty());
                assertTrue(world.mainDropRequests.isEmpty());
            }
        });
    }

    private void exerciseParts(boolean creative) throws Exception {
        withBindings(() -> {
            for (int type = 0; type < 4; type++) for (Direction facing : FACINGS) {
                List<BlockPos> cells = new ArrayList<>(world(type, facing).states.keySet());
                cells.remove(MAIN);
                for (BlockPos clicked : cells) {
                    TestWorld world = world(type, facing);
                    BlockState state = world.getBlockState(clicked);
                    state.removedByPlayer(world, clicked, player(creative), !creative, state.getFluidState());
                    assertTrue("Linked furniture left behind", world.states.isEmpty());
                    assertEquals(Collections.singletonList(!creative), world.mainDropRequests);
                    // Repeated callbacks after the main is gone must not request a second item.
                    state.onRemove(world, clicked, Blocks.AIR.defaultBlockState(), false);
                    assertEquals(1, world.mainDropRequests.size());
                }
            }
        });
    }

    private TestWorld world(int type, Direction facing) throws Exception {
        TestWorld world = allocate(TestWorld.class);
        world.states = new HashMap<>(); world.mainDropRequests = new ArrayList<>();
        Block main = type == 0 ? ModBlocks.BLACKJACK_TABLE.get() : type == 1 ? ModBlocks.CASINO_FOUNTAIN.get()
                : type == 2 ? ModBlocks.CASINO_FARKLE_TABLE.get() : ModBlocks.ROULETTE_TABLE.get();
        world.states.put(MAIN, main.defaultBlockState().setValue(HorizontalBlock.FACING, facing));
        if (type == 0) for (BlackjackTablePartBlock.TablePart part : BlackjackTablePartBlock.TablePart.values()) {
            world.states.put(BlackjackTableBlock.getPartBlockPos(MAIN, facing, part),
                    ModBlocks.BLACKJACK_TABLE_PART.get().defaultBlockState().setValue(BlackjackTablePartBlock.FACING, facing)
                            .setValue(BlackjackTablePartBlock.PART, part));
        }
        if (type == 1) for (CasinoFountainPartBlock.FountainPart part : CasinoFountainPartBlock.FountainPart.values()) {
            world.states.put(CasinoFountainBlock.getPartBlockPos(MAIN, facing, part),
                    ModBlocks.CASINO_FOUNTAIN_PART.get().defaultBlockState().setValue(CasinoFountainPartBlock.FACING, facing)
                            .setValue(CasinoFountainPartBlock.PART, part));
        }
        if (type >= 2) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (x == 0 && z == 0) continue;
            BlockState part = type == 2 ? ModBlocks.CASINO_FARKLE_TABLE_PART.get().defaultBlockState()
                    .setValue(CasinoFarkleTablePartBlock.OFFSET_X, x + 1).setValue(CasinoFarkleTablePartBlock.OFFSET_Z, z + 1)
                    : ModBlocks.ROULETTE_TABLE_PART.get().defaultBlockState().setValue(HorizontalBlock.FACING, facing)
                    .setValue(RouletteTablePartBlock.OFFSET_X, x + 1).setValue(RouletteTablePartBlock.OFFSET_Z, z + 1);
            world.states.put(MAIN.offset(x, 0, z), part);
        }
        if (type == 3) {
            world.roulette = new RouletteTableTileEntity(TileEntityType.CHEST);
            world.roulette.setLevelAndPosition(world, MAIN);
        }
        return world;
    }

    private Player player(boolean creative) throws Exception { Player player = allocate(Player.class); player.creative = creative; return player; }
    private interface Checked { void run() throws Exception; }
    private void withBindings(Checked task) throws Exception {
        RegistryObject<?>[] registries = {ModBlocks.BLACKJACK_TABLE, ModBlocks.BLACKJACK_TABLE_PART,
                ModBlocks.CASINO_FOUNTAIN, ModBlocks.CASINO_FOUNTAIN_PART,
                ModBlocks.CASINO_FARKLE_TABLE, ModBlocks.CASINO_FARKLE_TABLE_PART,
                ModBlocks.ROULETTE_TABLE, ModBlocks.ROULETTE_TABLE_PART};
        Block[] blocks = {new BlackjackTableBlock(properties()), new BlackjackTablePartBlock(properties()),
                new CasinoFountainBlock(properties()), new CasinoFountainPartBlock(properties()),
                new CasinoFarkleTableBlock(properties()), new CasinoFarkleTablePartBlock(properties()),
                new RouletteTableBlock(properties()), new RouletteTablePartBlock(properties())};
        Field value = RegistryObject.class.getDeclaredField("value"); value.setAccessible(true);
        Field tag = BlockTags.GUARDED_BY_PIGLINS.getClass().getDeclaredField("tag"); tag.setAccessible(true);
        Object originalTag = tag.get(BlockTags.GUARDED_BY_PIGLINS);
        // The headless fixture has no data-pack reload; these wooden furnishings are not guarded.
        tag.set(BlockTags.GUARDED_BY_PIGLINS, ITag.fromSet(Collections.emptySet()));
        Object[] originals = new Object[registries.length];
        for (int i = 0; i < registries.length; i++) { originals[i] = value.get(registries[i]); value.set(registries[i], blocks[i]); }
        try { task.run(); } finally {
            for (int i = 0; i < registries.length; i++) value.set(registries[i], originals[i]);
            tag.set(BlockTags.GUARDED_BY_PIGLINS, originalTag);
        }
    }
    private AbstractBlock.Properties properties() { return AbstractBlock.Properties.of(Material.WOOD); }
    private static <T> T allocate(Class<T> type) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
        return type.cast(((Unsafe) field.get(null)).allocateInstance(type));
    }
}
