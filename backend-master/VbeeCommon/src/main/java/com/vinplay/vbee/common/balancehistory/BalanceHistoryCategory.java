package com.vinplay.vbee.common.balancehistory;

public enum BalanceHistoryCategory {
    NAP("nap"),
    RUT("rut"),
    CASHBACK("cashback"),
    TRANSFER_IN("transfer_in"),
    TRANSFER_OUT("transfer_out"),
    GAME_BET("game_bet"),
    GAME_WIN("game_win"),
    GIFTCODE("giftcode"),
    ADMIN_ADJUST("admin_adjust"),
    OTHER("other");

    private final String wire;

    BalanceHistoryCategory(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static BalanceHistoryCategory fromWire(String s) {
        if (s == null) return null;
        String norm = s.trim().toLowerCase();
        for (BalanceHistoryCategory c : values()) {
            if (c.wire.equals(norm)) return c;
        }
        return null;
    }
}
