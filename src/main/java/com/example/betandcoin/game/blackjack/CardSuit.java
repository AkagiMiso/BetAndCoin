package com.example.betandcoin.game.blackjack;

public enum CardSuit {
    SPADES("spades"),
    HEARTS("hearts"),
    DIAMONDS("diamonds"),
    CLUBS("clubs");

    private final String textureName;

    CardSuit(String textureName) {
        this.textureName = textureName;
    }

    public String getTextureName() {
        return textureName;
    }
}