package com.example.betandcoin.game.blackjack;

import java.util.HashSet;
import java.util.Set;

public final class BlackjackTest {

    private BlackjackTest() {
    }

    public static void runTests() {
        testDeck();
        testHandValues();

        System.out.println("All blackjack tests passed.");
    }

    private static void testDeck() {
        Deck deck = new Deck();

        if (deck.size() != 52) {
            throw new IllegalStateException(
                    "Expected 52 cards, but found " + deck.size()
            );
        }

        Set<PlayingCard> drawnCards = new HashSet<>();

        while (!deck.isEmpty()) {
            PlayingCard card = deck.draw();

            if (!drawnCards.add(card)) {
                throw new IllegalStateException(
                        "Duplicate card found: " + card
                );
            }
        }

        if (drawnCards.size() != 52) {
            throw new IllegalStateException(
                    "Expected 52 unique cards, but found " + drawnCards.size()
            );
        }

        System.out.println("Deck test passed.");
    }

    private static void testHandValues() {
        BlackjackHand hand = new BlackjackHand();

        hand.addCard(new PlayingCard(CardSuit.SPADES, CardRank.ACE));
        hand.addCard(new PlayingCard(CardSuit.HEARTS, CardRank.KING));

        assertValue(hand, 21);

        if (!hand.isBlackjack()) {
            throw new IllegalStateException("A + K should be blackjack.");
        }

        hand.clear();

        hand.addCard(new PlayingCard(CardSuit.SPADES, CardRank.ACE));
        hand.addCard(new PlayingCard(CardSuit.HEARTS, CardRank.SIX));
        hand.addCard(new PlayingCard(CardSuit.CLUBS, CardRank.TEN));

        assertValue(hand, 17);

        if (hand.isBlackjack()) {
            throw new IllegalStateException(
                    "A + 6 + 10 is 17, not blackjack."
            );
        }

        hand.clear();

        hand.addCard(new PlayingCard(CardSuit.SPADES, CardRank.ACE));
        hand.addCard(new PlayingCard(CardSuit.HEARTS, CardRank.ACE));
        hand.addCard(new PlayingCard(CardSuit.DIAMONDS, CardRank.NINE));

        assertValue(hand, 21);

        hand.clear();

        hand.addCard(new PlayingCard(CardSuit.SPADES, CardRank.KING));
        hand.addCard(new PlayingCard(CardSuit.HEARTS, CardRank.QUEEN));
        hand.addCard(new PlayingCard(CardSuit.DIAMONDS, CardRank.TWO));

        assertValue(hand, 22);

        if (!hand.isBust()) {
            throw new IllegalStateException(
                    "K + Q + 2 should be bust."
            );
        }

        System.out.println("Hand value tests passed.");
    }

    private static void assertValue(BlackjackHand hand, int expectedValue) {
        int actualValue = hand.getValue();

        if (actualValue != expectedValue) {
            throw new IllegalStateException(
                    "Expected hand value "
                            + expectedValue
                            + ", but found "
                            + actualValue
                            + ". Hand: "
                            + hand
            );
        }
    }
}