/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.game568win.dao;

import game.third.usecase.game568win.entities.BonusGame568Win;
import game.third.usecase.game568win.entities.Status;

public interface BonusGame568WinDao {
    public boolean createBonus(BonusGame568Win var1);

    public boolean updateBonus(String var1, String var2, Status var3);

    public BonusGame568Win getBonus(String var1, String var2);
}

