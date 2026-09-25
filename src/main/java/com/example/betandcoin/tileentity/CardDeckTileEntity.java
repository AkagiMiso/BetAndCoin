package com.example.betandcoin.tileentity;

import com.example.betandcoin.game.blackjack.CardRank;
import com.example.betandcoin.game.blackjack.CardSuit;
import com.example.betandcoin.game.blackjack.Deck;
import com.example.betandcoin.game.blackjack.PlayingCard;
import com.example.betandcoin.registry.ModTileEntities;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.block.BlockState;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CardDeckTileEntity extends TileEntity {

    private final List<PlayingCard> remainingCards =
            new ArrayList<>();

    public CardDeckTileEntity() {
        super(ModTileEntities.CARD_DECK.get());

        if (remainingCards.isEmpty()) {
            resetAndShuffle();
        }
    }

    public void resetAndShuffle() {
        Deck deck = new Deck();
        deck.shuffle(new Random());

        remainingCards.clear();
        remainingCards.addAll(deck.getRemainingCards());

        setChanged();
    }

    public PlayingCard drawCard() {
        if (remainingCards.isEmpty()) {
            resetAndShuffle();
        }

        PlayingCard card =
                remainingCards.remove(remainingCards.size() - 1);

        setChanged();
        return card;
    }

    public int getRemainingCardCount() {
        return remainingCards.size();
    }

    @Override
    public void load(
            @Nonnull BlockState state,
            @Nonnull CompoundNBT compound
    ) {
        super.load(state, compound);

        remainingCards.clear();

        ListNBT cardsTag =
                compound.getList("RemainingCards", 10);

        for (int i = 0; i < cardsTag.size(); i++) {
            CompoundNBT cardTag =
                    cardsTag.getCompound(i);

            int suitIndex =
                    cardTag.getInt("Suit");

            int rankIndex =
                    cardTag.getInt("Rank");

            CardSuit[] suits =
                    CardSuit.values();

            CardRank[] ranks =
                    CardRank.values();

            if (suitIndex >= 0
                    && suitIndex < suits.length
                    && rankIndex >= 0
                    && rankIndex < ranks.length) {

                remainingCards.add(
                        new PlayingCard(
                                suits[suitIndex],
                                ranks[rankIndex]
                        )
                );
            }
        }

        if (remainingCards.isEmpty()) {
            resetAndShuffle();
        }
    }

    @Nonnull
    @Override
    public CompoundNBT save(
            @Nonnull CompoundNBT compound
    ) {
        super.save(compound);

        ListNBT cardsTag =
                new ListNBT();

        for (PlayingCard card : remainingCards) {
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

            cardsTag.add(cardTag);
        }

        compound.put(
                "RemainingCards",
                cardsTag
        );

        return compound;
    }
}