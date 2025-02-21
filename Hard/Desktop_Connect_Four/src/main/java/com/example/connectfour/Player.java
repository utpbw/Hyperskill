package com.example.connectfour;

public class Player {
    private final String symbol; // "X" or "O"

    public Player(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
