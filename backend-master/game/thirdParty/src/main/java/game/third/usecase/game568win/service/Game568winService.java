/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.game568win.service;

import game.third.usecase.game568win.entities.TransactionGame568Win;
import game.third.usecase.game568win.model.Bonus;
import game.third.usecase.game568win.model.ReturnStake;
import game.third.usecase.game568win.request.CancelData;
import game.third.usecase.game568win.request.RollbackData;
import game.third.usecase.game568win.request.SettleData;
import game.third.usecase.game568win.response.DeductResult;

public interface Game568winService {
    public DeductResult Deduct(TransactionGame568Win var1);

    public int Settle(SettleData var1);

    public int Rollback(RollbackData var1);

    public int Cancel(CancelData var1);

    public int Bonus(Bonus var1);

    public int ReturnStake(ReturnStake var1);

    public TransactionGame568Win GetBetStatus(String var1, String var2);
}

