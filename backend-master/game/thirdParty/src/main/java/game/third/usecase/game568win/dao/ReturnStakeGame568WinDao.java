/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.game568win.dao;

import game.third.usecase.game568win.entities.ReturnStakeGame568Win;
import game.third.usecase.game568win.entities.Status;

public interface ReturnStakeGame568WinDao {
    public boolean createReturnStake(ReturnStakeGame568Win var1);

    public boolean updateReturnStake(String var1, String var2, Status var3);

    public ReturnStakeGame568Win getReturnStake(String var1, String var2);
}

