/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.cardlib.models;

public enum GroupType {
    HighCard(0),
    OnePair(1),
    TwoPair(2),
    ThreeOfKind(3),
    Straight(4),
    Flush(5),
    FullHouse(6),
    FourOfKind(7),
    StraightFlush(8);
    
    private final int code;

    private GroupType(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

    public static GroupType findGroupType(int code) {
        for (GroupType entry : GroupType.values()) {
            if (entry.getCode() != code) continue;
            return entry;
        }
        return null;
    }
}

