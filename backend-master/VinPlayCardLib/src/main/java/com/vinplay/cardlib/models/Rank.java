/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.cardlib.models;

public enum Rank {
    Two(0, "2"),
    Three(1, "3"),
    Four(2, "4"),
    Five(3, "5"),
    Six(4, "6"),
    Seven(5, "7"),
    Eight(6, "8"),
    Nine(7, "9"),
    Ten(8, "10"),
    Jack(9, "J"),
    Queen(10, "Q"),
    King(11, "K"),
    Ace(12, "A");
    
    private final int rankValue;
    private final String name;

    private Rank(int rankValue, String name) {
        this.rankValue = rankValue;
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static Rank getRank(int rankValue) {
        Rank result = null;
        switch (rankValue) {
            case 0: {
                result = Two;
                break;
            }
            case 1: {
                result = Three;
                break;
            }
            case 2: {
                result = Four;
                break;
            }
            case 3: {
                result = Five;
                break;
            }
            case 4: {
                result = Six;
                break;
            }
            case 5: {
                result = Seven;
                break;
            }
            case 6: {
                result = Eight;
                break;
            }
            case 7: {
                result = Nine;
                break;
            }
            case 8: {
                result = Ten;
                break;
            }
            case 9: {
                result = Jack;
                break;
            }
            case 10: {
                result = Queen;
                break;
            }
            case 11: {
                result = King;
                break;
            }
            case 12: {
                result = Ace;
            }
        }
        return result;
    }

    public int getRank() {
        return this.rankValue;
    }

    public static Rank getRankFromId(int id) {
        for (Rank entry : Rank.values()) {
            if (entry.getRank() != id) continue;
            return entry;
        }
        return null;
    }
}

