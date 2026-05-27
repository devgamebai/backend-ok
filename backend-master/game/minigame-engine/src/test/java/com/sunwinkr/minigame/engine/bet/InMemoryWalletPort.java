package com.sunwinkr.minigame.engine.bet;

import com.sunwinkr.minigame.engine.port.MoneyResult;
import com.sunwinkr.minigame.engine.port.TransKind;
import com.sunwinkr.minigame.engine.port.WalletPort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test-only in-memory wallet adapter. Tracks every call so tests can
 * assert the expected sequence of {@link WalletPort#debit} /
 * {@link WalletPort#credit} invocations.
 */
class InMemoryWalletPort implements WalletPort {

    static final class Call {
        final String kind; // "debit" | "credit"
        final String user;
        final long amount;
        final String moneyType;
        final String source;
        final long gameId;
        final long txId;
        final TransKind transKind;

        Call(String kind, String user, long amount, String moneyType,
             String source, long gameId, long txId, TransKind transKind) {
            this.kind = kind;
            this.user = user;
            this.amount = amount;
            this.moneyType = moneyType;
            this.source = source;
            this.gameId = gameId;
            this.txId = txId;
            this.transKind = transKind;
        }

        @Override
        public String toString() {
            return kind + "(" + user + "," + amount + "," + moneyType
                + "," + source + ",txId=" + txId + "," + transKind + ")";
        }
    }

    private final Map<String, Long> balances = new HashMap<>();
    final List<Call> calls = new ArrayList<>();
    boolean failNextDebit;

    InMemoryWalletPort seed(String user, long balance) {
        balances.put(user, balance);
        return this;
    }

    @Override
    public MoneyResult debit(String user, long amount, String moneyType,
                             String source, long gameId, String desc,
                             long fee, long txId, TransKind transKind) {
        calls.add(new Call("debit", user, amount, moneyType, source, gameId, txId, transKind));
        if (failNextDebit) {
            failNextDebit = false;
            return MoneyResult.failure(balances.getOrDefault(user, 0L), "9999");
        }
        long bal = balances.getOrDefault(user, 0L);
        if (bal < amount) {
            return MoneyResult.failure(bal, "1003");
        }
        long after = bal - amount;
        balances.put(user, after);
        return MoneyResult.success(after);
    }

    @Override
    public MoneyResult credit(String user, long amount, String moneyType,
                              String source, long gameId, String desc,
                              long fee, long txId, TransKind transKind) {
        calls.add(new Call("credit", user, amount, moneyType, source, gameId, txId, transKind));
        long bal = balances.getOrDefault(user, 0L) + amount;
        balances.put(user, bal);
        return MoneyResult.success(bal);
    }

    @Override
    public long getBalance(String user, String moneyType) {
        return balances.getOrDefault(user, 0L);
    }

    int debitCount() {
        int n = 0;
        for (Call c : calls) {
            if ("debit".equals(c.kind)) {
                n++;
            }
        }
        return n;
    }

    int creditCount() {
        int n = 0;
        for (Call c : calls) {
            if ("credit".equals(c.kind)) {
                n++;
            }
        }
        return n;
    }
}
