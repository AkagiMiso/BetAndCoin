package com.example.betandcoin.tileentity;

import com.example.betandcoin.block.BlackjackTableBlock;
import com.example.betandcoin.util.ChipDelivery;
import com.example.betandcoin.game.blackjack.CardRank;
import com.example.betandcoin.game.blackjack.CardSuit;
import com.example.betandcoin.game.blackjack.Deck;
import com.example.betandcoin.entity.BlackjackChipEntity;
import com.example.betandcoin.entity.PlayingCardEntity;
import com.example.betandcoin.game.blackjack.PlayingCard;
import com.example.betandcoin.registry.ModEntityTypes;
import com.example.betandcoin.registry.ModItems;
import com.example.betandcoin.network.BlackjackSessionPacket;
import com.example.betandcoin.util.BlackjackSeats;
import com.example.betandcoin.network.ModNetwork;
import net.minecraft.block.BlockState;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.PacketDistributor;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class BlackjackTableTileEntity
        extends TileEntity
        implements IAnimatable, ITickableTileEntity {

    public static final int SLOT_COUNT = 3;

    private static final String ANIMATION_CONTROLLER =
            "blackjack_table_controller";

    private static final String ANIMATION_IDLE_LOOP =
            "idle_loop";

    private static final String ANIMATION_IDLE_LOOP_WITH_DECK =
            "idle_loop2";

    private static final String ANIMATION_FANCY_INTRO =
            "fancy_intro";

    private static final String ANIMATION_MAGIC_CARD_TO_DECK =
            "magic_card_to_deck";

    private static final String ANIMATION_DEAL_PREPARE =
            "deal_prepare";

    private static final String ANIMATION_INITIAL_DEAL_LEFT =
            "deal_left";

    private static final String ANIMATION_INITIAL_DEAL_MID =
            "deal_mid";

    private static final String ANIMATION_INITIAL_DEAL_RIGHT =
            "deal_right";

    private static final String ANIMATION_INITIAL_DEAL_DEALER =
            "dealer_draw_magic_card";

    private static final String ANIMATION_CARD_LEFT =
            "deck_card_to_left";

    private static final String ANIMATION_CARD_MID =
            "deck_card_to_mid";

    private static final String ANIMATION_CARD_RIGHT =
            "deck_card_to_right";

    private static final String ANIMATION_CARD_DEALER =
            "deck_card_to_dealer";

    private static final String ANIMATION_WINNER_LEFT =
            "winner_left";

    private static final String ANIMATION_WINNER_MID =
            "winner_mid";

    private static final String ANIMATION_WINNER_RIGHT =
            "winner_right";

    /*
     * clear_table2 只清理当前回合的双方手牌，并保留牌靴；
     * clear_table 用于最后一名玩家离桌后结束整场对局。
     */
    private static final String ANIMATION_CLEAR_ROUND =
            "clear_table2";

    private static final String ANIMATION_CLEAR_SESSION =
            "clear_table";

    /*
     * 玩家距离自己栏位方块中心超过 2 格时，
     * 自动视为离开牌桌。
     */
    public static final double MAX_SLOT_DISTANCE_SQUARED =
            4.0D;

    /*
     * 动画时长按 20 tick = 1 秒换算。
     * fancy_intro 播放期间仍允许新玩家加入本局；
     * deal_prepare 开始时锁定本局名单。
     */
    private static final int FANCY_INTRO_DURATION_TICKS = 75;
    private static final int DEAL_PREPARE_DURATION_TICKS = 20;

    /*
     * 初始发牌动画均为 1 秒，视觉牌在 0.5 秒落位并隐藏。
     */
    private static final int INITIAL_DEAL_CARD_SPAWN_TICK = 10;
    private static final int INITIAL_DEAL_ACTION_DURATION_TICKS = 20;

    /*
     * magic_card_to_deck 长度为 2.75 秒，即 55 tick。
     * 多保留 1 tick，确保客户端渲染最终关键帧后再进入 idle_loop2。
     */
    private static final int MAGIC_CARD_TO_DECK_DURATION_TICKS = 56;

    /*
     * deck_card_to_left / mid / right / dealer 均为 1.25 秒。
     * 视觉牌在最后一帧隐藏，因此第 25 tick 生成实体牌，
     * 第 26 tick 再切回 idle_loop2 或开始下一次动作。
     */
    private static final int CARD_DRAW_SPAWN_TICK = 25;
    private static final int CARD_DRAW_ACTION_DURATION_TICKS = 26;

    private static final int WINNER_ANIMATION_DURATION_TICKS = 35;
    private static final int CLEAR_TABLE_DURATION_TICKS = 40;

    /*
     * 回合清桌后先完整播放一轮带牌靴的 idle_loop2，
     * 再直接从 deck_thin 发下一回合的起手牌。
     */
    private static final int ROUND_END_IDLE_DURATION_TICKS = 33;

    /*
     * 结算结果显示 5 秒后统一清理牌桌。
     */
    private static final int SETTLEMENT_DURATION_TICKS = 100;

    /*
     * 每 5 tick 检查一次离座。
     */
    private static final int LEAVE_CHECK_INTERVAL = 5;
    private static final int CHIP_ENTITY_CHECK_INTERVAL = 20;
    private static final double CARD_SEARCH_RADIUS = 6.0D;
    private static final double CHIP_SEARCH_RADIUS = 6.0D;
    private static final double CARD_SPACING = 0.09D;

    private final AnimationFactory animationFactory =
            new AnimationFactory(this);

    /*
     * 当前正在播放的桌面动画。
     * 非空时优先播放一次性动作；
     * 为空时回到 idle_loop。
     */
    @Nonnull
    private String currentAnimation = ANIMATION_IDLE_LOOP;

    private boolean currentAnimationLoop = true;
    private int animationSequence = 0;
    private int lastAppliedAnimationSequence = Integer.MIN_VALUE;

    /*
     * activeSlots 表示栏位已被玩家占用；
     * roundParticipants 表示该栏位玩家参加当前这一局。
     */
    private final boolean[] activeSlots =
            new boolean[SLOT_COUNT];

    private final boolean[] roundParticipants =
            new boolean[SLOT_COUNT];

    private final UUID[] slotPlayers =
            new UUID[SLOT_COUNT];

    /*
     * 每个栏位在整场对局中固定使用首次下注的筹码面额。
     *
     * CURRENT_ROUND 表示桌沿筹码属于当前回合；
     * NEXT_ROUND_PREPAID 表示玩家在回合中途加入，
     * 已经为下一回合预付一枚筹码。
     */
    private final ItemStack[] sessionBetStacks =
            new ItemStack[SLOT_COUNT];

    private final WagerState[] wagerStates =
            new WagerState[SLOT_COUNT];

    private final RoundResult[] roundResults =
            new RoundResult[SLOT_COUNT];

    private boolean roundSettlementApplied = false;
    private boolean removing = false;
    private int chipEntityCheckTicker = 0;

    @SuppressWarnings("unchecked")
    private final List<PlayingCard>[] playerHands =
            new List[SLOT_COUNT];

    private final boolean[] playerStood =
            new boolean[SLOT_COUNT];

    private final boolean[] playerBusted =
            new boolean[SLOT_COUNT];

    private final boolean[] playerForfeited =
            new boolean[SLOT_COUNT];

    private final List<PlayingCard> dealerHand =
            new ArrayList<>();

    /*
     * 每个回合开始前都重新生成并洗乱一副 52 张牌。
     * remainingCards 只保存当前回合尚未发出的牌，
     * 不会与独立放置的 CardDeckBlock 共用状态。
     */
    private final List<PlayingCard> remainingCards =
            new ArrayList<>();

    private Phase phase = Phase.WAITING;
    private int currentTurnSlot = -1;
    private int phaseTicker = 0;
    private boolean roundRosterLocked = false;
    private int dealerActionTicker = 0;
    private int settlementTicker = 0;
    private int leaveCheckTicker = 0;

    private int dealingTicker = 0;
    private int dealingStep = 0;
    private boolean dealingCardsSpawned = false;

    /*
     * 第一回合之后，起手牌直接从 deck_thin 逐张发出。
     * pass 0 / 1 分别代表第一轮牌和第二轮牌；
     * target 0～2 为玩家栏位，3 为荷官。
     */
    private int nextRoundDealPass = 0;
    private int nextRoundDealTarget = 0;
    private int nextRoundDealTicker = 0;
    private boolean nextRoundDealCardSpawned = false;

    /*
     * 胜利动画按左、中、右栏位顺序依次播放。
     */
    private final boolean[] roundWinners =
            new boolean[SLOT_COUNT];
    private int winnerAnimationSlot = -1;
    private int winnerAnimationTicker = 0;

    /*
     * 玩家补牌和荷官补牌共用的单张发牌动作状态。
     */
    private int cardActionTicker = 0;
    private boolean cardActionSpawned = false;
    private int pendingPlayerHitSlot = -1;

    public BlackjackTableTileEntity(
            @Nonnull TileEntityType<?> tileEntityType
    ) {
        super(tileEntityType);

        for (int i = 0; i < SLOT_COUNT; i++) {
            this.playerHands[i] = new ArrayList<>();
            this.sessionBetStacks[i] = ItemStack.EMPTY;
            this.wagerStates[i] = WagerState.NONE;
            this.roundResults[i] = RoundResult.NONE;
        }
    }

    /**
     * 玩家使用筹码加入栏位。
     *
     * 空栏位在任意非清场阶段都可以占用；
     * 已经锁定当前回合时，玩家会以候补状态预付下一回合下注。
     */
    public boolean tryActivateSlotWithWager(
            int slotIndex,
            @Nonnull ServerPlayerEntity player,
            @Nonnull ItemStack heldChip
    ) {
        if (!isValidSlot(slotIndex)
                || this.phase == Phase.SESSION_CLEARING
                || this.activeSlots[slotIndex]
                || findPlayerSlot(player.getUUID()) >= 0
                || BlackjackSeats.hasSeat(player.getServer(), player.getUUID())
                || !isValidBlackjackChip(heldChip)
                || heldChip.isEmpty()) {
            return false;
        }

        ItemStack wagerTemplate = heldChip.copy();
        wagerTemplate.setCount(1);

        boolean joinsUpcomingRound =
                this.phase == Phase.WAITING
                        || (this.phase == Phase.FANCY_INTRO
                        && !this.roundRosterLocked)
                        || this.phase == Phase.ROUND_END_IDLE;

        this.activeSlots[slotIndex] = true;
        this.slotPlayers[slotIndex] = player.getUUID();
        this.sessionBetStacks[slotIndex] = wagerTemplate;
        this.roundParticipants[slotIndex] = joinsUpcomingRound;
        this.wagerStates[slotIndex] =
                joinsUpcomingRound
                        ? WagerState.CURRENT_ROUND
                        : WagerState.NEXT_ROUND_PREPAID;

        resetPlayerRoundState(slotIndex);

        /*
         * 只有所有状态检查都通过后才真正扣除筹码，
         * 避免栏位加入失败却损失筹码。
         * 创造模式与生存模式完全使用同一套规则。
         */
        heldChip.shrink(1);
        player.inventory.setChanged();

        spawnBlackjackChipEntity(slotIndex);

        if (this.phase == Phase.WAITING) {
            beginFancyIntro();
        } else {
            markAndSync();
        }

        return true;
    }

    /**
     * 正式发牌前直接释放栏位。
     * 对局开始后离开则由 forfeitSlot() 标记弃局。
     */
    public boolean releaseSlot(int slotIndex) {
        if (!isValidSlot(slotIndex)
                || !this.activeSlots[slotIndex]) {
            return false;
        }

        ServerPlayerEntity player = getServerPlayer(slotIndex);
        handleSlotDeparture(slotIndex, player);

        if (player != null) {
            clearClientSession(player);
        }

        if (getActiveSlotCount() == 0) {
            if (this.phase == Phase.WAITING) {
                returnToWaiting();
            } else {
                beginSessionClearing();
            }
        } else {
            markAndSync();
        }

        return true;
    }

    public boolean isSlotActive(int slotIndex) {
        return isValidSlot(slotIndex)
                && this.activeSlots[slotIndex];
    }

    @Nullable
    public UUID getSlotPlayer(int slotIndex) {
        if (!isValidSlot(slotIndex)) {
            return null;
        }

        return this.slotPlayers[slotIndex];
    }

    public boolean isSlotOwnedBy(
            int slotIndex,
            @Nonnull PlayerEntity player
    ) {
        UUID slotPlayer = getSlotPlayer(slotIndex);

        return slotPlayer != null
                && slotPlayer.equals(player.getUUID());
    }

    public Phase getPhase() {
        return this.phase;
    }

    public int getCurrentTurnSlot() {
        return this.currentTurnSlot;
    }

    /**
     * 手势包通过这个入口请求要牌或停牌。
     * 所有牌局状态校验都在服务器端完成。
     */
    public GestureResult handlePlayerGesture(
            int slotIndex,
            @Nonnull ServerPlayerEntity player,
            boolean hit
    ) {
        if (this.phase != Phase.PLAYER_TURNS) {
            return GestureResult.WRONG_PHASE;
        }

        if (!player.isAlive()) {
            return GestureResult.NOT_OWNER;
        }

        if (!isSlotOwnedBy(slotIndex, player)
                || !this.roundParticipants[slotIndex]) {
            return GestureResult.NOT_OWNER;
        }

        if (slotIndex != this.currentTurnSlot) {
            return GestureResult.NOT_YOUR_TURN;
        }

        if (this.playerStood[slotIndex]
                || this.playerBusted[slotIndex]
                || this.playerForfeited[slotIndex]) {
            return GestureResult.ALREADY_FINISHED;
        }

        if (hit) {
            beginPlayerHit(slotIndex);
        } else {
            this.playerStood[slotIndex] = true;
            advanceTurn();
        }

        markAndSync();
        return GestureResult.SUCCESS;
    }

    /**
     * 玩家要牌时先播放对应的单张发牌动画，
     * 到动画末端再生成实体牌并结算点数。
     *
     * 模型动画中的 left / right 与玩家看到的栏位方向相反：
     * 左侧栏位使用 deck_card_to_right，右侧栏位使用 deck_card_to_left。
     */
    private void beginPlayerHit(int slotIndex) {
        this.phase = Phase.PLAYER_HIT;
        this.pendingPlayerHitSlot = slotIndex;
        this.cardActionTicker = 0;
        this.cardActionSpawned = false;

        if (isValidSlot(slotIndex)) {
            playDeckCardAnimationForSlot(slotIndex);
        } else {
            this.phase = Phase.PLAYER_TURNS;
            this.pendingPlayerHitSlot = -1;
            playAnimation(
                    ANIMATION_IDLE_LOOP_WITH_DECK,
                    true
            );
        }
    }

    private void tickPlayerHit() {
        this.cardActionTicker++;

        if (!this.cardActionSpawned
                && this.cardActionTicker >= CARD_DRAW_SPAWN_TICK) {
            int slotIndex = this.pendingPlayerHitSlot;

            if (isValidSlot(slotIndex)
                    && this.roundParticipants[slotIndex]
                    && !this.playerForfeited[slotIndex]) {
                PlayingCard card = drawCard();
                this.playerHands[slotIndex].add(card);
                spawnPlayerCardEntity(
                        slotIndex,
                        card,
                        this.playerHands[slotIndex].size() - 1
                );

                int value = calculateHandValue(
                        this.playerHands[slotIndex]
                );

                if (value > 21) {
                    this.playerBusted[slotIndex] = true;
                    this.playerStood[slotIndex] = true;
                } else if (value == 21) {
                    this.playerStood[slotIndex] = true;
                }
            }

            this.cardActionSpawned = true;
            markAndSync();
        }

        if (this.cardActionTicker
                < CARD_DRAW_ACTION_DURATION_TICKS) {
            return;
        }

        int slotIndex = this.pendingPlayerHitSlot;
        this.pendingPlayerHitSlot = -1;
        this.cardActionTicker = 0;
        this.cardActionSpawned = false;

        if (!isValidSlot(slotIndex)
                || !this.roundParticipants[slotIndex]
                || this.playerStood[slotIndex]
                || this.playerBusted[slotIndex]
                || this.playerForfeited[slotIndex]) {
            this.currentTurnSlot = slotIndex;
            advanceTurn();
            return;
        }

        this.phase = Phase.PLAYER_TURNS;
        this.currentTurnSlot = slotIndex;
        playAnimation(ANIMATION_IDLE_LOOP_WITH_DECK, true);
        notifyCurrentPlayer();
        markAndSync();
    }

    @Nullable
    public Vector3d getSlotAnchor(int slotIndex) {
        if (!isValidSlot(slotIndex)
                || this.level == null) {
            return null;
        }

        BlockState state = this.getBlockState();

        if (!state.hasProperty(BlackjackTableBlock.FACING)) {
            return null;
        }

        Direction facing =
                state.getValue(BlackjackTableBlock.FACING);

        Direction right = facing.getClockWise();

        int localX;

        switch (slotIndex) {
            case 0:
                localX = -1;
                break;

            case 1:
                localX = 0;
                break;

            case 2:
                localX = 1;
                break;

            default:
                return null;
        }

        double anchorX =
                this.worldPosition.getX() + 0.5D
                        + right.getStepX() * localX
                        + facing.getStepX();

        double anchorY =
                this.worldPosition.getY() + 0.5D;

        double anchorZ =
                this.worldPosition.getZ() + 0.5D
                        + right.getStepZ() * localX
                        + facing.getStepZ();

        return new Vector3d(
                anchorX,
                anchorY,
                anchorZ
        );
    }

    @Override
    public void tick() {
        if (this.level == null
                || this.level.isClientSide
                || this.removing) {
            return;
        }

        checkPlayersStillPresent();
        tickWagerChipEntities();

        switch (this.phase) {
            case WAITING:
                break;

            case FANCY_INTRO:
                tickFancyIntro();
                break;

            case DEAL_PREPARE:
                tickDealPrepare();
                break;

            case DEALING:
                tickDealing();
                break;

            case NEXT_ROUND_DEALING:
                tickNextRoundDealing();
                break;

            case MAGIC_CARD_TO_DECK:
                tickMagicCardToDeck();
                break;

            case PLAYER_TURNS:
                break;

            case PLAYER_HIT:
                tickPlayerHit();
                break;

            case DEALER_TURN:
                tickDealerTurn();
                break;

            case DEALER_DRAW:
                tickDealerDraw();
                break;

            case SETTLEMENT:
                tickSettlement();
                break;

            case WINNER_ANIMATION:
                tickWinnerAnimation();
                break;

            case CLEARING:
                tickClearing();
                break;

            case SESSION_CLEARING:
                tickSessionClearing();
                break;

            case ROUND_END_IDLE:
                tickRoundEndIdle();
                break;

            default:
                break;
        }
    }

    private void beginFancyIntro() {
        this.phase = Phase.FANCY_INTRO;
        this.phaseTicker = FANCY_INTRO_DURATION_TICKS;
        this.roundRosterLocked = false;
        playAnimation(ANIMATION_FANCY_INTRO, false);
    }

    private void tickFancyIntro() {
        this.phaseTicker--;

        if (this.phaseTicker <= 0) {
            beginDealPrepare();
        }
    }

    private void beginDealPrepare() {
        this.phase = Phase.DEAL_PREPARE;
        this.phaseTicker = DEAL_PREPARE_DURATION_TICKS;
        this.roundRosterLocked = true;
        playAnimation(ANIMATION_DEAL_PREPARE, false);
    }

    private void tickDealPrepare() {
        this.phaseTicker--;

        if (this.phaseTicker <= 0) {
            startRound();
        }
    }

    private void startRound() {
        clearBlackjackCardEntities();
        resetRoundOnly();
        resetAndShuffleDeck();

        // Keep the locked roster, but remove players who left during deal_prepare.
        // resetRoundOnly clears their forfeiture flags, so ownership and wager
        // state must still be checked before any seat can receive cards.
        for (int slotIndex = 0; slotIndex < SLOT_COUNT; slotIndex++) {
            this.roundParticipants[slotIndex] = this.roundParticipants[slotIndex]
                    && this.activeSlots[slotIndex]
                    && this.wagerStates[slotIndex] == WagerState.CURRENT_ROUND;
        }

        this.phase = Phase.DEALING;
        this.dealingStep = 0;
        this.dealingTicker = 0;
        this.dealingCardsSpawned = false;

        beginCurrentInitialDealAction();
    }

    /**
     * 初始发牌顺序：
     *
     * 1. 依次检查玩家视角中的左、中、右三个栏位；
     * 2. 动画文件的 left / right 与栏位视觉方向相反，因此这里颠倒映射；
     * 3. 同一玩家的两张起手牌在一次动画中一起生成；
     * 4. 玩家处理完毕后播放 dealer_draw_magic_card，
     *    给荷官生成一张明牌和一张暗牌；
     * 5. 最后播放 magic_card_to_deck，将剩余魔法牌收成 deck_thin；
     * 6. 动画完成后进入 idle_loop2，等待玩家点头或摇头。
     */
    private void beginCurrentInitialDealAction() {
        while (this.dealingStep < SLOT_COUNT
                && !canDealToPlayer(this.dealingStep)) {
            this.dealingStep++;
        }

        this.dealingTicker = 0;
        this.dealingCardsSpawned = false;

        if (this.dealingStep < SLOT_COUNT) {
            switch (this.dealingStep) {
                case 0:
                    /* 玩家视角左侧栏位对应动画文件中的 right。 */
                    playAnimation(
                            ANIMATION_INITIAL_DEAL_RIGHT,
                            false
                    );
                    break;

                case 1:
                    playAnimation(
                            ANIMATION_INITIAL_DEAL_MID,
                            false
                    );
                    break;

                case 2:
                    /* 玩家视角右侧栏位对应动画文件中的 left。 */
                    playAnimation(
                            ANIMATION_INITIAL_DEAL_LEFT,
                            false
                    );
                    break;

                default:
                    break;
            }
        } else {
            playAnimation(
                    ANIMATION_INITIAL_DEAL_DEALER,
                    false
            );
        }

        markAndSync();
    }

    private void tickDealing() {
        this.dealingTicker++;

        /*
         * deal_left / deal_mid / deal_right / dealer_draw_magic_card
         * 均在 0.5 秒到达落牌关键帧，因此第 10 tick 生成实体牌。
         */
        if (!this.dealingCardsSpawned
                && this.dealingTicker
                >= INITIAL_DEAL_CARD_SPAWN_TICK) {

            if (this.dealingStep < SLOT_COUNT) {
                // The seat can leave or be occupied by a next-round candidate
                // after the animation starts, before its landing keyframe.
                if (canDealToPlayer(this.dealingStep)) {
                    PlayingCard firstCard = drawCard();
                    PlayingCard secondCard = drawCard();

                    this.playerHands[this.dealingStep].add(firstCard);
                    this.playerHands[this.dealingStep].add(secondCard);

                    spawnPlayerCardEntity(this.dealingStep, firstCard, 0);
                    spawnPlayerCardEntity(this.dealingStep, secondCard, 1);
                }
            } else {
                PlayingCard dealerUpCard = drawCard();
                PlayingCard dealerHoleCard = drawCard();

                this.dealerHand.add(dealerUpCard);
                this.dealerHand.add(dealerHoleCard);

                spawnDealerCardEntity(
                        dealerUpCard,
                        0,
                        true
                );
                spawnDealerCardEntity(
                        dealerHoleCard,
                        1,
                        false
                );
            }

            this.dealingCardsSpawned = true;
            markAndSync();
        }

        if (this.dealingTicker
                < INITIAL_DEAL_ACTION_DURATION_TICKS) {
            return;
        }

        if (this.dealingStep < SLOT_COUNT) {
            this.dealingStep++;
            beginCurrentInitialDealAction();
            return;
        }

        beginMagicCardToDeck();
    }

    /**
     * 初始发牌全部结束后，才将剩余魔法牌收拢成桌面左侧的 deck_thin。
     */
    private void beginMagicCardToDeck() {
        this.phase = Phase.MAGIC_CARD_TO_DECK;
        this.phaseTicker = MAGIC_CARD_TO_DECK_DURATION_TICKS;
        playAnimation(ANIMATION_MAGIC_CARD_TO_DECK, false);
    }

    private boolean canDealToPlayer(int slotIndex) {
        return isValidSlot(slotIndex)
                && this.roundParticipants[slotIndex]
                && this.activeSlots[slotIndex]
                && this.wagerStates[slotIndex] == WagerState.CURRENT_ROUND
                && !this.playerForfeited[slotIndex];
    }

    private void tickMagicCardToDeck() {
        this.phaseTicker--;

        if (this.phaseTicker <= 0) {
            beginPlayerTurns();
        }
    }

    /**
     * 首回合结束后，牌靴继续留在桌面上。
     * 下一回合按“玩家一轮、荷官一张、玩家第二轮、荷官暗牌”的顺序，
     * 使用 deck_card_to_* 动画逐张发出起手牌。
     */
    private void startNextRoundFromDeck() {
        if (getActiveSlotCount() <= 0) {
            beginSessionClearing();
            return;
        }

        resetRoundOnly();
        resetAndShuffleDeck();

        for (int slotIndex = 0;
             slotIndex < SLOT_COUNT;
             slotIndex++) {
            this.roundParticipants[slotIndex] =
                    this.activeSlots[slotIndex]
                            && this.wagerStates[slotIndex]
                            == WagerState.CURRENT_ROUND;
        }

        if (getRoundParticipantCount() <= 0) {
            beginSessionClearing();
            return;
        }

        this.phase = Phase.NEXT_ROUND_DEALING;
        this.roundRosterLocked = true;
        this.nextRoundDealPass = 0;
        this.nextRoundDealTarget = 0;
        this.nextRoundDealTicker = 0;
        this.nextRoundDealCardSpawned = false;

        beginCurrentNextRoundDealAction();
    }

    private void beginCurrentNextRoundDealAction() {
        while (this.nextRoundDealTarget < SLOT_COUNT
                && (!this.roundParticipants[
                        this.nextRoundDealTarget]
                || this.playerForfeited[
                        this.nextRoundDealTarget])) {
            this.nextRoundDealTarget++;
        }

        this.nextRoundDealTicker = 0;
        this.nextRoundDealCardSpawned = false;

        if (this.nextRoundDealTarget < SLOT_COUNT) {
            playDeckCardAnimationForSlot(
                    this.nextRoundDealTarget
            );
        } else {
            playAnimation(ANIMATION_CARD_DEALER, false);
        }

        markAndSync();
    }

    private void tickNextRoundDealing() {
        this.nextRoundDealTicker++;

        if (!this.nextRoundDealCardSpawned
                && this.nextRoundDealTicker
                >= CARD_DRAW_SPAWN_TICK) {

            if (this.nextRoundDealTarget < SLOT_COUNT) {
                int slotIndex = this.nextRoundDealTarget;

                if (this.roundParticipants[slotIndex]
                        && !this.playerForfeited[slotIndex]) {
                    PlayingCard card = drawCard();
                    this.playerHands[slotIndex].add(card);
                    spawnPlayerCardEntity(
                            slotIndex,
                            card,
                            this.playerHands[slotIndex].size() - 1
                    );
                }
            } else {
                PlayingCard card = drawCard();
                int cardIndex = this.dealerHand.size();
                this.dealerHand.add(card);

                spawnDealerCardEntity(
                        card,
                        cardIndex,
                        this.nextRoundDealPass == 0
                );
            }

            this.nextRoundDealCardSpawned = true;
            markAndSync();
        }

        if (this.nextRoundDealTicker
                < CARD_DRAW_ACTION_DURATION_TICKS) {
            return;
        }

        if (this.nextRoundDealTarget < SLOT_COUNT) {
            this.nextRoundDealTarget++;
            beginCurrentNextRoundDealAction();
            return;
        }

        if (this.nextRoundDealPass == 0) {
            this.nextRoundDealPass = 1;
            this.nextRoundDealTarget = 0;
            beginCurrentNextRoundDealAction();
            return;
        }

        this.nextRoundDealTicker = 0;
        this.nextRoundDealCardSpawned = false;
        beginPlayerTurns();
    }

    private void playDeckCardAnimationForSlot(int slotIndex) {
        switch (slotIndex) {
            case 0:
                /* 玩家视角左侧栏位对应动画文件中的 right。 */
                playAnimation(ANIMATION_CARD_RIGHT, false);
                break;

            case 1:
                playAnimation(ANIMATION_CARD_MID, false);
                break;

            case 2:
                /* 玩家视角右侧栏位对应动画文件中的 left。 */
                playAnimation(ANIMATION_CARD_LEFT, false);
                break;

            default:
                playAnimation(
                        ANIMATION_IDLE_LOOP_WITH_DECK,
                        true
                );
                break;
        }
    }

    private void beginPlayerTurns() {
        this.phase = Phase.PLAYER_TURNS;
        this.currentTurnSlot = -1;

        for (int slotIndex = 0;
             slotIndex < SLOT_COUNT;
             slotIndex++) {

            if (!this.roundParticipants[slotIndex]) {
                continue;
            }

            if (calculateHandValue(
                    this.playerHands[slotIndex]
            ) == 21) {
                this.playerStood[slotIndex] = true;
            }
        }

        this.currentTurnSlot =
                findNextPlayableSlot(-1);

        if (this.currentTurnSlot < 0) {
            beginDealerTurn();
            return;
        }

        playAnimation(
                ANIMATION_IDLE_LOOP_WITH_DECK,
                true
        );
        notifyCurrentPlayer();
        markAndSync();
    }

    private void advanceTurn() {
        int next =
                findNextPlayableSlot(
                        this.currentTurnSlot
                );

        if (next < 0) {
            beginDealerTurn();
            return;
        }

        this.phase = Phase.PLAYER_TURNS;
        this.currentTurnSlot = next;
        playAnimation(
                ANIMATION_IDLE_LOOP_WITH_DECK,
                true
        );
        notifyCurrentPlayer();
        markAndSync();
    }

    private int findNextPlayableSlot(int afterSlot) {
        for (int offset = 1;
             offset <= SLOT_COUNT;
             offset++) {

            int slotIndex =
                    (afterSlot + offset + SLOT_COUNT)
                            % SLOT_COUNT;

            if (this.roundParticipants[slotIndex]
                    && !this.playerStood[slotIndex]
                    && !this.playerBusted[slotIndex]
                    && !this.playerForfeited[slotIndex]) {
                return slotIndex;
            }
        }

        return -1;
    }

    private void beginDealerTurn() {
        this.phase = Phase.DEALER_TURN;
        this.currentTurnSlot = -1;
        revealDealerHoleCard();
        playAnimation(
                ANIMATION_IDLE_LOOP_WITH_DECK,
                true
        );
        markAndSync();
    }

    private void tickDealerTurn() {
        int dealerValue =
                calculateHandValue(this.dealerHand);

        /*
         * 荷官 17 点及以上停牌，包括软 17。
         * 低于 17 时，每补一张牌都完整播放一次 deck_card_to_dealer。
         */
        if (dealerValue < 17) {
            beginDealerDraw();
            return;
        }

        settleRound();
    }

    private void beginDealerDraw() {
        this.phase = Phase.DEALER_DRAW;
        this.cardActionTicker = 0;
        this.cardActionSpawned = false;
        playAnimation(ANIMATION_CARD_DEALER, false);
    }

    private void tickDealerDraw() {
        this.cardActionTicker++;

        if (!this.cardActionSpawned
                && this.cardActionTicker >= CARD_DRAW_SPAWN_TICK) {
            PlayingCard card = drawCard();
            this.dealerHand.add(card);
            spawnDealerCardEntity(
                    card,
                    this.dealerHand.size() - 1,
                    true
            );

            this.cardActionSpawned = true;
            markAndSync();
        }

        if (this.cardActionTicker
                < CARD_DRAW_ACTION_DURATION_TICKS) {
            return;
        }

        this.cardActionTicker = 0;
        this.cardActionSpawned = false;

        if (calculateHandValue(this.dealerHand) < 17) {
            beginDealerDraw();
        } else {
            settleRound();
        }
    }

    private void settleRound() {
        this.phase = Phase.SETTLEMENT;
        this.settlementTicker =
                SETTLEMENT_DURATION_TICKS;
        this.roundSettlementApplied = false;
        playAnimation(
                ANIMATION_IDLE_LOOP_WITH_DECK,
                true
        );
        markAndSync();
    }

    private void tickSettlement() {
        this.settlementTicker--;

        if (this.settlementTicker <= 0) {
            beginWinnerAnimations();
        }
    }

    private void beginWinnerAnimations() {
        applyRoundSettlement();
        this.winnerAnimationSlot = findNextWinnerSlot(-1);

        if (this.winnerAnimationSlot < 0) {
            beginClearing();
            return;
        }

        this.phase = Phase.WINNER_ANIMATION;
        this.winnerAnimationTicker =
                WINNER_ANIMATION_DURATION_TICKS;
        playWinnerAnimation(this.winnerAnimationSlot);
        markAndSync();
    }

    private void applyRoundSettlement() {
        if (this.roundSettlementApplied) {
            return;
        }

        Arrays.fill(this.roundWinners, false);
        Arrays.fill(this.roundResults, RoundResult.NONE);

        int dealerValue =
                calculateHandValue(this.dealerHand);
        boolean dealerBusted = dealerValue > 21;

        for (int slotIndex = 0;
             slotIndex < SLOT_COUNT;
             slotIndex++) {

            if (!this.roundParticipants[slotIndex]) {
                continue;
            }

            RoundResult result;

            if (this.playerForfeited[slotIndex]) {
                result = RoundResult.FORFEIT;
            } else if (this.playerBusted[slotIndex]) {
                result = RoundResult.LOSS;
            } else {
                int playerValue =
                        calculateHandValue(
                                this.playerHands[slotIndex]
                        );

                if (dealerBusted
                        || playerValue > dealerValue) {
                    result = RoundResult.WIN;
                } else if (playerValue == dealerValue) {
                    result = RoundResult.PUSH;
                } else {
                    result = RoundResult.LOSS;
                }
            }

            this.roundResults[slotIndex] = result;
            this.roundWinners[slotIndex] =
                    result == RoundResult.WIN;

            if (!this.activeSlots[slotIndex]
                    || this.wagerStates[slotIndex]
                    != WagerState.CURRENT_ROUND) {
                continue;
            }

            ServerPlayerEntity player =
                    getServerPlayer(slotIndex);

            if (result == RoundResult.WIN) {
                giveChipPayout(
                        slotIndex,
                        player,
                        2
                );
            } else if (result == RoundResult.PUSH) {
                giveChipPayout(
                        slotIndex,
                        player,
                        1
                );
            }
        }

        this.roundSettlementApplied = true;
        markAndSync();
    }

    private void giveChipPayout(
            int slotIndex,
            @Nullable ServerPlayerEntity player,
            int count
    ) {
        if (!isValidSlot(slotIndex)
                || count <= 0
                || this.sessionBetStacks[slotIndex].isEmpty()) {
            return;
        }

        ItemStack payout =
                this.sessionBetStacks[slotIndex].copy();
        payout.setCount(count);

        if (player == null) {
            return;
        }

        ChipDelivery.give(player, payout);
    }

    private int findNextWinnerSlot(int afterSlot) {
        for (int slotIndex = afterSlot + 1;
             slotIndex < SLOT_COUNT;
             slotIndex++) {
            if (this.roundWinners[slotIndex]) {
                return slotIndex;
            }
        }

        return -1;
    }

    private void playWinnerAnimation(int slotIndex) {
        switch (slotIndex) {
            case 0:
                /* 玩家视角左侧栏位对应动画文件中的 right。 */
                playAnimation(ANIMATION_WINNER_RIGHT, false);
                break;

            case 1:
                playAnimation(ANIMATION_WINNER_MID, false);
                break;

            case 2:
                /* 玩家视角右侧栏位对应动画文件中的 left。 */
                playAnimation(ANIMATION_WINNER_LEFT, false);
                break;

            default:
                beginClearing();
                break;
        }
    }

    private void tickWinnerAnimation() {
        this.winnerAnimationTicker--;

        if (this.winnerAnimationTicker > 0) {
            return;
        }

        this.winnerAnimationSlot =
                findNextWinnerSlot(
                        this.winnerAnimationSlot
                );

        if (this.winnerAnimationSlot < 0) {
            beginClearing();
            return;
        }

        this.winnerAnimationTicker =
                WINNER_ANIMATION_DURATION_TICKS;
        playWinnerAnimation(this.winnerAnimationSlot);
        markAndSync();
    }

    /**
     * 检查离线、换维度和离开栏位范围。
     */
    private void checkPlayersStillPresent() {
        if (this.phase == Phase.SESSION_CLEARING) {
            return;
        }

        this.leaveCheckTicker++;

        if (this.leaveCheckTicker
                < LEAVE_CHECK_INTERVAL) {
            return;
        }

        this.leaveCheckTicker = 0;

        ServerWorld serverWorld =
                (ServerWorld) this.level;

        for (int slotIndex = 0;
             slotIndex < SLOT_COUNT;
             slotIndex++) {

            if (!this.activeSlots[slotIndex]) {
                continue;
            }

            UUID playerUuid =
                    this.slotPlayers[slotIndex];

            ServerPlayerEntity player =
                    playerUuid == null
                            ? null
                            : serverWorld.getServer()
                                    .getPlayerList()
                                    .getPlayer(playerUuid);

            boolean leftTable =
                    player == null
                            || !player.isAlive()
                            || player.level != serverWorld;

            if (!leftTable) {
                Vector3d slotAnchor =
                        getSlotAnchor(slotIndex);

                leftTable =
                        slotAnchor != null
                                && player.distanceToSqr(
                                        slotAnchor.x,
                                        slotAnchor.y,
                                        slotAnchor.z
                                ) > MAX_SLOT_DISTANCE_SQUARED;
            }

            if (!leftTable) {
                continue;
            }

            handleSlotDeparture(slotIndex, player);
            // A candidate can leave while another player is waiting for a gesture.
            // Persist the consumed reservation even when no phase transition follows.
            markAndSync();

            if (player != null) {
                clearClientSession(player);
            }

            if (getActiveSlotCount() == 0) {
                beginSessionClearing();
                break;
            }
        }
    }

    private static boolean isWagerAtRiskPhase(
            @Nonnull Phase phase
    ) {
        switch (phase) {
            case DEAL_PREPARE:
            case DEALING:
            case NEXT_ROUND_DEALING:
            case MAGIC_CARD_TO_DECK:
            case PLAYER_TURNS:
            case PLAYER_HIT:
            case DEALER_TURN:
            case DEALER_DRAW:
            case SETTLEMENT:
                return true;

            default:
                return false;
        }
    }

    private void handleSlotDeparture(
            int slotIndex,
            @Nullable ServerPlayerEntity player
    ) {
        if (!isValidSlot(slotIndex)
                || !this.activeSlots[slotIndex]) {
            return;
        }

        boolean prepaidCandidate =
                this.wagerStates[slotIndex]
                        == WagerState.NEXT_ROUND_PREPAID;

        boolean wagerNotYetAtRisk =
                this.wagerStates[slotIndex]
                        == WagerState.CURRENT_ROUND
                        && !isWagerAtRiskPhase(this.phase)
                        && !this.roundSettlementApplied
                        && (this.phase == Phase.WAITING
                        || this.phase == Phase.FANCY_INTRO
                        || this.phase == Phase.ROUND_END_IDLE);

        if (prepaidCandidate || wagerNotYetAtRisk) {
            refundReservedWager(slotIndex, player);
            clearOccupiedSlot(slotIndex);
            return;
        }

        if (this.roundParticipants[slotIndex]
                && this.wagerStates[slotIndex]
                == WagerState.CURRENT_ROUND
                && isWagerAtRiskPhase(this.phase)
                && !this.roundSettlementApplied) {
            forfeitSlot(slotIndex);
            return;
        }

        clearOccupiedSlot(slotIndex);
    }

    /** Cancel a removed table, refund unsettled reservations once and close all sessions. */
    public void releaseStakesOnBreak() {
        if (this.level == null || this.level.isClientSide || this.removing) return;
        this.removing = true;

        ServerPlayerEntity[] players = new ServerPlayerEntity[SLOT_COUNT];
        for (int slot = 0; slot < SLOT_COUNT; slot++) players[slot] = getServerPlayer(slot);
        ItemStack[] refunds = takeBreakRefunds();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (players[slot] != null) clearClientSession(players[slot]);
            if (!refunds[slot].isEmpty()) {
                if (players[slot] != null) ChipDelivery.give(players[slot], refunds[slot]);
                else dropChipAtSlot(slot, refunds[slot]);
            }
        }

        clearBlackjackCardEntities();
        clearBlackjackChipEntities();
        Arrays.fill(this.activeSlots, false);
        Arrays.fill(this.roundParticipants, false);
        Arrays.fill(this.slotPlayers, null);
        resetRoundOnly();
        this.remainingCards.clear();
        this.phase = Phase.WAITING;
        this.phaseTicker = 0;
        this.roundRosterLocked = false;
        setChanged();
    }

    /** Consume the refund ledger before delivery, including next-round prepaid seats. */
    private ItemStack[] takeBreakRefunds() {
        ItemStack[] refunds = new ItemStack[SLOT_COUNT];
        Arrays.fill(refunds, ItemStack.EMPTY);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (this.wagerStates[slot] == WagerState.NEXT_ROUND_PREPAID
                    || this.wagerStates[slot] == WagerState.CURRENT_ROUND
                    && !this.roundSettlementApplied) {
                refunds[slot] = this.sessionBetStacks[slot].copy();
                if (!refunds[slot].isEmpty()) refunds[slot].setCount(1);
            }
            this.sessionBetStacks[slot] = ItemStack.EMPTY;
            this.wagerStates[slot] = WagerState.NONE;
        }
        return refunds;
    }

    private void refundReservedWager(
            int slotIndex,
            @Nullable ServerPlayerEntity player
    ) {
        if (!isValidSlot(slotIndex)
                || this.sessionBetStacks[slotIndex].isEmpty()) {
            return;
        }

        ItemStack refund =
                this.sessionBetStacks[slotIndex].copy();
        refund.setCount(1);

        if (player != null) {
            ChipDelivery.give(player, refund);
        } else {
            dropChipAtSlot(slotIndex, refund);
        }
    }

    private void clearOccupiedSlot(int slotIndex) {
        if (!isValidSlot(slotIndex)) {
            return;
        }

        removeBlackjackChipEntity(slotIndex);
        this.activeSlots[slotIndex] = false;
        this.slotPlayers[slotIndex] = null;
        this.roundParticipants[slotIndex] = false;
        this.sessionBetStacks[slotIndex] = ItemStack.EMPTY;
        this.wagerStates[slotIndex] = WagerState.NONE;
        this.roundResults[slotIndex] = RoundResult.NONE;
        resetPlayerRoundState(slotIndex);
    }

    private void forfeitSlot(int slotIndex) {
        if (!isValidSlot(slotIndex)
                || !this.activeSlots[slotIndex]
                || this.playerForfeited[slotIndex]) {
            return;
        }

        removeBlackjackChipEntity(slotIndex);
        this.activeSlots[slotIndex] = false;
        this.slotPlayers[slotIndex] = null;
        this.sessionBetStacks[slotIndex] = ItemStack.EMPTY;
        this.wagerStates[slotIndex] = WagerState.NONE;
        this.roundResults[slotIndex] = RoundResult.FORFEIT;
        this.playerForfeited[slotIndex] = true;
        this.playerStood[slotIndex] = true;
        this.roundWinners[slotIndex] = false;

        if (this.phase == Phase.PLAYER_TURNS
                && this.currentTurnSlot == slotIndex) {
            advanceTurn();
        }

        markAndSync();
    }

    private void beginClearing() {
        this.phase = Phase.CLEARING;
        this.phaseTicker = CLEAR_TABLE_DURATION_TICKS;
        playAnimation(ANIMATION_CLEAR_ROUND, false);
        markAndSync();
    }

    private void tickClearing() {
        this.phaseTicker--;

        if (this.phaseTicker <= 0) {
            finishClearing();
        }
    }

    private void finishClearing() {
        clearBlackjackCardEntities();
        prepareWagersForNextRound();

        this.dealerHand.clear();
        this.currentTurnSlot = -1;
        this.dealerActionTicker = 0;
        this.settlementTicker = 0;
        this.dealingTicker = 0;
        this.dealingStep = 0;
        this.dealingCardsSpawned = false;
        this.nextRoundDealPass = 0;
        this.nextRoundDealTarget = 0;
        this.nextRoundDealTicker = 0;
        this.nextRoundDealCardSpawned = false;
        this.cardActionTicker = 0;
        this.cardActionSpawned = false;
        this.pendingPlayerHitSlot = -1;
        this.winnerAnimationSlot = -1;
        this.winnerAnimationTicker = 0;
        Arrays.fill(this.roundWinners, false);
        Arrays.fill(this.roundResults, RoundResult.NONE);
        this.roundSettlementApplied = false;
        this.roundRosterLocked = false;

        if (getActiveSlotCount() > 0) {
            beginRoundEndIdle();
        } else {
            beginSessionClearing();
        }
    }

    private void prepareWagersForNextRound() {
        for (int slotIndex = 0;
             slotIndex < SLOT_COUNT;
             slotIndex++) {

            resetPlayerRoundState(slotIndex);

            if (!this.activeSlots[slotIndex]) {
                this.roundParticipants[slotIndex] = false;
                continue;
            }

            if (this.wagerStates[slotIndex]
                    == WagerState.NEXT_ROUND_PREPAID) {
                this.wagerStates[slotIndex] =
                        WagerState.CURRENT_ROUND;
                this.roundParticipants[slotIndex] = true;
                ensureBlackjackChipEntity(slotIndex);
                continue;
            }

            removeBlackjackChipEntity(slotIndex);

            ServerPlayerEntity player =
                    getServerPlayer(slotIndex);

            if (this.wagerStates[slotIndex]
                    == WagerState.CURRENT_ROUND
                    && player != null
                    && consumeMatchingChip(
                            player,
                            this.sessionBetStacks[slotIndex]
                    )) {
                this.roundParticipants[slotIndex] = true;
                spawnBlackjackChipEntity(slotIndex);
                continue;
            }

            if (player != null) {
                clearClientSession(player);
            }

            this.activeSlots[slotIndex] = false;
            this.slotPlayers[slotIndex] = null;
            this.roundParticipants[slotIndex] = false;
            this.sessionBetStacks[slotIndex] = ItemStack.EMPTY;
            this.wagerStates[slotIndex] = WagerState.NONE;
        }
    }

    private boolean consumeMatchingChip(
            @Nonnull ServerPlayerEntity player,
            @Nonnull ItemStack wagerTemplate
    ) {
        if (!isValidBlackjackChip(wagerTemplate)) {
            return false;
        }

        for (int inventorySlot = 0;
             inventorySlot < player.inventory.getContainerSize();
             inventorySlot++) {

            ItemStack stack =
                    player.inventory.getItem(inventorySlot);

            if (stack.isEmpty()
                    || !isSameChipDenomination(
                            stack,
                            wagerTemplate
                    )) {
                continue;
            }

            stack.shrink(1);
            player.inventory.setChanged();
            return true;
        }

        return false;
    }

    /**
     * clear_table2 完成后保留 deck_thin，先回到 idle_loop2。
     * 仍有玩家就座时，完整播放一轮后直接发下一回合起手牌。
     */
    private void beginRoundEndIdle() {
        this.phase = Phase.ROUND_END_IDLE;
        this.phaseTicker = ROUND_END_IDLE_DURATION_TICKS;
        playAnimation(ANIMATION_IDLE_LOOP_WITH_DECK, true);
        markAndSync();
    }

    private void tickRoundEndIdle() {
        this.phaseTicker--;

        if (this.phaseTicker > 0) {
            return;
        }

        if (getActiveSlotCount() > 0) {
            startNextRoundFromDeck();
        } else {
            beginSessionClearing();
        }
    }

    private void beginSessionClearing() {
        if (this.phase == Phase.SESSION_CLEARING) {
            return;
        }

        this.phase = Phase.SESSION_CLEARING;
        this.phaseTicker = CLEAR_TABLE_DURATION_TICKS;
        this.currentTurnSlot = -1;
        playAnimation(ANIMATION_CLEAR_SESSION, false);
        markAndSync();
    }

    private void tickSessionClearing() {
        this.phaseTicker--;

        if (this.phaseTicker <= 0) {
            finishSessionClearing();
        }
    }

    private void finishSessionClearing() {
        clearBlackjackCardEntities();
        clearBlackjackChipEntities();

        for (int slotIndex = 0;
             slotIndex < SLOT_COUNT;
             slotIndex++) {
            this.activeSlots[slotIndex] = false;
            this.roundParticipants[slotIndex] = false;
            this.slotPlayers[slotIndex] = null;
            this.sessionBetStacks[slotIndex] = ItemStack.EMPTY;
            this.wagerStates[slotIndex] = WagerState.NONE;
            this.roundResults[slotIndex] = RoundResult.NONE;
            resetPlayerRoundState(slotIndex);
        }

        this.dealerHand.clear();
        this.remainingCards.clear();
        this.roundSettlementApplied = false;
        returnToWaiting();
        markAndSync();
    }

    private void returnToWaiting() {
        this.phase = Phase.WAITING;
        this.phaseTicker = 0;
        this.currentTurnSlot = -1;
        this.dealerActionTicker = 0;
        this.settlementTicker = 0;
        this.dealingTicker = 0;
        this.dealingStep = 0;
        this.dealingCardsSpawned = false;
        this.nextRoundDealPass = 0;
        this.nextRoundDealTarget = 0;
        this.nextRoundDealTicker = 0;
        this.nextRoundDealCardSpawned = false;
        this.cardActionTicker = 0;
        this.cardActionSpawned = false;
        this.pendingPlayerHitSlot = -1;
        this.winnerAnimationSlot = -1;
        this.winnerAnimationTicker = 0;
        Arrays.fill(this.roundWinners, false);
        Arrays.fill(this.roundResults, RoundResult.NONE);
        this.roundSettlementApplied = false;
        this.roundRosterLocked = false;
        playAnimation(ANIMATION_IDLE_LOOP, true);
    }


    public void clearBlackjackCardEntities() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        AxisAlignedBB searchArea =
                new AxisAlignedBB(this.worldPosition)
                        .inflate(CARD_SEARCH_RADIUS);

        this.level.getEntitiesOfClass(
                PlayingCardEntity.class,
                searchArea,
                card -> card.belongsToBlackjackTable(
                        this.worldPosition
                )
        ).forEach(PlayingCardEntity::remove);
    }

    public void clearBlackjackChipEntities() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        AxisAlignedBB searchArea =
                new AxisAlignedBB(this.worldPosition)
                        .inflate(CHIP_SEARCH_RADIUS);

        this.level.getEntitiesOfClass(
                BlackjackChipEntity.class,
                searchArea,
                chip -> chip.belongsToBlackjackTable(
                        this.worldPosition
                )
        ).forEach(BlackjackChipEntity::remove);
    }

    private void removeBlackjackChipEntity(int slotIndex) {
        if (this.level == null
                || this.level.isClientSide
                || !isValidSlot(slotIndex)) {
            return;
        }

        AxisAlignedBB searchArea =
                new AxisAlignedBB(this.worldPosition)
                        .inflate(CHIP_SEARCH_RADIUS);

        this.level.getEntitiesOfClass(
                BlackjackChipEntity.class,
                searchArea,
                chip -> chip.belongsToBlackjackTable(
                        this.worldPosition
                )
                        && chip.getBlackjackSlotIndex()
                        == slotIndex
        ).forEach(BlackjackChipEntity::remove);
    }

    private boolean hasBlackjackChipEntity(int slotIndex) {
        if (this.level == null
                || this.level.isClientSide
                || !isValidSlot(slotIndex)) {
            return false;
        }

        AxisAlignedBB searchArea =
                new AxisAlignedBB(this.worldPosition)
                        .inflate(CHIP_SEARCH_RADIUS);

        return !this.level.getEntitiesOfClass(
                BlackjackChipEntity.class,
                searchArea,
                chip -> chip.belongsToBlackjackTable(
                        this.worldPosition
                )
                        && chip.getBlackjackSlotIndex()
                        == slotIndex
        ).isEmpty();
    }

    private void ensureBlackjackChipEntity(int slotIndex) {
        if (!hasBlackjackChipEntity(slotIndex)) {
            spawnBlackjackChipEntity(slotIndex);
        }
    }

    private void tickWagerChipEntities() {
        if (this.phase == Phase.SESSION_CLEARING) {
            return;
        }

        this.chipEntityCheckTicker++;

        if (this.chipEntityCheckTicker
                < CHIP_ENTITY_CHECK_INTERVAL) {
            return;
        }

        this.chipEntityCheckTicker = 0;

        for (int slotIndex = 0;
             slotIndex < SLOT_COUNT;
             slotIndex++) {

            boolean shouldExist =
                    this.activeSlots[slotIndex]
                            && this.wagerStates[slotIndex]
                            != WagerState.NONE
                            && !this.sessionBetStacks[
                                    slotIndex].isEmpty();

            if (shouldExist) {
                ensureBlackjackChipEntity(slotIndex);
            } else {
                removeBlackjackChipEntity(slotIndex);
            }
        }
    }

    private void spawnBlackjackChipEntity(int slotIndex) {
        if (this.level == null
                || this.level.isClientSide
                || !isValidSlot(slotIndex)
                || this.sessionBetStacks[slotIndex].isEmpty()) {
            return;
        }

        removeBlackjackChipEntity(slotIndex);

        BlackjackChipEntity chipEntity =
                ModEntityTypes.BLACKJACK_CHIP.get()
                        .create(this.level);

        if (chipEntity == null) {
            return;
        }

        ItemStack chipStack =
                this.sessionBetStacks[slotIndex].copy();
        chipStack.setCount(1);

        chipEntity.setChipStack(chipStack);
        chipEntity.setBlackjackSource(
                this.worldPosition,
                slotIndex
        );

        Vector3d position =
                getChipPosition(slotIndex);

        chipEntity.setPos(
                position.x,
                position.y,
                position.z
        );

        Direction facing =
                this.getBlockState()
                        .getValue(
                                BlackjackTableBlock.FACING
                        );

        chipEntity.yRot = facing.toYRot();
        chipEntity.yRotO = chipEntity.yRot;
        this.level.addFreshEntity(chipEntity);
    }

    @Nonnull
    private Vector3d getChipPosition(int slotIndex) {
        Direction facing =
                this.getBlockState()
                        .getValue(
                                BlackjackTableBlock.FACING
                        );

        Direction right = facing.getClockWise();

        double localX = slotIndex - 1.0D;

        /*
         * 玩家位于 facing 方向。将筹码从原来的 1.02D
         * 向牌桌内部移动 0.30 格，避免与玩家手牌重叠。
         */
        double localZ = 0.72D;

        double x =
                this.worldPosition.getX() + 0.5D
                        + right.getStepX() * localX
                        + facing.getStepX() * localZ;

        double y =
                this.worldPosition.getY() + 0.92D;

        double z =
                this.worldPosition.getZ() + 0.5D
                        + right.getStepZ() * localX
                        + facing.getStepZ() * localZ;

        return new Vector3d(x, y, z);
    }

    private void dropChipAtSlot(
            int slotIndex,
            @Nonnull ItemStack stack
    ) {
        if (this.level == null
                || this.level.isClientSide
                || stack.isEmpty()) {
            return;
        }

        Vector3d position =
                getChipPosition(slotIndex);

        ItemEntity itemEntity =
                new ItemEntity(
                        this.level,
                        position.x,
                        position.y + 0.15D,
                        position.z,
                        stack
                );

        this.level.addFreshEntity(itemEntity);
    }

    public static boolean isValidBlackjackChip(
            @Nonnull ItemStack stack
    ) {
        return !stack.isEmpty()
                && (stack.getItem() == ModItems.CHIP_5.get()
                || stack.getItem() == ModItems.CHIP_20.get());
    }

    private static boolean isSameChipDenomination(
            @Nonnull ItemStack first,
            @Nonnull ItemStack second
    ) {
        return isValidBlackjackChip(first)
                && isValidBlackjackChip(second)
                && first.getItem() == second.getItem();
    }

    private void spawnPlayerCardEntity(
            int slotIndex,
            @Nonnull PlayingCard card,
            int cardIndex
    ) {
        spawnBlackjackCardEntity(
                card,
                false,
                slotIndex,
                cardIndex,
                true
        );
    }

    private void spawnDealerCardEntity(
            @Nonnull PlayingCard card,
            int cardIndex,
            boolean faceUp
    ) {
        spawnBlackjackCardEntity(
                card,
                true,
                -1,
                cardIndex,
                faceUp
        );
    }

    private void spawnBlackjackCardEntity(
            @Nonnull PlayingCard card,
            boolean dealerCard,
            int slotIndex,
            int cardIndex,
            boolean faceUp
    ) {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        PlayingCardEntity cardEntity =
                ModEntityTypes.PLAYING_CARD.get()
                        .create(this.level);

        if (cardEntity == null) {
            return;
        }

        cardEntity.setCard(
                card.getSuit(),
                card.getRank()
        );
        cardEntity.setFaceUp(faceUp);
        cardEntity.setBlackjackSource(
                this.worldPosition,
                dealerCard,
                slotIndex,
                cardIndex
        );

        Vector3d position =
                getCardPosition(
                        dealerCard,
                        slotIndex,
                        cardIndex
                );

        cardEntity.setPos(
                position.x,
                position.y,
                position.z
        );

        Direction facing =
                this.getBlockState()
                        .getValue(
                                BlackjackTableBlock.FACING
                        );

        cardEntity.yRot = facing.toYRot();
        this.level.addFreshEntity(cardEntity);
    }

    @Nonnull
    private Vector3d getCardPosition(
            boolean dealerCard,
            int slotIndex,
            int cardIndex
    ) {
        Direction facing =
                this.getBlockState()
                        .getValue(
                                BlackjackTableBlock.FACING
                        );

        Direction right = facing.getClockWise();

        double localX =
                dealerCard
                        ? 0.0D
                        : slotIndex - 1.0D;

        double localZ =
                dealerCard
                        ? 0.37D
                        : 1.20D;

        /*
         * 手牌从左向右延展。
         *
         * 第一张牌先整体向左移动，
         * 后续牌再按较小间距向右叠放。
         */
        double initialLeftOffset =
                dealerCard
                        ? 0.10D
                        : 0.15D;

        /*
         * 牌桌局部坐标中的 right 方向，
         * 与玩家面对牌桌时看到的屏幕左右相反。
         * 因此这里使用减号，视觉上才会从左向右延展。
         */
        localX +=
                initialLeftOffset
                        - cardIndex * CARD_SPACING;

        double x =
                this.worldPosition.getX() + 0.5D
                        + right.getStepX() * localX
                        + facing.getStepX() * localZ;

        /*
         * 纸牌贴近桌面放置。
         * 如模型桌面高度之后发生变化，只需调整 0.90D。
         */
        double y =
                this.worldPosition.getY() + 0.90D
                        + cardIndex * 0.002D;

        double z =
                this.worldPosition.getZ() + 0.5D
                        + right.getStepZ() * localX
                        + facing.getStepZ() * localZ;

        return new Vector3d(x, y, z);
    }

    private void revealDealerHoleCard() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        AxisAlignedBB searchArea =
                new AxisAlignedBB(this.worldPosition)
                        .inflate(CARD_SEARCH_RADIUS);

        this.level.getEntitiesOfClass(
                PlayingCardEntity.class,
                searchArea,
                card -> card.belongsToBlackjackTable(
                        this.worldPosition
                )
                        && card.isBlackjackDealerCard()
        ).forEach(card -> card.setFaceUp(true));
    }

    private void resetRoundOnly() {
        for (int slotIndex = 0;
             slotIndex < SLOT_COUNT;
             slotIndex++) {

            this.playerHands[slotIndex].clear();
            this.playerStood[slotIndex] = false;
            this.playerBusted[slotIndex] = false;
            this.playerForfeited[slotIndex] = false;
            this.roundResults[slotIndex] = RoundResult.NONE;
        }

        this.dealerHand.clear();
        this.currentTurnSlot = -1;
        this.dealerActionTicker = 0;
        this.settlementTicker = 0;
        this.nextRoundDealPass = 0;
        this.nextRoundDealTarget = 0;
        this.nextRoundDealTicker = 0;
        this.nextRoundDealCardSpawned = false;
        this.cardActionTicker = 0;
        this.cardActionSpawned = false;
        this.pendingPlayerHitSlot = -1;
        this.winnerAnimationSlot = -1;
        this.winnerAnimationTicker = 0;
        this.roundSettlementApplied = false;
        Arrays.fill(this.roundWinners, false);
    }

    private void resetPlayerRoundState(int slotIndex) {
        if (!isValidSlot(slotIndex)) {
            return;
        }

        this.playerHands[slotIndex].clear();
        this.playerStood[slotIndex] = false;
        this.playerBusted[slotIndex] = false;
        this.playerForfeited[slotIndex] = false;
        this.roundWinners[slotIndex] = false;
        this.roundResults[slotIndex] = RoundResult.NONE;
    }

    private void resetAndShuffleDeck() {
        Deck deck = new Deck();
        deck.shuffle(new Random());

        this.remainingCards.clear();
        this.remainingCards.addAll(
                deck.getRemainingCards()
        );
    }

    @Nonnull
    private PlayingCard drawCard() {
        if (this.remainingCards.isEmpty()) {
            resetAndShuffleDeck();
        }

        return this.remainingCards.remove(
                this.remainingCards.size() - 1
        );
    }

    public static int calculateHandValue(
            @Nonnull List<PlayingCard> cards
    ) {
        int total = 0;
        int aceCount = 0;

        for (PlayingCard card : cards) {
            total += card.getBlackjackValue();

            if (card.getRank() == CardRank.ACE) {
                aceCount++;
            }
        }

        while (total > 21 && aceCount > 0) {
            total -= 10;
            aceCount--;
        }

        return total;
    }

    private int getActiveSlotCount() {
        int count = 0;

        for (boolean active : this.activeSlots) {
            if (active) {
                count++;
            }
        }

        return count;
    }

    private int getRoundParticipantCount() {
        int count = 0;

        for (boolean participant : this.roundParticipants) {
            if (participant) {
                count++;
            }
        }

        return count;
    }

    private int findPlayerSlot(@Nonnull UUID playerUuid) {
        for (int slotIndex = 0;
             slotIndex < SLOT_COUNT;
             slotIndex++) {

            if (playerUuid.equals(
                    this.slotPlayers[slotIndex]
            )) {
                return slotIndex;
            }
        }

        return -1;
    }

    @Nullable
    private ServerPlayerEntity getServerPlayer(int slotIndex) {
        if (!(this.level instanceof ServerWorld)
                || !isValidSlot(slotIndex)) {
            return null;
        }

        UUID uuid = this.slotPlayers[slotIndex];

        if (uuid == null) {
            return null;
        }

        return ((ServerWorld) this.level)
                .getServer()
                .getPlayerList()
                .getPlayer(uuid);
    }

    private void notifyCurrentPlayer() {
        /*
         * 当前行动玩家的世界内提示将在后续单独设计。
         */
    }

    private void clearClientSession(
            @Nonnull ServerPlayerEntity player
    ) {
        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(
                        () -> player
                ),
                BlackjackSessionPacket.clear(this.worldPosition)
        );
    }

    @Nonnull
    private static String getCardDisplayName(
            @Nonnull PlayingCard card
    ) {
        return getSuitDisplayName(card.getSuit())
                + getRankDisplayName(card.getRank());
    }

    @Nonnull
    private static String getHandDisplayText(
            @Nonnull List<PlayingCard> cards
    ) {
        StringBuilder builder =
                new StringBuilder();

        for (int i = 0; i < cards.size(); i++) {
            if (i > 0) {
                builder.append("、");
            }

            builder.append(
                    getCardDisplayName(cards.get(i))
            );
        }

        return builder.toString();
    }

    @Nonnull
    private static String getSuitDisplayName(
            @Nonnull CardSuit suit
    ) {
        switch (suit) {
            case SPADES:
                return "黑桃";
            case HEARTS:
                return "红桃";
            case DIAMONDS:
                return "方块";
            case CLUBS:
                return "梅花";
            default:
                return "";
        }
    }

    @Nonnull
    private static String getRankDisplayName(
            @Nonnull CardRank rank
    ) {
        switch (rank) {
            case ACE:
                return "A";
            case JACK:
                return "J";
            case QUEEN:
                return "Q";
            case KING:
                return "K";
            default:
                return String.valueOf(
                        rank.getBlackjackValue()
                );
        }
    }

    private boolean isValidSlot(int slotIndex) {
        return slotIndex >= 0
                && slotIndex < SLOT_COUNT;
    }


    /**
     * 扩大 TileEntity 的客户端渲染范围。
     *
     * 牌桌实际模型覆盖 3×2 个方块区域，
     * 但 Minecraft 默认只根据 TileEntity 所在单个方块进行视锥裁剪。
     * 当玩家视角离开主方块范围时，会导致整个 GeckoLib 模型停止渲染。
     *
     * 返回覆盖整张牌桌的包围盒，避免附属方块方向移动时突然消失。
     */
    @Override
    @Nonnull
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(
                this.worldPosition.offset(-1, 0, 0),
                this.worldPosition.offset(2, 2, 2)
        );
    }

    /**
     * 保存完整牌局状态。
     */
    @Override
    public void load(
            @Nonnull BlockState state,
            @Nonnull CompoundNBT compound
    ) {
        super.load(state, compound);

        String phaseName =
                compound.getString("BlackjackPhase");

        try {
            this.phase =
                    Phase.valueOf(phaseName);
        } catch (IllegalArgumentException exception) {
            this.phase = Phase.WAITING;
        }

        this.currentTurnSlot =
                compound.getInt("CurrentTurnSlot");

        this.phaseTicker =
                compound.getInt("PhaseTicker");

        this.roundRosterLocked =
                compound.getBoolean("RoundRosterLocked");

        this.currentAnimation =
                compound.contains("CurrentAnimation")
                        ? compound.getString("CurrentAnimation")
                        : ANIMATION_IDLE_LOOP;

        this.currentAnimationLoop =
                compound.getBoolean("CurrentAnimationLoop");

        this.animationSequence =
                compound.getInt("AnimationSequence");

        this.dealerActionTicker =
                compound.getInt("DealerActionTicker");

        this.dealingStep =
                compound.getInt("DealingStep");

        this.dealingTicker =
                compound.getInt("DealingTicker");

        this.dealingCardsSpawned =
                compound.getBoolean("DealingCardsSpawned");

        this.nextRoundDealPass =
                compound.getInt("NextRoundDealPass");

        this.nextRoundDealTarget =
                compound.getInt("NextRoundDealTarget");

        this.nextRoundDealTicker =
                compound.getInt("NextRoundDealTicker");

        this.nextRoundDealCardSpawned =
                compound.getBoolean(
                        "NextRoundDealCardSpawned"
                );

        this.winnerAnimationSlot =
                compound.contains("WinnerAnimationSlot")
                        ? compound.getInt(
                                "WinnerAnimationSlot"
                        )
                        : -1;

        this.winnerAnimationTicker =
                compound.getInt("WinnerAnimationTicker");

        this.cardActionTicker =
                compound.getInt("CardActionTicker");

        this.cardActionSpawned =
                compound.getBoolean("CardActionSpawned");

        this.pendingPlayerHitSlot =
                compound.contains("PendingPlayerHitSlot")
                        ? compound.getInt("PendingPlayerHitSlot")
                        : -1;

        this.settlementTicker =
                compound.getInt("SettlementTicker");

        this.roundSettlementApplied =
                compound.getBoolean(
                        "RoundSettlementApplied"
                );

        for (int slotIndex = 0;
             slotIndex < SLOT_COUNT;
             slotIndex++) {

            String prefix =
                    "Slot" + slotIndex;

            this.activeSlots[slotIndex] =
                    compound.getBoolean(
                            prefix + "Active"
                    );

            this.roundParticipants[slotIndex] =
                    compound.getBoolean(
                            prefix + "RoundParticipant"
                    );

            if (compound.hasUUID(
                    prefix + "Player"
            )) {
                this.slotPlayers[slotIndex] =
                        compound.getUUID(
                                prefix + "Player"
                        );
            } else {
                this.slotPlayers[slotIndex] = null;
            }

            if (compound.contains(prefix + "BetStack")) {
                ItemStack wager =
                        ItemStack.of(
                                compound.getCompound(
                                        prefix + "BetStack"
                                )
                        );

                if (isValidBlackjackChip(wager)) {
                    wager.setCount(1);
                    this.sessionBetStacks[slotIndex] = wager;
                } else {
                    this.sessionBetStacks[slotIndex] =
                            ItemStack.EMPTY;
                }
            } else {
                this.sessionBetStacks[slotIndex] =
                        ItemStack.EMPTY;
            }

            try {
                this.wagerStates[slotIndex] =
                        WagerState.valueOf(
                                compound.getString(
                                        prefix + "WagerState"
                                )
                        );
            } catch (IllegalArgumentException exception) {
                this.wagerStates[slotIndex] =
                        WagerState.NONE;
            }

            try {
                this.roundResults[slotIndex] =
                        RoundResult.valueOf(
                                compound.getString(
                                        prefix + "RoundResult"
                                )
                        );
            } catch (IllegalArgumentException exception) {
                this.roundResults[slotIndex] =
                        RoundResult.NONE;
            }

            this.playerStood[slotIndex] =
                    compound.getBoolean(
                            prefix + "Stood"
                    );

            this.playerBusted[slotIndex] =
                    compound.getBoolean(
                            prefix + "Busted"
                    );

            this.playerForfeited[slotIndex] =
                    compound.getBoolean(
                            prefix + "Forfeited"
                    );

            this.roundWinners[slotIndex] =
                    compound.getBoolean(
                            prefix + "RoundWinner"
                    );

            readCardList(
                    compound.getList(
                            prefix + "Hand",
                            10
                    ),
                    this.playerHands[slotIndex]
            );

            if (this.slotPlayers[slotIndex] == null
                    || this.sessionBetStacks[
                            slotIndex].isEmpty()
                    || this.wagerStates[slotIndex]
                            == WagerState.NONE) {
                this.activeSlots[slotIndex] = false;
                this.roundParticipants[slotIndex] = false;
                this.slotPlayers[slotIndex] = null;
                this.sessionBetStacks[slotIndex] =
                        ItemStack.EMPTY;
                this.wagerStates[slotIndex] =
                        WagerState.NONE;
            }
        }

        readCardList(
                compound.getList(
                        "DealerHand",
                        10
                ),
                this.dealerHand
        );

        readCardList(
                compound.getList(
                        "RemainingCards",
                        10
                ),
                this.remainingCards
        );
    }

    @Nonnull
    @Override
    public CompoundNBT save(
            @Nonnull CompoundNBT compound
    ) {
        super.save(compound);

        compound.putString(
                "BlackjackPhase",
                this.phase.name()
        );

        compound.putInt(
                "CurrentTurnSlot",
                this.currentTurnSlot
        );

        compound.putInt(
                "PhaseTicker",
                this.phaseTicker
        );

        compound.putBoolean(
                "RoundRosterLocked",
                this.roundRosterLocked
        );

        compound.putString(
                "CurrentAnimation",
                this.currentAnimation
        );

        compound.putBoolean(
                "CurrentAnimationLoop",
                this.currentAnimationLoop
        );

        compound.putInt(
                "AnimationSequence",
                this.animationSequence
        );

        compound.putInt(
                "DealerActionTicker",
                this.dealerActionTicker
        );

        compound.putInt(
                "DealingStep",
                this.dealingStep
        );

        compound.putInt(
                "DealingTicker",
                this.dealingTicker
        );

        compound.putBoolean(
                "DealingCardsSpawned",
                this.dealingCardsSpawned
        );

        compound.putInt(
                "NextRoundDealPass",
                this.nextRoundDealPass
        );

        compound.putInt(
                "NextRoundDealTarget",
                this.nextRoundDealTarget
        );

        compound.putInt(
                "NextRoundDealTicker",
                this.nextRoundDealTicker
        );

        compound.putBoolean(
                "NextRoundDealCardSpawned",
                this.nextRoundDealCardSpawned
        );

        compound.putInt(
                "WinnerAnimationSlot",
                this.winnerAnimationSlot
        );

        compound.putInt(
                "WinnerAnimationTicker",
                this.winnerAnimationTicker
        );

        compound.putInt(
                "CardActionTicker",
                this.cardActionTicker
        );

        compound.putBoolean(
                "CardActionSpawned",
                this.cardActionSpawned
        );

        compound.putInt(
                "PendingPlayerHitSlot",
                this.pendingPlayerHitSlot
        );

        compound.putInt(
                "SettlementTicker",
                this.settlementTicker
        );

        compound.putBoolean(
                "RoundSettlementApplied",
                this.roundSettlementApplied
        );

        for (int slotIndex = 0;
             slotIndex < SLOT_COUNT;
             slotIndex++) {

            String prefix =
                    "Slot" + slotIndex;

            compound.putBoolean(
                    prefix + "Active",
                    this.activeSlots[slotIndex]
            );

            compound.putBoolean(
                    prefix + "RoundParticipant",
                    this.roundParticipants[slotIndex]
            );

            UUID playerUuid =
                    this.slotPlayers[slotIndex];

            if (playerUuid != null) {
                compound.putUUID(
                        prefix + "Player",
                        playerUuid
                );
            }

            ItemStack wager =
                    this.sessionBetStacks[slotIndex];

            if (!wager.isEmpty()) {
                compound.put(
                        prefix + "BetStack",
                        wager.save(new CompoundNBT())
                );
            }

            compound.putString(
                    prefix + "WagerState",
                    this.wagerStates[slotIndex].name()
            );

            compound.putString(
                    prefix + "RoundResult",
                    this.roundResults[slotIndex].name()
            );

            compound.putBoolean(
                    prefix + "Stood",
                    this.playerStood[slotIndex]
            );

            compound.putBoolean(
                    prefix + "Busted",
                    this.playerBusted[slotIndex]
            );

            compound.putBoolean(
                    prefix + "Forfeited",
                    this.playerForfeited[slotIndex]
            );

            compound.putBoolean(
                    prefix + "RoundWinner",
                    this.roundWinners[slotIndex]
            );

            compound.put(
                    prefix + "Hand",
                    writeCardList(
                            this.playerHands[slotIndex]
                    )
            );
        }

        compound.put(
                "DealerHand",
                writeCardList(this.dealerHand)
        );

        compound.put(
                "RemainingCards",
                writeCardList(this.remainingCards)
        );

        return compound;
    }

    private static void readCardList(
            @Nonnull ListNBT listTag,
            @Nonnull List<PlayingCard> output
    ) {
        output.clear();

        CardSuit[] suits = CardSuit.values();
        CardRank[] ranks = CardRank.values();

        for (int i = 0; i < listTag.size(); i++) {
            CompoundNBT cardTag =
                    listTag.getCompound(i);

            int suitIndex =
                    cardTag.getInt("Suit");

            int rankIndex =
                    cardTag.getInt("Rank");

            if (suitIndex >= 0
                    && suitIndex < suits.length
                    && rankIndex >= 0
                    && rankIndex < ranks.length) {
                output.add(
                        new PlayingCard(
                                suits[suitIndex],
                                ranks[rankIndex]
                        )
                );
            }
        }
    }

    @Nonnull
    private static ListNBT writeCardList(
            @Nonnull List<PlayingCard> cards
    ) {
        ListNBT listTag = new ListNBT();

        for (PlayingCard card : cards) {
            CompoundNBT cardTag =
                    new CompoundNBT();

            cardTag.putInt(
                    "Suit",
                    card.getSuit().ordinal()
            );

            cardTag.putInt(
                    "Rank",
                    card.getRank().ordinal()
            );

            listTag.add(cardTag);
        }

        return listTag;
    }

    public enum WagerState {
        NONE,
        CURRENT_ROUND,
        NEXT_ROUND_PREPAID
    }

    public enum RoundResult {
        NONE,
        WIN,
        PUSH,
        LOSS,
        FORFEIT
    }

    /**
     * 当前牌桌阶段。
     */
    public enum Phase {
        WAITING,
        FANCY_INTRO,
        DEAL_PREPARE,
        DEALING,
        NEXT_ROUND_DEALING,
        MAGIC_CARD_TO_DECK,
        PLAYER_TURNS,
        PLAYER_HIT,
        DEALER_TURN,
        DEALER_DRAW,
        SETTLEMENT,
        WINNER_ANIMATION,
        CLEARING,
        SESSION_CLEARING,
        ROUND_END_IDLE
    }

    /**
     * 手势请求的服务器处理结果。
     */
    public enum GestureResult {
        SUCCESS,
        WRONG_PHASE,
        NOT_OWNER,
        NOT_YOUR_TURN,
        ALREADY_FINISHED
    }

    public void playAnimation(
            @Nonnull String animation,
            boolean loop
    ) {
        this.currentAnimation = animation;
        this.currentAnimationLoop = loop;
        this.animationSequence++;
        markAndSync();
    }

    private void markAndSync() {
        this.setChanged();

        if (this.level != null
                && !this.level.isClientSide) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(
                    this.worldPosition,
                    state,
                    state,
                    3
            );
        }
    }

    @Nullable
    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(
                this.worldPosition,
                1,
                this.getUpdateTag()
        );
    }

    @Nonnull
    @Override
    public CompoundNBT getUpdateTag() {
        return this.save(new CompoundNBT());
    }

    @Override
    public void onDataPacket(
            @Nonnull NetworkManager networkManager,
            @Nonnull SUpdateTileEntityPacket packet
    ) {
        CompoundNBT tag = packet.getTag();

        if (tag != null) {
            this.handleUpdateTag(
                    this.getBlockState(),
                    tag
            );
        }
    }

    private <E extends IAnimatable> PlayState animationPredicate(
            @Nonnull AnimationEvent<E> event
    ) {
        AnimationController<E> controller =
                event.getController();

        if (this.lastAppliedAnimationSequence
                != this.animationSequence) {
            controller.markNeedsReload();
            this.lastAppliedAnimationSequence =
                    this.animationSequence;
        }

        controller.setAnimation(
                new AnimationBuilder().addAnimation(
                        this.currentAnimation,
                        this.currentAnimationLoop
                )
        );

        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(
            @Nonnull AnimationData animationData
    ) {
        animationData.addAnimationController(
                new AnimationController<>(
                        this,
                        ANIMATION_CONTROLLER,
                        0,
                        this::animationPredicate
                )
        );
    }

    @Nonnull
    @Override
    public AnimationFactory getFactory() {
        return this.animationFactory;
    }
}
