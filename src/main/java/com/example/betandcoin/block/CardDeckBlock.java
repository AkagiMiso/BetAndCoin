package com.example.betandcoin.block;

import com.example.betandcoin.entity.PlayingCardEntity;
import com.example.betandcoin.game.blackjack.PlayingCard;
import com.example.betandcoin.registry.ModEntityTypes;
import com.example.betandcoin.tileentity.CardDeckTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CardDeckBlock extends HorizontalBlock {

    public static final DirectionProperty FACING =
            HorizontalBlock.FACING;

    /*
     * true：使用 card_deck_full 模型
     * false：使用 card_deck_thin 模型
     */
    public static final BooleanProperty FULL =
            BooleanProperty.create("full");

    public CardDeckBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(FULL, true)
        );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(
            @Nonnull BlockItemUseContext context
    ) {
        return this.defaultBlockState()
                .setValue(
                        FACING,
                        context.getHorizontalDirection().getOpposite()
                )
                .setValue(FULL, true);
    }

    @Override
    protected void createBlockStateDefinition(
            @Nonnull StateContainer.Builder<Block, BlockState> builder
    ) {
        builder.add(FACING, FULL);
    }

    @Override
    public boolean hasTileEntity(@Nonnull BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(
            @Nonnull BlockState state,
            @Nonnull IBlockReader world
    ) {
        return new CardDeckTileEntity();
    }

    @Nonnull
    @Override
    public ActionResultType use(
            @Nonnull BlockState state,
            @Nonnull World world,
            @Nonnull BlockPos pos,
            @Nonnull PlayerEntity player,
            @Nonnull Hand hand,
            @Nonnull BlockRayTraceResult hit
    ) {
        if (world.isClientSide) {
            return ActionResultType.SUCCESS;
        }

        TileEntity tileEntity = world.getBlockEntity(pos);

        if (!(tileEntity instanceof CardDeckTileEntity)) {
            return ActionResultType.FAIL;
        }

        CardDeckTileEntity cardDeck =
                (CardDeckTileEntity) tileEntity;

        /*
         * 潜行右键：
         * 重置为完整的 52 张牌并重新洗牌，
         * 同时把模型切换回 full。
         */
        if (player.isShiftKeyDown()) {
            /*
             * 查找牌堆附近由当前牌堆发出的扑克牌，
             * 并且只删除来源坐标与当前牌堆一致的实体。
             */
            AxisAlignedBB searchArea =
                    new AxisAlignedBB(pos).inflate(8.0D);

            world.getEntitiesOfClass(
                    PlayingCardEntity.class,
                    searchArea,
                    card -> card.belongsToDeck(pos)
            ).forEach(PlayingCardEntity::remove);

            cardDeck.resetAndShuffle();

            world.setBlock(
                    pos,
                    state.setValue(FULL, true),
                    3
            );

            return ActionResultType.CONSUME;
        }

        /*
         * 普通右键：
         * 从牌堆抽出一张牌。
         */
        PlayingCard drawnCard = cardDeck.drawCard();

        PlayingCardEntity cardEntity =
                ModEntityTypes.PLAYING_CARD.get().create(world);

        if (cardEntity == null) {
            return ActionResultType.FAIL;
        }

        cardEntity.setCard(
                drawnCard.getSuit(),
                drawnCard.getRank()
        );

        cardEntity.setFaceUp(true);

        /*
         * 记录这张牌是由当前牌堆发出的。
         */
        cardEntity.setSourceDeckPos(pos);

        /*
         * 只要成功抽出第一张牌，
         * 就立即将牌堆切换为 thin。
         */
        if (state.getValue(FULL)) {

            state = state.setValue(FULL, false);

            world.setBlock(
                    pos,
                    state,
                    3
            );
        }

        Direction facing = state.getValue(FACING);
        Direction right = facing.getClockWise();

        /*
         * 第一张抽出后剩余 51 张，因此索引为 0。
         */
        int drawnIndex =
                51 - cardDeck.getRemainingCardCount();

        /*
         * 每行放 13 张，共四行。
         */
        int column = drawnIndex % 13;
        int row = drawnIndex / 13;

        double sideOffset =
                (column - 6) * 0.32D;

        double forwardOffset =
                0.75D + row * 0.45D;

        double spawnX =
                pos.getX()
                        + 0.5D
                        + facing.getStepX() * forwardOffset
                        + right.getStepX() * sideOffset;

        double spawnY =
                pos.getY() + 0.15D;

        double spawnZ =
                pos.getZ()
                        + 0.5D
                        + facing.getStepZ() * forwardOffset
                        + right.getStepZ() * sideOffset;

        cardEntity.setPos(
                spawnX,
                spawnY,
                spawnZ
        );

        cardEntity.yRot = facing.toYRot();

        world.addFreshEntity(cardEntity);

        return ActionResultType.CONSUME;
    }
}