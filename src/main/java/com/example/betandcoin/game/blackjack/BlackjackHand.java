package com.example.betandcoin.game.blackjack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class BlackjackHand {

    private final List<PlayingCard> cards = new ArrayList<>();

    public void addCard(PlayingCard card) {
        cards.add(Objects.requireNonNull(card, "card cannot be null"));
    }

    public void clear() {
        cards.clear();
    }

    public int getValue() {
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

    public boolean isBlackjack() {
        return cards.size() == 2 && getValue() == 21;
    }

    public boolean isBust() {
        return getValue() > 21;
    }

    public int size() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public List<PlayingCard> getCards() {
        return Collections.unmodifiableList(cards);
    }

    @Override
    public String toString() {
        return cards + " = " + getValue();
    }
}