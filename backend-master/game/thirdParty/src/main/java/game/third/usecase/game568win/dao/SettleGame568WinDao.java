/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.game568win.dao;

import game.third.usecase.game568win.entities.SettleGame568Win;
import game.third.usecase.game568win.entities.Status;

public interface SettleGame568WinDao {
    public boolean createSettle(SettleGame568Win var1);

    public boolean updateStatus(String var1, Status var2);

    public boolean updateWinLoss(String var1, Status var2, double var3);

    public SettleGame568Win getSettle(String var1);
}

