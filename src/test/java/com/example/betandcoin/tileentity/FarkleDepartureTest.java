package com.example.betandcoin.tileentity;

import com.example.betandcoin.game.farkle.FarklePhase;
import com.example.betandcoin.registry.ModTileEntities;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedPlayerList;
import net.minecraft.tileentity.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Bootstrap;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.RegistryObject;
import org.junit.Test;
import sun.misc.Unsafe;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;

/** Run actual departure, refund and payout code with controlled players and captured world drops. */
public class FarkleDepartureTest {
    static { Bootstrap.bootStrap(); }
    private static final UUID LEFT = new UUID(1, 1), RIGHT = new UUID(2, 2);

    private static class TestPlayers extends DedicatedPlayerList {
        Map<UUID, ServerPlayerEntity> online;
        private TestPlayers() { super(null, null, null); }
        @Override public ServerPlayerEntity getPlayer(UUID id) { return online.get(id); }
    }
    private static class TestWorld extends ServerWorld {
        MinecraftServer server;
        List<ItemStack> drops;
        CasinoFarkleTableTileEntity table;
        boolean assertRefundConsumed;
        private TestWorld() { super(null, null, null, null, null, null, null, null, false, 0L, null, false); }
        @Override public MinecraftServer getServer() { return server; }
        @Override public BlockState getBlockState(BlockPos pos) { return Blocks.AIR.defaultBlockState(); }
        @Override public void blockEntityChanged(BlockPos pos, TileEntity tile) {}
        @Override public void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState state, int flags) {}
        @Override public boolean addFreshEntity(Entity entity) {
            if (assertRefundConsumed) {
                try { for (ItemStack stake : (ItemStack[]) get(table, "stakes")) assertTrue(stake.isEmpty()); }
                catch (Exception error) { throw new AssertionError(error); }
            }
            drops.add(((ItemEntity) entity).getItem().copy());
            return true;
        }
    }
    private static class Player extends ServerPlayerEntity {
        double distance;
        List<ItemStack> drops;
        private Player() { super(null, null, new GameProfile(LEFT, "audit"), null); }
        @Override public boolean isAlive() { return true; }
        @Override public double distanceToSqr(double x, double y, double z) { return distance; }
        @Override public ItemEntity drop(ItemStack stack, boolean random) { drops.add(stack.copy()); return null; }
    }
    private static class Fixture {
        CasinoFarkleTableTileEntity table;
        TestWorld world;
        TestPlayers players;
    }

    @Test public void bothOfflineRefundSeparatelyAcrossEveryUndecidedPhase() throws Exception {
        for (FarklePhase phase : FarklePhase.values()) {
            if (phase == FarklePhase.SETTLING) continue;
            Fixture f = fixture(phase);
            f.world.assertRefundConsumed = true;
            ItemStack[] before = ((ItemStack[]) get(f.table, "stakes")).clone();
            check(f.table);
            assertEquals(FarklePhase.WAITING, f.table.getPhase());
            assertEquals(2, f.world.drops.size());
            for (int side = 0; side < 2; side++) {
                assertSame(before[side].getItem(), f.world.drops.get(side).getItem());
                assertEquals(before[side].getCount(), f.world.drops.get(side).getCount());
                assertTrue(ItemStack.tagMatches(before[side], f.world.drops.get(side)));
            }
            assertEquals(-1, (int) get(f.table, "winningSide"));
            for (UUID player : (UUID[]) get(f.table, "players")) assertNull(player);
        }
    }

    @Test public void oneDepartureKeepsOtherSideWinningRegardlessOfSeatOrder() throws Exception {
        for (FarklePhase phase : new FarklePhase[] {FarklePhase.NEED_ROLL, FarklePhase.ROLLING,
                FarklePhase.SELECTING, FarklePhase.DECISION}) {
            for (int winner = 0; winner < 2; winner++) {
                Fixture f = fixture(phase);
                Player remaining = player(f, 0);
                f.players.online.put(winner == 0 ? LEFT : RIGHT, remaining);
                check(f.table);
                assertEquals(FarklePhase.SETTLING, f.table.getPhase());
                assertEquals(winner, (int) get(f.table, "winningSide"));
                assertTrue(f.world.drops.isEmpty());
                invoke(f.table, "completePayout");
                assertEquals(9, count(remaining, Items.EMERALD));
                assertEquals(22, count(remaining, Items.DIAMOND));
            }
        }
    }

    @Test public void oneDepartureBeforeStartRefundsOnlyDepartingStake() throws Exception {
        for (FarklePhase phase : new FarklePhase[] {FarklePhase.WAITING, FarklePhase.STARTING}) {
            Fixture f = fixture(phase);
            f.players.online.put(RIGHT, player(f, 0));
            check(f.table);
            assertEquals(FarklePhase.WAITING, f.table.getPhase());
            assertEquals(1, f.world.drops.size());
            assertSame(Items.EMERALD, f.world.drops.get(0).getItem());
            assertEquals(RIGHT, ((UUID[]) get(f.table, "players"))[1]);
            assertEquals(22, ((ItemStack[]) get(f.table, "stakes"))[1].getCount());
        }
    }

    @Test public void twoPresentPlayersKeepCurrentGameAndWagers() throws Exception {
        Fixture f = fixture(FarklePhase.SELECTING);
        f.players.online.put(LEFT, player(f, 0));
        f.players.online.put(RIGHT, player(f, 0));
        check(f.table);
        assertEquals(FarklePhase.SELECTING, f.table.getPhase());
        assertEquals(9, ((ItemStack[]) get(f.table, "stakes"))[0].getCount());
        assertEquals(22, ((ItemStack[]) get(f.table, "stakes"))[1].getCount());
        assertTrue(f.world.drops.isEmpty());
    }

    @Test public void bothOnlineButAwayReceiveTheirOwnRefundsInInventory() throws Exception {
        Fixture f = fixture(FarklePhase.DECISION);
        Player left = player(f, 100), right = player(f, 100);
        f.players.online.put(LEFT, left);
        f.players.online.put(RIGHT, right);
        check(f.table);
        assertEquals(9, count(left, Items.EMERALD));
        assertEquals(0, count(left, Items.DIAMOND));
        assertEquals(22, count(right, Items.DIAMOND));
        assertEquals(0, count(right, Items.EMERALD));
        assertTrue(f.world.drops.isEmpty());
    }

    @Test public void establishedWinnerIsNotOverriddenAndPayoutOccursOnce() throws Exception {
        for (int winner = 0; winner < 2; winner++) {
            Fixture f = fixture(FarklePhase.SETTLING);
            set(f.table, "winningSide", winner);
            set(f.table, "settlementLocked", true);
            check(f.table);
            assertEquals(FarklePhase.SETTLING, f.table.getPhase());
            assertEquals(winner, (int) get(f.table, "winningSide"));
            assertTrue(f.world.drops.isEmpty());
            invoke(f.table, "completePayout");
            invoke(f.table, "completePayout");
            assertEquals(2, f.world.drops.size());
            assertTrue((boolean) get(f.table, "payoutCompleted"));
        }
    }

    @Test public void repeatedCheckTickAndTableRemovalDoNotDuplicateRefunds() throws Exception {
        Fixture f = fixture(FarklePhase.ROLLING);
        check(f.table);
        check(f.table);
        f.table.tick();
        f.table.releaseStakesOnBreak();
        assertEquals(2, f.world.drops.size());
        for (ItemStack stack : (ItemStack[]) get(f.table, "stakes")) assertTrue(stack.isEmpty());
    }

    @Test public void restoredUndecidedGameRefundsBothOfflinePlayersWithoutAssigningWinner() throws Exception {
        Fixture f = fixture(FarklePhase.SELECTING);
        CompoundNBT saved = f.table.save(new CompoundNBT());
        Fixture restored = fixture(FarklePhase.WAITING);
        restored.table.load(Blocks.AIR.defaultBlockState(), saved);
        check(restored.table);
        assertEquals(2, restored.world.drops.size());
        assertEquals(-1, (int) get(restored.table, "winningSide"));
        restored.table.load(Blocks.AIR.defaultBlockState(), restored.table.save(new CompoundNBT()));
        check(restored.table);
        assertEquals(2, restored.world.drops.size());
    }

    private Fixture fixture(FarklePhase phase) throws Exception {
        Fixture f = new Fixture();
        f.world = allocate(TestWorld.class);
        f.world.drops = new ArrayList<>();
        f.players = allocate(TestPlayers.class);
        f.players.online = new HashMap<>();
        f.world.server = allocate(DedicatedServer.class);
        field(MinecraftServer.class, "playerList").set(f.world.server, f.players);
        Field value = field(RegistryObject.class, "value");
        Object original = value.get(ModTileEntities.CASINO_FARKLE_TABLE);
        try {
            value.set(ModTileEntities.CASINO_FARKLE_TABLE, TileEntityType.CHEST);
            f.table = new CasinoFarkleTableTileEntity();
        } finally { value.set(ModTileEntities.CASINO_FARKLE_TABLE, original); }
        f.world.table = f.table;
        f.table.setLevelAndPosition(f.world, BlockPos.ZERO);
        set(f.table, "phase", phase);
        UUID[] ids = (UUID[]) get(f.table, "players");
        ids[0] = LEFT; ids[1] = RIGHT;
        ItemStack[] stakes = (ItemStack[]) get(f.table, "stakes");
        stakes[0] = new ItemStack(Items.EMERALD, 9);
        stakes[1] = new ItemStack(Items.DIAMOND, 22);
        stakes[0].setHoverName(new StringTextComponent("left"));
        stakes[1].setHoverName(new StringTextComponent("right"));
        return f;
    }
    private Player player(Fixture f, double distance) throws Exception {
        Player player = allocate(Player.class);
        player.level = f.world;
        player.distance = distance;
        player.drops = new ArrayList<>();
        field(PlayerEntity.class, "abilities").set(player, new PlayerAbilities());
        field(PlayerEntity.class, "inventory").set(player, new PlayerInventory(player));
        return player;
    }
    private int count(Player player, Item item) {
        int count = 0;
        for (ItemStack stack : player.inventory.items) if (stack.getItem() == item) count += stack.getCount();
        return count;
    }
    private void check(CasinoFarkleTableTileEntity table) throws Exception { invoke(table, "checkDepartures"); }
    private void invoke(CasinoFarkleTableTileEntity table, String name) throws Exception {
        Method method = CasinoFarkleTableTileEntity.class.getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(table);
    }
    private static Object get(CasinoFarkleTableTileEntity table, String name) throws Exception {
        return field(CasinoFarkleTableTileEntity.class, name).get(table);
    }
    private void set(CasinoFarkleTableTileEntity table, String name, Object value) throws Exception {
        field(CasinoFarkleTableTileEntity.class, name).set(table, value);
    }
    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
    private <T> T allocate(Class<T> type) throws Exception {
        return type.cast(((Unsafe) field(Unsafe.class, "theUnsafe").get(null)).allocateInstance(type));
    }
}
