/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.gsc.dao.impl;

import game.third.usecase.gsc.dao.GscTransactionDao;
import game.third.usecase.gsc.entities.Transaction;
import java.util.List;

public class GscTransactionDaoImpl
implements GscTransactionDao {
    @Override
    public boolean insertTransaction(Transaction transaction) {
        return false;
    }

    @Override
    public boolean insertManyTransaction(List<Transaction> transactions) {
        return false;
    }

    @Override
    public Transaction getTransaction(String id) {
        return null;
    }
}

