package com.example.betandcoin.tileentity;

import com.example.betandcoin.registry.ModTileEntities;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.registry.Bootstrap;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.RegistryObject;
import org.junit.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;
import static org.junit.Assert.*;

/** Exercise the shared settlement ledger without starting a Minecraft server. */
public class SlotMachineSettlementTest {
    static { Bootstrap.bootStrap(); }

    @Test public void everyDrawnCombinationRetainsOriginalWinningRule() throws Exception {
        for (int left = 0; left < 4; left++) {
            for (int middle = 0; middle < 4; middle++) {
                for (int right = 0; right < 4; right++) {
                    SlotMachineTileEntity machine = machine(left, middle, right, new ItemStack(Items.EMERALD));
                    ItemStack reward = settle(machine);
                    boolean win = left == middle && middle == right;
                    assertEquals(win ? 2 : 0, reward.getCount());
                    assertEquals(win, machine.isSignLit());
                    assertFalse(machine.isSpinning());
                    assertTrue(machine.getCurrentBet().isEmpty());
                }
            }
        }
    }

    @Test public void removingAtAnyReelStageUsesAlreadyDrawnResult() throws Exception {
        for (int ticks : new int[] {0, 39, 40, 54, 55, 69, 70}) {
            for (boolean win : new boolean[] {false, true}) {
                SlotMachineTileEntity machine = machine(3, 3, win ? 3 : 0, new ItemStack(Items.DIAMOND));
                Field timer = SlotMachineTileEntity.class.getDeclaredField("spinTicks");
                timer.setAccessible(true);
                timer.set(machine, ticks);
                assertEquals(win ? 2 : 0, settle(machine).getCount());
                assertEquals(3, machine.getLeftReel());
                assertEquals(3, machine.getMiddleReel());
                assertEquals(win ? 3 : 0, machine.getRightReel());
            }
        }
    }

    @Test public void repeatedSettlementAndSubsequentTicksCannotPayAgain() throws Exception {
        SlotMachineTileEntity machine = machine(1, 1, 1, new ItemStack(Items.EMERALD));
        assertEquals(2, settle(machine).getCount());
        assertTrue(settle(machine).isEmpty());
        machine.tick();
        assertTrue(settle(machine).isEmpty());
        assertTrue(machine.getCurrentBet().isEmpty());
    }

    @Test public void rewardPreservesDenominationAndTagsWithoutChangingInput() throws Exception {
        for (ItemStack bet : new ItemStack[] {new ItemStack(Items.EMERALD, 12), new ItemStack(Items.DIAMOND, 8)}) {
            bet.setHoverName(new StringTextComponent("Named chip"));
            int originalCount = bet.getCount();
            ItemStack reward = settle(machine(2, 2, 2, bet));
            assertEquals(2, reward.getCount());
            assertSame(bet.getItem(), reward.getItem());
            assertTrue(ItemStack.tagMatches(bet, reward));
            assertEquals(originalCount, bet.getCount());
        }
    }

    @Test public void completedSettlementCannotPayAgainAfterSaveAndLoad() throws Exception {
        SlotMachineTileEntity original = machine(0, 0, 0, new ItemStack(Items.EMERALD));
        assertEquals(2, settle(original).getCount());
        SlotMachineTileEntity restored = emptyMachine();
        restored.load(Blocks.AIR.defaultBlockState(), original.save(new CompoundNBT()));
        assertFalse(restored.isSpinning());
        assertTrue(restored.getCurrentBet().isEmpty());
        assertTrue(settle(restored).isEmpty());
    }

    @Test public void inProgressSavedDrawStillSettlesWithoutRerolling() throws Exception {
        for (boolean win : new boolean[] {true, false}) {
            SlotMachineTileEntity original = machine(1, 1, win ? 1 : 2, new ItemStack(Items.DIAMOND));
            SlotMachineTileEntity restored = emptyMachine();
            restored.load(Blocks.AIR.defaultBlockState(), original.save(new CompoundNBT()));
            assertTrue(restored.isSpinning());
            assertEquals(win ? 2 : 0, settle(restored).getCount());
            assertTrue(settle(restored).isEmpty());
        }
    }

    @Test public void settledMachineCanRunAnotherIndependentSpin() throws Exception {
        SlotMachineTileEntity machine = machine(0, 0, 0, new ItemStack(Items.EMERALD));
        assertEquals(2, settle(machine).getCount());
        machine.startSpin(draw(2, 2, 2), new ItemStack(Items.DIAMOND));
        ItemStack second = settle(machine);
        assertEquals(2, second.getCount());
        assertSame(Items.DIAMOND, second.getItem());
        assertTrue(settle(machine).isEmpty());
    }

    private SlotMachineTileEntity machine(int left, int middle, int right, ItemStack bet) throws Exception {
        SlotMachineTileEntity machine = emptyMachine();
        machine.startSpin(draw(left, middle, right), bet);
        return machine;
    }

    private Random draw(int left, int middle, int right) {
        return new Random() {
            private int index;
            private final int[] results = {left, middle, right};
            @Override public int nextInt(int bound) { return results[index++]; }
        };
    }

    private SlotMachineTileEntity emptyMachine() throws Exception {
        Field value = RegistryObject.class.getDeclaredField("value");
        value.setAccessible(true);
        Object previous = value.get(ModTileEntities.SLOT_MACHINE);
        try {
            value.set(ModTileEntities.SLOT_MACHINE, TileEntityType.CHEST);
            return new SlotMachineTileEntity();
        } finally { value.set(ModTileEntities.SLOT_MACHINE, previous); }
    }

    private ItemStack settle(SlotMachineTileEntity machine) throws Exception {
        Method settle = SlotMachineTileEntity.class.getDeclaredMethod("takeSpinReward");
        settle.setAccessible(true);
        return (ItemStack) settle.invoke(machine);
    }
}
