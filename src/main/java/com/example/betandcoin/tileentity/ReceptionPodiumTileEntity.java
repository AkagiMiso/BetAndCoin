package com.example.betandcoin.tileentity;

import com.example.betandcoin.block.ReceptionPodiumBlock;
import com.example.betandcoin.registry.ModTileEntities;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;

public class ReceptionPodiumTileEntity extends TileEntity implements ITickableTileEntity {
    private int flickerTick = -1;

    public ReceptionPodiumTileEntity() {
        super(ModTileEntities.RECEPTION_PODIUM.get());
    }

    public void startPlacementFlicker() {
        if (level != null && !level.isClientSide) {
            flickerTick = 0;
            setChanged();
        }
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide || flickerTick < 0) {
            return;
        }
        flickerTick++;
        switch (flickerTick) {
            case 3:
            case 7:
                setLit(false);
                break;
            case 5:
            case 16:
                setLit(true);
                break;
            default:
                break;
        }
        if (flickerTick >= 30) {
            flickerTick = -1;
            setLit(true);
        }
        setChanged();
    }

    private void setLit(boolean lit) {
        BlockState state = getBlockState();
        if (state.hasProperty(ReceptionPodiumBlock.LIT)
                && state.getValue(ReceptionPodiumBlock.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(ReceptionPodiumBlock.LIT, lit), 3);
            level.getLightEngine().checkBlock(worldPosition);
        }
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.putInt("FlickerTick", flickerTick);
        return nbt;
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        flickerTick = nbt.contains("FlickerTick") ? nbt.getInt("FlickerTick") : -1;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(worldPosition, worldPosition.offset(1, 2, 1));
    }
}
