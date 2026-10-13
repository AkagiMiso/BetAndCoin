package com.example.betandcoin.util;

import com.example.betandcoin.block.BlackjackTableBlock;
import com.example.betandcoin.entity.BlackjackChipEntity;
import com.example.betandcoin.entity.PlayingCardEntity;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Bootstrap;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.junit.Test;
import sun.misc.Unsafe;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

/** Actual display entities with a controlled world that rejects unloaded reads and tile lookups. */
public class BlackjackDisplayCleanupTest {
    static { Bootstrap.bootStrap(); }
    private static final BlockPos SOURCE = new BlockPos(15, 64, 15);
    private static final EntityType<PlayingCardEntity> CARD = EntityType.Builder
            .<PlayingCardEntity>of(PlayingCardEntity::new, EntityClassification.MISC).build("audit:card");
    private static final EntityType<BlackjackChipEntity> CHIP = EntityType.Builder
            .<BlackjackChipEntity>of(BlackjackChipEntity::new, EntityClassification.MISC).build("audit:chip");

    private static class TestWorld extends ServerWorld {
        boolean sourceLoaded;
        BlockState sourceState;
        int stateReads;
        private TestWorld() { super(null, null, null, null, null, null, null, null, false, 0L, null, false); }
        @Override public boolean hasChunkAt(BlockPos pos) {
            assertEquals(SOURCE, pos);
            return sourceLoaded;
        }
        @Override public BlockState getBlockState(BlockPos pos) {
            assertEquals(SOURCE, pos);
            assertTrue("Unloaded chunks must never be read", sourceLoaded);
            stateReads++;
            return sourceState;
        }
        @Override public TileEntity getBlockEntity(BlockPos pos) {
            throw new AssertionError("Do not depend on tile entity initialization order");
        }
    }

    @Test public void unloadedSourceKeepsBothDisplaysWithoutReadingOrLoadingChunk() throws Exception {
        TestWorld world = world(false, false, Blocks.AIR.defaultBlockState());
        Entity[] displays = displays(world);
        for (Entity display : displays) check(display);
        assertEquals(0, world.stateReads);
        for (Entity display : displays) assertFalse(display.removed);
        world.sourceLoaded = true;
        for (Entity display : displays) check(display);
        for (Entity display : displays) assertTrue(display.removed);
    }

    @Test public void loadedTableKeepsDisplaysEvenBeforeTileEntityInitialization() throws Exception {
        BlockState table = new BlackjackTableBlock(AbstractBlock.Properties.of(Material.WOOD)).defaultBlockState();
        TestWorld world = world(false, true, table);
        for (Entity display : displays(world)) {
            check(display);
            assertFalse(display.removed);
        }
        assertEquals(2, world.stateReads);
    }

    @Test public void confirmedMissingOrReplacedTableRemovesOnlyItsDisplays() throws Exception {
        for (BlockState replacement : new BlockState[] {Blocks.AIR.defaultBlockState(), Blocks.STONE.defaultBlockState()}) {
            TestWorld world = world(false, true, replacement);
            for (Entity display : displays(world)) {
                check(display);
                assertTrue(display.removed);
                check(display);
            }
            assertEquals(2, world.stateReads);
        }
    }

    @Test public void deckCardsAndLegacyDisplaysWithoutBlackjackSourceAreUntouched() throws Exception {
        TestWorld world = world(false, true, Blocks.AIR.defaultBlockState());
        PlayingCardEntity deckCard = new PlayingCardEntity(CARD, null);
        deckCard.setSourceDeckPos(SOURCE);
        deckCard.setLevel(world);
        BlackjackChipEntity legacyChip = new BlackjackChipEntity(CHIP, null);
        legacyChip.setLevel(world);
        check(deckCard);
        check(legacyChip);
        assertFalse(deckCard.removed);
        assertFalse(legacyChip.removed);
        assertEquals(0, world.stateReads);
    }

    @Test public void clientDoesNotRemoveDisplaysUsingUnsynchronizedSourceData() throws Exception {
        TestWorld world = world(true, true, Blocks.AIR.defaultBlockState());
        for (Entity display : displays(world)) {
            check(display);
            assertFalse(display.removed);
        }
        assertEquals(0, world.stateReads);
    }

    @Test public void checksAreThrottledWithoutChangingVisualMetadata() throws Exception {
        TestWorld world = world(false, true, Blocks.AIR.defaultBlockState());
        Entity[] displays = displays(world);
        for (Entity display : displays) {
            display.tickCount = 19;
            check(display);
            assertFalse(display.removed);
        }
        assertEquals(0, world.stateReads);
        assertEquals(2, ((PlayingCardEntity) displays[0]).getBlackjackSlotIndex());
        assertEquals(3, ((PlayingCardEntity) displays[0]).getBlackjackCardIndex());
        assertSame(Items.EMERALD, ((BlackjackChipEntity) displays[1]).getChipStack().getItem());
        for (Entity display : displays) {
            display.tickCount = 20;
            check(display);
            assertTrue(display.removed);
        }
    }

    @Test public void reloadedDisplaysRetainSourceAndAreCleanedWhenTableIsGone() throws Exception {
        TestWorld world = world(false, true, Blocks.AIR.defaultBlockState());
        for (Entity original : displays(world)) {
            CompoundNBT saved = new CompoundNBT();
            Method write = original.getClass().getDeclaredMethod("addAdditionalSaveData", CompoundNBT.class);
            write.setAccessible(true);
            write.invoke(original, saved);
            Entity restored = original instanceof PlayingCardEntity
                    ? new PlayingCardEntity(CARD, null) : new BlackjackChipEntity(CHIP, null);
            Method read = restored.getClass().getDeclaredMethod("readAdditionalSaveData", CompoundNBT.class);
            read.setAccessible(true);
            read.invoke(restored, saved);
            restored.setLevel(world);
            check(restored);
            assertTrue(restored.removed);
        }
    }

    private Entity[] displays(TestWorld world) {
        PlayingCardEntity card = new PlayingCardEntity(CARD, null);
        card.setBlackjackSource(SOURCE, false, 2, 3);
        card.setLevel(world);
        BlackjackChipEntity chip = new BlackjackChipEntity(CHIP, null);
        chip.setBlackjackSource(SOURCE, 2);
        chip.setChipStack(new ItemStack(Items.EMERALD));
        chip.setLevel(world);
        return new Entity[] {card, chip};
    }

    private void check(Entity display) {
        BlockPos source = display instanceof PlayingCardEntity
                ? ((PlayingCardEntity) display).getSourceBlackjackTablePos()
                : ((BlackjackChipEntity) display).getSourceBlackjackTablePos();
        BlackjackDisplayCleanup.removeIfSourceMissing(display, source);
    }

    private TestWorld world(boolean client, boolean loaded, BlockState state) throws Exception {
        Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
        unsafe.setAccessible(true);
        TestWorld world = (TestWorld) ((Unsafe) unsafe.get(null)).allocateInstance(TestWorld.class);
        Field clientFlag = World.class.getDeclaredField("isClientSide");
        clientFlag.setAccessible(true);
        clientFlag.set(world, client);
        world.sourceLoaded = loaded;
        world.sourceState = state;
        return world;
    }
}
