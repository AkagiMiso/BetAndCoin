package com.example.betandcoin.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.material.*;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.util.registry.Bootstrap;
import org.junit.Test;
import static org.junit.Assert.*;

public class SlotMachineStructureTest {
    static { Bootstrap.bootStrap(); }
    @Test public void pistonCannotMoveEitherHalfOfTheMachineSeparately() {
        SlotMachineBlock block = new SlotMachineBlock(AbstractBlock.Properties.of(Material.METAL));
        for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
            assertEquals(PushReaction.BLOCK, block.defaultBlockState()
                    .setValue(SlotMachineBlock.HALF, half).getPistonPushReaction());
        }
    }
}
