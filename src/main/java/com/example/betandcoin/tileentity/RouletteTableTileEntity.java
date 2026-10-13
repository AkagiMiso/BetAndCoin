package com.example.betandcoin.tileentity;

import com.example.betandcoin.game.roulette.*;
import com.example.betandcoin.util.ChipDelivery;
import com.example.betandcoin.game.roulette.RouletteRules.Color;
import com.example.betandcoin.game.roulette.RouletteRules.Phase;
import com.example.betandcoin.registry.ModItems;
import com.example.betandcoin.registry.ModTileEntities;
import net.minecraft.block.BlockState;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.server.ServerWorld;

import java.util.*;

public class RouletteTableTileEntity extends TileEntity implements ITickableTileEntity {
    private Phase phase = Phase.IDLE;
    private long phaseStart;
    private int startSegment, targetSegment;
    private final List<Bet> bets = new ArrayList<>();
    private final List<Bet> visibleChips = new ArrayList<>();
    // Offline rewards/refunds survive chunk unloading, saving and subsequent rounds.
    private final List<Bet> credits = new ArrayList<>();
    public boolean removing;

    public RouletteTableTileEntity() { this(ModTileEntities.ROULETTE_TABLE.get()); }
    public RouletteTableTileEntity(TileEntityType<?> type) { super(type); }
    public Phase getPhase() { return phase; }
    public List<Bet> getBets() { return Collections.unmodifiableList(bets); }
    public List<Bet> getVisibleChips() { return Collections.unmodifiableList(visibleChips); }
    public static boolean isChip(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() == ModItems.CHIP_5.get()
                || stack.getItem() == ModItems.CHIP_20.get());
    }

    public boolean placeBet(PlayerEntity player, Color color, ItemStack held) {
        if (level == null || level.isClientSide || !isChip(held) || !nearby(player)) return false;
        if (phase == Phase.SPINNING || phase == Phase.RESULT_FLASH
                || phase == Phase.BETTING && level.getGameTime() - phaseStart >= RouletteRules.BETTING_TICKS) {
            return false;
        }
        if (phase == Phase.SETTLED_RESULT) reset();
        Bet matching = null;
        for (Bet bet : bets) if (bet.player.equals(player.getUUID()) && bet.color == color
                && ItemStack.isSame(bet.chips, held)) { matching = bet; break; }
        if (matching != null && matching.chips.getCount() >= RouletteRules.MAX_CHIPS_PER_BET
                || matching == null && bets.size() >= RouletteRules.MAX_BETS) return false;
        if (matching == null) {
            ItemStack stake = held.copy();
            stake.setCount(1);
            bets.add(new Bet(player.getUUID(), color, stake));
        } else matching.chips.grow(1);
        rememberVisibleChip(player.getUUID(), color, held);
        held.shrink(1); // Creative players must also escrow actual chips.
        player.inventory.setChanged();
        if (phase == Phase.IDLE) { phase = Phase.BETTING; phaseStart = level.getGameTime(); }
        sync();
        return true;
    }

    private void rememberVisibleChip(UUID player, Color color, ItemStack held) {
        int count = 0;
        for (Bet chip : visibleChips) if (chip.color == color) count++;
        if (count >= RouletteRules.MAX_VISIBLE_CHIPS) return;
        ItemStack chip = held.copy(); chip.setCount(1);
        visibleChips.add(new Bet(player, color, chip));
    }

    public int currentSegment() { return currentSegment(0); }
    public int currentSegment(float partialTicks) {
        if (phase != Phase.SPINNING || level == null) return targetSegment;
        int total = RouletteRules.totalSteps(startSegment, targetSegment);
        return (startSegment + RouletteRules.elapsedSteps(level.getGameTime() - phaseStart + partialTicks, total)) % RouletteLayout.COUNT;
    }
    public boolean centerFrame45() {
        return phase == Phase.SPINNING && level != null && ((level.getGameTime() - phaseStart) / 3) % 2 != 0;
    }
    public boolean showGlow() {
        return phase == Phase.SPINNING || (phase == Phase.RESULT_FLASH || phase == Phase.SETTLED_RESULT)
                && level != null && ((level.getGameTime() - phaseStart) / RouletteRules.RESULT_FLASH_TICKS) % 2 == 0;
    }

    @Override public void tick() {
        if (level == null || level.isClientSide || removing) return;
        long now = level.getGameTime();
        if (now % 20 == 0 && !credits.isEmpty()) {
            Iterator<Bet> iterator = credits.iterator();
            boolean changed = false;
            while (iterator.hasNext()) {
                Bet credit = iterator.next();
                ServerPlayerEntity player = player(credit.player);
                if (player != null) { deliver(player, credit.chips); iterator.remove(); changed = true; }
            }
            if (changed) setChanged();
        }
        switch (phase) {
            case BETTING:
                if (now % 20 == 0) {
                    Iterator<Bet> iterator = bets.iterator();
                    boolean changed = false;
                    while (iterator.hasNext()) {
                        Bet bet = iterator.next();
                        if (!nearby(player(bet.player))) {
                            payOrQueue(bet.player, bet.color, bet.chips, 1);
                            iterator.remove(); changed = true;
                            visibleChips.removeIf(chip -> chip.player.equals(bet.player) && chip.color == bet.color
                                    && ItemStack.isSame(chip.chips, bet.chips));
                        }
                    }
                    if (bets.isEmpty()) { reset(); sync(); return; }
                    if (changed) sync();
                }
                if (now - phaseStart >= RouletteRules.BETTING_TICKS) {
                    startSegment = targetSegment;
                    targetSegment = level.random.nextInt(RouletteLayout.COUNT);
                    phase = Phase.SPINNING; phaseStart = now; sync();
                }
                break;
            case SPINNING:
                int duration = RouletteRules.duration(RouletteRules.totalSteps(startSegment, targetSegment));
                if (now - phaseStart >= duration) {
                    phase = Phase.RESULT_FLASH; phaseStart += duration; sync();
                }
                break;
            case RESULT_FLASH:
                if (now - phaseStart >= RouletteRules.RESULT_PAUSE_TICKS) { settle(); sync(); }
                break;
            case SETTLED_RESULT:
                if (now % 20 == 0 && bets.stream().noneMatch(bet -> nearby(player(bet.player)))) {
                    reset(); sync();
                }
                break;
            default: break;
        }
    }

    private void settle() {
        if (phase == Phase.SETTLED_RESULT || phase == Phase.IDLE) return;
        Color winner = RouletteLayout.segment(targetSegment).color;
        phase = Phase.SETTLED_RESULT; // settlement guard; never refund a locked losing wager
        setChanged();
        for (Bet bet : bets) {
            if (bet.color == winner) payOrQueue(bet.player, bet.color, bet.chips, winner.totalReturn);
        }
    }

    private void reset() { bets.clear(); visibleChips.clear(); phase = Phase.IDLE; phaseStart = 0; }

    public void releaseOnBreak() {
        if (level == null || level.isClientSide) return;
        if (phase == Phase.SPINNING || phase == Phase.RESULT_FLASH) settle();
        else if (phase == Phase.BETTING) for (Bet bet : bets) payOrQueue(bet.player, bet.color, bet.chips, 1);
        // No block remains to retain offline credits. Offline stacks fall at the table.
        for (Bet credit : credits) deliver(player(credit.player), credit.chips);
        credits.clear(); reset(); setChanged();
    }

    private void payOrQueue(UUID owner, Color color, ItemStack stake, int multiplier) {
        ItemStack amount = stake.copy();
        amount.setCount(stake.getCount() * multiplier);
        ServerPlayerEntity player = player(owner);
        if (player != null) deliver(player, amount);
        else {
            for (Bet credit : credits) if (credit.player.equals(owner) && ItemStack.isSame(credit.chips, amount)) {
                credit.chips.grow(amount.getCount()); setChanged(); return;
            }
            credits.add(new Bet(owner, color, amount)); setChanged();
        }
    }

    private void deliver(ServerPlayerEntity player, ItemStack amount) {
        if (player != null) {
            ChipDelivery.give(player, amount);
            return;
        }
        int remaining = amount.getCount();
        while (remaining > 0) {
            ItemStack stack = amount.copy();
            int count = Math.min(remaining, stack.getMaxStackSize());
            stack.setCount(count); remaining -= count;
            if (!stack.isEmpty()) {
                ItemEntity dropped = new ItemEntity(level, worldPosition.getX() + 0.5,
                        worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, stack);
                level.addFreshEntity(dropped);
            }
        }
    }

    private ServerPlayerEntity player(UUID id) {
        return level instanceof ServerWorld ? ((ServerWorld) level).getServer().getPlayerList().getPlayer(id) : null;
    }
    private boolean nearby(PlayerEntity player) {
        return player != null && player.isAlive() && player.level == level
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= RouletteRules.LEAVE_DISTANCE * RouletteRules.LEAVE_DISTANCE;
    }
    private void sync() { setChanged(); level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }

    @Override public CompoundNBT save(CompoundNBT tag) {
        super.save(tag);
        tag.putString("Phase", phase.name()); tag.putLong("PhaseStart", phaseStart);
        tag.putInt("StartSegment", startSegment); tag.putInt("TargetSegment", targetSegment);
        tag.put("Bets", writeBets(bets)); tag.put("Credits", writeBets(credits));
        tag.put("VisibleChips", writeBets(visibleChips));
        return tag;
    }
    private static ListNBT writeBets(List<Bet> list) {
        ListNBT tags = new ListNBT();
        for (Bet bet : list) {
            CompoundNBT tag = new CompoundNBT();
            tag.putUUID("Player", bet.player); tag.putString("Color", bet.color.name());
            ItemStack single = bet.chips.copy(); single.setCount(1);
            tag.put("Chips", single.save(new CompoundNBT()));
            // ItemStack NBT's Count is a byte; aggregated stakes need their own integer.
            tag.putInt("Count", bet.chips.getCount()); tags.add(tag);
        }
        return tags;
    }
    @Override public void load(BlockState state, CompoundNBT tag) {
        super.load(state, tag);
        try { phase = Phase.valueOf(tag.getString("Phase")); } catch (IllegalArgumentException e) { phase = Phase.IDLE; }
        phaseStart = tag.getLong("PhaseStart");
        startSegment = Math.floorMod(tag.getInt("StartSegment"), RouletteLayout.COUNT);
        targetSegment = Math.floorMod(tag.getInt("TargetSegment"), RouletteLayout.COUNT);
        readBets(tag.getList("Bets", 10), bets); readBets(tag.getList("Credits", 10), credits);
        visibleChips.clear();
        List<Bet> display = new ArrayList<>();
        if (tag.contains("VisibleChips")) readBets(tag.getList("VisibleChips", 10), display);
        else display.addAll(bets); // Existing first-version saves remain usable.
        for (Bet bet : display) for (int i = 0; i < Math.min(bet.chips.getCount(), RouletteRules.MAX_VISIBLE_CHIPS); i++)
            rememberVisibleChip(bet.player, bet.color, bet.chips);
    }
    private static void readBets(ListNBT tags, List<Bet> list) {
        list.clear();
        for (int i = 0; i < tags.size(); i++) {
            CompoundNBT tag = tags.getCompound(i);
            if (!tag.hasUUID("Player") || tag.getInt("Count") <= 0) continue;
            ItemStack chips = ItemStack.of(tag.getCompound("Chips"));
            chips.setCount(tag.getInt("Count"));
            if (!isChip(chips)) continue;
            try { list.add(new Bet(tag.getUUID("Player"), Color.valueOf(tag.getString("Color")), chips)); }
            catch (IllegalArgumentException ignored) { }
        }
    }
    @Override public CompoundNBT getUpdateTag() { return save(new CompoundNBT()); }
    @Override public SUpdateTileEntityPacket getUpdatePacket() { return new SUpdateTileEntityPacket(worldPosition, 1, getUpdateTag()); }
    @Override public void onDataPacket(NetworkManager manager, SUpdateTileEntityPacket packet) {
        if (packet.getTag() != null) load(getBlockState(), packet.getTag());
    }
    @Override public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 2, 2));
    }

    public static final class Bet {
        public final UUID player;
        public final Color color;
        public final ItemStack chips;
        private Bet(UUID player, Color color, ItemStack chips) { this.player = player; this.color = color; this.chips = chips; }
    }
}
