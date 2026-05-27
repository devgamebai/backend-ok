/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.cardlib.models;

public enum Suit {
    Spades(0, "\u2660"),
    Clubs(1, "\u2663"),
    Diamonds(2, "\u2666"),
    Hearts(3, "\u2665");
    
    private final int suitValue;
    private final String name;

    public static Suit getSuit(int suitValue) {
        Suit result = null;
        switch (suitValue) {
            case 0: {
                result = Spades;
                break;
            }
            case 1: {
                result = Clubs;
                break;
            }
            case 2: {
                result = Diamonds;
                break;
            }
            case 3: {
                result = Hearts;
            }
        }
        return result;
    }

    private Suit(int suitValue, String name) {
        this.suitValue = suitValue;
        this.name = name;
    }

    public int getSuitValue() {
        return this.suitValue;
    }

    public String getName() {
        return this.name;
    }

    public static Suit getSuitFromId(int id) {
        for (Suit entry : Suit.values()) {
            if (entry.getSuitValue() != id) continue;
            return entry;
        }
        return null;
    }
}

