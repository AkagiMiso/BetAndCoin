package com.example.betandcoin.game.blackjack;

public enum CardRank {
    ACE("ace", 11),
    TWO("02", 2),
    THREE("03", 3),
    FOUR("04", 4),
    FIVE("05", 5),
    SIX("06", 6),
    SEVEN("07", 7),
    EIGHT("08", 8),
    NINE("09", 9),
    TEN("10", 10),
    JACK("jack", 10),
    QUEEN("queen", 10),
    KING("king", 10);

    private final String textureName;
    private final int blackjackValue;

    CardRank(String textureName, int blackjackValue) {
        this.textureName = textureName;
        this.blackjackValue = blackjackValue;
    }

    public String getTextureName() {
        return textureName;
    }

    public int getBlackjackValue() {
        return blackjackValue;
    }
}