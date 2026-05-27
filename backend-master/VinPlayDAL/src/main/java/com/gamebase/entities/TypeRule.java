/*
 * Decompiled with CFR 0.152.
 */
package com.gamebase.entities;

public enum TypeRule {
    SumDeposit("SumDeposit"),
    SumBet("SumBet"),
    SumWin("SumWin"),
    BetGame("BetGame"),
    WinGame("WinGame"),
    BetGameMultiWithDeposit("BetGameMultiWithDeposit"),
    SumBetMultiWithDeposit("SumBetMultiWithDeposit");

    private final String name;

    private TypeRule(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static TypeRule fromString(String name) {
        for (TypeRule type : TypeRule.values()) {
            if (!type.name.equals(name)) continue;
            return type;
        }
        return null;
    }
}

