package com.example.betandcoin.tileentity;

import com.example.betandcoin.block.BlackjackTableBlock;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.*;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Bootstrap;
import net.minecraft.world.server.ServerWorld;
import org.junit.Test;
import sun.misc.Unsafe;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;
import static com.example.betandcoin.tileentity.BlackjackTableTileEntity.*;
import static org.junit.Assert.*;

/** Real periodic departure check, with captured refunds and dirty-state notifications. */
public class BlackjackDeparturePersistenceTest {
    static { Bootstrap.bootStrap(); }
    private static final UUID STAYING = new UUID(1, 1), LEAVING = new UUID(2, 2);
    private static class Players extends DedicatedPlayerList {
        ServerPlayerEntity staying;
        private Players() { super(null, null, null); }
        @Override public ServerPlayerEntity getPlayer(UUID id) { return STAYING.equals(id) ? staying : null; }
    }
    private static class Player extends ServerPlayerEntity {
        boolean alive;
        private Player() { super(null, null, new GameProfile(STAYING, "audit"), null); }
        @Override public boolean isAlive() { return alive; }
        @Override public double distanceToSqr(double x, double y, double z) { return 0; }
    }
    private static class World extends ServerWorld {
        MinecraftServer server;
        List<ItemStack> drops;
        private World() { super(null, null, null, null, null, null, null, null, false, 0L, null, false); }
        @Override public MinecraftServer getServer() { return server; }
        @Override public BlockState getBlockState(BlockPos pos) {
            return new BlackjackTableBlock(AbstractBlock.Properties.of(Material.WOOD)).defaultBlockState();
        }
        @Override public <T extends Entity> List<T> getEntitiesOfClass(Class<? extends T> type,
                AxisAlignedBB box, Predicate<? super T> predicate) { return Collections.emptyList(); }
        @Override public boolean addFreshEntity(Entity entity) {
            drops.add(((ItemEntity) entity).getItem().copy()); return true;
        }
        @Override public void sendBlockUpdated(BlockPos pos, BlockState old, BlockState next, int flags) {}
    }
    private static class Table extends BlackjackTableTileEntity {
        int changes;
        Table() { super(TileEntityType.CHEST); }
        @Override public void setChanged() { changes++; }
        @Override public Vector3d getSlotAnchor(int slot) { return Vector3d.ZERO; }
        @Override public void playAnimation(String animation, boolean loop) {}
    }

    @Test public void offlineCandidateRefundIsPersistedWhileCurrentPlayerWaits() throws Exception {
        World world = allocate(World.class);
        world.drops = new ArrayList<>();
        DedicatedServer server = allocate(DedicatedServer.class);
        Players players = allocate(Players.class);
        Player staying = allocate(Player.class);
        staying.alive = true; staying.level = world; players.staying = staying;
        server.setPlayerList(players); world.server = server;
        Table table = new Table();
        table.setLevelAndPosition(world, new BlockPos(0, 64, 0));
        set(table, "phase", Phase.PLAYER_TURNS);
        set(table, "currentTurnSlot", 0);
        ((boolean[]) get(table, "activeSlots"))[0] = true;
        ((boolean[]) get(table, "activeSlots"))[1] = true;
        ((UUID[]) get(table, "slotPlayers"))[0] = STAYING;
        ((UUID[]) get(table, "slotPlayers"))[1] = LEAVING;
        ((WagerState[]) get(table, "wagerStates"))[0] = WagerState.CURRENT_ROUND;
        ((WagerState[]) get(table, "wagerStates"))[1] = WagerState.NEXT_ROUND_PREPAID;
        ((ItemStack[]) get(table, "sessionBetStacks"))[1] = new ItemStack(Items.EMERALD);
        Method check = BlackjackTableTileEntity.class.getDeclaredMethod("checkPlayersStillPresent");
        check.setAccessible(true);
        for (int i = 0; i < 5; i++) check.invoke(table);
        assertEquals(Phase.PLAYER_TURNS, table.getPhase());
        assertTrue(table.isSlotActive(0));
        assertFalse(table.isSlotActive(1));
        assertEquals(1, world.drops.size());
        assertEquals(1, world.drops.get(0).getCount());
        assertTrue("Refund and cleared reservation must dirty the table without a phase change", table.changes > 0);
        for (int i = 0; i < 5; i++) check.invoke(table);
        assertEquals(1, world.drops.size());
    }

    @Test public void deadPlayerCannotMakeAGestureEvenBeforePeriodicDepartureCheck() throws Exception {
        Table table = new Table();
        set(table, "phase", Phase.PLAYER_TURNS);
        set(table, "currentTurnSlot", 0);
        Player dead = allocate(Player.class);
        assertEquals(GestureResult.NOT_OWNER, table.handlePlayerGesture(0, dead, true));
        assertEquals(Phase.PLAYER_TURNS, table.getPhase());
    }

    private static Object get(Object table, String name) throws Exception {
        Field f = BlackjackTableTileEntity.class.getDeclaredField(name);
        f.setAccessible(true); return f.get(table);
    }
    private static void set(Object table, String name, Object value) throws Exception {
        Field f = BlackjackTableTileEntity.class.getDeclaredField(name);
        f.setAccessible(true); f.set(table, value);
    }
    private static <T> T allocate(Class<T> type) throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true);
        return type.cast(((Unsafe) f.get(null)).allocateInstance(type));
    }
}
