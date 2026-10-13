package com.example.betandcoin.tileentity;

import com.example.betandcoin.block.CasinoFarkleTableBlock;
import com.example.betandcoin.util.ChipDelivery;
import com.example.betandcoin.game.farkle.FarklePhase;
import com.example.betandcoin.game.farkle.FarkleScoring;
import com.example.betandcoin.registry.ModItems;
import com.example.betandcoin.registry.ModTileEntities;
import net.minecraft.block.BlockState;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.loading.FMLLoader;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

/** Server-owned wager, dice, scoring and settlement state for one Farkle table. */
public class CasinoFarkleTableTileEntity extends TileEntity
        implements IAnimatable, ITickableTileEntity {
    private static final int DICE_COUNT = 6;
    private static final int BUTTON_PRESS_DURATION_TICKS = 7;
    private static final int WIN_SCORE = 10000;
    private static final int WIN_FLASH_TICKS = 60;
    private static final double MAX_DISTANCE_SQUARED = 25.0;
    private final AnimationFactory factory = new AnimationFactory(this);
    private final Random random = new Random();
    private final UUID[] players = new UUID[2];
    private final ItemStack[] stakes = {ItemStack.EMPTY, ItemStack.EMPTY};
    private final int[] bankedScores = new int[2];
    private final int[] dice = new int[DICE_COUNT];
    private final boolean[] available = new boolean[DICE_COUNT];
    private final boolean[] selected = new boolean[DICE_COUNT];
    private final int[] scoringSlots = new int[DICE_COUNT];
    private final int[] dieTransferTicks = new int[DICE_COUNT];
    private FarklePhase phase = FarklePhase.WAITING;
    private int currentSide = -1;
    private int turnSubtotal;
    private int phaseTicks;
    private int matchedWagerTicks;
    private int winningSide = -1;
    private boolean settlementLocked;
    private boolean payoutCompleted;
    private String armAnimation = "arm_hold_a";
    private int animationSequence;
    private int appliedAnimationSequence = Integer.MIN_VALUE;
    private String buttonAnimation = "";
    private int buttonAnimationSequence;
    private int appliedButtonAnimationSequence = Integer.MIN_VALUE;
    private int buttonPressAge = BUTTON_PRESS_DURATION_TICKS;

    public CasinoFarkleTableTileEntity() {
        super(ModTileEntities.CASINO_FARKLE_TABLE.get());
        Arrays.fill(available, true);
        Arrays.fill(scoringSlots, -1);
        Arrays.fill(dieTransferTicks, 8);
    }

    public FarklePhase getPhase() { return phase; }
    public int getCurrentSide() { return currentSide; }
    public float getArmAngle(float partialTicks) {
        if (phaseTicks < 10 && armAnimation.startsWith("arm_to_") && currentSide >= 0) {
            float progress = Math.min(1.0f, (phaseTicks + partialTicks) / 10.0f);
            return currentSide == 1 ? 10.0f * progress : 10.0f * (1.0f - progress);
        }
        return currentSide == 1 ? 10.0f : 0.0f;
    }
    public int getBankedScore(int side) { return side >= 0 && side < 2 ? bankedScores[side] : 0; }
    public int getTurnSubtotal() { return turnSubtotal; }
    public int getSelectedScore() { return FarkleScoring.scoreSelection(dice, available, selected); }
    public int getSelectedCount() {
        int count = 0;
        for (boolean value : selected) if (value) count++;
        return count;
    }
    public int getPhaseTicks() { return phaseTicks; }
    public int getDieValue(int index) { return index >= 0 && index < 6 ? dice[index] : 0; }
    public boolean isDieAvailable(int index) { return index >= 0 && index < 6 && available[index]; }
    public boolean isDieSelected(int index) { return index >= 0 && index < 6 && selected[index]; }
    public int getScoringSlot(int index) {
        return index >= 0 && index < DICE_COUNT ? scoringSlots[index] : -1;
    }
    public float getDieTransferProgress(int index, float partialTicks) {
        return Math.min(1.0f, (dieTransferTicks[index] + partialTicks) / 8.0f);
    }
    public ItemStack getStake(int side) { return stakes[side].copy(); }

    /** -1 means all segments are dark. Only banked scores are displayed. */
    public int getDisplayValue(int side) {
        if (phase == FarklePhase.WAITING) return -1;
        if (phase == FarklePhase.STARTING) return (phaseTicks / 5) % 2 == 0 ? 88888 : -1;
        if (phase == FarklePhase.SETTLING && phaseTicks < WIN_FLASH_TICKS
                && side == winningSide && (phaseTicks / 5) % 2 != 0) return -1;
        return bankedScores[side];
    }

    /** Each accepted click transfers exactly one real chip from the player's hand. */
    public boolean wager(int side, ServerPlayerEntity player, ItemStack held) {
        if (level == null || level.isClientSide || phase != FarklePhase.WAITING
                || side < 0 || side > 1 || !isChip(held)) return false;
        UUID uuid = player.getUUID();
        if ((players[side] != null && !players[side].equals(uuid))
                || (players[1 - side] != null && players[1 - side].equals(uuid)
                    && !canUseBothSeatsForTesting(player))) return false;
        if (!stakes[side].isEmpty() && !sameChip(stakes[side], held)) return false;
        // The visible stack and the accepted wager share the same five-chip limit.
        if (stakes[side].getCount() >= 5) return false;
        if (players[side] == null) players[side] = uuid;
        if (stakes[side].isEmpty()) {
            stakes[side] = held.copy();
            stakes[side].setCount(1);
        } else {
            stakes[side].grow(1);
        }
        held.shrink(1);
        player.inventory.setChanged();
        // Give players a short quiet interval to raise an equal one-chip stake.
        matchedWagerTicks = 0;
        sync();
        return true;
    }

    /** Creative players may control both seats in the development client only. */
    private static boolean canUseBothSeatsForTesting(ServerPlayerEntity player) {
        return !FMLLoader.isProduction() && player.abilities.instabuild;
    }

    private static boolean isChip(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() == ModItems.CHIP_5.get()
                || stack.getItem() == ModItems.CHIP_20.get());
    }

    private static boolean sameChip(ItemStack a, ItemStack b) {
        return a.getItem() == b.getItem() && ItemStack.tagMatches(a, b);
    }

    private void beginGame() {
        phase = FarklePhase.STARTING;
        phaseTicks = 0;
        currentSide = random.nextBoolean() ? 0 : 1;
        winningSide = -1;
        settlementLocked = false;
        payoutCompleted = false;
        Arrays.fill(bankedScores, 0);
        Arrays.fill(dice, 0);
        Arrays.fill(available, true);
        Arrays.fill(selected, false);
        Arrays.fill(scoringSlots, -1);
        turnSubtotal = 0;
        armAnimation = currentSide == 0 ? "arm_hold_a" : "arm_hold_b";
        animationSequence++;
        sync();
    }

    public boolean handleAction(ServerPlayerEntity player, int action, int index) {
        if (level == null || level.isClientSide || currentSide < 0
                || !player.getUUID().equals(players[currentSide])
                || !isConnectedParticipant(player)) return false;
        if (action == 0) { // select die
            if ((phase != FarklePhase.SELECTING && phase != FarklePhase.DECISION)
                    || index < 0 || index >= DICE_COUNT || !available[index]) return false;
            selected[index] = !selected[index];
        } else if (action == 1) { // submit selected scoring dice
            if (phase != FarklePhase.SELECTING && phase != FarklePhase.DECISION) return false;
            int score = getSelectedScore();
            if (score <= 0) return false;
            turnSubtotal += score;
            int nextSlot = 0;
            for (int slot : scoringSlots) nextSlot = Math.max(nextSlot, slot + 1);
            for (int i = 0; i < DICE_COUNT; i++) {
                if (selected[i]) {
                    available[i] = false;
                    scoringSlots[i] = nextSlot++;
                }
                selected[i] = false;
            }
            // Submitted dice stay in the scoring frame until the next roll.
            // If all six scored, the next roll restores all six (hot dice).
            phase = FarklePhase.DECISION;
        } else if (action == 2) { // continue roll
            if (phase != FarklePhase.DECISION || getSelectedCount() != 0) return false;
            roll();
        } else if (action == 3) { // bank
            if (phase != FarklePhase.DECISION || turnSubtotal <= 0
                    || getSelectedCount() != 0) return false;
            bankedScores[currentSide] += turnSubtotal;
            turnSubtotal = 0;
            if (bankedScores[currentSide] >= WIN_SCORE) settle(currentSide);
            else switchTurn();
        } else {
            return false;
        }
        sync();
        return true;
    }

    /** Play the authored press/release motion for a recognized button click. */
    public void pressButton(int side, int action) {
        if (level == null || level.isClientSide || side < 0 || side > 1
                || action < 1 || action > 3) return;
        String prefix = side == 0 ? "a_" : "b_";
        buttonAnimation = prefix + (action == 1 ? "submit" : action == 2 ? "roll" : "end") + "_press";
        buttonAnimationSequence++;
        sync();
    }

    private void roll() {
        boolean hotDice = true;
        for (boolean value : available) if (value) hotDice = false;
        if (hotDice) {
            Arrays.fill(available, true);
            Arrays.fill(scoringSlots, -1);
        }
        Arrays.fill(selected, false);
        for (int i = 0; i < DICE_COUNT; i++) {
            if (available[i]) dice[i] = random.nextInt(6) + 1;
        }
        phase = FarklePhase.ROLLING;
        phaseTicks = 0;
    }

    private void finishRoll() {
        int count = 0;
        for (boolean value : available) if (value) count++;
        int[] rolled = new int[count];
        int next = 0;
        for (int i = 0; i < DICE_COUNT; i++) if (available[i]) rolled[next++] = dice[i];
        if (!FarkleScoring.hasScoringMove(rolled)) {
            turnSubtotal = 0;
            switchTurn();
        } else {
            phase = FarklePhase.SELECTING;
            if (armAnimation.startsWith("arm_to_")) {
                armAnimation = currentSide == 0 ? "arm_hold_a" : "arm_hold_b";
                animationSequence++;
            }
        }
        sync();
    }

    private void switchTurn() {
        currentSide = 1 - currentSide;
        Arrays.fill(dice, 0);
        Arrays.fill(available, true);
        Arrays.fill(selected, false);
        Arrays.fill(scoringSlots, -1);
        armAnimation = currentSide == 0 ? "arm_to_a" : "arm_to_b";
        animationSequence++;
        roll();
    }

    private void settle(int winner) {
        if (settlementLocked) return;
        settlementLocked = true;
        winningSide = winner;
        payoutCompleted = false;
        phase = FarklePhase.SETTLING;
        phaseTicks = 0;
        sync();
    }

    private void finishSettlement() {
        clearGame();
        sync();
    }

    private void completePayout() {
        if (payoutCompleted) return;
        if (winningSide >= 0) {
            pay(winningSide, stakes[0]);
            pay(winningSide, stakes[1]);
        }
        Arrays.fill(stakes, ItemStack.EMPTY);
        payoutCompleted = true;
        sync();
    }

    private void clearGame() {
        Arrays.fill(players, null);
        Arrays.fill(stakes, ItemStack.EMPTY);
        Arrays.fill(bankedScores, 0);
        Arrays.fill(dice, 0);
        Arrays.fill(available, true);
        Arrays.fill(selected, false);
        Arrays.fill(scoringSlots, -1);
        currentSide = -1;
        winningSide = -1;
        turnSubtotal = 0;
        phaseTicks = 0;
        matchedWagerTicks = 0;
        phase = FarklePhase.WAITING;
        settlementLocked = false;
        payoutCompleted = false;
        armAnimation = "arm_hold_a";
        animationSequence++;
    }

    /** Breaking the block releases every stake once, including during settlement. */
    public void releaseStakesOnBreak() {
        if (level == null || level.isClientSide) return;
        if (phase == FarklePhase.SETTLING && winningSide >= 0) {
            pay(winningSide, stakes[0]);
            pay(winningSide, stakes[1]);
        } else {
            for (int side = 0; side < 2; side++) pay(side, stakes[side]);
        }
        clearGame();
    }

    private void pay(int side, ItemStack stack) {
        if (stack.isEmpty() || level == null || level.isClientSide) return;
        ItemStack payout = stack.copy();
        ServerPlayerEntity player = getPlayer(side);
        if (player != null) {
            ChipDelivery.give(player, payout);
            return;
        }
        ItemEntity dropped = new ItemEntity(level, worldPosition.getX() + 0.5,
                worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, payout);
        level.addFreshEntity(dropped);
    }

    @Nullable
    private ServerPlayerEntity getPlayer(int side) {
        if (level == null || level.isClientSide || players[side] == null) return null;
        return ((ServerWorld) level).getServer().getPlayerList().getPlayer(players[side]);
    }

    private boolean isConnectedParticipant(ServerPlayerEntity player) {
        return player != null && player.isAlive() && player.level == level;
    }

    private boolean isValidParticipant(ServerPlayerEntity player) {
        return isConnectedParticipant(player)
                && player.distanceToSqr(worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= MAX_DISTANCE_SQUARED;
    }

    /** One block beyond the actual tabletop footprint, measured in the horizontal plane. */
    private boolean isWithinWinnerDisplayRange(ServerPlayerEntity player) {
        if (!isConnectedParticipant(player)) return false;
        Direction facing = getBlockState().getValue(CasinoFarkleTableBlock.FACING);
        boolean rotated = facing.getAxis() == Direction.Axis.X;
        double halfX = (rotated ? 13.0 : 26.0) / 16.0 + 1.0;
        double halfZ = (rotated ? 26.0 : 13.0) / 16.0 + 1.0;
        return Math.abs(player.getX() - worldPosition.getX() - 0.5) <= halfX
                && Math.abs(player.getZ() - worldPosition.getZ() - 0.5) <= halfZ
                && Math.abs(player.getY() - worldPosition.getY()) <= 2.5;
    }

    private void checkDepartures() {
        if (phase == FarklePhase.SETTLING || settlementLocked) return;
        boolean[] departed = new boolean[2];
        for (int side = 0; side < 2; side++) {
            departed[side] = players[side] != null && !isValidParticipant(getPlayer(side));
        }
        if (departed[0] && departed[1]) {
            // Observe both seats before deciding a winner; consume each refund before delivery.
            ItemStack[] refunds = {stakes[0].copy(), stakes[1].copy()};
            Arrays.fill(stakes, ItemStack.EMPTY);
            for (int side = 0; side < 2; side++) pay(side, refunds[side]);
            clearGame();
            sync();
            return;
        }
        for (int side = 0; side < 2; side++) {
            if (!departed[side]) continue;
            if (phase == FarklePhase.WAITING || phase == FarklePhase.STARTING) {
                pay(side, stakes[side]);
                players[side] = null;
                stakes[side] = ItemStack.EMPTY;
                phase = FarklePhase.WAITING;
                currentSide = -1;
                sync();
            } else if (phase != FarklePhase.SETTLING) {
                settle(1 - side);
                return;
            }
        }
    }

    @Override
    public void tick() {
        if (level == null) return;
        if (level.isClientSide) {
            if (buttonPressAge < BUTTON_PRESS_DURATION_TICKS) buttonPressAge++;
            for (int i = 0; i < DICE_COUNT; i++) {
                if (dieTransferTicks[i] < 8) dieTransferTicks[i]++;
            }
            if (phase == FarklePhase.ROLLING || phase == FarklePhase.STARTING
                    || phase == FarklePhase.SETTLING || phase == FarklePhase.NEED_ROLL) phaseTicks++;
            return;
        }
        if (phase != FarklePhase.SETTLING) checkDepartures();
        if (phase == FarklePhase.WAITING) {
            if (!stakes[0].isEmpty() && !stakes[1].isEmpty()
                    && sameChip(stakes[0], stakes[1])
                    && stakes[0].getCount() == stakes[1].getCount()) {
                if (++matchedWagerTicks >= 40) beginGame();
            } else {
                matchedWagerTicks = 0;
            }
        } else if (phase == FarklePhase.STARTING) {
            phaseTicks++;
            if (phaseTicks >= 30) {
                roll();
                sync();
            } else if (phaseTicks % 5 == 0) sync();
        } else if (phase == FarklePhase.ROLLING) {
            if (++phaseTicks >= 10) finishRoll();
        } else if (phase == FarklePhase.SETTLING) {
            phaseTicks++;
            if (phaseTicks >= WIN_FLASH_TICKS) completePayout();
            if (payoutCompleted && !isWithinWinnerDisplayRange(getPlayer(0))
                    && !isWithinWinnerDisplayRange(getPlayer(1))) {
                finishSettlement();
            } else if (phaseTicks < WIN_FLASH_TICKS && phaseTicks % 5 == 0) {
                sync();
            }
        } else if (phase == FarklePhase.NEED_ROLL) {
            // Resume tables saved by an earlier build that waited for a manual first roll.
            roll();
            sync();
        }
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(worldPosition).inflate(2.0, 2.0, 2.0);
    }

    @Override
    public CompoundNBT save(CompoundNBT tag) {
        tag.putString("Phase", phase.name());
        tag.putInt("CurrentSide", currentSide);
        tag.putInt("TurnSubtotal", turnSubtotal);
        tag.putInt("PhaseTicks", phaseTicks);
        tag.putInt("MatchedWagerTicks", matchedWagerTicks);
        tag.putInt("WinningSide", winningSide);
        tag.putBoolean("SettlementLocked", settlementLocked);
        tag.putBoolean("PayoutCompleted", payoutCompleted);
        tag.putString("ArmAnimation", armAnimation);
        tag.putInt("AnimationSequence", animationSequence);
        tag.putString("ButtonAnimation", buttonAnimation);
        tag.putInt("ButtonAnimationSequence", buttonAnimationSequence);
        for (int side = 0; side < 2; side++) {
            tag.putInt("Score" + side, bankedScores[side]);
            if (players[side] != null) tag.putUUID("Player" + side, players[side]);
            tag.put("Stake" + side, stakes[side].save(new CompoundNBT()));
        }
        tag.putIntArray("Dice", dice);
        tag.putIntArray("ScoringSlots", scoringSlots);
        for (int i = 0; i < DICE_COUNT; i++) {
            tag.putBoolean("Available" + i, available[i]);
            tag.putBoolean("Selected" + i, selected[i]);
        }
        return super.save(tag);
    }

    @Override
    public void load(BlockState state, CompoundNBT tag) {
        super.load(state, tag);
        try { phase = FarklePhase.valueOf(tag.getString("Phase")); }
        catch (IllegalArgumentException ex) { phase = FarklePhase.WAITING; }
        currentSide = tag.getInt("CurrentSide");
        turnSubtotal = tag.getInt("TurnSubtotal");
        phaseTicks = tag.getInt("PhaseTicks");
        matchedWagerTicks = tag.getInt("MatchedWagerTicks");
        winningSide = tag.getInt("WinningSide");
        settlementLocked = tag.getBoolean("SettlementLocked");
        payoutCompleted = tag.getBoolean("PayoutCompleted");
        armAnimation = tag.contains("ArmAnimation") ? tag.getString("ArmAnimation") : "arm_hold_a";
        animationSequence = tag.getInt("AnimationSequence");
        buttonAnimation = tag.getString("ButtonAnimation");
        buttonAnimationSequence = tag.getInt("ButtonAnimationSequence");
        for (int side = 0; side < 2; side++) {
            players[side] = tag.hasUUID("Player" + side) ? tag.getUUID("Player" + side) : null;
            stakes[side] = ItemStack.of(tag.getCompound("Stake" + side));
            bankedScores[side] = tag.getInt("Score" + side);
        }
        int[] savedDice = tag.getIntArray("Dice");
        int[] savedSlots = tag.getIntArray("ScoringSlots");
        int legacySlot = 0;
        for (int i = 0; i < DICE_COUNT; i++) {
            dice[i] = i < savedDice.length ? savedDice[i] : 0;
            available[i] = !tag.contains("Available" + i) || tag.getBoolean("Available" + i);
            selected[i] = tag.getBoolean("Selected" + i);
            scoringSlots[i] = i < savedSlots.length ? savedSlots[i]
                    : available[i] ? -1 : legacySlot++;
        }
    }

    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(worldPosition, 1, getUpdateTag());
    }

    @Override
    public CompoundNBT getUpdateTag() { return save(new CompoundNBT()); }

    @Override
    public void onDataPacket(NetworkManager manager, SUpdateTileEntityPacket packet) {
        if (packet.getTag() == null) return;
        boolean[] wasAvailable = available.clone();
        int previousButtonSequence = buttonAnimationSequence;
        handleUpdateTag(getBlockState(), packet.getTag());
        if (buttonAnimationSequence != previousButtonSequence) buttonPressAge = 0;
        for (int i = 0; i < DICE_COUNT; i++) {
            if (wasAvailable[i] && !available[i]) dieTransferTicks[i] = 0;
            else if (available[i]) dieTransferTicks[i] = 8;
        }
    }

    private <E extends IAnimatable> PlayState animationPredicate(AnimationEvent<E> event) {
        AnimationController<E> controller = event.getController();
        if (appliedAnimationSequence != animationSequence) {
            controller.markNeedsReload();
            appliedAnimationSequence = animationSequence;
        }
        controller.setAnimation(new AnimationBuilder().addAnimation(armAnimation, true));
        return PlayState.CONTINUE;
    }

    private <E extends IAnimatable> PlayState buttonAnimationPredicate(AnimationEvent<E> event) {
        if (buttonAnimation.isEmpty() || buttonPressAge >= BUTTON_PRESS_DURATION_TICKS) return PlayState.STOP;
        AnimationController<E> controller = event.getController();
        if (appliedButtonAnimationSequence != buttonAnimationSequence) {
            controller.markNeedsReload();
            appliedButtonAnimationSequence = buttonAnimationSequence;
        }
        controller.setAnimation(new AnimationBuilder().addAnimation(buttonAnimation, false));
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this,
                "farkle_arm", 0, this::animationPredicate));
        data.addAnimationController(new AnimationController<>(this,
                "farkle_button", 0, this::buttonAnimationPredicate));
    }

    @Override
    public AnimationFactory getFactory() { return factory; }
}
