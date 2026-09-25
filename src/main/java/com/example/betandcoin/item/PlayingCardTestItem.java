package com.example.betandcoin.item;

import com.example.betandcoin.entity.PlayingCardEntity;
import com.example.betandcoin.game.blackjack.CardRank;
import com.example.betandcoin.game.blackjack.CardSuit;
import com.example.betandcoin.registry.ModEntityTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class PlayingCardTestItem extends Item {

    public PlayingCardTestItem(Properties properties) {
        super(properties);
    }

    @Nonnull
    @Override
    public ActionResultType useOn(@Nonnull ItemUseContext context) {
        World world = context.getLevel();

        if (!world.isClientSide) {
            PlayingCardEntity card =
                    ModEntityTypes.PLAYING_CARD.get().create(world);

            if (card == null) {
                return ActionResultType.FAIL;
            }

            setRandomCard(card, world);
            card.setFaceUp(world.random.nextBoolean());

            card.setPos(
                    context.getClickedPos().getX() + 0.5D,
                    context.getClickedPos().getY() + 1.01D,
                    context.getClickedPos().getZ() + 0.5D
            );

            card.yRot = context.getPlayer() == null
                    ? 0.0F
                    : context.getPlayer().yRot;

            world.addFreshEntity(card);
        }

        return ActionResultType.sidedSuccess(world.isClientSide);
    }

    @Nonnull
    @Override
    public ActionResult<ItemStack> use(
            @Nonnull World world,
            @Nonnull PlayerEntity player,
            @Nonnull Hand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide) {
            PlayingCardEntity card =
                    ModEntityTypes.PLAYING_CARD.get().create(world);

            if (card == null) {
                return ActionResult.fail(stack);
            }

            setRandomCard(card, world);
            card.setFaceUp(world.random.nextBoolean());

            card.setPos(
                    player.getX(),
                    player.getEyeY() - 0.5D,
                    player.getZ()
            );

            card.yRot = player.yRot;

            world.addFreshEntity(card);
        }

        return ActionResult.sidedSuccess(
                stack,
                world.isClientSide
        );
    }

    private void setRandomCard(
            @Nonnull PlayingCardEntity card,
            @Nonnull World world
    ) {
        CardSuit[] suits = CardSuit.values();
        CardRank[] ranks = CardRank.values();

        CardSuit randomSuit =
                suits[world.random.nextInt(suits.length)];

        CardRank randomRank =
                ranks[world.random.nextInt(ranks.length)];

        card.setCard(randomSuit, randomRank);
    }
}