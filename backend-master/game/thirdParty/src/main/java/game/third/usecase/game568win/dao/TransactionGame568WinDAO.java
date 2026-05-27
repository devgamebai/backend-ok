/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.game568win.dao;

import game.third.usecase.game568win.entities.Status;
import game.third.usecase.game568win.entities.TransactionGame568Win;
import java.util.List;

public interface TransactionGame568WinDAO {
    public boolean createTransaction(TransactionGame568Win var1);

    public boolean updateSettleTransaction(String var1, String var2, Status var3, double var4, int var6, String var7);

    public boolean updateRollbackTransaction(String var1);

    public boolean updateStatusTransaction(String var1, String var2, Status var3);

    public boolean updateAmountTransaction(String var1, String var2, double var3);

    public boolean updateAmountTransaction(String var1, String var2, double var3, Status var5);

    public boolean updateStatusTransaction(String var1, String var2, Status var3, double var4);

    public boolean updateStatusTransaction(String var1, Status var2);

    public TransactionGame568Win getFirstTransactionById(String var1);

    public List<TransactionGame568Win> getTransactionById(String var1);

    public TransactionGame568Win getTransactionByTransferCodeAndTransactionId(String var1, String var2);
}

