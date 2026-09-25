package com.example.betandcoin.entity;

import com.example.betandcoin.game.blackjack.CardRank;
import com.example.betandcoin.game.blackjack.CardSuit;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PlayingCardEntity extends Entity {

    /*
     * 使用枚举下标同步花色和点数。
     *
     * SUIT 保存 CardSuit 的 ordinal。
     * RANK 保存 CardRank 的 ordinal。
     * FACE_UP 保存扑克牌当前是否正面朝上。
     */
    private static final DataParameter<Integer> SUIT =
            EntityDataManager.defineId(
                    PlayingCardEntity.class,
                    DataSerializers.INT
            );

    private static final DataParameter<Integer> RANK =
            EntityDataManager.defineId(
                    PlayingCardEntity.class,
                    DataSerializers.INT
            );

    private static final DataParameter<Boolean> FACE_UP =
            EntityDataManager.defineId(
                    PlayingCardEntity.class,
                    DataSerializers.BOOLEAN
            );

    /*
     * 客户端渲染需要知道这张牌是否属于 Blackjack 牌桌，
     * 因此单独同步一个布尔值。
     */
    private static final DataParameter<Boolean> BLACKJACK_CARD =
            EntityDataManager.defineId(
                    PlayingCardEntity.class,
                    DataSerializers.BOOLEAN
            );

    /*
     * 记录这张牌是由哪个牌堆发出的。
     *
     * 目前这个数据只需要在服务端用于识别和清理纸牌，
     * 因此不需要通过 EntityDataManager 同步给客户端。
     */
    @Nullable
    private BlockPos sourceDeckPos;

    @Nullable
    private BlockPos sourceBlackjackTablePos;

    private boolean blackjackDealerCard = false;
    private int blackjackSlotIndex = -1;
    private int blackjackCardIndex = -1;

    public PlayingCardEntity(
            @Nonnull EntityType<? extends PlayingCardEntity> entityType,
            @Nonnull World world
    ) {
        super(entityType, world);
    }

    @Override
    protected void defineSynchedData() {
        /*
         * 默认使用枚举中的第一个花色和第一个点数。
         * 这样不依赖枚举常量的具体排列名称。
         */
        this.entityData.define(SUIT, 0);
        this.entityData.define(RANK, 0);
        this.entityData.define(FACE_UP, false);
        this.entityData.define(BLACKJACK_CARD, false);
    }

    /**
     * 设置扑克牌的花色和点数。
     */
    public void setCard(
            @Nonnull CardSuit suit,
            @Nonnull CardRank rank
    ) {
        setSuit(suit);
        setRank(rank);
    }

    /**
     * 设置花色。
     */
    public void setSuit(@Nonnull CardSuit suit) {
        this.entityData.set(SUIT, suit.ordinal());
    }

    /**
     * 获取花色。
     */
    @Nonnull
    public CardSuit getSuit() {
        CardSuit[] suits = CardSuit.values();
        int index = this.entityData.get(SUIT);

        if (index < 0 || index >= suits.length) {
            index = 0;
        }

        return suits[index];
    }

    /**
     * 设置点数。
     */
    public void setRank(@Nonnull CardRank rank) {
        this.entityData.set(RANK, rank.ordinal());
    }

    /**
     * 获取点数。
     */
    @Nonnull
    public CardRank getRank() {
        CardRank[] ranks = CardRank.values();
        int index = this.entityData.get(RANK);

        if (index < 0 || index >= ranks.length) {
            index = 0;
        }

        return ranks[index];
    }

    /**
     * 设置扑克牌是否正面朝上。
     */
    public void setFaceUp(boolean faceUp) {
        this.entityData.set(FACE_UP, faceUp);
    }

    /**
     * 判断扑克牌是否正面朝上。
     */
    public boolean isFaceUp() {
        return this.entityData.get(FACE_UP);
    }

    /**
     * 翻转扑克牌。
     */
    public void flip() {
        setFaceUp(!isFaceUp());
    }

    /**
     * 设置生成这张牌的来源牌堆位置。
     *
     * immutable() 会保存一份独立且不可变的坐标，
     * 避免外部可变坐标对象影响这里的数据。
     */
    public void setSourceDeckPos(@Nonnull BlockPos sourceDeckPos) {
        this.sourceDeckPos = sourceDeckPos.immutable();
    }

    /**
     * 获取生成这张牌的来源牌堆位置。
     *
     * 对于旧版本中已经存在、尚未记录来源的纸牌，
     * 返回值可能为 null。
     */
    @Nullable
    public BlockPos getSourceDeckPos() {
        return this.sourceDeckPos;
    }

    /**
     * 判断这张牌是否属于指定牌堆。
     */
    public boolean belongsToDeck(@Nonnull BlockPos deckPos) {
        return this.sourceDeckPos != null
                && this.sourceDeckPos.equals(deckPos);
    }

    public void setBlackjackSource(
            @Nonnull BlockPos tablePos,
            boolean dealerCard,
            int slotIndex,
            int cardIndex
    ) {
        this.sourceBlackjackTablePos = tablePos.immutable();
        this.entityData.set(BLACKJACK_CARD, true);
        this.blackjackDealerCard = dealerCard;
        this.blackjackSlotIndex = slotIndex;
        this.blackjackCardIndex = cardIndex;
    }

    @Nullable
    public BlockPos getSourceBlackjackTablePos() {
        return this.sourceBlackjackTablePos;
    }

    public boolean belongsToBlackjackTable(
            @Nonnull BlockPos tablePos
    ) {
        return this.sourceBlackjackTablePos != null
                && this.sourceBlackjackTablePos.equals(tablePos);
    }

    public boolean isBlackjackCard() {
        return this.entityData.get(BLACKJACK_CARD);
    }

    public boolean isBlackjackDealerCard() {
        return this.blackjackDealerCard;
    }

    public int getBlackjackSlotIndex() {
        return this.blackjackSlotIndex;
    }

    public int getBlackjackCardIndex() {
        return this.blackjackCardIndex;
    }

    @Override
    protected void readAdditionalSaveData(
            @Nonnull CompoundNBT compound
    ) {
        int suitIndex = compound.getInt("CardSuit");
        int rankIndex = compound.getInt("CardRank");

        CardSuit[] suits = CardSuit.values();
        CardRank[] ranks = CardRank.values();

        if (suitIndex >= 0 && suitIndex < suits.length) {
            setSuit(suits[suitIndex]);
        }

        if (rankIndex >= 0 && rankIndex < ranks.length) {
            setRank(ranks[rankIndex]);
        }

        setFaceUp(compound.getBoolean("FaceUp"));

        /*
         * 兼容旧世界：
         * 只有 NBT 中确实存在来源坐标时才读取。
         */
        if (compound.contains("SourceDeckPos")) {
            this.sourceDeckPos = BlockPos.of(
                    compound.getLong("SourceDeckPos")
            );
        } else {
            this.sourceDeckPos = null;
        }

        if (compound.contains("SourceBlackjackTablePos")) {
            this.sourceBlackjackTablePos = BlockPos.of(
                    compound.getLong("SourceBlackjackTablePos")
            );
            this.entityData.set(BLACKJACK_CARD, true);
            this.blackjackDealerCard =
                    compound.getBoolean("BlackjackDealerCard");
            this.blackjackSlotIndex =
                    compound.getInt("BlackjackSlotIndex");
            this.blackjackCardIndex =
                    compound.getInt("BlackjackCardIndex");
        } else {
            this.sourceBlackjackTablePos = null;
            this.entityData.set(BLACKJACK_CARD, false);
            this.blackjackDealerCard = false;
            this.blackjackSlotIndex = -1;
            this.blackjackCardIndex = -1;
        }
    }

    @Override
    protected void addAdditionalSaveData(
            @Nonnull CompoundNBT compound
    ) {
        compound.putInt("CardSuit", getSuit().ordinal());
        compound.putInt("CardRank", getRank().ordinal());
        compound.putBoolean("FaceUp", isFaceUp());

        /*
         * BlockPos 可以压缩成一个 long 保存，
         * 世界重新加载后仍能恢复来源关系。
         */
        if (this.sourceDeckPos != null) {
            compound.putLong(
                    "SourceDeckPos",
                    this.sourceDeckPos.asLong()
            );
        }

        if (this.sourceBlackjackTablePos != null) {
            compound.putLong(
                    "SourceBlackjackTablePos",
                    this.sourceBlackjackTablePos.asLong()
            );
            compound.putBoolean(
                    "BlackjackDealerCard",
                    this.blackjackDealerCard
            );
            compound.putInt(
                    "BlackjackSlotIndex",
                    this.blackjackSlotIndex
            );
            compound.putInt(
                    "BlackjackCardIndex",
                    this.blackjackCardIndex
            );
        }
    }

    @Nonnull
    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}