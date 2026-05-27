/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.gsc.dao;

import game.third.usecase.gsc.entities.Transaction;
import java.util.List;

public interface GscTransactionDao {
    public boolean insertTransaction(Transaction var1);

    public boolean insertManyTransaction(List<Transaction> var1);

    public Transaction getTransaction(String var1);
}

