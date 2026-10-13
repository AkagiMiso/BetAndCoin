package com.example.betandcoin.tileentity;

import com.example.betandcoin.game.roulette.*;
import com.example.betandcoin.registry.ModItems;
import net.minecraft.block.Blocks;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.RegistryObject;
import org.junit.*;
import java.lang.reflect.*;
import java.util.UUID;
import static org.junit.Assert.*;

/** Exercise real settlement and NBT without a running server. Vanilla items stand
 * in for the deferred chip registrations, and are restored after each test. */
public class RouletteTablePersistenceTest {
    static { net.minecraft.util.registry.Bootstrap.bootStrap(); }
    private final UUID player = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private Field value;
    private Object original5, original20;

    @Before public void chips() throws Exception {
        value = RegistryObject.class.getDeclaredField("value"); value.setAccessible(true);
        original5 = value.get(ModItems.CHIP_5); original20 = value.get(ModItems.CHIP_20);
        value.set(ModItems.CHIP_5, Items.EMERALD); value.set(ModItems.CHIP_20, Items.DIAMOND);
    }
    @After public void restore() throws Exception {
        value.set(ModItems.CHIP_5, original5); value.set(ModItems.CHIP_20, original20);
    }

    private CompoundNBT bet(String color, Item item, int count) {
        CompoundNBT bet = new CompoundNBT();
        bet.putUUID("Player", player); bet.putString("Color", color); bet.putInt("Count", count);
        bet.put("Chips", new ItemStack(item).save(new CompoundNBT()));
        return bet;
    }
    private RouletteTableTileEntity table(String phase, int target, CompoundNBT... bets) {
        RouletteTableTileEntity table = new RouletteTableTileEntity(TileEntityType.CHEST);
        CompoundNBT tag = new CompoundNBT(); tag.putString("Phase", phase);
        tag.putInt("TargetSegment", target); tag.putLong("PhaseStart", 12345);
        ListNBT list = new ListNBT(); for (CompoundNBT bet : bets) list.add(bet);
        tag.put("Bets", list); table.load(Blocks.AIR.defaultBlockState(), tag);
        return table;
    }
    private void settle(RouletteTableTileEntity table) throws Exception {
        Method settle = RouletteTableTileEntity.class.getDeclaredMethod("settle");
        settle.setAccessible(true); settle.invoke(table);
    }

    @Test public void offlineCreditsKeepDifferentChipTagsThroughSettlementAndSave() throws Exception {
        CompoundNBT first = bet("GREEN", Items.EMERALD, 1);
        CompoundNBT second = bet("GREEN", Items.EMERALD, 2);
        ItemStack namedFirst = new ItemStack(Items.EMERALD);
        ItemStack namedSecond = new ItemStack(Items.EMERALD);
        namedFirst.setHoverName(new StringTextComponent("First chip"));
        namedSecond.setHoverName(new StringTextComponent("Second chip"));
        first.put("Chips", namedFirst.save(new CompoundNBT()));
        second.put("Chips", namedSecond.save(new CompoundNBT()));
        RouletteTableTileEntity table = table("RESULT_FLASH", 14, first, second);
        settle(table);
        ListNBT credits = table.save(new CompoundNBT()).getList("Credits", 10);
        assertEquals(2, credits.size());
        assertEquals(10, credits.getCompound(0).getInt("Count"));
        assertEquals(20, credits.getCompound(1).getInt("Count"));
        assertTrue(ItemStack.tagMatches(namedFirst, ItemStack.of(credits.getCompound(0).getCompound("Chips"))));
        assertTrue(ItemStack.tagMatches(namedSecond, ItemStack.of(credits.getCompound(1).getCompound("Chips"))));
        RouletteTableTileEntity restored = new RouletteTableTileEntity(TileEntityType.CHEST);
        restored.load(Blocks.AIR.defaultBlockState(), table.save(new CompoundNBT()));
        assertEquals(credits, restored.save(new CompoundNBT()).getList("Credits", 10));
    }

    @Test public void largeStakesAndOfflineRewardsSurviveSaveAndDoNotPayTwice() throws Exception {
        RouletteTableTileEntity table = table("RESULT_FLASH", 14,
                bet("GREEN", Items.DIAMOND, 320), bet("RED", Items.EMERALD, 200));
        assertEquals(320, table.getBets().get(0).chips.getCount());
        settle(table);
        CompoundNBT saved = table.save(new CompoundNBT());
        ListNBT credits = saved.getList("Credits", 10);
        assertEquals(1, credits.size());
        assertEquals(3200, credits.getCompound(0).getInt("Count"));
        assertEquals(1, saved.getList("Bets", 10).getCompound(0).getCompound("Chips").getByte("Count"));
        RouletteTableTileEntity restored = new RouletteTableTileEntity(TileEntityType.CHEST);
        restored.load(Blocks.AIR.defaultBlockState(), saved);
        assertEquals(320, restored.getBets().get(0).chips.getCount());
        assertEquals(RouletteRules.Phase.SETTLED_RESULT, restored.getPhase());
        settle(restored);
        assertEquals(credits, restored.save(new CompoundNBT()).getList("Credits", 10));
        Method reset = RouletteTableTileEntity.class.getDeclaredMethod("reset");
        reset.setAccessible(true); reset.invoke(restored);
        assertTrue(restored.getBets().isEmpty());
        assertEquals(credits, restored.save(new CompoundNBT()).getList("Credits", 10));
    }

    @Test public void settlementPaysOnlyWinningColorAndIncludesPrincipal() throws Exception {
        for (int target : new int[] {0, 1, 14}) {
            RouletteTableTileEntity table = table("SPINNING", target,
                    bet("RED", Items.EMERALD, 4), bet("BLACK", Items.EMERALD, 4), bet("GREEN", Items.EMERALD, 4));
            settle(table);
            ListNBT credits = table.save(new CompoundNBT()).getList("Credits", 10);
            assertEquals(1, credits.size());
            assertEquals(4 * RouletteLayout.segment(target).color.totalReturn, credits.getCompound(0).getInt("Count"));
        }
    }

    @Test public void mixedPlayersAndDenominationsCannotChangeAFullVisibleStack() throws Exception {
        RouletteTableTileEntity table = table("BETTING", 0);
        Method remember = RouletteTableTileEntity.class.getDeclaredMethod("rememberVisibleChip", UUID.class,
                RouletteRules.Color.class, ItemStack.class);
        remember.setAccessible(true);
        UUID other = UUID.randomUUID();
        for (int i = 0; i < 5; i++) remember.invoke(table, i % 2 == 0 ? player : other,
                RouletteRules.Color.RED, new ItemStack(i % 2 == 0 ? Items.EMERALD : Items.DIAMOND));
        ListNBT original = table.save(new CompoundNBT()).getList("VisibleChips", 10);
        for (int i = 0; i < 100; i++) remember.invoke(table, other, RouletteRules.Color.RED, new ItemStack(Items.DIAMOND));
        assertEquals(5, table.getVisibleChips().size());
        assertEquals(original, table.save(new CompoundNBT()).getList("VisibleChips", 10));
        remember.invoke(table, player, RouletteRules.Color.GREEN, new ItemStack(Items.EMERALD));
        assertEquals(6, table.getVisibleChips().size());
        CompoundNBT saved = table.save(new CompoundNBT());
        RouletteTableTileEntity restored = new RouletteTableTileEntity(TileEntityType.CHEST);
        restored.load(Blocks.AIR.defaultBlockState(), saved);
        assertEquals(saved.getList("VisibleChips", 10), restored.save(new CompoundNBT()).getList("VisibleChips", 10));
    }
}
