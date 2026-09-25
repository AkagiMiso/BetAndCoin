package com.example.betandcoin.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlackjackChipEntity extends Entity {

    private static final DataParameter<ItemStack> CHIP_STACK =
            EntityDataManager.defineId(
                    BlackjackChipEntity.class,
                    DataSerializers.ITEM_STACK
            );

    @Nullable
    private BlockPos sourceBlackjackTablePos;

    private int blackjackSlotIndex = -1;

    public BlackjackChipEntity(
            @Nonnull EntityType<? extends BlackjackChipEntity> entityType,
            @Nonnull World world
    ) {
        super(entityType, world);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(
                CHIP_STACK,
                ItemStack.EMPTY
        );
    }

    public void setChipStack(@Nonnull ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(copy.isEmpty() ? 0 : 1);
        this.entityData.set(CHIP_STACK, copy);
    }

    @Nonnull
    public ItemStack getChipStack() {
        return this.entityData.get(CHIP_STACK);
    }

    public void setBlackjackSource(
            @Nonnull BlockPos tablePos,
            int slotIndex
    ) {
        this.sourceBlackjackTablePos =
                tablePos.immutable();
        this.blackjackSlotIndex = slotIndex;
    }

    @Nullable
    public BlockPos getSourceBlackjackTablePos() {
        return this.sourceBlackjackTablePos;
    }

    public int getBlackjackSlotIndex() {
        return this.blackjackSlotIndex;
    }

    public boolean belongsToBlackjackTable(
            @Nonnull BlockPos tablePos
    ) {
        return this.sourceBlackjackTablePos != null
                && this.sourceBlackjackTablePos.equals(
                        tablePos
                );
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(Vector3d.ZERO);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(
            @Nonnull CompoundNBT compound
    ) {
        if (compound.contains("ChipStack")) {
            setChipStack(
                    ItemStack.of(
                            compound.getCompound(
                                    "ChipStack"
                            )
                    )
            );
        } else {
            setChipStack(ItemStack.EMPTY);
        }

        if (compound.contains(
                "SourceBlackjackTablePos"
        )) {
            this.sourceBlackjackTablePos =
                    BlockPos.of(
                            compound.getLong(
                                    "SourceBlackjackTablePos"
                            )
                    );
        } else {
            this.sourceBlackjackTablePos = null;
        }

        this.blackjackSlotIndex =
                compound.contains("BlackjackSlotIndex")
                        ? compound.getInt(
                                "BlackjackSlotIndex"
                        )
                        : -1;
    }

    @Override
    protected void addAdditionalSaveData(
            @Nonnull CompoundNBT compound
    ) {
        ItemStack stack = getChipStack();

        if (!stack.isEmpty()) {
            compound.put(
                    "ChipStack",
                    stack.save(new CompoundNBT())
            );
        }

        if (this.sourceBlackjackTablePos != null) {
            compound.putLong(
                    "SourceBlackjackTablePos",
                    this.sourceBlackjackTablePos.asLong()
            );
        }

        compound.putInt(
                "BlackjackSlotIndex",
                this.blackjackSlotIndex
        );
    }

    @Nonnull
    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
