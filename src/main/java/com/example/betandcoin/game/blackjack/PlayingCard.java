package com.example.betandcoin.game.blackjack;

import java.util.Objects;

public final class PlayingCard {

    private final CardSuit suit;
    private final CardRank rank;

    public PlayingCard(CardSuit suit, CardRank rank) {
        this.suit = Objects.requireNonNull(suit, "suit cannot be null");
        this.rank = Objects.requireNonNull(rank, "rank cannot be null");
    }

    public CardSuit getSuit() {
        return suit;
    }

    public CardRank getRank() {
        return rank;
    }

    public String getTextureFileName() {
        return "card_"
                + suit.getTextureName()
                + "_"
                + rank.getTextureName()
                + ".png";
    }

    public int getBlackjackValue() {
        return rank.getBlackjackValue();
    }

    @Override
    public String toString() {
        return rank.name() + " OF " + suit.name();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof PlayingCard)) {
            return false;
        }

        PlayingCard other = (PlayingCard) object;
        return suit == other.suit && rank == other.rank;
    }

    @Override
    public int hashCode() {
        return Objects.hash(suit, rank);
    }
}